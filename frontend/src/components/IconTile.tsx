import type { LucideIcon } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';

import { colors, ICON_STROKE, radii } from '../theme/tokens';

type Props = {
  icon: LucideIcon;
  color: string;
  /** 30 px (padrão, raio 8) ou 44 px (grande, raio 12). */
  size?: 'regular' | 'large';
  iconColor?: string;
};

/** Quadrado colorido com ícone branco (estilo Ajustes do iOS). */
export function IconTile({ icon: Icone, color, size = 'regular', iconColor = colors.white }: Props) {
  const grande = size === 'large';
  return (
    <View style={[grande ? styles.large : styles.regular, { backgroundColor: color }]}>
      <Icone size={grande ? 24 : 18} color={iconColor} strokeWidth={grande ? 2 : ICON_STROKE} />
    </View>
  );
}

const styles = StyleSheet.create({
  regular: {
    width: 30,
    height: 30,
    borderRadius: radii.tile,
    alignItems: 'center',
    justifyContent: 'center',
  },
  large: {
    width: 44,
    height: 44,
    borderRadius: radii.tileLarge,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
