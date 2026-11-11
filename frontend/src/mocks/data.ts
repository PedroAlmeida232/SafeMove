/**
 * Dados de exemplo — usados SOMENTE quando EXPO_PUBLIC_USE_MOCK=true.
 * As posições são deslocamentos (em graus) a partir do centro consultado,
 * para que a demonstração funcione em qualquer ponto do mapa.
 */
import type { AreaRisco, CategoriaIncidente, TipoOcorrencia } from '../api/types';

type Desloc = { dLat: number; dLng: number };

export const ocorrenciasBase: (Desloc & {
  tipo: TipoOcorrencia;
  descricao: string | null;
  minutosAtras: number;
  confirmacoes: number;
})[] = [
  { dLat: 0.0042, dLng: -0.0031, tipo: 'ASSALTO', descricao: 'Dois homens de moto abordando pedestres perto do ponto de ônibus.', minutosAtras: 12, confirmacoes: 4 },
  { dLat: -0.0061, dLng: 0.0048, tipo: 'BURACO', descricao: 'Buraco grande na faixa da direita.', minutosAtras: 47, confirmacoes: 2 },
  { dLat: 0.0083, dLng: 0.0072, tipo: 'ILUMINACAO', descricao: 'Postes apagados no quarteirão inteiro.', minutosAtras: 180, confirmacoes: 6 },
  { dLat: -0.0021, dLng: -0.0089, tipo: 'ALAGAMENTO', descricao: null, minutosAtras: 25, confirmacoes: 1 },
  { dLat: 0.0111, dLng: -0.0063, tipo: 'ASSALTO', descricao: 'Celular roubado na saída do metrô.', minutosAtras: 95, confirmacoes: 3 },
];

export const incidentesBase: (Desloc & {
  id: string;
  categoria: CategoriaIncidente;
  gravidade: number;
  descricao: string | null;
  via: string | null;
  atrasoSegundos: number | null;
  minutosAtras: number;
})[] = [
  { id: 'tt-1', dLat: 0.0025, dLng: 0.0061, categoria: 'ACIDENTE', gravidade: 3, descricao: 'Colisão entre dois carros', via: 'Av. Brigadeiro Luís Antônio', atrasoSegundos: 420, minutosAtras: 8 },
  { id: 'tt-2', dLat: -0.0102, dLng: -0.0024, categoria: 'CONGESTIONAMENTO', gravidade: 2, descricao: 'Trânsito lento', via: 'Av. 23 de Maio', atrasoSegundos: 300, minutosAtras: 3 },
  { id: 'tt-3', dLat: 0.0151, dLng: 0.0012, categoria: 'CONGESTIONAMENTO', gravidade: 1, descricao: 'Trânsito com lentidão leve', via: 'R. da Consolação', atrasoSegundos: 60, minutosAtras: 5 },
  { id: 'tt-4', dLat: -0.0043, dLng: 0.0135, categoria: 'OBRAS', gravidade: 1, descricao: 'Obras na pista', via: 'Av. do Estado', atrasoSegundos: 120, minutosAtras: 240 },
  { id: 'tt-5', dLat: 0.0189, dLng: -0.0118, categoria: 'VIA_INTERDITADA', gravidade: 4, descricao: 'Via interditada', via: 'R. Augusta', atrasoSegundos: null, minutosAtras: 30 },
];

export const radaresBase: (Desloc & { id: number; velocidadeMax: number | null })[] = [
  { id: 1, dLat: 0.0058, dLng: 0.0011, velocidadeMax: 50 },
  { id: 2, dLat: -0.0079, dLng: -0.0052, velocidadeMax: 40 },
  { id: 3, dLat: 0.0012, dLng: 0.0104, velocidadeMax: 60 },
];

/** Áreas de risco com coordenadas reais dos distritos de São Paulo. */
export const areasRisco: AreaRisco[] = [
  { distrito: 'Sé', latitude: -23.5505, longitude: -46.6333, nivel: 5, segura: false, assaltosReportados: 12, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
  { distrito: 'República', latitude: -23.5431, longitude: -46.6424, nivel: 4, segura: false, assaltosReportados: 7, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
  { distrito: 'Brás', latitude: -23.5435, longitude: -46.6158, nivel: 4, segura: false, assaltosReportados: 7, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
  { distrito: 'Bela Vista', latitude: -23.5614, longitude: -46.6457, nivel: 3, segura: false, assaltosReportados: 4, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
  { distrito: 'Santa Cecília', latitude: -23.5373, longitude: -46.6536, nivel: 3, segura: false, assaltosReportados: 4, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
  { distrito: 'Pinheiros', latitude: -23.5672, longitude: -46.6917, nivel: 2, segura: true, assaltosReportados: 1, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
  { distrito: 'Moema', latitude: -23.6009, longitude: -46.6624, nivel: 1, segura: true, assaltosReportados: 0, roubosOficiais: null, furtosOficiais: null, mesOficial: null },
];
