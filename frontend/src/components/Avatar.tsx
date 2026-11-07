import { User } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';

import { colors } from '../theme/tokens';
import { Text } from './Text';

type Props = {
  initials: string;
  size?: number;
};

/** Avatar redondo com iniciais em accent sobre #F2F2F7. */
export function Avatar({ initials, size = 32 }: Props) {
  return (
    <View style={[styles.base, { width: size, height: size, borderRadius: size / 2 }]}>
      {initials ? (
        <Text
          variant={size >= 60 ? 'largeTitle' : 'captionStrong'}
          color={colors.accent}
          style={size >= 60 ? { fontSize: size * 0.38, lineHeight: size * 0.46 } : undefined}
        >
          {initials}
        </Text>
      ) : (
        <User size={size * 0.55} color={colors.accent} strokeWidth={1.8} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    backgroundColor: colors.groupedBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
