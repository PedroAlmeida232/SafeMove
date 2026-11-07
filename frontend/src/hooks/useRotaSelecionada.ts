import { useMemo } from 'react';

import { useAreasRisco, useOcorrencias } from '../api/hooks';
import type { RotaAvaliada } from '../api/types';
import { useRotas } from '../state/RotaStore';
import { distanciaAteRota, pontoMedio, type Coordenada } from '../utils/geo';
import { corAreaRisco, raioAreaRisco } from '../utils/incidente';

export function useRotaPorIndice(indice: string | undefined): RotaAvaliada | undefined {
  const { planejamento } = useRotas();
  return useMemo(
    () => planejamento?.rotas.find((r) => String(r.indice) === indice),
    [planejamento, indice],
  );
}

export type PontoDeRisco = { id: string; centro: Coordenada; raio: number; cor: string };

const RAIO_OCORRENCIA_M = 160;
const PROXIMIDADE_OCORRENCIA_M = 150;
const PROXIMIDADE_AREA_M = 900;

/**
 * Pontos de risco ao longo da rota, a partir dos dados da API:
 * ocorrências recentes a até 150 m do trajeto e áreas de risco (nível ≥ 3) que ele cruza.
 */
export function usePontosDeRisco(rota: RotaAvaliada | undefined): PontoDeRisco[] {
  const meio = rota ? pontoMedio(rota.pontos) : null;
  const ocorrencias = useOcorrencias(meio, rota !== undefined);
  const areas = useAreasRisco(rota !== undefined);

  return useMemo(() => {
    if (!rota) return [];
    const pontos: PontoDeRisco[] = [];
    for (const a of areas.data ?? []) {
      const cor = corAreaRisco(a);
      if (cor && distanciaAteRota(a, rota.pontos) <= PROXIMIDADE_AREA_M) {
        pontos.push({ id: `a-${a.distrito}`, centro: a, raio: raioAreaRisco(a) * 0.6, cor });
      }
    }
    for (const o of ocorrencias.data ?? []) {
      if (distanciaAteRota(o, rota.pontos) <= PROXIMIDADE_OCORRENCIA_M) {
        pontos.push({ id: `o-${o.id}`, centro: o, raio: RAIO_OCORRENCIA_M, cor: '#FF3B30' });
      }
    }
    return pontos;
  }, [rota, ocorrencias.data, areas.data]);
}
