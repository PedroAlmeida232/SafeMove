import AsyncStorage from '@react-native-async-storage/async-storage';
import { useSyncExternalStore } from 'react';

/**
 * Ocorrências que o usuário escolheu ocultar (ou denunciou) neste aparelho.
 * Some do mapa na hora, mesmo antes de a equipe avaliar a denúncia.
 */
const CHAVE = 'safemove:ocorrencias-ocultas';

let ocultas: ReadonlySet<number> = new Set();
const ouvintes = new Set<() => void>();

function emitir(novo: ReadonlySet<number>) {
  ocultas = novo;
  ouvintes.forEach((f) => f());
}

AsyncStorage.getItem(CHAVE)
  .then((json) => {
    if (!json) return;
    const ids = JSON.parse(json) as number[];
    emitir(new Set([...ocultas, ...ids]));
  })
  .catch(() => undefined);

export function ocultarOcorrencia(id: number): void {
  if (ocultas.has(id)) return;
  const novo = new Set(ocultas).add(id);
  emitir(novo);
  AsyncStorage.setItem(CHAVE, JSON.stringify([...novo])).catch(() => undefined);
}

function assinar(f: () => void) {
  ouvintes.add(f);
  return () => {
    ouvintes.delete(f);
  };
}

export function useOcorrenciasOcultas(): ReadonlySet<number> {
  return useSyncExternalStore(assinar, () => ocultas, () => ocultas);
}
