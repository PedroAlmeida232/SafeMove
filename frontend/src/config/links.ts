import { Alert, Linking } from 'react-native';

/** Política de privacidade publicada no GitHub (mesma URL informada na App Store Connect). */
export const URL_PRIVACIDADE = 'https://github.com/PedroAlmeida232/SafeMove/blob/main/docs/PRIVACIDADE.md';

/** E-mail que recebe denúncias e pedidos de suporte (definido no .env / eas.json). */
export const EMAIL_SUPORTE = process.env.EXPO_PUBLIC_SUPPORT_EMAIL?.trim() || null;

export async function abrirLink(url: string): Promise<void> {
  try {
    await Linking.openURL(url);
  } catch {
    Alert.alert('Não foi possível abrir', url);
  }
}

/** Abre o app de e-mail já com destinatário, assunto e corpo preenchidos. */
export async function escreverParaSuporte(assunto: string, corpo = ''): Promise<void> {
  if (!EMAIL_SUPORTE) {
    Alert.alert('Suporte indisponível', 'O e-mail de suporte não foi configurado neste build.');
    return;
  }
  const url = `mailto:${EMAIL_SUPORTE}?subject=${encodeURIComponent(assunto)}&body=${encodeURIComponent(corpo)}`;
  try {
    await Linking.openURL(url);
  } catch {
    Alert.alert('Nenhum app de e-mail', `Escreva para ${EMAIL_SUPORTE} com o assunto "${assunto}".`);
  }
}
