import { useUsuarioMe } from '../api/hooks';
import { useAuth } from '../auth/AuthProvider';
import { iniciais, primeiroNome } from '../utils/format';

/** Nome, e-mail e iniciais do usuário logado (de /usuarios/me, com fallback no Firebase). */
export function useIdentidade() {
  const { usuario } = useAuth();
  const me = useUsuarioMe(usuario?.uid);
  const nome = me.data?.nome || usuario?.nome || null;
  const email = me.data?.email || usuario?.email || null;
  return {
    logado: usuario !== null,
    nome,
    email,
    iniciais: iniciais(nome ?? email),
    primeiroNome: primeiroNome(nome ?? email),
    perfil: me,
  };
}
