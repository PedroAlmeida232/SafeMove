# Rodando o SafeMove em outro PC

O projeto tem duas partes independentes: `backend/` (Java/Spring Boot) e `frontend/` (app Expo/React Native). O frontend roda sozinho no modo demonstração, mas para ter dados reais você precisa dos dois.

## 1. Dependências a instalar

| Ferramenta | Versão | Para quê |
|---|---|---|
| Git | qualquer | clonar o repositório |
| JDK | **21** | backend (`java.version=21` no `pom.xml`) |
| Node.js | **20 ou mais recente** (inclui npm) | frontend |
| Expo Go (celular) | compatível com **SDK 58** | ver o app no celular. Alternativa: emulador Android ou simulador iOS |

Não é preciso instalar o Maven: o projeto usa o wrapper (`mvnw`). As bibliotecas do backend (Spring Boot 4.1.2, JPA, Security, H2, driver Oracle, springdoc) e do frontend (Expo, React Native etc.) são baixadas automaticamente nos passos abaixo.

Ubuntu/Debian:

```bash
sudo apt update && sudo apt install -y git openjdk-21-jdk
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
# abra um novo terminal e rode:
nvm install 22
```

Windows: instale o JDK 21 (Temurin) e o Node LTS pelos instaladores oficiais.

Confira com `java -version` e `node -v`.

## 2. Baixar o projeto

```bash
git clone <URL-do-repositorio> SafeMove
cd SafeMove
git checkout feature/incidentes-tomtom
```

## 3. Backend

```bash
cd backend
cp .env.example .env
```

Edite o `.env`:

| Variável | O que é |
|---|---|
| `ORACLE_URL`, `ORACLE_USER`, `ORACLE_PASSWORD` | Banco Oracle da FIAP. Só é necessário no perfil padrão e exige rede/VPN com acesso ao `oracle.fiap.com.br` |
| `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY` | Console do Firebase › Configurações do projeto › Geral. Ative também Authentication › E-mail/senha |
| `TOMTOM_API_KEY` | Chave em developer.tomtom.com (sincronização de incidentes a cada 2 min) |

O `.env` está no `.gitignore`: ele **não vem com o clone** e precisa ser recriado no outro PC.

**Opção A: sem Oracle (recomendada para testar).** Perfil `dev` com H2 em memória; os dados somem ao parar o app.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Opção B: com Oracle (perfil padrão).**

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

A API sobe em `http://localhost:8080`; o Swagger fica em `http://localhost:8080/swagger-ui.html`. Os testes rodam com `./mvnw test`.

Rode os comandos **de dentro de `backend/`**, pois o `.env` é lido a partir da pasta atual.

## 4. Frontend

```bash
cd frontend
npm install
cp .env.example .env
```

Edite o `.env`:

```
EXPO_PUBLIC_API_URL=http://localhost:8080
EXPO_PUBLIC_USE_MOCK=true
```

- `EXPO_PUBLIC_USE_MOCK=true` usa dados de exemplo, sem backend. O login aceita qualquer e-mail com senha de 6 ou mais caracteres.
- Para usar o backend de verdade, ponha `EXPO_PUBLIC_USE_MOCK=false` e preencha as variáveis `EXPO_PUBLIC_FIREBASE_API_KEY`, `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN`, `EXPO_PUBLIC_FIREBASE_PROJECT_ID` e `EXPO_PUBLIC_FIREBASE_APP_ID` (app **Web** no console do Firebase).
- `EXPO_PUBLIC_API_URL` depende de onde o app roda:

  | Onde roda | URL |
  |---|---|
  | Simulador iOS ou web | `http://localhost:8080` |
  | Emulador Android | `http://10.0.2.2:8080` |
  | Celular físico | `http://<IP-do-PC-na-rede>:8080` (mesma rede Wi-Fi) |

Inicie o app:

```bash
npx expo start
```

Escaneie o QR code com o Expo Go (Android) ou com a câmera (iOS). Atalhos: `a` abre o emulador Android e `i` o simulador iOS. Depois de mudar o `.env`, reinicie com `npx expo start -c`.

## Problemas comuns

- **`Port 8080 already in use`:** feche o outro processo que usa a porta.
- **Erro de conexão com o Oracle:** use o perfil `dev` (opção A) ou conecte-se à rede/VPN da FIAP.
- **O celular não alcança a API:** confira o IP do PC, o firewall liberando a porta 8080 e a rede Wi-Fi em comum.
