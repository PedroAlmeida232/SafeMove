import { router, useLocalSearchParams } from 'expo-router';
import { Check, Users } from 'lucide-react-native';
import { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { TipoOcorrencia } from '../../src/api/types';
import { PrimaryButton } from '../../src/components/Buttons';
import { GroupedList } from '../../src/components/GroupedList';
import { IconTile } from '../../src/components/IconTile';
import { Row } from '../../src/components/Row';
import { Text } from '../../src/components/Text';
import { useIdentidade } from '../../src/hooks/useIdentidade';
import { colors, spacing } from '../../src/theme/tokens';
import { META_OCORRENCIA } from '../../src/utils/ocorrencia';

function ehTipo(v: string | undefined): v is TipoOcorrencia {
  return v !== undefined && v in META_OCORRENCIA;
}

export default function OcorrenciaEnviadaScreen() {
  const params = useLocalSearchParams<{ tipo: string; rua: string; lat?: string; lng?: string }>();
  const insets = useSafeAreaInsets();
  const { primeiroNome } = useIdentidade();
  const meta = ehTipo(params.tipo) ? META_OCORRENCIA[params.tipo] : META_OCORRENCIA.BURACO;
  const rua = params.rua || 'este local';

  const escala = useSharedValue(0.4);
  useEffect(() => {
    escala.value = withSpring(1, { damping: 12, stiffness: 180 });
  }, [escala]);
  const anim = useAnimatedStyle(() => ({ transform: [{ scale: escala.value }] }));

  const voltar = () => {
    router.dismissAll();
    router.navigate({
      pathname: '/',
      params: params.lat && params.lng ? { focoLat: params.lat, focoLng: params.lng } : {},
    });
  };

  const saudacao = primeiroNome ? `Obrigado, ${primeiroNome}.` : 'Obrigado.';

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom + 16 }]}>
      <View style={styles.centro}>
        <Animated.View style={[styles.circulo, anim]} accessibilityElementsHidden importantForAccessibility="no">
          <Check size={44} color={colors.white} strokeWidth={3} />
        </Animated.View>
        <Text variant="sheetTitle" accessibilityRole="header" style={styles.titulo}>
          Ocorrência enviada
        </Text>
        <Text variant="body" color={colors.secondaryLabel} style={styles.texto}>
          {saudacao} Quem passar por {rua} vai ser avisado.
        </Text>

        <GroupedList style={styles.lista} separatorInset={56}>
          <Row
            title={meta.rotulo}
            subtitle={`${rua} · agora`}
            left={<IconTile icon={meta.icone} color={meta.cor} />}
          />
          <Row title="Visível para quem passa por aqui" left={<IconTile icon={Users} color={colors.blue} />} />
        </GroupedList>
      </View>

      <PrimaryButton title="Voltar ao mapa" onPress={voltar} style={styles.botao} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  centro: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: spacing.gutter,
  },
  circulo: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: colors.green,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
  },
  titulo: {
    textAlign: 'center',
    marginTop: 20,
  },
  texto: {
    textAlign: 'center',
    marginTop: 8,
    paddingHorizontal: 16,
  },
  lista: {
    marginTop: 28,
  },
  botao: {
    marginHorizontal: spacing.gutter,
  },
});
