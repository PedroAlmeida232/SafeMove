import { router, useLocalSearchParams } from 'expo-router';
import { useRef, useState, type ComponentProps } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, TextInput, View, type TextInputInstance } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { mensagemDeErro, USE_MOCK } from '../src/api/client';
import { useAuth } from '../src/auth/AuthProvider';
import { AppLogo } from '../src/components/AppLogo';
import { PrimaryButton, TextButton } from '../src/components/Buttons';
import { GroupedList } from '../src/components/GroupedList';
import { Text } from '../src/components/Text';
import { colors, spacing, type as typography } from '../src/theme/tokens';

type Modo = 'entrar' | 'criar';

function Campo(props: ComponentProps<typeof TextInput> & { label: string }) {
  const { label, ...rest } = props;
  return (
    <View style={styles.campo}>
      <Text variant="body" style={styles.campoLabel}>
        {label}
      </Text>
      <TextInput
        placeholderTextColor={colors.tertiary}
        style={styles.input}
        accessibilityLabel={label}
        {...rest}
      />
    </View>
  );
}

export default function LoginScreen() {
  const insets = useSafeAreaInsets();
  const { next } = useLocalSearchParams<{ next?: string }>();
  const { entrar, criarConta } = useAuth();

  const [modo, setModo] = useState<Modo>('entrar');
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const senhaRef = useRef<TextInputInstance>(null);
  const emailRef = useRef<TextInputInstance>(null);

  const criando = modo === 'criar';

  const continuar = () => {
    if (next === '/reportar') router.replace('/reportar');
    else if (router.canGoBack()) router.back();
    else router.replace('/');
  };

  const enviar = async () => {
    setErro(null);
    if (!email.trim() || !senha) {
      setErro('Preencha e-mail e senha.');
      return;
    }
    if (criando && senha.length < 6) {
      setErro('A senha precisa ter pelo menos 6 caracteres.');
      return;
    }
    setEnviando(true);
    try {
      if (criando) await criarConta(nome, email, senha);
      else await entrar(email, senha);
      continuar();
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView
        style={styles.flex}
        contentContainerStyle={[styles.conteudo, { paddingTop: insets.top + 24, paddingBottom: insets.bottom + 24 }]}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.fechar}>
          <TextButton title="Agora não" weight="regular" onPress={() => (router.canGoBack() ? router.back() : router.replace('/'))} />
        </View>

        <View style={styles.marca}>
          <AppLogo size={72} />
          <Text variant="largeTitle" accessibilityRole="header" style={styles.titulo}>
            SafeMove
          </Text>
          <Text variant="secondary" color={colors.secondaryLabel} style={styles.subtitulo}>
            {criando
              ? 'Crie sua conta para registrar ocorrências e ajudar quem passa por São Paulo.'
              : next === '/reportar'
                ? 'Entre para registrar uma ocorrência.'
                : 'Entre para registrar e confirmar ocorrências.'}
          </Text>
        </View>

        <GroupedList separatorInset={spacing.rowPadding}>
          {criando ? (
            <Campo
              label="Nome"
              value={nome}
              onChangeText={setNome}
              placeholder="Seu nome"
              autoComplete="name"
              textContentType="name"
              returnKeyType="next"
              onSubmitEditing={() => emailRef.current?.focus()}
            />
          ) : null}
          <Campo
            ref={emailRef}
            label="E-mail"
            value={email}
            onChangeText={setEmail}
            placeholder="voce@email.com"
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            textContentType="emailAddress"
            returnKeyType="next"
            onSubmitEditing={() => senhaRef.current?.focus()}
          />
          <Campo
            ref={senhaRef}
            label="Senha"
            value={senha}
            onChangeText={setSenha}
            placeholder={criando ? 'Mínimo de 6 caracteres' : 'Obrigatória'}
            secureTextEntry
            autoComplete={criando ? 'new-password' : 'current-password'}
            textContentType={criando ? 'newPassword' : 'password'}
            returnKeyType="go"
            onSubmitEditing={enviar}
          />
        </GroupedList>

        {erro ? (
          <Text variant="secondary" color={colors.redText} style={styles.erro} accessibilityLiveRegion="polite">
            {erro}
          </Text>
        ) : null}

        {USE_MOCK ? (
          <Text variant="caption" color={colors.secondaryLabel} style={styles.demo}>
            Modo demonstração: qualquer e-mail e senha com 6+ caracteres funcionam.
          </Text>
        ) : null}

        <PrimaryButton
          title={criando ? 'Criar conta' : 'Entrar'}
          onPress={enviar}
          loading={enviando}
          style={styles.botao}
        />
        <TextButton
          title={criando ? 'Já tenho conta' : 'Criar conta'}
          onPress={() => {
            setErro(null);
            setModo(criando ? 'entrar' : 'criar');
          }}
          style={styles.alternar}
        />
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
    flexGrow: 1,
  },
  fechar: {
    position: 'absolute',
    top: 8,
    right: spacing.gutter,
    zIndex: 1,
  },
  marca: {
    alignItems: 'center',
    marginTop: 24,
    marginBottom: 32,
  },
  titulo: {
    marginTop: 16,
  },
  subtitulo: {
    textAlign: 'center',
    marginTop: 6,
    paddingHorizontal: 24,
  },
  campo: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: spacing.rowMinHeight,
    paddingHorizontal: spacing.rowPadding,
  },
  campoLabel: {
    width: 72,
  },
  input: {
    ...typography.body,
    flex: 1,
    color: colors.label,
    paddingVertical: 12,
  },
  erro: {
    marginTop: 12,
    textAlign: 'center',
  },
  demo: {
    marginTop: 12,
    textAlign: 'center',
  },
  botao: {
    marginTop: 24,
  },
  alternar: {
    alignSelf: 'center',
    marginTop: 8,
  },
});
