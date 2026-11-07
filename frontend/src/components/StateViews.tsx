import { CircleAlert, MapPinned } from 'lucide-react-native';
import { useEffect } from 'react';
import { ActivityIndicator, StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withRepeat, withTiming } from 'react-native-reanimated';

import { colors, radii } from '../theme/tokens';
import { TextButton } from './Buttons';
import { Text } from './Text';

export const MENSAGEM_VAZIO = 'Nenhuma ocorrência por aqui nas últimas 24 h';

export function Loading({ label, style }: { label?: string; style?: StyleProp<ViewStyle> }) {
  return (
    <View style={[styles.center, style]} accessibilityLabel={label ?? 'Carregando'} accessible>
      <ActivityIndicator color={colors.tertiary} />
      {label ? (
        <Text variant="secondary" color={colors.secondaryLabel} style={styles.gap}>
          {label}
        </Text>
      ) : null}
    </View>
  );
}

export function EmptyState({
  message = MENSAGEM_VAZIO,
  style,
}: {
  message?: string;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={[styles.center, style]}>
      <MapPinned size={28} color={colors.tertiary} strokeWidth={1.7} />
      <Text variant="secondary" color={colors.secondaryLabel} style={[styles.gap, styles.textCenter]}>
        {message}
      </Text>
    </View>
  );
}

export function ErrorState({
  message,
  onRetry,
  style,
}: {
  message: string;
  onRetry?: () => void;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={[styles.center, style]} accessibilityLiveRegion="polite">
      <CircleAlert size={28} color={colors.tertiary} strokeWidth={1.7} />
      <Text variant="secondary" color={colors.secondaryLabel} style={[styles.gap, styles.textCenter]}>
        {message}
      </Text>
      {onRetry ? <TextButton title="Tentar de novo" onPress={onRetry} /> : null}
    </View>
  );
}

/** Bloco cinza pulsante para skeletons. */
export function Skeleton({ width, height, radius = 6, style }: {
  width: number | `${number}%`;
  height: number;
  radius?: number;
  style?: StyleProp<ViewStyle>;
}) {
  const o = useSharedValue(0.5);
  useEffect(() => {
    o.value = withRepeat(withTiming(1, { duration: 700 }), -1, true);
  }, [o]);
  const anim = useAnimatedStyle(() => ({ opacity: o.value }));
  return (
    <Animated.View
      style={[{ width, height, borderRadius: radius, backgroundColor: 'rgba(120,120,128,0.16)' }, anim, style]}
    />
  );
}

/** Skeleton de uma lista agrupada com N linhas. */
export function SkeletonList({ rows = 3 }: { rows?: number }) {
  return (
    <View style={styles.skeletonGroup} accessibilityLabel="Carregando" accessible>
      {Array.from({ length: rows }).map((_, i) => (
        <View key={i} style={styles.skeletonRow}>
          <Skeleton width={30} height={30} radius={8} />
          <View style={styles.skeletonTexts}>
            <Skeleton width="60%" height={14} />
            <Skeleton width="35%" height={11} style={styles.gapSmall} />
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  center: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 24,
    paddingHorizontal: 32,
  },
  gap: {
    marginTop: 8,
  },
  gapSmall: {
    marginTop: 6,
  },
  textCenter: {
    textAlign: 'center',
  },
  skeletonGroup: {
    backgroundColor: colors.groupedBg,
    borderRadius: radii.grouped,
    paddingVertical: 4,
  },
  skeletonRow: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
  },
  skeletonTexts: {
    flex: 1,
    marginLeft: 12,
  },
});
