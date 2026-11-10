import { router, useLocalSearchParams } from 'expo-router';
import { ArrowUpDown, ChevronLeft, CircleAlert, ShieldCheck, TriangleAlert, type LucideIcon } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { Keyboard, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ApiError, mensagemDeErro } from '../../src/api/client';
import { useAvaliarRota } from '../../src/api/hooks';
import { buscarLugares, type Lugar } from '../../src/api/lugares';
import type { Classificacao, ModoTransporte, Ponto, RotaAvaliada } from '../../src/api/types';
import { PrimaryButton } from '../../src/components/Buttons';
import { GroupedList } from '../../src/components/GroupedList';
import { Touchable } from '../../src/components/Pressable';
import { SegmentedControl } from '../../src/components/SegmentedControl';
import { ErrorState, SkeletonList } from '../../src/components/StateViews';
import { MENSAGEM_FORA_DE_SP, SugestoesEndereco } from '../../src/components/SugestoesEndereco';
import { Text } from '../../src/components/Text';
import { useLocation } from '../../src/state/LocationProvider';
import { useRotas, type Planejamento } from '../../src/state/RotaStore';
import { colors, hairline, spacing, type as typography } from '../../src/theme/tokens';
import { metrosParaKm, segundosParaTexto } from '../../src/utils/format';
import { corTextoClassificacao } from '../../src/utils/incidente';

const MINHA_LOCALIZACAO = 'Minha localização';

const MODOS: { value: ModoTransporte; label: string }[] = [
  { value: 'CARRO', label: 'Carro' },
  { value: 'A_PE', label: 'A pé' },
  { value: 'BICICLETA', label: 'Bicicleta' },
];

type Campo = { texto: string; ponto: Ponto | null };
type NomeCampo = 'origem' | 'destino';

const ICONE_STATUS: Record<Classificacao, LucideIcon> = {
  SEGURA: ShieldCheck,
  ATENCAO: TriangleAlert,
  EVITE: CircleAlert,
};

function textoStatus(r: RotaAvaliada, primeira: boolean): string {
  if (r.classificacao === 'SEGURA') return primeira ? 'Trajeto mais seguro' : 'Trajeto seguro';
  if (r.classificacao === 'ATENCAO') return 'Atenção no trajeto';
  return 'Risco no trajeto';
}

async function resolverPonto(campo: Campo, posicao: Ponto | null, nome: string): Promise<Ponto> {
  if (campo.ponto) return campo.ponto;
  const texto = campo.texto.trim();
  if (!texto || texto === MINHA_LOCALIZACAO) {
    if (!posicao) throw new Error('Não conseguimos sua localização. Digite um endereço de origem.');
    return posicao;
  }
  // Texto digitado sem escolher uma sugestão: vale o primeiro endereço encontrado na cidade.
  const { lugares, foraDeSaoPaulo } = await buscarLugares(texto);
  const achado = lugares[0];
  if (!achado) {
    throw new Error(
      foraDeSaoPaulo
        ? `O endereço de ${nome} fica fora de São Paulo. ${MENSAGEM_FORA_DE_SP}`
        : `Não encontramos o endereço de ${nome} em São Paulo.`,
    );
  }
  return { latitude: achado.latitude, longitude: achado.longitude };
}

function LinhaRota({
  rota,
  numero,
  onPress,
}: {
  rota: RotaAvaliada;
  numero: number;
  onPress: () => void;
}) {
  const primeira = numero === 1;
  const Icone = ICONE_STATUS[rota.classificacao];
  const cor = corTextoClassificacao(rota.classificacao);
  const status = textoStatus(rota, primeira);
  const nome = `Rota ${numero}`;
  return (
    <Touchable
      onPress={onPress}
      activeOpacity={0.6}
      accessibilityRole="button"
      accessibilityLabel={`${nome}, ${segundosParaTexto(rota.duracaoSegundos)}, ${metrosParaKm(rota.distanciaMetros)}, ${status}`}
      style={styles.rotaLinha}
    >
      <View style={styles.rotaTopo}>
        <Text variant="bodyStrong">{nome}</Text>
        <Text variant="bodyStrong">{segundosParaTexto(rota.duracaoSegundos)}</Text>
      </View>
      <View style={styles.rotaBase}>
        <View style={styles.status}>
          <Icone size={15} color={cor} strokeWidth={2.2} />
          <Text variant="captionStrong" color={cor}>
            {status}
          </Text>
        </View>
        <Text variant="caption" color={colors.secondaryLabel}>
          {metrosParaKm(rota.distanciaMetros)}
        </Text>
      </View>
    </Touchable>
  );
}

