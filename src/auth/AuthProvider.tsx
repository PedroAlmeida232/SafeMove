import AsyncStorage from '@react-native-async-storage/async-storage';
import { useQueryClient } from '@tanstack/react-query';
import { FirebaseError } from 'firebase/app';
import {
  createUserWithEmailAndPassword,
  onIdTokenChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  updateProfile,
} from 'firebase/auth';
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';

import { setTokenProvider, USE_MOCK } from '../api/client';
import { buscarUsuarioMe, queryKeys } from '../api/hooks';
import { auth, firebaseConfigurado } from './firebase';

export type UsuarioAuth = {
  uid: string;
  email: string | null;
  nome: string | null;
};

type AuthContextValue = {
  usuario: UsuarioAuth | null;
  carregando: boolean;
  entrar: (email: string, senha: string) => Promise<void>;
  criarConta: (nome: string, email: string, senha: string) => Promise<void>;
  sair: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

/** Sem Firebase configurado e com USE_MOCK=true, usamos uma sessão de demonstração. */
const MODO_DEMO = !firebaseConfigurado && USE_MOCK;
const CHAVE_DEMO = 'safemove:demo-user';

function traduzirErroFirebase(e: unknown): Error {
  if (e instanceof FirebaseError) {
    const mapa: Record<string, string> = {
      'auth/invalid-email': 'E-mail inválido.',
      'auth/missing-password': 'Digite sua senha.',
      'auth/invalid-credential': 'E-mail ou senha incorretos.',
      'auth/wrong-password': 'E-mail ou senha incorretos.',
      'auth/user-not-found': 'Não encontramos uma conta com esse e-mail.',
      'auth/email-already-in-use': 'Já existe uma conta com esse e-mail.',
      'auth/weak-password': 'A senha precisa ter pelo menos 6 caracteres.',
      'auth/too-many-requests': 'Muitas tentativas. Aguarde um pouco e tente de novo.',
      'auth/network-request-failed': 'Sem conexão. Verifique sua internet.',
    };
    return new Error(mapa[e.code] ?? 'Não foi possível entrar agora. Tente de novo.');
  }
  return e instanceof Error ? e : new Error('Não foi possível entrar agora.');
}

function erroSemFirebase(): Error {
  return new Error(
    'Login indisponível: preencha as variáveis EXPO_PUBLIC_FIREBASE_* no arquivo .env.',
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [usuario, setUsuario] = useState<UsuarioAuth | null>(null);
  const [carregando, setCarregando] = useState(true);
  // Lido pelo token provider do modo demo; atualizado já no render para que
  // queries disparadas pelos filhos no mesmo ciclo enxerguem o usuário.
  const usuarioRef = useRef<UsuarioAuth | null>(null);
  usuarioRef.current = usuario;

  useEffect(() => {
    if (auth) {
      setTokenProvider(async () => (auth?.currentUser ? auth.currentUser.getIdToken() : null));
      return onIdTokenChanged(auth, (u) => {
        setUsuario(u ? { uid: u.uid, email: u.email, nome: u.displayName } : null);
        setCarregando(false);
      });
    }
    if (MODO_DEMO) {
      // No modo demonstração o "token" é fixo, só para liberar as rotas 🔒 do mock.
      setTokenProvider(async () => (usuarioRef.current ? 'demo-token' : null));
      AsyncStorage.getItem(CHAVE_DEMO)
        .then((json) => setUsuario(json ? (JSON.parse(json) as UsuarioAuth) : null))
        .catch(() => setUsuario(null))
        .finally(() => setCarregando(false));
      return;
    }
    setCarregando(false);
  }, []);

  /** Chamado logo após o login, como pede o contrato. */
  const carregarPerfil = useCallback(
    async (uid: string) => {
      try {
        await queryClient.fetchQuery({ queryKey: queryKeys.usuarioMe(uid), queryFn: buscarUsuarioMe });
      } catch {
        // O perfil é recarregado na aba Perfil; não bloqueia o login.
      }
    },
    [queryClient],
  );

  const entrar = useCallback(
    async (email: string, senha: string) => {
      if (auth) {
        try {
          const cred = await signInWithEmailAndPassword(auth, email.trim(), senha);
          await carregarPerfil(cred.user.uid);
        } catch (e) {
          throw traduzirErroFirebase(e);
        }
        return;
      }
      if (!MODO_DEMO) throw erroSemFirebase();
      if (!email.includes('@') || senha.length < 6) throw new Error('E-mail ou senha incorretos.');
      const demo: UsuarioAuth = { uid: 'demo', email: email.trim(), nome: null };
      await AsyncStorage.setItem(CHAVE_DEMO, JSON.stringify(demo));
      usuarioRef.current = demo;
      setUsuario(demo);
    },
    [carregarPerfil],
  );

  const criarConta = useCallback(
    async (nome: string, email: string, senha: string) => {
      if (auth) {
        try {
          const cred = await createUserWithEmailAndPassword(auth, email.trim(), senha);
          if (nome.trim()) await updateProfile(cred.user, { displayName: nome.trim() });
          // Força um token novo com o nome atualizado antes de chamar a API.
          await cred.user.getIdToken(true);
          setUsuario({ uid: cred.user.uid, email: cred.user.email, nome: nome.trim() || null });
          await carregarPerfil(cred.user.uid);
        } catch (e) {
          throw traduzirErroFirebase(e);
        }
        return;
      }
      if (!MODO_DEMO) throw erroSemFirebase();
      const demo: UsuarioAuth = { uid: 'demo', email: email.trim(), nome: nome.trim() || null };
      await AsyncStorage.setItem(CHAVE_DEMO, JSON.stringify(demo));
      usuarioRef.current = demo;
      setUsuario(demo);
    },
    [carregarPerfil],
  );

  const sair = useCallback(async () => {
    if (auth) await firebaseSignOut(auth);
    else await AsyncStorage.removeItem(CHAVE_DEMO);
    setUsuario(null);
    queryClient.removeQueries({ queryKey: ['usuarios'] });
  }, [queryClient]);

  const value = useMemo(
    () => ({ usuario, carregando, entrar, criarConta, sair }),
    [usuario, carregando, entrar, criarConta, sair],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth precisa estar dentro de <AuthProvider>.');
  return ctx;
}
