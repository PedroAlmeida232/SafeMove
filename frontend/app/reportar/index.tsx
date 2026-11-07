import * as Haptics from 'expo-haptics';
import { router } from 'expo-router';
import { MapPin } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { mensagemDeErro } from '../../src/api/client';
import { useCriarOcorrencia } from '../../src/api/hooks';
import type { TipoOcorrencia } from '../../src/api/types';
import { useAuth } from '../../src/auth/AuthProvider';
import { PrimaryButton } from '../../src/components/Buttons';
import { GroupedList } from '../../src/components/GroupedList';
import { IconTile } from '../../src/components/IconTile';
import { AjustarLocal } from '../../src/components/map/AjustarLocal';
import { ModalNavBar } from '../../src/components/ModalNavBar';
import { Touchable } from '../../src/components/Pressable';
import { Row } from '../../src/components/Row';
import { Text } from '../../src/components/Text';
import { useEndereco } from '../../src/hooks/useEndereco';
import { useLocation } from '../../src/state/LocationProvider';
import { colors, radii, spacing, type as typography } from '../../src/theme/tokens';
import type { Coordenada } from '../../src/utils/geo';
import { META_OCORRENCIA, TIPOS_OCORRENCIA } from '../../src/utils/ocorrencia';

const MAX_DESCRICAO = 500;
const GAP = 8;

function CardTipo({
  tipo,
  selecionado,
  largura,
  onPress,
}: {
  tipo: TipoOcorrencia;
  selecionado: boolean;
  largura: number;
  onPress: () => void;
}) {
  const { rotulo, icone: Icone } = META_OCORRENCIA[tipo];
  const cor = selecionado ? colors.accent : colors.label;
  return (
    <Touchable
      onPress={onPress}
      activeOpacity={0.7}
      accessibilityRole="radio"
      accessibilityState={{ selected: selecionado, checked: selecionado }}
      accessibilityLabel={rotulo}
      style={[styles.card, { width: largura }, selecionado && styles.cardSelecionado]}
    >
      <Icone size={26} color={cor} strokeWidth={selecionado ? 2 : 1.7} />
      <Text variant="captionStrong" color={cor} style={styles.cardRotulo}>
        {rotulo}
      </Text>
    </Touchable>
  );
}

