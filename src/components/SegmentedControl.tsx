import { useState } from 'react';
import { Pressable, StyleSheet, View, type LayoutChangeEvent } from 'react-native';
import Animated, { useAnimatedStyle, withSpring } from 'react-native-reanimated';

import { colors } from '../theme/tokens';
import { Text } from './Text';

type Opcao<T extends string> = { value: T; label: string };

type Props<T extends string> = {
  options: Opcao<T>[];
  value: T;
  onChange: (v: T) => void;
  accessibilityLabel: string;
};

/** Controle segmentado no padrão iOS. */
export function SegmentedControl<T extends string>({ options, value, onChange, accessibilityLabel }: Props<T>) {
  const [largura, setLargura] = useState(0);
  const indice = Math.max(0, options.findIndex((o) => o.value === value));
  const segmento = largura > 0 ? (largura - 4) / options.length : 0;

  const thumb = useAnimatedStyle(() => ({
    width: segmento,
    transform: [{ translateX: withSpring(indice * segmento, { damping: 22, stiffness: 300 }) }],
  }));

  return (
    <View
      style={styles.track}
      onLayout={(e: LayoutChangeEvent) => setLargura(e.nativeEvent.layout.width)}
      accessibilityRole="tablist"
      accessibilityLabel={accessibilityLabel}
    >
      {largura > 0 ? <Animated.View style={[styles.thumb, thumb]} /> : null}
      {options.map((o) => {
        const ativo = o.value === value;
        return (
          <Pressable
            key={o.value}
            onPress={() => onChange(o.value)}
            style={styles.segment}
            accessibilityRole="tab"
            accessibilityState={{ selected: ativo }}
            accessibilityLabel={o.label}
          >
            <Text variant={ativo ? 'captionStrong' : 'caption'} style={styles.label}>
              {o.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    flexDirection: 'row',
    backgroundColor: 'rgba(118,118,128,0.12)',
    borderRadius: 9,
    padding: 2,
    height: 36,
  },
  thumb: {
    position: 'absolute',
    top: 2,
    bottom: 2,
    left: 2,
    borderRadius: 7,
    backgroundColor: colors.background,
    boxShadow: '0px 3px 8px rgba(0,0,0,0.12), 0px 3px 1px rgba(0,0,0,0.04)',
  },
  segment: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 32,
  },
  label: {
    fontSize: 13,
  },
});
