import {
  Ban,
  Car,
  CarFront,
  CircleAlert,
  CloudFog,
  CloudRain,
  Construction,
  Droplets,
  LightbulbOff,
  ShieldAlert,
  Snowflake,
  TrafficCone,
  TriangleAlert,
  Wind,
  Wrench,
  type LucideIcon,
} from 'lucide-react-native';

import type { CategoriaIncidente, TipoOcorrencia } from '../api/types';
import { colors } from '../theme/tokens';

type MetaTipo = { rotulo: string; icone: LucideIcon; cor: string };

/** Ordem da grade 3×2 da tela "Nova ocorrência". */
export const TIPOS_OCORRENCIA: TipoOcorrencia[] = [
  'BURACO',
  'ACIDENTE',
  'TRANSITO',
  'ILUMINACAO',
  'ASSALTO',
  'ALAGAMENTO',
];

export const META_OCORRENCIA: Record<TipoOcorrencia, MetaTipo> = {
  BURACO: { rotulo: 'Buraco', icone: Construction, cor: colors.orange },
  ACIDENTE: { rotulo: 'Acidente', icone: CarFront, cor: colors.red },
  TRANSITO: { rotulo: 'Trânsito', icone: TrafficCone, cor: colors.orange },
  ILUMINACAO: { rotulo: 'Iluminação', icone: LightbulbOff, cor: colors.orange },
  ASSALTO: { rotulo: 'Assalto', icone: ShieldAlert, cor: colors.red },
  ALAGAMENTO: { rotulo: 'Alagamento', icone: Droplets, cor: colors.red },
};

export const ICONE_INCIDENTE: Record<CategoriaIncidente, LucideIcon> = {
  ACIDENTE: CarFront,
  CONGESTIONAMENTO: Car,
  VIA_INTERDITADA: Ban,
  FAIXA_INTERDITADA: TrafficCone,
  OBRAS: Construction,
  ALAGAMENTO: Droplets,
  PERIGO: TriangleAlert,
  VEICULO_QUEBRADO: Wrench,
  CHUVA: CloudRain,
  NEBLINA: CloudFog,
  VENTO: Wind,
  GELO: Snowflake,
  DESCONHECIDO: CircleAlert,
};