export default function NovaOcorrenciaScreen() {
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  const { usuario, carregando: carregandoAuth } = useAuth();
  const { posicao, centro } = useLocation();
  const criar = useCriarOcorrencia();

  const [tipo, setTipo] = useState<TipoOcorrencia | null>(null);
  const [descricao, setDescricao] = useState('');
  const [ponto, setPonto] = useState<Coordenada | null>(posicao);
  const [ajustado, setAjustado] = useState(false);
  const [mapaAberto, setMapaAberto] = useState(false);
  const { endereco, carregando: carregandoEndereco } = useEndereco(ponto);

  // Quando a posição chega depois de abrir o modal, usa-a (se o usuário não ajustou).
  useEffect(() => {
    if (!ajustado && posicao && !ponto) setPonto(posicao);
  }, [posicao, ajustado, ponto]);

  // O login é exigido para reportar.
  useEffect(() => {
    if (!carregandoAuth && !usuario) router.replace({ pathname: '/login', params: { next: '/reportar' } });
  }, [carregandoAuth, usuario]);

  const larguraCard = (width - spacing.gutter * 2 - GAP * 2) / 3;
  const podeEnviar = tipo !== null && ponto !== null && !criar.isPending;

  const enviar = () => {
    if (!tipo || !ponto) return;
    const texto = descricao.trim();
    criar.mutate(
      { tipo, latitude: ponto.latitude, longitude: ponto.longitude, ...(texto ? { descricao: texto } : {}) },
      {
        onSuccess: (o) => {
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => undefined);
          router.replace({
            pathname: '/reportar/enviada',
            params: {
              tipo: o.tipo,
              rua: endereco?.rua ?? 'este local',
              lat: String(o.latitude),
              lng: String(o.longitude),
            },
          });
        },
        onError: () => {
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error).catch(() => undefined);
        },
      },
    );
  };

  const subtituloLocal = [ajustado ? 'Local ajustado no mapa' : posicao ? 'Localização atual' : 'Toque para escolher', endereco?.bairro]
    .filter(Boolean)
    .join(' · ');

  return (
    <View style={styles.container}>
      <ModalNavBar
        title="Nova ocorrência"
        leftLabel="Cancelar"
        onLeft={() => router.back()}
        rightLabel="Enviar"
        onRight={enviar}
        rightDisabled={!podeEnviar}
      />
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          contentContainerStyle={[styles.conteudo, { paddingBottom: insets.bottom + 24 }]}
          keyboardShouldPersistTaps="handled"
        >
          <Text variant="caption" color={colors.secondaryLabel} style={styles.secaoTitulo}>
            O QUE ACONTECEU?
          </Text>
          <View style={styles.grade} accessibilityRole="radiogroup">
            {TIPOS_OCORRENCIA.map((t) => (
              <CardTipo key={t} tipo={t} selecionado={tipo === t} largura={larguraCard} onPress={() => setTipo(t)} />
            ))}
          </View>

          <GroupedList header="Local" style={styles.secao} separatorInset={56}>
            <Row
              title={
                ponto
                  ? carregandoEndereco && !endereco
                    ? 'Buscando endereço…'
                    : endereco?.rua ?? 'Endereço não identificado'
                  : 'Escolher no mapa'
              }
              subtitle={subtituloLocal}
              left={<IconTile icon={MapPin} color={colors.accent} />}
              right={carregandoEndereco ? <ActivityIndicator size="small" color={colors.tertiary} /> : undefined}
              chevron
              onPress={() => setMapaAberto(true)}
              accessibilityHint="Abre o mapa para ajustar o local"
            />
          </GroupedList>

          <Text variant="caption" color={colors.secondaryLabel} style={[styles.secaoTitulo, styles.secao]}>
            DESCRIÇÃO
          </Text>
          <View style={styles.descricaoBox}>
            <TextInput
              value={descricao}
              onChangeText={setDescricao}
              maxLength={MAX_DESCRICAO}
              multiline
              placeholder="Conte o que você viu (opcional)"
              placeholderTextColor={colors.tertiary}
              style={styles.descricao}
              textAlignVertical="top"
              accessibilityLabel="Descrição"
            />
            <Text variant="caption" color={colors.tertiary} style={styles.contador}>
              {descricao.length}/{MAX_DESCRICAO}
            </Text>
          </View>

          {criar.error ? (
            <Text variant="secondary" color={colors.redText} style={styles.erro} accessibilityLiveRegion="polite">
              {mensagemDeErro(criar.error)}
            </Text>
          ) : null}

          <PrimaryButton
            title="Enviar ocorrência"
            onPress={enviar}
            disabled={tipo === null || ponto === null}
            loading={criar.isPending}
            style={styles.botao}
            accessibilityHint={tipo === null ? 'Escolha o tipo de ocorrência primeiro' : undefined}
          />
        </ScrollView>
      </KeyboardAvoidingView>

      <AjustarLocal
        visible={mapaAberto}
        inicial={ponto ?? centro}
        onCancel={() => setMapaAberto(false)}
        onConfirm={(c) => {
          setPonto(c);
          setAjustado(true);
          setMapaAberto(false);
        }}
      />
    </View>
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
  conteudo: {
    paddingHorizontal: spacing.gutter,
    paddingTop: 20,
  },
  secaoTitulo: {
    paddingHorizontal: spacing.rowPadding,
    marginBottom: 6,
  },
  secao: {
    marginTop: 24,
  },
  grade: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: GAP,
  },
  card: {
    height: 84,
    borderRadius: radii.grouped,
    backgroundColor: colors.groupedBg,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  cardSelecionado: {
    borderColor: colors.accent,
    backgroundColor: colors.accentTint,
  },
  cardRotulo: {
    marginTop: 6,
  },
  descricaoBox: {
    backgroundColor: colors.groupedBg,
    borderRadius: radii.grouped,
    paddingHorizontal: spacing.rowPadding,
    paddingTop: 12,
    paddingBottom: 8,
  },
  descricao: {
    ...typography.body,
    color: colors.label,
    minHeight: 96,
    padding: 0,
  },
  contador: {
    textAlign: 'right',
    marginTop: 4,
  },
  erro: {
    marginTop: 16,
    textAlign: 'center',
  },
  botao: {
    marginTop: 24,
  },
});
