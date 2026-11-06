import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { colors } from '../theme/tokens';
import { Text } from './Text';

type Props = {
  title: string;
  subtitle?: string;
  /** Tile grande à esquerda (telas 03 e 04). */
  tile?: ReactNode;
  right?: ReactNode;
};

/** Título de sheet 22/700 com subtítulo secundário. */
export function SheetHeader({ title, subtitle, tile, right }: Props) {
  return (
    <View style={styles.row}>
      {tile ? <View style={styles.tile}>{tile}</View> : null}
      <View style={styles.texts}>
        <Text variant="sheetTitle" accessibilityRole="header">
          {title}
        </Text>
        {subtitle ? (
          <Text variant="secondary" color={colors.secondaryLabel} style={styles.subtitle}>
            {subtitle}
          </Text>
        ) : null}
      </View>
      {right}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  tile: {
    marginRight: 12,
  },
  texts: {
    flex: 1,
  },
  subtitle: {
    marginTop: 2,
  },
});
