# Publicar o SafeMove na App Store

Passo a passo para levar o app (`frontend/`) até a App Store usando o EAS (serviço de build do Expo). Não precisa de Mac.

## 0. O que já está pronto no código

| Item | Onde |
|---|---|
| Bundle ID `com.pedroalmeida232.safemove` | `frontend/app.json` |
| Declaração de criptografia (não usa criptografia própria) | `ios.config.usesNonExemptEncryption: false` |
| Privacy Manifest (dados coletados e APIs usadas) | `ios.privacyManifests` em `frontend/app.json` |
| Perfis de build com a API do Render e sem dados de exemplo | `frontend/eas.json` |
| Excluir conta no app (Diretriz 5.1.1(v)) | Perfil › Excluir conta (`DELETE /usuarios/me`) |
| Política de privacidade e suporte no app | Perfil › Sobre e tela de cadastro |
| Denunciar e ocultar ocorrências (Diretriz 1.2) | Card da ocorrência no mapa |
| Filtro de palavras ofensivas na descrição | `frontend/src/utils/moderacao.ts` |
| Esqueci minha senha | Tela de login (`POST /auth/recuperar-senha`) |
| Fora de São Paulo, o mapa abre no centro da cidade (o revisor fica nos EUA) | `frontend/src/state/LocationProvider.tsx` |
| Comandos | `npm run build:ios`, `npm run submit:ios` |

O Bundle ID precisa ser único na App Store. Se quiser outro, troque em `app.json` **antes** do primeiro build. Depois de publicado, ele não muda mais.

## 1. Resolva antes de enviar (senão a Apple recusa)

1. **Endpoints que faltam na API.** O app chama `POST /rotas/avaliar`, `GET /radares` e `GET /areas-risco`, que não existem no backend. Sem eles, a aba Rotas e o monitoramento do trajeto não funcionam, e a Apple recusa por app incompleto (Diretriz 2.1).
2. **Servidor dormindo.** No plano Free, o Render desliga a API após 15 min parado e leva uns 50 s para voltar. O app desiste da chamada em 20 s, então o revisor vai ver "Sem conexão com o servidor". Durante a revisão, use o plano Starter (US$ 7/mês) ou um serviço que chame a API a cada 10 min.
3. **E-mail de suporte.** Troque `PREENCHER_EMAIL_DE_SUPORTE` pelo e-mail real em `frontend/eas.json` e em `docs/PRIVACIDADE.md`. Ele recebe as denúncias.
4. **Política de privacidade publicada.** Faça o push de `docs/PRIVACIDADE.md` na `main`. A URL usada no app e na App Store Connect é `https://github.com/PedroAlmeida232/SafeMove/blob/main/docs/PRIVACIDADE.md`. O repositório precisa ser público.
5. **Exclusão de conta no backend.** Confirme que `DELETE /usuarios/me` apaga o usuário no Firebase e as ocorrências e confirmações dele, como diz a política.
6. **Conta de teste.** Crie uma conta (ex.: `revisao.apple@...`) para o revisor da Apple entrar.

## 2. Contas

1. **Apple Developer Program:** https://developer.apple.com/programs/ , US$ 99 por ano. A conta individual fica no nome de quem paga, e esse nome aparece na App Store como desenvolvedor. A aprovação leva de 1 a 2 dias.
2. **Expo:** crie uma conta grátis em https://expo.dev/signup.

## 3. Primeiro build

Na pasta `frontend/`:

```bash
npm install -g eas-cli
eas login
eas init                  # liga o projeto à sua conta Expo (grava o projectId no app.json)
npm run build:ios         # eas build --platform ios --profile production
```

Na primeira vez, o EAS pede o login da Apple e cria sozinho o certificado e o provisioning profile. Responda "Yes" quando ele oferecer. O build roda na nuvem do Expo e leva de 15 a 30 minutos.

## 4. Enviar para a Apple

```bash
npm run submit:ios        # eas submit --platform ios --latest
```

O EAS cria o app na App Store Connect (se ainda não existir) e envia o build. Uns 15 minutos depois, o build aparece em **TestFlight**.

## 5. Testar no TestFlight

1. Em https://appstoreconnect.apple.com abra **Apps › SafeMove › TestFlight**.
2. Adicione o grupo (vocês 4) em **Internal Testing**.
3. Instale o **TestFlight** no iPhone e teste tudo: login, cadastro, excluir conta, mapa, rotas, reportar, denunciar.

## 6. Preencher a App Store Connect

### Informações do app

