import AsyncStorage from '@react-native-async-storage/async-storage';
import { useCallback, useEffect, useRef } from 'react';

import { avaliarArea } from '../api/hooks';
import { useAlertas } from '../state/AlertasStore';
import { useLocation } from '../state/LocationProvider';
import { distanciaMetros, type Coordenada } from '../utils/geo';

/** Distritos já avisados: { distrito: timestamp do aviso }. Sobrevive ao fechar o app. */
const CHAVE = 'safemove:areas-avisadas';
/** Reavalia ao andar esta distância desde a última verificação… */
const DESLOCAMENTO_M = 250;
/** …ou, parado, depois deste tempo. */
const INTERVALO_MS = 10 * 60_000;
/** Não repete o alerta do mesmo distrito antes deste tempo. */
const ESPERA_REAVISO_MS = 3 * 60 * 60_000;

type Avisadas = Record<string, number>;

async function lerAvisadas(): Promise<Avisadas> {
  try {
    const json = await AsyncStorage.getItem(CHAVE);
    const todas = json ? (JSON.parse(json) as Avisadas) : {};
    const limite = Date.now() - ESPERA_REAVISO_MS;
    return Object.fromEntries(Object.entries(todas).filter(([, quando]) => quando > limite));
  } catch {
    return {};
  }
}

/**
 * Acompanha a posição real do usuário (não a Praça da Sé de reserva) e pergunta à API se a região é
 * segura (GET /areas-risco/avaliar), com base nos assaltos reportados no último mês e nas estatísticas
 * da SSP. Se não for, adiciona um alerta na aba Alertas e dispara uma notificação local —
 * no máximo uma vez a cada 3 h por distrito.
 */
export function useAlertaAreaRisco(): void {
  const { posicao } = useLocation();
  const { notificar } = useAlertas();

  const posicaoAtual = useRef<Coordenada | null>(null);
  const ultimaVerificada = useRef<Coordenada | null>(null);
  const ultimaVez = useRef(0);
  const avisadas = useRef<Avisadas | null>(null);
  const verificando = useRef(false);

  const verificar = useCallback(
    async (ponto: Coordenada) => {
      if (verificando.current) return;
      verificando.current = true;
      ultimaVerificada.current = ponto;
      ultimaVez.current = Date.now();
      try {
        const avaliacao = await avaliarArea(ponto);
        const { area } = avaliacao;
        if (area.segura) return;

        avisadas.current ??= await lerAvisadas();
        const anterior = avisadas.current[area.distrito];
        if (anterior !== undefined && Date.now() - anterior < ESPERA_REAVISO_MS) return;
        avisadas.current[area.distrito] = Date.now();
        AsyncStorage.setItem(CHAVE, JSON.stringify(avisadas.current)).catch(() => undefined);

        await notificar({
          titulo: avaliacao.titulo,
          texto: avaliacao.mensagem,
          nivel: area.nivel >= 4 ? 'risco' : 'atencao',
        });
      } catch {
        // 404 (fora de São Paulo) ou sem rede: tentamos de novo ao andar ou no próximo ciclo.
      } finally {
        verificando.current = false;
      }
    },
    [notificar],
  );

  // Ao se deslocar o suficiente (ou na primeira posição obtida).
  useEffect(() => {
    posicaoAtual.current = posicao;
    if (!posicao) return;
    const ultima = ultimaVerificada.current;
    if (!ultima || distanciaMetros(ultima, posicao) >= DESLOCAMENTO_M) verificar(posicao);
  }, [posicao, verificar]);

  // Parado no mesmo lugar: reavalia de tempos em tempos (a região pode piorar).
  useEffect(() => {
    const timer = setInterval(() => {
      const p = posicaoAtual.current;
      if (p && Date.now() - ultimaVez.current >= INTERVALO_MS) verificar(p);
    }, 60_000);
    return () => clearInterval(timer);
  }, [verificar]);
}
