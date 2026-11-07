import { ShieldCheck } from 'lucide-react-native';
import { View } from 'react-native';

import { colors } from '../theme/tokens';

/** Ícone do SafeMove: escudo branco sobre fundo accent. */
export function AppLogo({ size = 40 }: { size?: number }) {
  return (
    <View
      accessibilityLabel="SafeMove"
      style={{
        width: size,
        height: size,
        borderRadius: size * 0.225,
        backgroundColor: colors.accent,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <ShieldCheck size={size * 0.56} color={colors.white} strokeWidth={2} />
    </View>
  );
}
