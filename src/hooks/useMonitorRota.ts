import { useEffect, useRef } from 'react';

import { avaliarRota, buscarIncidentes } from '../api/hooks';
import type { Classificacao, RotaAvaliada } from '../api/types';
import { useAlertas } from '../state/AlertasStore';
import { useRotas } from '../state/RotaStore';
import { rotuloCategoria } from '../utils/incidente';
import { distanciaAteRota, pontoMedio } from '../utils/geo';

const INTERVALO_MS = 60_000;
/** Distância máxima (m) para considerar um incidente "perto da rota". */
const DISTANCIA_ROTA_M = 300;
const GRAVIDADE_GRAVE = 3;

const PESO: Record<Classificacao, number> = { SEGURA: 0, ATENCAO: 1, EVITE: 2 };

/** Encontra, numa nova avaliação, a rota que corresponde à rota ativa. */
function rotaCorrespondente(nova: RotaAvaliada[], atual: RotaAvaliada): RotaAvaliada | undefined {
  return (
    nova.find((r) => r.indice === atual.indice) ??
    nova.find((r) => Math.abs(r.distanciaMetros - atual.distanciaMetros) / atual.distanciaMetros < 0.05)
  );
}

/**
 * Enquanto houver um trajeto ativo, verifica a cada 60 s:
 * (a) incidentes graves perto da rota; (b) mudança da rota para ATENCAO ou EVITE.
 * Em ambos os casos dispara uma notificação local.
 */
export function useMonitorRota(): void {
  const { ativo, atualizarRotaAtiva } = useRotas();
  const { notificar } = useAlertas();
  const avisados = useRef(new Set<string>());
  const ativoRef = useRef(ativo);
  ativoRef.current = ativo;

  const iniciadoEm = ativo?.iniciadoEm;

  useEffect(() => {
    if (iniciadoEm === undefined) return;
    avisados.current = new Set();
    let cancelado = false;

    async function verificar() {
      const trajeto = ativoRef.current;
      if (!trajeto || cancelado) return;

      try {
        const incidentes = await buscarIncidentes(pontoMedio(trajeto.rota.pontos));
        for (const inc of incidentes) {
          if (inc.gravidade < GRAVIDADE_GRAVE || avisados.current.has(inc.id)) continue;
          if (distanciaAteRota(inc, trajeto.rota.pontos) > DISTANCIA_ROTA_M) continue;
          avisados.current.add(inc.id);
          await notificar({
            titulo: `${rotuloCategoria(inc.categoria)} perto da sua rota`,
            texto: [inc.via, inc.descricao].filter(Boolean).join(' · ') || 'Incidente grave no trajeto.',
            nivel: 'risco',
          });
        }
      } catch {
        // Falha de rede: tentamos de novo no próximo ciclo.
      }

      try {
        const novas = await avaliarRota({
          origem: trajeto.origem,
          destino: trajeto.destino,
          modo: trajeto.modo,
        });
        const atual = ativoRef.current;
        if (!atual || cancelado) return;
        const nova = rotaCorrespondente(novas, atual.rota);
        if (!nova) return;
        const piorou = PESO[nova.classificacao] > PESO[atual.rota.classificacao];
        atualizarRotaAtiva({ ...nova, pontos: atual.rota.pontos });
        if (piorou && nova.classificacao !== 'SEGURA') {
          await notificar({
            titulo: nova.classificacao === 'EVITE' ? 'Risco na sua rota' : 'Atenção na sua rota',
            texto: nova.alertas[0] ?? `A nota da rota caiu para ${nova.nota}/100.`,
            nivel: nova.classificacao === 'EVITE' ? 'risco' : 'atencao',
          });
        }
      } catch {
        // 503 ou sem rede: mantemos a última avaliação.
      }
    }

    const timer = setInterval(verificar, INTERVALO_MS);
    verificar();
    return () => {
      cancelado = true;
      clearInterval(timer);
    };
  }, [iniciadoEm, notificar, atualizarRotaAtiva]);
}
