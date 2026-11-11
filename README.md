# SafeMove

App de mobilidade urbana para São Paulo: trânsito em tempo real, ocorrências registradas pelos usuários, radares, avaliação de segurança de rotas e registro de ocorrências.

Expo (SDK 58) · TypeScript · expo-router · react-native-maps · Firebase Auth · TanStack Query · @gorhom/bottom-sheet · lucide-react-native.

## 1. Instalar

Pré-requisitos: Node 20 ou mais recente e o app **Expo Go** (SDK 58) no celular, ou um simulador/emulador.

```bash
npm install
```

## 2. Configurar o `.env`

```bash
cp .env.example .env
```

| Variável | O que é |
|---|---|
| `EXPO_PUBLIC_API_URL` | URL da API Java (veja a seção 4) |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | Console do Firebase › Configurações do projeto › Seus apps › app **Web** |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | idem (ex.: `meu-projeto.firebaseapp.com`) |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | idem |
| `EXPO_PUBLIC_FIREBASE_APP_ID` | idem |
| `EXPO_PUBLIC_USE_MOCK` | `true` usa os dados de exemplo de `src/mocks` (não chama a API) |

No Firebase, ative **Authentication › Método de login › E-mail/senha**.

Nenhuma chave fica no código: tudo vem do `.env`, que está no `.gitignore`.

### Modo demonstração

Com `EXPO_PUBLIC_USE_MOCK=true` o app responde com os dados de `src/mocks`, seguindo exatamente o contrato da API. Se as variáveis do Firebase estiverem vazias nesse modo, o login também é simulado: qualquer e-mail com senha de 6 ou mais caracteres funciona.

## 3. Rodar

```bash
npx expo start
```

Escaneie o QR code com o Expo Go (Android) ou com a câmera (iOS). Atalhos no terminal: `i` abre o simulador iOS e `a` abre o emulador Android.

Depois de mudar o `.env`, reinicie com cache limpo:

```bash
npx expo start -c
```

Verificar os tipos:

```bash
npm run typecheck
```

## 4. Apontar para a API local

A API Java precisa estar rodando (por padrão na porta 8080).

| Onde o app roda | `EXPO_PUBLIC_API_URL` |
|---|---|
| Simulador iOS | `http://localhost:8080` |
| Emulador Android | `http://10.0.2.2:8080` |
| Celular físico com Expo Go | `http://<IP-do-computador>:8080`, por exemplo `http://192.168.0.12:8080` |

No celular físico, o computador e o celular precisam estar na mesma rede Wi-Fi, e a API precisa escutar em todas as interfaces (`server.address=0.0.0.0` no Spring). O `app.json` já libera HTTP sem TLS na rede local (`NSAllowsLocalNetworking` no iOS e `usesCleartextTraffic` no Android).

As rotas marcadas com 🔒 recebem `Authorization: Bearer <idToken>` do Firebase. Os erros no formato ProblemDetail (`{ status, detail, campos? }`) aparecem para o usuário com o texto de `detail`.

## Estrutura

```
app/                      rotas (expo-router)
  _layout.tsx             providers: React Query, Auth, Localização, Alertas, Rota, GestureHandler, BottomSheet
  login.tsx               login e cadastro (modal)
  (tabs)/                 Mapa (01), Rotas (02), Alertas (05), Perfil; "Reportar" abre o modal
  rota/alerta.tsx         03 · rota com risco
  rota/alternativa.tsx    04 · rota mais segura e trajeto em andamento
  reportar/index.tsx      06 · nova ocorrência (modal)
  reportar/enviada.tsx    07 · ocorrência enviada
src/
  theme/tokens.ts         cores, tipografia, raios, sombras
  components/             componentes do design system (SearchBar, Pill, Sheet, GroupedList, Row, IconTile,
                          Switch, PrimaryButton, TextButton, StatBlock, map/MapPin, map/UserDot,
                          map/RiskCircle, map/RouteLine…)
  api/                    client.ts (fetch, token e ProblemDetail), hooks.ts (React Query), types.ts (contrato)
  auth/                   Firebase Auth com persistência em AsyncStorage
  state/                  localização, rota planejada/ativa e alertas
  hooks/useMonitorRota.ts notificações locais durante o trajeto
  mocks/                  dados de exemplo (só com EXPO_PUBLIC_USE_MOCK=true)
  utils/                  format.ts ("há X min", km, min), geo.ts, incidente.ts, ocorrencia.ts
```

## Comportamentos

- **Mapa**: centraliza na sua posição (ou na Praça da Sé, se a permissão for negada). Incidentes e ocorrências são recarregados a cada 60 s. A pílula "N alertas perto de você" soma os incidentes graves (gravidade 3 ou mais) e as ocorrências num raio de 2 km.
- **Login**: só é pedido ao tocar em **Reportar** ou em **Eu também vi**. O mapa e as rotas funcionam sem login.
- **Notificações (05)**: durante um trajeto iniciado, o app verifica a cada 60 s se há incidente grave a até 300 m da rota e reavalia a rota (`POST /rotas/avaliar`). Se a rota piorar para ATENCAO ou EVITE, dispara uma notificação local (expo-notifications). As notificações ficam listadas na aba Alertas.
- **Áreas de risco**: o backend classifica cada um dos distritos de São Paulo de 1 a 5 (`GET /areas-risco`) somando duas fontes: os assaltos reportados no app nos últimos 30 dias (0 → 1, 1–2 → 2, 3–5 → 3, 6–9 → 4, 10+ → 5) e, se houver, os roubos e furtos do último mês da SSP-SP, comparados entre os distritos em quintis. Vale o maior dos dois; até o nível 2 a região é segura. A SSP não tem API, então os dados oficiais entram por um CSV (`distrito;mes;roubos;furtos`, ex.: `Sé;2026-10;412;980`) indicado em `SSP_ESTATISTICAS_CSV` no `backend/.env`, relido a cada hora.
- **Alerta de área de risco**: com a localização concedida, o app chama `GET /areas-risco/avaliar` ao se deslocar 250 m ou a cada 10 min. Se a região não for segura, adiciona um alerta na aba Alertas e dispara uma notificação local, no máximo uma vez a cada 3 h por distrito.
- **Círculos de risco na tela 03**: a API não envia coordenadas para os `alertas`, que são só texto. Por isso, os círculos marcam as ocorrências recentes a até 150 m do trajeto (`GET /ocorrencias`) e as áreas de risco de nível 3 ou mais cruzadas pela rota (`GET /areas-risco`).

## Observações para builds fora do Expo Go

- Android: o Google Maps precisa de uma chave em `android.config.googleMaps.apiKey` no `app.json` (no Expo Go não precisa).
- Gere o build de desenvolvimento com `npx expo run:ios`, `npx expo run:android` ou `eas build`.