| Campo | Valor sugerido |
|---|---|
| Nome (até 30) | SafeMove: rotas seguras em SP |
| Subtítulo (até 30) | Alertas de trânsito e risco |
| Categoria principal | Navegação |
| Categoria secundária | Utilidades |
| URL de suporte | https://github.com/PedroAlmeida232/SafeMove |
| URL da política de privacidade | https://github.com/PedroAlmeida232/SafeMove/blob/main/docs/PRIVACIDADE.md |
| Direitos autorais | 2026 SafeMove |
| Disponibilidade | Brasil (o app só cobre São Paulo) |
| Preço | Gratuito |

**Palavras-chave (até 100 caracteres):**

```
trânsito,rota segura,alerta,assalto,buraco,alagamento,radar,acidente,mobilidade,são paulo,denúncia
```

**Texto promocional:**

```
Veja o trânsito, os alertas da comunidade e o risco do seu trajeto antes de sair de casa.
```

**Descrição:**

```
O SafeMove ajuda você a se deslocar por São Paulo com mais segurança.

• Mapa em tempo real: acidentes, congestionamentos, obras, alagamentos e radares perto de você.
• Rotas avaliadas: compare caminhos e veja uma nota de segurança para cada um.
• Alertas no trajeto: durante o percurso, o app avisa se surgir um incidente grave na sua rota.
• Ocorrências da comunidade: registre assaltos, buracos, falta de iluminação e outros problemas, e confirme o que outras pessoas viram.

Você pode usar o mapa sem criar conta. Para registrar ocorrências, basta um e-mail.

Projeto da Global Solution 2026 da FIAP.
```

### Classificação etária

No questionário, marque **Conteúdo gerado por usuários: Sim** (as descrições das ocorrências). Responda "Nenhum" no resto. O resultado esperado é 12+ ou próximo disso.

### Privacidade do app (rótulo de privacidade)

Em **App Privacy**, responda "Sim, coletamos dados" e marque:

| Tipo de dado | Ligado à identidade | Usado para rastreamento | Finalidade |
|---|---|---|---|
| Nome | Sim | Não | Funcionalidade do app |
| Endereço de e-mail | Sim | Não | Funcionalidade do app |
| ID do usuário | Sim | Não | Funcionalidade do app |
| Localização precisa | Sim | Não | Funcionalidade do app |
| Outro conteúdo do usuário | Sim | Não | Funcionalidade do app |

Essas respostas são as mesmas do Privacy Manifest em `app.json`. Se mudar uma, mude a outra.

### Capturas de tela

- Obrigatório: **iPhone 6,9"** (1320 × 2868 px). Use o simulador do iPhone 17 Pro Max ou um aparelho grande e tire de 3 a 10 prints.
- Sugestão de telas: Mapa com alertas, Rotas com nota de segurança, Rota com risco, Nova ocorrência, Alertas.
- iPad não é necessário (`supportsTablet: false`).

### Informações para a revisão

Em **App Review Information**:

- **Login necessário:** Sim. Informe o e-mail e a senha da conta de teste.
- **Notas** (podem ir em inglês):

```
SafeMove shows traffic incidents and community reports for the city of São Paulo, Brazil.
The map works without an account. Log in with the demo account to report an occurrence
(tab "Reportar") or confirm one ("Eu também vi").
Account deletion: Perfil > Excluir conta.
Report or hide user content: tap any community pin on the map > "Denunciar ou ocultar".
Location is only used while the app is in use. If you are outside São Paulo, the map
centers on Praça da Sé so you can still see sample data.
```

## 7. Enviar para revisão

1. Em **Distribuição › iOS App › 1.0**, escolha o build do TestFlight.
2. Clique em **Adicionar para revisão** e depois em **Enviar para revisão**.
3. A revisão costuma levar de 1 a 3 dias. Se recusarem, a mensagem diz a diretriz. Corrija, rode `npm run build:ios` e `npm run submit:ios` de novo e responda pela própria App Store Connect.

## 8. Próximas versões

1. Aumente `version` em `frontend/app.json` (ex.: `1.0.1`). O número do build sobe sozinho (`autoIncrement` no `eas.json`).
2. `npm run build:ios` e `npm run submit:ios`.
3. Crie a nova versão na App Store Connect, escolha o build e envie para revisão.

## Motivos comuns de recusa

| Diretriz | Problema | Como evitar |
|---|---|---|
| 2.1 | App trava, tela vazia ou erro de servidor | API com os endpoints completos e sem dormir |
| 2.1 | Revisor não consegue entrar | Conta de teste válida nas notas |
| 5.1.1(v) | Não dá para excluir a conta | Já resolvido: Perfil › Excluir conta |
| 5.1.1 | Texto de permissão de localização vago | Já está explicado no `app.json` |
| 1.2 | Conteúdo de usuário sem denúncia ou filtro | Já resolvido: denunciar, ocultar e filtro de palavras |
| 4.2 | App parece só um site ou protótipo | Não envie com dados de exemplo (`EXPO_PUBLIC_USE_MOCK=false` no `eas.json`) |
