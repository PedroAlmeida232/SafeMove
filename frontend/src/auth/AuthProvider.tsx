import AsyncStorage from '@react-native-async-storage/async-storage';
import { useQueryClient } from '@tanstack/react-query';
import * as SecureStore from 'expo-secure-store';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { Platform } from 'react-native';

import { ApiError, request, setRefreshHandler, setTokenProvider } from '../api/client';
import { buscarUsuarioMe } from '../api/hooks';
import type { AuthResponse, Usuario } from '../api/types';

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

const CHAVE_SESSAO = 'safemove:sessao';

type Sessao = { token: string; refreshToken: string; usuario: UsuarioAuth };

// A autenticação é feita pelo backend (que usa o Firebase); aqui só guardamos os tokens que ele devolve.
// SecureStore (Keychain/Keystore) no celular; AsyncStorage na web, onde ele não existe.
const armazenamento = {
  ler: () => (Platform.OS === 'web' ? AsyncStorage.getItem(CHAVE_SESSAO) : SecureStore.getItemAsync(CHAVE_SESSAO)),
  gravar: (v: string) =>
    Platform.OS === 'web' ? AsyncStorage.setItem(CHAVE_SESSAO, v) : SecureStore.setItemAsync(CHAVE_SESSAO, v),
  apagar: () =>
    Platform.OS === 'web' ? AsyncStorage.removeItem(CHAVE_SESSAO) : SecureStore.deleteItemAsync(CHAVE_SESSAO),
};

function paraUsuarioAuth(u: Usuario): UsuarioAuth {
  return { uid: u.uid, email: u.email, nome: u.nome };
}

// Lidos pelo client HTTP a cada chamada autenticada.
let tokenAtual: string | null = null;
let refreshAtual: string | null = null;
let usuarioAtual: UsuarioAuth | null = null;
setTokenProvider(async () => tokenAtual);

async function guardarSessao(token: string, refreshToken: string, usuario: UsuarioAuth) {
  tokenAtual = token;
  refreshAtual = refreshToken;
  usuarioAtual = usuario;
  const sessao: Sessao = { token, refreshToken, usuario };
  await armazenamento.gravar(JSON.stringify(sessao)).catch(() => undefined);
}

// Várias chamadas podem receber 401 ao mesmo tempo: todas esperam a mesma renovação
// (o refresh token do Firebase rotaciona, então não podemos renovar duas vezes em paralelo).
let renovacao: Promise<boolean> | null = null;
setRefreshHandler(() => {
  if (!refreshAtual || !usuarioAtual) return Promise.resolve(false);
  renovacao ??= request<AuthResponse>('/auth/refresh', { method: 'POST', body: { refreshToken: refreshAtual } })
    .then(async (r) => {
      await guardarSessao(r.token, r.refreshToken, paraUsuarioAuth(r.usuario));
      return true;
    })
    .catch(() => false)
    .finally(() => {
      renovacao = null;
    });
  return renovacao;
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [usuario, setUsuario] = useState<UsuarioAuth | null>(null);
  const [carregando, setCarregando] = useState(true);

  const limparSessao = useCallback(async () => {
    tokenAtual = null;
    refreshAtual = null;
    usuarioAtual = null;
    setUsuario(null);
    queryClient.removeQueries({ queryKey: ['usuarios'] });
    await armazenamento.apagar().catch(() => undefined);
  }, [queryClient]);

  // Restaura a sessão salva e confirma com o backend que o token ainda vale.
  useEffect(() => {
    let ativo = true;
    (async () => {
      try {
        const json = await armazenamento.ler();
        if (!json) return;
        const sessao = JSON.parse(json) as Sessao;
        tokenAtual = sessao.token;
        refreshAtual = sessao.refreshToken;
        usuarioAtual = sessao.usuario;
        if (ativo) setUsuario(sessao.usuario);
        try {
          const me = await buscarUsuarioMe();
          if (ativo) setUsuario(paraUsuarioAuth(me));
        } catch (e) {
          // Token expirado/inválido: encerra a sessão. Falha de rede mantém a sessão salva.
          if (e instanceof ApiError && e.status === 401 && ativo) await limparSessao();
        }
      } catch {
        tokenAtual = null;
      } finally {
        if (ativo) setCarregando(false);
      }
    })();
    return () => {
      ativo = false;
    };
  }, [limparSessao]);

  const iniciarSessao = useCallback(async ({ token, refreshToken, usuario: u }: AuthResponse) => {
    const dados = paraUsuarioAuth(u);
    await guardarSessao(token, refreshToken, dados);
    setUsuario(dados);
  }, []);

  const entrar = useCallback(
    async (email: string, senha: string) => {
      const resp = await request<AuthResponse>('/auth/login', { method: 'POST', body: { email: email.trim(), senha } });
      await iniciarSessao(resp);
    },
    [iniciarSessao],
  );

  const criarConta = useCallback(
    async (nome: string, email: string, senha: string) => {
      const resp = await request<AuthResponse>('/auth/register', {
        method: 'POST',
        body: { nome: nome.trim(), email: email.trim(), senha },
      });
      await iniciarSessao(resp);
    },
    [iniciarSessao],
  );

  const sair = useCallback(() => limparSessao(), [limparSessao]);

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
