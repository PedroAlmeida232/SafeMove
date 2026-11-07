import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

import type { ModoTransporte, Ponto, RotaAvaliada } from '../api/types';

export type Planejamento = {
  origem: Ponto;
  destino: Ponto;
  origemLabel: string;
  destinoLabel: string;
  modo: ModoTransporte;
  /** Já ordenadas da mais segura para a menos segura (como vêm da API). */
  rotas: RotaAvaliada[];
};

export type TrajetoAtivo = Omit<Planejamento, 'rotas'> & {
  rota: RotaAvaliada;
  iniciadoEm: number;
};

type RotaContextValue = {
  planejamento: Planejamento | null;
  definirPlanejamento: (p: Planejamento | null) => void;
  ativo: TrajetoAtivo | null;
  iniciarTrajeto: (rota: RotaAvaliada) => void;
  atualizarRotaAtiva: (rota: RotaAvaliada) => void;
  encerrarTrajeto: () => void;
};

const RotaContext = createContext<RotaContextValue | null>(null);

export function RotaProvider({ children }: { children: ReactNode }) {
  const [planejamento, setPlanejamento] = useState<Planejamento | null>(null);
  const [ativo, setAtivo] = useState<TrajetoAtivo | null>(null);

  const iniciarTrajeto = useCallback(
    (rota: RotaAvaliada) => {
      if (!planejamento) return;
      const { rotas: _rotas, ...resto } = planejamento;
      setAtivo({ ...resto, rota, iniciadoEm: Date.now() });
    },
    [planejamento],
  );

  const atualizarRotaAtiva = useCallback((rota: RotaAvaliada) => {
    setAtivo((a) => (a ? { ...a, rota } : a));
  }, []);

  const encerrarTrajeto = useCallback(() => setAtivo(null), []);

  const value = useMemo(
    () => ({
      planejamento,
      definirPlanejamento: setPlanejamento,
      ativo,
      iniciarTrajeto,
      atualizarRotaAtiva,
      encerrarTrajeto,
    }),
    [planejamento, ativo, iniciarTrajeto, atualizarRotaAtiva, encerrarTrajeto],
  );

  return <RotaContext.Provider value={value}>{children}</RotaContext.Provider>;
}

export function useRotas(): RotaContextValue {
  const ctx = useContext(RotaContext);
  if (!ctx) throw new Error('useRotas precisa estar dentro de <RotaProvider>.');
  return ctx;
}

/** Rota de maior nota dentre as avaliadas. */
export function melhorRota(rotas: RotaAvaliada[]): RotaAvaliada | undefined {
  return rotas.reduce<RotaAvaliada | undefined>((m, r) => (!m || r.nota > m.nota ? r : m), undefined);
}
