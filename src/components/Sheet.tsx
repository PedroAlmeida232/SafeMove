import BottomSheet, { BottomSheetView, useBottomSheetSpringConfigs } from '@gorhom/bottom-sheet';
import { forwardRef, type ReactNode } from 'react';
import { StyleSheet } from 'react-native';

import { colors, radii, shadows } from '../theme/tokens';

type Props = {
  children: ReactNode;
  /** Pontos de parada; sem eles a sheet assume a altura do conteúdo. */
  snapPoints?: (string | number)[];
  index?: number;
  onChange?: (index: number) => void;
  /** Espaço extra no fim do conteúdo (home indicator). */
  bottomPadding?: number;
  /** Permite arrastar até fechar. */
  closable?: boolean;
  /** Usar quando o conteúdo é uma BottomSheetScrollView. */
  scrollable?: boolean;
  /** Acrescenta a altura do conteúdo como ponto de parada extra. */
  dynamic?: boolean;
};

/** Bottom sheet com raio superior 16, sombra suave e pegador de 36×5. */
export const Sheet = forwardRef<BottomSheet, Props>(function Sheet(
  { children, snapPoints, index = 0, onChange, bottomPadding = 0, closable = false, scrollable = false, dynamic },
  ref,
) {
  const animationConfigs = useBottomSheetSpringConfigs({
    damping: 80,
    overshootClamping: true,
    stiffness: 500,
  });

  return (
    <BottomSheet
      ref={ref}
      index={index}
      snapPoints={snapPoints}
      enableDynamicSizing={dynamic ?? !snapPoints}
      onChange={onChange}
      enablePanDownToClose={closable}
      animationConfigs={animationConfigs}
      backgroundStyle={styles.background}
      handleIndicatorStyle={styles.grabber}
      handleStyle={styles.handle}
      style={styles.shadow}
      keyboardBehavior="interactive"
      keyboardBlurBehavior="restore"
      accessible={false}
    >
      {scrollable ? (
        children
      ) : (
        <BottomSheetView style={{ paddingBottom: bottomPadding }}>{children}</BottomSheetView>
      )}
    </BottomSheet>
  );
});

const styles = StyleSheet.create({
  background: {
    backgroundColor: colors.background,
    borderTopLeftRadius: radii.sheet,
    borderTopRightRadius: radii.sheet,
    borderBottomLeftRadius: 0,
    borderBottomRightRadius: 0,
  },
  shadow: {
    boxShadow: shadows.sheet,
    borderTopLeftRadius: radii.sheet,
    borderTopRightRadius: radii.sheet,
  },
  grabber: {
    width: 36,
    height: 5,
    borderRadius: 2.5,
    backgroundColor: colors.grabber,
  },
  handle: {
    paddingTop: 6,
    paddingBottom: 8,
  },
});
