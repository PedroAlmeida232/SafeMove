import { mockRequest } from '../mocks/handler';
import { ApiError, type Metodo } from './errors';
import type { ProblemDetail } from './types';

export { ApiError, type Metodo };

export const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080').replace(/\/+$/, '');
export const USE_MOCK = process.env.EXPO_PUBLIC_USE_MOCK === 'true';

const TIMEOUT_MS = 20000;

type TokenProvider = () => Promise<string | null>;
let tokenProvider: TokenProvider = async () => null;

/** O AuthProvider registra aqui como obter o token (JWT do backend) da sessão atual. */
export function setTokenProvider(fn: TokenProvider): void {
  tokenProvider = fn;
}

type RefreshHandler = () => Promise<boolean>;
let refreshHandler: RefreshHandler | null = null;

/** O ID token dura 1h: ao receber 401 numa rota 🔒, o client pede ao AuthProvider para renovar e repete a chamada. */
export function setRefreshHandler(fn: RefreshHandler | null): void {
  refreshHandler = fn;
}

type Opcoes = {
  method?: Metodo;
  body?: unknown;
  /** Rotas 🔒: envia Authorization: Bearer <token>. */
  auth?: boolean;
  query?: Record<string, string | number | undefined>;
};

function montarQuery(query: Opcoes['query']): string {
  if (!query) return '';
  const partes = Object.entries(query)
    .filter((e): e is [string, string | number] => e[1] !== undefined)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  return partes.length ? `?${partes.join('&')}` : '';
}

function isProblemDetail(v: unknown): v is ProblemDetail {
  return typeof v === 'object' && v !== null && 'detail' in v && typeof (v as { detail: unknown }).detail === 'string';
}

function mensagemPadrao(status: number): string {
  if (status === 401) return 'Sua sessão expirou. Entre novamente.';
  if (status === 403) return 'Você não tem permissão para fazer isso.';
  if (status === 404) return 'Não encontramos o que você procurava.';
  if (status === 503) return 'O serviço está indisponível no momento.';
  if (status >= 500) return 'O servidor teve um problema. Tente de novo em instantes.';
  return 'Não foi possível concluir a ação.';
}

export async function request<T>(path: string, opcoes: Opcoes = {}, jaRenovou = false): Promise<T> {
  const { method = 'GET', body, auth = false, query } = opcoes;
  const caminho = `${path}${montarQuery(query)}`;

  let token: string | null = null;
  if (auth) {
    token = await tokenProvider();
    if (!token) throw new ApiError(401, 'Entre na sua conta para continuar.');
  }

  if (USE_MOCK) {
    return mockRequest<T>(method, caminho, body);
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  let resposta: Response;
  try {
    resposta = await fetch(`${API_URL}${caminho}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch {
    throw new ApiError(0, 'Sem conexão com o servidor. Verifique sua internet.');
  } finally {
    clearTimeout(timer);
  }

  const texto = await resposta.text();
  let json: unknown = null;
  if (texto) {
    try {
      json = JSON.parse(texto);
    } catch {
      json = null;
    }
  }

  if (resposta.status === 401 && auth && !jaRenovou && refreshHandler && (await refreshHandler())) {
    return request<T>(path, opcoes, true);
  }

  if (!resposta.ok) {
    if (isProblemDetail(json)) throw new ApiError(resposta.status, json.detail, json.campos);
    throw new ApiError(resposta.status, mensagemPadrao(resposta.status));
  }

  return json as T;
}

export function mensagemDeErro(e: unknown): string {
  if (e instanceof Error && e.message) return e.message;
  return 'Algo deu errado. Tente de novo.';
}
