import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { colors, radii } from '../theme/tokens';
import { Text } from './Text';

type Props = {
  value: string;
  label: string;
  color?: string;
  /** Texto pequeno ao lado do valor (ex.: "/100"). */
  suffix?: string;
};

/** Estatística: número grande em cima, legenda embaixo. */
export function StatBlock({ value, label, color = colors.label, suffix }: Props) {
  return (
    <View style={styles.block} accessible accessibilityLabel={`${value}${suffix ?? ''} ${label}`}>
      <View style={styles.valueRow}>
        <Text variant="stat" color={color}>
          {value}
        </Text>
        {suffix ? (
          <Text variant="captionStrong" color={colors.secondaryLabel} style={styles.suffix}>
            {suffix}
          </Text>
        ) : null}
      </View>
      <Text variant="caption" color={colors.secondaryLabel} numberOfLines={2}>
        {label}
      </Text>
    </View>
  );
}

export function StatRow({ children }: { children: ReactNode }) {
  return <View style={styles.row}>{children}</View>;
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
  },
  block: {
    flex: 1,
    backgroundColor: colors.groupedBg,
    borderRadius: radii.grouped,
    paddingVertical: 12,
    paddingHorizontal: 12,
  },
  valueRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  suffix: {
    marginLeft: 2,
  },
});
