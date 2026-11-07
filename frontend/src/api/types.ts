/** Contrato da API Java do SafeMove. Não alterar sem alinhar com o back-end. */

export type TipoOcorrencia =
  | 'ASSALTO'
  | 'BURACO'
  | 'ACIDENTE'
  | 'ALAGAMENTO'
  | 'TRANSITO'
  | 'ILUMINACAO';

export type Ocorrencia = {
  id: number;
  tipo: TipoOcorrencia;
  descricao: string | null;
  latitude: number;
  longitude: number;
  criadoEm: string;
  confirmacoes: number;
};

export type NovaOcorrencia = {
  tipo: TipoOcorrencia;
  descricao?: string;
  latitude: number;
  longitude: number;
};

export type CategoriaIncidente =
  | 'ACIDENTE'
  | 'CONGESTIONAMENTO'
  | 'VIA_INTERDITADA'
  | 'FAIXA_INTERDITADA'
  | 'OBRAS'
  | 'ALAGAMENTO'
  | 'PERIGO'
  | 'VEICULO_QUEBRADO'
  | 'CHUVA'
  | 'NEBLINA'
  | 'VENTO'
  | 'GELO'
  | 'DESCONHECIDO';

export type Incidente = {
  id: string;
  categoria: CategoriaIncidente;
  /** 0 a 4 */
  gravidade: number;
  descricao: string | null;
  via: string | null;
  atrasoSegundos: number | null;
  latitude: number;
  longitude: number;
  atualizadoEm: string;
};

export type Radar = {
  id: number;
  latitude: number;
  longitude: number;
  velocidadeMax: number | null;
};

export type AreaRisco = {
  distrito: string;
  latitude: number;
  longitude: number;
  /** 1 a 5 */
  nivel: number;
};

export type ModoTransporte = 'CARRO' | 'A_PE' | 'BICICLETA';

export type Ponto = { latitude: number; longitude: number };

export type AvaliarRotaBody = {
  origem: Ponto;
  destino: Ponto;
  modo?: ModoTransporte;
};

export type Classificacao = 'SEGURA' | 'ATENCAO' | 'EVITE';

export type ResumoRota = {
  incidentesGraves: number;
  incidentesLeves: number;
  ocorrenciasRecentes: number;
  assaltosRecentes: number;
  riscoMedioRegiao: number;
  radares: number;
};

export type RotaAvaliada = {
  indice: number;
  /** 0 a 100 */
  nota: number;
  classificacao: Classificacao;
  distanciaMetros: number;
  duracaoSegundos: number;
  atrasoTransitoSegundos: number;
  resumo: ResumoRota;
  alertas: string[];
  pontos: Ponto[];
};

export type Usuario = {
  uid: string;
  nome: string;
  email: string;
  criadoEm: string;
};

/** Resposta de POST /auth/login e /auth/register. */
export type AuthResponse = {
  token: string;
  refreshToken: string;
  usuario: Usuario;
};

/** Corpo de erro (RFC 7807 / Spring ProblemDetail). */
export type ProblemDetail = {
  status: number;
  detail: string;
  campos?: Record<string, string>;
};
