import type BottomSheet from '@gorhom/bottom-sheet';
import * as Location from 'expo-location';
import { router, useLocalSearchParams } from 'expo-router';
import { Camera, Car, Shield, ShieldCheck, TriangleAlert } from 'lucide-react-native';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Keyboard, Linking, StyleSheet, View } from 'react-native';
import MapView, { PROVIDER_DEFAULT } from 'react-native-maps';

import { mensagemDeErro } from '../../src/api/client';
import { useAreasRisco, useIncidentes, useOcorrencias, useRadares } from '../../src/api/hooks';
import { GroupedList } from '../../src/components/GroupedList';
import { IconTile } from '../../src/components/IconTile';
import { MapButtons } from '../../src/components/map/MapButtons';
import { MapPin } from '../../src/components/map/MapPin';
import { PinDetail, type Selecao } from '../../src/components/map/PinDetail';
import { RiskCircle } from '../../src/components/map/RiskCircle';
import { RouteLine } from '../../src/components/map/RouteLine';
import { UserDot } from '../../src/components/map/UserDot';
import { Pill } from '../../src/components/Pill';
import { Touchable } from '../../src/components/Pressable';
import { Row } from '../../src/components/Row';
import { SearchBar } from '../../src/components/SearchBar';
import { Sheet } from '../../src/components/Sheet';
import { SheetHeader } from '../../src/components/SheetHeader';
import { MENSAGEM_VAZIO } from '../../src/components/StateViews';
import { Switch } from '../../src/components/Switch';
import { Text } from '../../src/components/Text';
import { useIdentidade } from '../../src/hooks/useIdentidade';
import { useTopoFlutuante } from '../../src/hooks/useMapaBottom';
import { useLocation } from '../../src/state/LocationProvider';
import { useRotas } from '../../src/state/RotaStore';
import { colors, shadows } from '../../src/theme/tokens';
import { dentroDeSaoPaulo, distanciaMetros, type Coordenada } from '../../src/utils/geo';
import { corAreaRisco, corIncidente, GRAVIDADE_GRAVE, raioAreaRisco, rotuloCategoria } from '../../src/utils/incidente';
import { ICONE_INCIDENTE, META_OCORRENCIA } from '../../src/utils/ocorrencia';

const DELTA = { latitudeDelta: 0.025, longitudeDelta: 0.025 };
const RAIO_ALERTAS_M = 2000;
const SNAP_RECOLHIDO = 112;

type Camadas = { transito: boolean; areasRisco: boolean; ocorrencias: boolean; radares: boolean };

function Legenda() {
  const itens = [
    { cor: colors.green, rotulo: 'Livre' },
    { cor: colors.orange, rotulo: 'Moderado' },
    { cor: colors.red, rotulo: 'Intenso' },
  ];
  return (
    <View style={styles.legenda} accessible accessibilityLabel="Legenda: verde livre, laranja moderado, vermelho intenso">
      {itens.map((i) => (
        <View key={i.rotulo} style={styles.legendaItem}>
          <View style={[styles.legendaBarra, { backgroundColor: i.cor }]} />
          <Text variant="caption" color={colors.secondaryLabel}>
            {i.rotulo}
          </Text>
        </View>
      ))}
    </View>
  );
}

