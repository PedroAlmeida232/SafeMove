// Stub web de react-native-maps (a lib não suporta web). Mostra um aviso no lugar do mapa.
import { forwardRef, useImperativeHandle, type ReactNode } from 'react';
import { StyleSheet, Text, View, type StyleProp, type ViewStyle } from 'react-native';

export const PROVIDER_DEFAULT = null;
export const PROVIDER_GOOGLE = 'google';
export type EdgePadding = { top: number; right: number; bottom: number; left: number };

type MapProps = { style?: StyleProp<ViewStyle>; children?: ReactNode; [key: string]: any };

const MapView = forwardRef<unknown, MapProps>(function MapView({ style }, ref) {
  useImperativeHandle(ref, () => ({
    animateToRegion: () => {},
    animateCamera: () => {},
    fitToCoordinates: () => {},
    fitToElements: () => {},
  }));
  return (
    <View style={[styles.box, style]}>
      <Text style={styles.text}>O mapa está disponível apenas no app (Android/iOS).</Text>
    </View>
  );
});

const Vazio = () => null;
export const Marker = Vazio;
export const Circle = Vazio;
export const Polyline = Vazio;
export const Polygon = Vazio;

export default MapView;

const styles = StyleSheet.create({
  box: { alignItems: 'center', justifyContent: 'center', backgroundColor: '#e5e5ea' },
  text: { color: '#6c6c70', padding: 16, textAlign: 'center' },
});
