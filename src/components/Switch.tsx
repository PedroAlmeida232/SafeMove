import { useEffect } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import Animated, { interpolateColor, useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';

import { colors } from '../theme/tokens';

type Props = {
  value: boolean;
  onValueChange: (v: boolean) => void;
  accessibilityLabel: string;
  disabled?: boolean;
};

const W = 51;
const H = 31;
const KNOB = 27;
const OFF_TRACK = 'rgba(120,120,128,0.16)';

/** Switch no padrão iOS (51×31), ligado em verde, igual nas duas plataformas. */
export function Switch({ value, onValueChange, accessibilityLabel, disabled }: Props) {
  const p = useSharedValue(value ? 1 : 0);

  useEffect(() => {
    p.value = withSpring(value ? 1 : 0, { damping: 18, stiffness: 260, mass: 0.6 });
  }, [value, p]);

  const track = useAnimatedStyle(() => ({
    backgroundColor: interpolateColor(p.value, [0, 1], [OFF_TRACK, colors.green]),
  }));
  const knob = useAnimatedStyle(() => ({
    transform: [{ translateX: p.value * (W - KNOB - 4) }],
  }));

  return (
    <Pressable
      onPress={() => onValueChange(!value)}
      disabled={disabled}
      hitSlop={8}
      accessibilityRole="switch"
      accessibilityLabel={accessibilityLabel}
      accessibilityState={{ checked: value, disabled }}
      style={{ opacity: disabled ? 0.4 : 1 }}
    >
      <Animated.View style={[styles.track, track]}>
        <Animated.View style={[styles.knob, knob]} />
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  track: {
    width: W,
    height: H,
    borderRadius: H / 2,
    padding: 2,
    justifyContent: 'center',
  },
  knob: {
    width: KNOB,
    height: KNOB,
    borderRadius: KNOB / 2,
    backgroundColor: colors.white,
    boxShadow: '0px 3px 8px rgba(0,0,0,0.15), 0px 3px 1px rgba(0,0,0,0.06)',
  },
});
