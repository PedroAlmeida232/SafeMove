export class ApiError extends Error {
  readonly status: number;
  readonly campos?: Record<string, string>;

  constructor(status: number, detail: string, campos?: Record<string, string>) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.campos = campos;
  }
}

export type Metodo = 'GET' | 'POST';
