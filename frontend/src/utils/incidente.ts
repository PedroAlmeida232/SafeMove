import type { AreaRisco, CategoriaIncidente, Classificacao, Incidente } from '../api/types';
import { colors } from '../theme/tokens';

const ROTULOS: Record<CategoriaIncidente, string> = {
  ACIDENTE: 'Acidente',
  CONGESTIONAMENTO: 'Congestionamento',
  VIA_INTERDITADA: 'Via interditada',
  FAIXA_INTERDITADA: 'Faixa interditada',
  OBRAS: 'Obras',
  ALAGAMENTO: 'Alagamento',
  PERIGO: 'Perigo na via',
  VEICULO_QUEBRADO: 'Veículo quebrado',
  CHUVA: 'Chuva',
  NEBLINA: 'Neblina',
  VENTO: 'Vento forte',
  GELO: 'Gelo na pista',
  DESCONHECIDO: 'Incidente',
};

export function rotuloCategoria(c: CategoriaIncidente): string {
  return ROTULOS[c];
}

/** Regra de cores dos pinos de incidente. */
export function corIncidente(i: Pick<Incidente, 'categoria' | 'gravidade'>): string {
  switch (i.categoria) {
    case 'ACIDENTE':
    case 'VIA_INTERDITADA':
    case 'ALAGAMENTO':
      return colors.red;
    case 'CONGESTIONAMENTO':
      return i.gravidade >= 2 ? colors.orange : colors.green;
    case 'OBRAS':
    case 'FAIXA_INTERDITADA':
      return colors.orange;
    default:
      return colors.gray;
  }
}

/** Áreas de risco: 4–5 vermelho, 3 laranja, 1–2 não aparecem. */
export function corAreaRisco(a: AreaRisco): string | null {
  if (a.nivel >= 4) return colors.red;
  if (a.nivel === 3) return colors.orange;
  return null;
}

/** Raio (m) do círculo desenhado para cada nível de risco do distrito. */
export function raioAreaRisco(a: AreaRisco): number {
  return a.nivel >= 5 ? 1100 : a.nivel === 4 ? 900 : 700;
}

export function corClassificacao(c: Classificacao): string {
  return c === 'SEGURA' ? colors.green : c === 'ATENCAO' ? colors.orange : colors.red;
}

/** Cor de texto com contraste AA sobre fundo branco. */
export function corTextoClassificacao(c: Classificacao): string {
  return c === 'SEGURA' ? colors.greenText : c === 'ATENCAO' ? colors.orangeText : colors.redText;
}

/** "Incidentes graves": gravidade 3 ou 4. */
export const GRAVIDADE_GRAVE = 3;
