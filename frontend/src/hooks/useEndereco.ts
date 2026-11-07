import * as Location from 'expo-location';
import { useEffect, useState } from 'react';

import type { Coordenada } from '../utils/geo';

export type Endereco = {
  /** "Rua Augusta, 1200" */
  rua: string;
  bairro: string | null;
};

/** Endereço legível do ponto via reverseGeocodeAsync. */
export function useEndereco(ponto: Coordenada | null): { endereco: Endereco | null; carregando: boolean } {
  const [endereco, setEndereco] = useState<Endereco | null>(null);
  const [carregando, setCarregando] = useState(false);
  const lat = ponto?.latitude;
  const lng = ponto?.longitude;

  useEffect(() => {
    if (lat === undefined || lng === undefined) return;
    let ativo = true;
    setCarregando(true);
    Location.reverseGeocodeAsync({ latitude: lat, longitude: lng })
      .then(([e]) => {
        if (!ativo) return;
        if (!e) {
          setEndereco(null);
          return;
        }
        const rua = e.street ? [e.street, e.streetNumber].filter(Boolean).join(', ') : e.name ?? 'Local sem nome';
        setEndereco({ rua, bairro: e.district ?? e.subregion ?? null });
      })
      .catch(() => ativo && setEndereco(null))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [lat, lng]);

  return { endereco, carregando };
}
