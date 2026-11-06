import * as Location from 'expo-location';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { PRACA_DA_SE, type Coordenada } from '../utils/geo';

type Permissao = 'pendente' | 'concedida' | 'negada';

type LocationContextValue = {
  /** Posição real do usuário (null se negada ou ainda não obtida). */
  posicao: Coordenada | null;
  /** Posição do usuário ou, na falta dela, a Praça da Sé. */
  centro: Coordenada;
  permissao: Permissao;
  pedirPermissao: () => Promise<void>;
};

const LocationContext = createContext<LocationContextValue | null>(null);

export function LocationProvider({ children }: { children: ReactNode }) {
  const [posicao, setPosicao] = useState<Coordenada | null>(null);
  const [permissao, setPermissao] = useState<Permissao>('pendente');
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    let assinatura: Location.LocationSubscription | null = null;

    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (!ativo) return;
      if (status !== Location.PermissionStatus.GRANTED) {
        setPermissao('negada');
        return;
      }
      setPermissao('concedida');
      try {
        const ultima = await Location.getLastKnownPositionAsync();
        if (ultima && ativo) setPosicao(ultima.coords);
        const atual = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        if (ativo) setPosicao(atual.coords);
      } catch {
        // GPS indisponível: seguimos com a Praça da Sé até chegar uma posição.
      }
      assinatura = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.Balanced, distanceInterval: 15, timeInterval: 5000 },
        (loc) => ativo && setPosicao({ latitude: loc.coords.latitude, longitude: loc.coords.longitude }),
      );
      if (!ativo) assinatura.remove();
    })().catch(() => ativo && setPermissao('negada'));

    return () => {
      ativo = false;
      assinatura?.remove();
    };
  }, [tentativa]);

  const pedirPermissao = useCallback(async () => setTentativa((t) => t + 1), []);

  const value = useMemo<LocationContextValue>(
    () => ({
      posicao: posicao ? { latitude: posicao.latitude, longitude: posicao.longitude } : null,
      centro: posicao ? { latitude: posicao.latitude, longitude: posicao.longitude } : PRACA_DA_SE,
      permissao,
      pedirPermissao,
    }),
    [posicao, permissao, pedirPermissao],
  );

  return <LocationContext.Provider value={value}>{children}</LocationContext.Provider>;
}

export function useLocation(): LocationContextValue {
  const ctx = useContext(LocationContext);
  if (!ctx) throw new Error('useLocation precisa estar dentro de <LocationProvider>.');
  return ctx;
}
