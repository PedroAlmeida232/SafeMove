import { MapPin } from 'lucide-react-native';
import { useState } from 'react';
import { Modal, StyleSheet, View } from 'react-native';
import MapView, { PROVIDER_DEFAULT } from 'react-native-maps';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, shadows } from '../../theme/tokens';
import { dentroDeSaoPaulo, type Coordenada } from '../../utils/geo';
import { ModalNavBar } from '../ModalNavBar';
import { Text } from '../Text';

type Props = {
  visible: boolean;
  inicial: Coordenada;
  onCancel: () => void;
  onConfirm: (c: Coordenada) => void;
};

/** Mapa para ajustar o local da ocorrência: o pino fica fixo no centro. */
export function AjustarLocal({ visible, inicial, onCancel, onConfirm }: Props) {
  const insets = useSafeAreaInsets();
  const [centro, setCentro] = useState<Coordenada>(inicial);
  const foraDeSP = !dentroDeSaoPaulo(centro);

  return (
    <Modal
      visible={visible}
      animationType="slide"
      presentationStyle="pageSheet"
      onRequestClose={onCancel}
      onShow={() => setCentro(inicial)}
    >
      <View style={styles.container}>
        <ModalNavBar
          title="Ajustar local"
          leftLabel="Cancelar"
          onLeft={onCancel}
          rightLabel="OK"
          onRight={() => onConfirm(centro)}
          rightDisabled={foraDeSP}
        />
        <View style={styles.flex}>
          <MapView
            style={StyleSheet.absoluteFill}
            provider={PROVIDER_DEFAULT}
            initialRegion={{ ...inicial, latitudeDelta: 0.006, longitudeDelta: 0.006 }}
            onRegionChangeComplete={(r) => setCentro({ latitude: r.latitude, longitude: r.longitude })}
            showsUserLocation
            showsCompass={false}
            toolbarEnabled={false}
            pitchEnabled={false}
            rotateEnabled={false}
          />
          <View style={styles.pinWrap} pointerEvents="none">
            <View style={styles.pin}>
              <MapPin size={18} color={colors.white} strokeWidth={2.2} />
            </View>
            <View style={styles.haste} />
          </View>
          <View style={[styles.dica, { bottom: insets.bottom + 16 }]} pointerEvents="none">
            <Text variant="captionStrong" color={foraDeSP ? colors.redText : colors.label} style={styles.dicaTexto}>
              {foraDeSP ? 'Escolha um ponto dentro da cidade de São Paulo' : 'Arraste o mapa para posicionar o pino'}
            </Text>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  flex: {
    flex: 1,
  },
  pinWrap: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
    // Desloca para a ponta da haste coincidir com o centro do mapa.
    paddingBottom: 46,
  },
  pin: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.accent,
    borderWidth: 2.5,
    borderColor: colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: shadows.pin,
  },
  haste: {
    width: 3,
    height: 10,
    borderRadius: 1.5,
    backgroundColor: colors.accent,
  },
  dica: {
    position: 'absolute',
    alignSelf: 'center',
    backgroundColor: colors.background,
    borderRadius: 99,
    paddingVertical: 8,
    paddingHorizontal: 14,
    boxShadow: shadows.floating,
  },
  dicaTexto: {
    textAlign: 'center',
  },
});