export default function PlanejarRotaScreen() {
  const insets = useSafeAreaInsets();
  const { posicao } = useLocation();
  const { planejamento, definirPlanejamento } = useRotas();
  const avaliar = useAvaliarRota();
  const params = useLocalSearchParams<{ destLat?: string; destLng?: string; destLabel?: string }>();

  const [origem, setOrigem] = useState<Campo>({ texto: MINHA_LOCALIZACAO, ponto: null });
  const [destino, setDestino] = useState<Campo>({ texto: '', ponto: null });
  const [modo, setModo] = useState<ModoTransporte>('CARRO');
  const [erroLocal, setErroLocal] = useState<string | null>(null);
  const [resolvendo, setResolvendo] = useState(false);
  /** Campo em edição: as sugestões aparecem para ele até um endereço ser escolhido. */
  const [editando, setEditando] = useState<NomeCampo | null>(null);

  // Destino vindo da busca no mapa ("Planejar rota até aqui").
  useEffect(() => {
    const lat = Number(params.destLat);
    const lng = Number(params.destLng);
    if (params.destLat && Number.isFinite(lat) && Number.isFinite(lng)) {
      setDestino({ texto: params.destLabel ?? 'Destino', ponto: { latitude: lat, longitude: lng } });
    }
  }, [params.destLat, params.destLng, params.destLabel]);

  const campoEditado = editando === 'origem' ? origem : editando === 'destino' ? destino : null;
  const sugerir = campoEditado !== null && campoEditado.ponto === null && campoEditado.texto !== MINHA_LOCALIZACAO;

  const escolherLugar = (lugar: Lugar) => {
    const campo: Campo = { texto: lugar.titulo, ponto: { latitude: lugar.latitude, longitude: lugar.longitude } };
    if (editando === 'origem') setOrigem(campo);
    else setDestino(campo);
    setEditando(null);
    setErroLocal(null);
    Keyboard.dismiss();
  };

  const inverter = () => {
    setOrigem(destino.texto ? destino : { texto: '', ponto: null });
    setDestino(origem);
  };

  const executar = async () => {
    Keyboard.dismiss();
    setEditando(null);
    setErroLocal(null);
    avaliar.reset();
    if (!destino.texto.trim()) {
      setErroLocal('Informe o destino.');
      return;
    }
    setResolvendo(true);
    let o: Ponto;
    let d: Ponto;
    try {
      o = await resolverPonto(origem, posicao, 'origem');
      d = await resolverPonto(destino, posicao, 'destino');
    } catch (e) {
      setErroLocal(mensagemDeErro(e));
      setResolvendo(false);
      return;
    }
    setResolvendo(false);
    avaliar.mutate(
      { origem: o, destino: d, modo },
      {
        onSuccess: (rotas) => {
          const p: Planejamento = {
            origem: o,
            destino: d,
            origemLabel: origem.texto.trim() || MINHA_LOCALIZACAO,
            destinoLabel: destino.texto.trim(),
            modo,
            rotas,
          };
          definirPlanejamento(p);
        },
      },
    );
  };

  const escolher = (rota: RotaAvaliada) => {
    const indice = String(rota.indice);
    if (rota.classificacao === 'SEGURA') router.push({ pathname: '/rota/alternativa', params: { segura: indice } });
    else router.push({ pathname: '/rota/alerta', params: { indice } });
  };

  const erroApi = avaliar.error
    ? avaliar.error instanceof ApiError && avaliar.error.status === 503
      ? 'Não foi possível calcular rotas agora'
      : mensagemDeErro(avaliar.error)
    : null;
  const carregando = resolvendo || avaliar.isPending;
  const rotas = planejamento?.rotas ?? [];

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView
        style={styles.flex}
        contentContainerStyle={[styles.conteudo, { paddingTop: insets.top + 4 }]}
        keyboardShouldPersistTaps="handled"
      >
        <Touchable
          onPress={() => router.navigate('/')}
          style={styles.voltar}
          accessibilityRole="button"
          accessibilityLabel="Voltar ao mapa"
        >
          <ChevronLeft size={24} color={colors.accent} strokeWidth={2.2} />
          <Text variant="body" color={colors.accent}>
            Mapa
          </Text>
        </Touchable>

        <Text variant="largeTitle" accessibilityRole="header" style={styles.titulo}>
          Planejar rota
        </Text>

        <View style={styles.camposWrap}>
          <GroupedList separatorInset={40}>
            <View style={styles.campo}>
              <View style={[styles.ponto, { backgroundColor: colors.blue }]} />
              <TextInput
                value={origem.texto}
                onChangeText={(t) => setOrigem({ texto: t, ponto: null })}
                onFocus={() => {
                  setEditando('origem');
                  if (origem.texto === MINHA_LOCALIZACAO) setOrigem({ texto: '', ponto: null });
                }}
                onBlur={() => !origem.texto.trim() && setOrigem({ texto: MINHA_LOCALIZACAO, ponto: null })}
                placeholder="Origem"
                placeholderTextColor={colors.tertiary}
                style={[styles.input, origem.texto === MINHA_LOCALIZACAO && { color: colors.blue }]}
                accessibilityLabel="Origem"
                returnKeyType="next"
              />
            </View>
            <View style={styles.campo}>
              <View style={[styles.ponto, { backgroundColor: colors.accent }]} />
              <TextInput
                value={destino.texto}
                onChangeText={(t) => setDestino({ texto: t, ponto: null })}
                onFocus={() => setEditando('destino')}
                placeholder="Destino"
                placeholderTextColor={colors.tertiary}
                style={styles.input}
                accessibilityLabel="Destino"
                returnKeyType="search"
                onSubmitEditing={executar}
              />
            </View>
          </GroupedList>
          <Touchable
            onPress={inverter}
            style={styles.inverter}
            accessibilityRole="button"
            accessibilityLabel="Inverter origem e destino"
          >
            <ArrowUpDown size={18} color={colors.accent} strokeWidth={2} />
          </Touchable>
        </View>

        <SugestoesEndereco texto={campoEditado?.texto ?? ''} ativo={sugerir} onEscolher={escolherLugar} />

        <View style={styles.secao}>
          <SegmentedControl options={MODOS} value={modo} onChange={setModo} accessibilityLabel="Meio de transporte" />
        </View>

        <PrimaryButton
          title="Avaliar segurança da rota"
          onPress={executar}
          loading={carregando}
          style={styles.botao}
        />

        {erroLocal ? (
          <Text variant="secondary" color={colors.redText} style={styles.erroLocal} accessibilityLiveRegion="polite">
            {erroLocal}
          </Text>
        ) : null}

        {carregando ? (
          <View style={styles.secao}>
            <Text variant="caption" color={colors.secondaryLabel} style={styles.header}>
              OPÇÕES DE TRAJETO
            </Text>
            <SkeletonList rows={3} />
          </View>
        ) : erroApi ? (
          <ErrorState message={erroApi} onRetry={executar} />
        ) : rotas.length > 0 ? (
          <View style={styles.secao}>
            <GroupedList
              header="Opções de trajeto"
              footer="O SafeMove cruza o trajeto com ocorrências recentes, trânsito e áreas de risco."
            >
              {rotas.map((r, i) => (
                <LinhaRota key={r.indice} rota={r} numero={i + 1} onPress={() => escolher(r)} />
              ))}
            </GroupedList>
          </View>
        ) : avaliar.isSuccess ? (
          <ErrorState message="Nenhuma rota encontrada entre esses pontos." />
        ) : null}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
    backgroundColor: colors.background,
  },
  conteudo: {
    paddingHorizontal: spacing.gutter,
    paddingBottom: 32,
  },
  voltar: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    minHeight: 44,
    marginLeft: -8,
    paddingRight: 8,
  },
  titulo: {
    marginTop: 2,
    marginBottom: 16,
  },
  camposWrap: {
    position: 'relative',
  },
  campo: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: spacing.rowMinHeight,
    paddingLeft: spacing.rowPadding,
    paddingRight: 56,
  },
  ponto: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: 16,
  },
  input: {
    ...typography.body,
    flex: 1,
    color: colors.label,
    paddingVertical: 12,
  },
  inverter: {
    position: 'absolute',
    right: 6,
    top: spacing.rowMinHeight / 2 + 2,
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 22,
    backgroundColor: colors.background,
    borderWidth: hairline,
    borderColor: colors.separator,
  },
  secao: {
    marginTop: 20,
  },
  botao: {
    marginTop: 20,
  },
  erroLocal: {
    marginTop: 12,
    textAlign: 'center',
  },
  header: {
    paddingHorizontal: spacing.rowPadding,
    marginBottom: 6,
  },
  rotaLinha: {
    paddingHorizontal: spacing.rowPadding,
    paddingVertical: 12,
    minHeight: 64,
    justifyContent: 'center',
  },
  rotaTopo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  rotaBase: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 4,
  },
  status: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
});