export default function MapaScreen() {
  const mapRef = useRef<MapView>(null);
  const sheetRef = useRef<BottomSheet>(null);
  const topo = useTopoFlutuante();
  const { posicao, centro, permissao, pedirPermissao } = useLocation();
  const { iniciais, logado } = useIdentidade();
  const { ativo } = useRotas();
  const params = useLocalSearchParams<{ focoLat?: string; focoLng?: string }>();

  const [camadas, setCamadas] = useState<Camadas>({
    transito: true,
    areasRisco: true,
    ocorrencias: true,
    radares: false,
  });
  const [selecao, setSelecao] = useState<Selecao | null>(null);
  const [busca, setBusca] = useState('');
  const [buscando, setBuscando] = useState(false);

  // Os dados de incidentes e ocorrências alimentam a pílula de alertas mesmo com a camada desligada.
  const incidentes = useIncidentes(centro);
  const ocorrencias = useOcorrencias(centro);
  const radares = useRadares(centro, camadas.radares);
  const areas = useAreasRisco(camadas.areasRisco);

  // Centraliza na posição real assim que ela chega pela primeira vez.
  const centralizouRef = useRef(false);
  useEffect(() => {
    if (posicao && !centralizouRef.current) {
      centralizouRef.current = true;
      mapRef.current?.animateToRegion({ ...posicao, ...DELTA }, 500);
    }
  }, [posicao]);

  // Foco vindo de outra tela (ex.: depois de enviar uma ocorrência).
  useEffect(() => {
    const lat = Number(params.focoLat);
    const lng = Number(params.focoLng);
    if (params.focoLat && Number.isFinite(lat) && Number.isFinite(lng)) {
      mapRef.current?.animateToRegion({ latitude: lat, longitude: lng, ...DELTA }, 500);
    }
  }, [params.focoLat, params.focoLng]);

  const totalAlertas = useMemo(() => {
    const graves = (incidentes.data ?? []).filter(
      (i) => i.gravidade >= GRAVIDADE_GRAVE && distanciaMetros(centro, i) <= RAIO_ALERTAS_M,
    ).length;
    return graves + (ocorrencias.data?.length ?? 0);
  }, [incidentes.data, ocorrencias.data, centro]);

  const selecionar = useCallback((s: Selecao) => {
    setSelecao(s);
    sheetRef.current?.snapToIndex(1);
  }, []);

  const fecharDetalhe = useCallback(() => setSelecao(null), []);

  const alternar = (chave: keyof Camadas) => (v: boolean) => setCamadas((c) => ({ ...c, [chave]: v }));

  const centralizar = () => {
    if (posicao) {
      mapRef.current?.animateToRegion({ ...posicao, ...DELTA }, 400);
      return;
    }
    Alert.alert(
      'Localização desativada',
      'Permita o acesso à localização para centralizar o mapa em você.',
      [
        { text: 'Agora não', style: 'cancel' },
        {
          text: 'Permitir',
          onPress: () => (permissao === 'negada' ? Linking.openSettings() : pedirPermissao()),
        },
      ],
    );
  };

  const abrirCamadas = () => {
    setSelecao(null);
    sheetRef.current?.snapToIndex(1);
  };

  const buscar = async () => {
    const termo = busca.trim();
    if (!termo) return;
    Keyboard.dismiss();
    setBuscando(true);
    try {
      const resultados = await Location.geocodeAsync(`${termo}, São Paulo, SP`);
      const achado = resultados.find((r) => dentroDeSaoPaulo(r));
      if (!achado) {
        Alert.alert('Endereço não encontrado', 'Não encontramos esse endereço na cidade de São Paulo.');
        return;
      }
      const coordenada: Coordenada = { latitude: achado.latitude, longitude: achado.longitude };
      let subtitulo = 'São Paulo, SP';
      try {
        const [end] = await Location.reverseGeocodeAsync(coordenada);
        if (end) subtitulo = [end.district, end.city ?? end.subregion].filter(Boolean).join(' · ') || subtitulo;
      } catch {
        // Mantém o subtítulo padrão.
      }
      mapRef.current?.animateToRegion({ ...coordenada, ...DELTA }, 500);
      selecionar({ tipo: 'lugar', titulo: termo, subtitulo, coordenada });
    } catch {
      Alert.alert('Busca indisponível', 'Não foi possível buscar o endereço agora. Tente de novo.');
    } finally {
      setBuscando(false);
    }
  };

  const erroDados = incidentes.error ?? ocorrencias.error;
  const carregandoAlertas = incidentes.isPending || ocorrencias.isPending;

  return (
    <View style={styles.container}>
      <MapView
        ref={mapRef}
        style={StyleSheet.absoluteFill}
        provider={PROVIDER_DEFAULT}
        initialRegion={{ ...centro, ...DELTA }}
        showsUserLocation={false}
        showsMyLocationButton={false}
        showsCompass={false}
        showsTraffic={camadas.transito}
        toolbarEnabled={false}
        pitchEnabled={false}
        onPress={() => Keyboard.dismiss()}
        mapPadding={{ top: 0, right: 0, bottom: SNAP_RECOLHIDO, left: 0 }}
      >
        {camadas.areasRisco
          ? (areas.data ?? []).map((a) => {
              const cor = corAreaRisco(a);
              return cor ? <RiskCircle key={a.distrito} center={a} radius={raioAreaRisco(a)} color={cor} /> : null;
            })
          : null}

        {ativo ? (
          <RouteLine
            coordinates={ativo.rota.pontos}
            variant={ativo.rota.classificacao === 'SEGURA' ? 'segura' : 'atual'}
          />
        ) : null}

        {camadas.transito
          ? (incidentes.data ?? []).map((i) => (
              <MapPin
                key={`i-${i.id}`}
                coordinate={i}
                color={corIncidente(i)}
                icon={ICONE_INCIDENTE[i.categoria]}
                selected={selecao?.tipo === 'incidente' && selecao.item.id === i.id}
                onPress={() => selecionar({ tipo: 'incidente', item: i })}
                accessibilityLabel={`${rotuloCategoria(i.categoria)}${i.via ? ` na ${i.via}` : ''}`}
              />
            ))
          : null}

        {camadas.ocorrencias
          ? (ocorrencias.data ?? []).map((o) => (
              <MapPin
                key={`o-${o.id}`}
                coordinate={o}
                color={META_OCORRENCIA[o.tipo].cor}
                icon={META_OCORRENCIA[o.tipo].icone}
                selected={selecao?.tipo === 'ocorrencia' && selecao.item.id === o.id}
                onPress={() => selecionar({ tipo: 'ocorrencia', item: o })}
                accessibilityLabel={`Ocorrência: ${META_OCORRENCIA[o.tipo].rotulo}`}
                zIndex={10}
              />
            ))
          : null}

        {camadas.radares
          ? (radares.data ?? []).map((r) => (
              <MapPin
                key={`r-${r.id}`}
                coordinate={r}
                color={colors.blue}
                icon={Camera}
                selected={selecao?.tipo === 'radar' && selecao.item.id === r.id}
                onPress={() => selecionar({ tipo: 'radar', item: r })}
                accessibilityLabel={`Radar${r.velocidadeMax ? `, ${r.velocidadeMax} km/h` : ''}`}
              />
            ))
          : null}

        {selecao?.tipo === 'lugar' ? (
          <MapPin
            coordinate={selecao.coordenada}
            color={colors.accent}
            icon={ShieldCheck}
            selected
            accessibilityLabel={selecao.titulo}
          />
        ) : null}

        {posicao ? <UserDot coordinate={posicao} /> : null}
      </MapView>

      <View style={[styles.topo, { top: topo }]} pointerEvents="box-none">
        <SearchBar
          value={busca}
          onChangeText={setBusca}
          onSubmit={buscar}
          onClear={() => {
            setBusca('');
            if (selecao?.tipo === 'lugar') setSelecao(null);
          }}
          loading={buscando}
          initials={logado ? iniciais : ''}
          onAvatarPress={() => router.navigate('/perfil')}
        />

        <View style={styles.linhaFlutuante} pointerEvents="box-none">
          <View style={styles.pilulaArea} pointerEvents="box-none">
            {carregandoAlertas ? (
              <View style={styles.pilulaCarregando} accessibilityLabel="Carregando alertas" accessible>
                <ActivityIndicator size="small" color={colors.tertiary} />
                <Text variant="captionStrong" color={colors.secondaryLabel}>
                  Buscando alertas…
                </Text>
              </View>
            ) : erroDados ? (
              <Touchable
                onPress={() => {
                  incidentes.refetch();
                  ocorrencias.refetch();
                }}
                style={styles.erro}
                accessibilityRole="button"
                accessibilityLabel={`${mensagemDeErro(erroDados)} Tentar de novo`}
              >
                <Text variant="captionStrong" color={colors.label} numberOfLines={2}>
                  {mensagemDeErro(erroDados)}
                </Text>
                <Text variant="captionStrong" color={colors.accent}>
                  Tentar de novo
                </Text>
              </Touchable>
            ) : totalAlertas > 0 ? (
              <Pill
                label={`${totalAlertas} ${totalAlertas === 1 ? 'alerta' : 'alertas'} perto de você`}
                icon={TriangleAlert}
                color={colors.red}
                onPress={() => setCamadas((c) => ({ ...c, ocorrencias: true, transito: true }))}
              />
            ) : null}
          </View>
          <MapButtons onCenter={centralizar} onLayers={abrirCamadas} />
        </View>
      </View>

      <Sheet ref={sheetRef} snapPoints={[SNAP_RECOLHIDO]} dynamic index={1} bottomPadding={16}>
        {selecao ? (
          <PinDetail selecao={selecao} onClose={fecharDetalhe} />
        ) : (
          <>
            <SheetHeader title="Camadas" subtitle="São Paulo · agora" />
            <GroupedList style={styles.lista} separatorInset={56}>
              <Row
                title="Trânsito"
                left={<IconTile icon={Car} color={colors.green} />}
                right={<Switch value={camadas.transito} onValueChange={alternar('transito')} accessibilityLabel="Trânsito" />}
              />
              <Row
                title="Áreas de risco"
                left={<IconTile icon={Shield} color={colors.red} />}
                right={<Switch value={camadas.areasRisco} onValueChange={alternar('areasRisco')} accessibilityLabel="Áreas de risco" />}
              />
              <Row
                title="Ocorrências"
                left={<IconTile icon={TriangleAlert} color={colors.orange} />}
                right={<Switch value={camadas.ocorrencias} onValueChange={alternar('ocorrencias')} accessibilityLabel="Ocorrências" />}
              />
              <Row
                title="Radares"
                left={<IconTile icon={Camera} color={colors.blue} />}
                right={<Switch value={camadas.radares} onValueChange={alternar('radares')} accessibilityLabel="Radares" />}
              />
            </GroupedList>
            <Legenda />
            {camadas.ocorrencias && ocorrencias.isSuccess && ocorrencias.data.length === 0 ? (
              <Text variant="caption" color={colors.secondaryLabel} style={styles.vazio}>
                {MENSAGEM_VAZIO}
              </Text>
            ) : null}
          </>
        )}
      </Sheet>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.groupedBg,
  },
  topo: {
    position: 'absolute',
    left: 16,
    right: 16,
  },
  linhaFlutuante: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 12,
  },
  pilulaArea: {
    flex: 1,
    marginRight: 12,
  },
  pilulaCarregando: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 7,
    paddingHorizontal: 12,
    borderRadius: 99,
    backgroundColor: colors.background,
    boxShadow: shadows.floating,
  },
  erro: {
    alignSelf: 'flex-start',
    backgroundColor: colors.background,
    borderRadius: 12,
    paddingVertical: 8,
    paddingHorizontal: 12,
    boxShadow: shadows.floating,
    gap: 2,
    maxWidth: 260,
  },
  lista: {
    marginHorizontal: 16,
  },
  legenda: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 20,
    marginTop: 14,
  },
  legendaItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  legendaBarra: {
    width: 18,
    height: 5,
    borderRadius: 2.5,
  },
  vazio: {
    textAlign: 'center',
    marginTop: 10,
    paddingHorizontal: 16,
  },
});
