import { Platform } from 'react-native';
import { Polyline } from 'react-native-maps';

import { colors } from '../../theme/tokens';
import type { Coordenada } from '../../utils/geo';

type Variante = 'atual' | 'segura' | 'descartada';

const COR: Record<Variante, string> = {
  atual: colors.blue,
  segura: colors.green,
  descartada: colors.tertiary,
};

type Props = {
  coordinates: Coordenada[];
  variant: Variante;
};

/**
 * Linha de rota de 7 px com contorno branco de 11 px.
 * A rota descartada é cinza e pontilhada (sem contorno).
 */
export function RouteLine({ coordinates, variant }: Props) {
  if (coordinates.length < 2) return null;
  const z = variant === 'descartada' ? 2 : 3;

  if (variant === 'descartada') {
    return (
      <Polyline
        coordinates={coordinates}
        strokeColor={COR.descartada}
        strokeWidth={5}
        lineCap="round"
        lineJoin="round"
        lineDashPattern={Platform.OS === 'ios' ? [1, 10] : [2, 12]}
        zIndex={z}
      />
    );
  }

  return (
    <>
      <Polyline
        coordinates={coordinates}
        strokeColor={colors.white}
        strokeWidth={11}
        lineCap="round"
        lineJoin="round"
        zIndex={z}
      />
      <Polyline
        coordinates={coordinates}
        strokeColor={COR[variant]}
        strokeWidth={7}
        lineCap="round"
        lineJoin="round"
        zIndex={z + 1}
      />
    </>
  );
}
