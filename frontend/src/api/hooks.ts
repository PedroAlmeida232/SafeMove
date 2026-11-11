import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { chaveCoord, type Coordenada } from '../utils/geo';
import { request } from './client';
import type {
  AreaRisco,
  AvaliacaoArea,
  AvaliarRotaBody,
  Incidente,
  NovaOcorrencia,
  Ocorrencia,
  Radar,
  RotaAvaliada,
  Usuario,
} from './types';

const UM_MINUTO = 60_000;

export const RAIO_OCORRENCIAS = 2000;
export const RAIO_INCIDENTES = 5000;
export const RAIO_RADARES = 2000;

export const queryKeys = {
  ocorrencias: (lat: number, lng: number) => ['ocorrencias', lat, lng] as const,
  incidentes: (lat: number, lng: number) => ['incidentes', lat, lng] as const,
  radares: (lat: number, lng: number) => ['radares', lat, lng] as const,
  areasRisco: ['areas-risco'] as const,
  usuarioMe: (uid: string | undefined) => ['usuarios', 'me', uid] as const,
};

export function buscarOcorrencias(c: Coordenada, raio = RAIO_OCORRENCIAS) {
  return request<Ocorrencia[]>('/ocorrencias', {
    query: { lat: c.latitude, lng: c.longitude, raio, horas: 24 },
  });
}

export function buscarIncidentes(c: Coordenada, raio = RAIO_INCIDENTES) {
  return request<Incidente[]>('/incidentes', { query: { lat: c.latitude, lng: c.longitude, raio } });
}

/** GET /areas-risco/avaliar — diz se a região do ponto é segura e traz o texto do alerta. */
export function avaliarArea(c: Coordenada) {
  return request<AvaliacaoArea>('/areas-risco/avaliar', { query: { lat: c.latitude, lng: c.longitude } });
}

export function avaliarRota(body: AvaliarRotaBody) {
  return request<RotaAvaliada[]>('/rotas/avaliar', { method: 'POST', body });
}

/** GET /ocorrencias — recarrega a cada 60 s. */
export function useOcorrencias(centro: Coordenada | null, enabled = true) {
  const [lat, lng] = centro ? chaveCoord(centro) : [0, 0];
  return useQuery({
    queryKey: queryKeys.ocorrencias(lat, lng),
    queryFn: () => buscarOcorrencias({ latitude: lat, longitude: lng }),
    enabled: enabled && centro !== null,
    refetchInterval: UM_MINUTO,
    placeholderData: (anterior) => anterior,
  });
}

/** GET /incidentes — recarrega a cada 60 s. */
export function useIncidentes(centro: Coordenada | null, enabled = true) {
  const [lat, lng] = centro ? chaveCoord(centro) : [0, 0];
  return useQuery({
    queryKey: queryKeys.incidentes(lat, lng),
    queryFn: () => buscarIncidentes({ latitude: lat, longitude: lng }),
    enabled: enabled && centro !== null,
    refetchInterval: UM_MINUTO,
    placeholderData: (anterior) => anterior,
  });
}

export function useRadares(centro: Coordenada | null, enabled = true) {
  const [lat, lng] = centro ? chaveCoord(centro) : [0, 0];
  return useQuery({
    queryKey: queryKeys.radares(lat, lng),
    queryFn: () =>
      request<Radar[]>('/radares', { query: { lat, lng, raio: RAIO_RADARES } }),
    enabled: enabled && centro !== null,
    staleTime: 10 * UM_MINUTO,
    placeholderData: (anterior) => anterior,
  });
}

export function useAreasRisco(enabled = true) {
  return useQuery({
    queryKey: queryKeys.areasRisco,
    queryFn: () => request<AreaRisco[]>('/areas-risco'),
    enabled,
    staleTime: 60 * UM_MINUTO,
  });
}

/** POST /rotas/avaliar */
export function useAvaliarRota() {
  return useMutation({ mutationFn: avaliarRota });
}

/** 🔒 POST /ocorrencias */
export function useCriarOcorrencia() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: NovaOcorrencia) =>
      request<Ocorrencia>('/ocorrencias', { method: 'POST', body, auth: true }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ocorrencias'] }),
  });
}

/** 🔒 POST /ocorrencias/{id}/confirmar */
export function useConfirmarOcorrencia() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      request<Ocorrencia>(`/ocorrencias/${id}/confirmar`, { method: 'POST', auth: true }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ocorrencias'] }),
  });
}

/** 🔒 GET /usuarios/me */
export function buscarUsuarioMe() {
  return request<Usuario>('/usuarios/me', { auth: true });
}

export function useUsuarioMe(uid: string | undefined) {
  return useQuery({
    queryKey: queryKeys.usuarioMe(uid),
    queryFn: buscarUsuarioMe,
    enabled: uid !== undefined,
    staleTime: 30 * UM_MINUTO,
  });
}
