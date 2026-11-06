import { Circle } from 'react-native-maps';

import { colors } from '../../theme/tokens';
import type { Coordenada } from '../../utils/geo';

type Props = {
  center: Coordenada;
  /** Raio em metros. */
  radius: number;
  color?: string;
};

function comAlpha(hex: string, alpha: number): string {
  const n = parseInt(hex.replace('#', ''), 16);
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${alpha})`;
}

/** Área de risco: preenchimento a 14% com borda tracejada. */
export function RiskCircle({ center, radius, color = colors.red }: Props) {
  return (
    <Circle
      center={center}
      radius={radius}
      fillColor={comAlpha(color, 0.14)}
      strokeColor={color}
      strokeWidth={1.5}
      lineDashPattern={[6, 5]}
      zIndex={1}
    />
  );
}
