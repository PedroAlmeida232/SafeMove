import { StyleSheet, View } from 'react-native';

import { colors, hairline } from '../theme/tokens';
import { TextButton } from './Buttons';
import { Text } from './Text';

type Props = {
  title: string;
  leftLabel: string;
  onLeft: () => void;
  rightLabel?: string;
  onRight?: () => void;
  rightDisabled?: boolean;
};

/** Barra de modal iOS: ação à esquerda, título central em negrito, ação à direita. */
export function ModalNavBar({ title, leftLabel, onLeft, rightLabel, onRight, rightDisabled }: Props) {
  return (
    <View style={styles.bar}>
      <View style={styles.side}>
        <TextButton title={leftLabel} weight="regular" onPress={onLeft} />
      </View>
      <Text variant="bodyStrong" accessibilityRole="header" numberOfLines={1} style={styles.title}>
        {title}
      </Text>
      <View style={[styles.side, styles.right]}>
        {rightLabel && onRight ? (
          <TextButton title={rightLabel} onPress={onRight} disabled={rightDisabled} />
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    borderBottomWidth: hairline,
    borderBottomColor: colors.separator,
    backgroundColor: colors.background,
  },
  side: {
    width: 96,
    alignItems: 'flex-start',
  },
  right: {
    alignItems: 'flex-end',
  },
  title: {
    flex: 1,
    textAlign: 'center',
  },
});
