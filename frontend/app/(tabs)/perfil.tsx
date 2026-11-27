import { router } from 'expo-router';
import { LifeBuoy, LogOut, ShieldCheck, Trash2 } from 'lucide-react-native';
import { useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { mensagemDeErro } from '../../src/api/client';
import { useAuth } from '../../src/auth/AuthProvider';
import { Avatar } from '../../src/components/Avatar';
import { PrimaryButton } from '../../src/components/Buttons';
import { GroupedList } from '../../src/components/GroupedList';
import { IconTile } from '../../src/components/IconTile';
import { Row } from '../../src/components/Row';
import { ErrorState, Loading, Skeleton } from '../../src/components/StateViews';
import { Text } from '../../src/components/Text';
import { abrirLink, EMAIL_SUPORTE, escreverParaSuporte, URL_PRIVACIDADE } from '../../src/config/links';
import { useIdentidade } from '../../src/hooks/useIdentidade';
import { colors, spacing } from '../../src/theme/tokens';

export default function PerfilScreen() {
  const insets = useSafeAreaInsets();
  const { carregando, sair, excluirConta } = useAuth();
  const [excluindo, setExcluindo] = useState(false);
  const { logado, nome, email, iniciais, perfil } = useIdentidade();

  const confirmarSaida = () =>
    Alert.alert('Sair da conta?', 'Você ainda poderá ver o mapa sem entrar.', [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Sair', style: 'destructive', onPress: () => sair().catch(() => undefined) },
    ]);

  // Diretriz 5.1.1(v) da App Store: quem cria conta no app precisa conseguir excluí-la no app.
  const confirmarExclusao = () =>
    Alert.alert(
      'Excluir sua conta?',
      'Seus dados de cadastro serão apagados e você sairá do app. Essa ação não pode ser desfeita.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Excluir conta',
          style: 'destructive',
          onPress: async () => {
            setExcluindo(true);
            try {
              await excluirConta();
              Alert.alert('Conta excluída', 'Seus dados foram apagados.');
            } catch (e) {
              Alert.alert('Não foi possível excluir', mensagemDeErro(e));
            } finally {
              setExcluindo(false);
            }
          },
        },
      ],
    );

  const sobre = (
    <GroupedList header="Sobre" style={styles.lista} separatorInset={56}>
      <Row
        title="Política de privacidade"
        left={<IconTile icon={ShieldCheck} color={colors.blue} />}
        chevron
        onPress={() => abrirLink(URL_PRIVACIDADE)}
      />
      <Row
        title="Ajuda e suporte"
        subtitle={EMAIL_SUPORTE ?? undefined}
        left={<IconTile icon={LifeBuoy} color={colors.green} />}
        chevron
        onPress={() => escreverParaSuporte('Suporte SafeMove')}
      />
    </GroupedList>
  );

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.conteudo, { paddingTop: insets.top + 12 }]}
    >
      <Text variant="largeTitle" accessibilityRole="header">
        Perfil
      </Text>

      {carregando ? (
        <Loading />
      ) : !logado ? (
        <View style={styles.anonimo}>
          <Avatar initials="" size={88} />
          <Text variant="bodyStrong" style={styles.nome}>
            Você não entrou
          </Text>
          <Text variant="secondary" color={colors.secondaryLabel} style={styles.centro}>
            Entre para registrar ocorrências e confirmar o que outras pessoas viram.
          </Text>
          <PrimaryButton title="Entrar" onPress={() => router.push('/login')} style={styles.entrar} />
          {sobre}
        </View>
      ) : (
        <>
          <View style={styles.cabecalho}>
            <Avatar initials={iniciais} size={88} />
            {perfil.isPending && !nome ? (
              <>
                <Skeleton width={160} height={22} style={styles.nome} />
                <Skeleton width={200} height={15} style={styles.gap} />
              </>
            ) : (
              <>
                <Text variant="sheetTitle" style={styles.nome}>
                  {nome ?? 'Usuário SafeMove'}
                </Text>
                {email ? (
                  <Text variant="secondary" color={colors.secondaryLabel} style={styles.gap}>
                    {email}
                  </Text>
                ) : null}
              </>
            )}
          </View>

          {perfil.isError ? (
            <ErrorState
              message={`Não foi possível carregar seu perfil. ${mensagemDeErro(perfil.error)}`}
              onRetry={() => perfil.refetch()}
            />
          ) : null}

          <GroupedList style={styles.lista} separatorInset={56}>
            <Row
              title="Sair"
              titleColor={colors.redText}
              left={<IconTile icon={LogOut} color={colors.red} />}
              onPress={confirmarSaida}
            />
          </GroupedList>

          {sobre}

          <GroupedList style={styles.lista} separatorInset={56}>
            <Row
              title="Excluir conta"
              titleColor={colors.redText}
              left={<IconTile icon={Trash2} color={colors.red} />}
              right={excluindo ? <ActivityIndicator size="small" color={colors.tertiary} /> : undefined}
              onPress={excluindo ? undefined : confirmarExclusao}
              accessibilityHint="Apaga sua conta e seus dados de cadastro"
            />
          </GroupedList>
        </>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  conteudo: {
    paddingHorizontal: spacing.gutter,
    paddingBottom: 32,
  },
  cabecalho: {
    alignItems: 'center',
    marginTop: 24,
    marginBottom: 28,
  },
  anonimo: {
    alignItems: 'center',
    marginTop: 48,
  },
  nome: {
    marginTop: 14,
  },
  gap: {
    marginTop: 4,
  },
  centro: {
    textAlign: 'center',
    marginTop: 6,
    paddingHorizontal: 16,
  },
  entrar: {
    alignSelf: 'stretch',
    marginTop: 24,
  },
  lista: {
    marginTop: 8,
    alignSelf: 'stretch',
  },
});
