# SafeMove

**A rota que vou fazer agora é boa e segura?** O SafeMove reúne, em um único mapa, trânsito em tempo real, ocorrências registradas pelos cidadãos e áreas de risco de São Paulo, e avisa o usuário antes e durante o trajeto.

Global Solution 2026 · 2º semestre · FIAP · Engenharia de Software · CitizenTech

## Ficha da CitizenTech

| | |
|---|---|
| **Nome da CitizenTech** | SafeMove |
| **Cidade e bairro** | São Paulo (capital), todos os bairros. Cenários de teste: Sé, Brás, Mooca e Itaquera |
| **Cidadão beneficiado** | Quem se desloca todo dia por São Paulo a pé, de transporte público ou de carro, com atenção a quem circula à noite (estudantes, trabalhadores, motoristas de aplicativo) |
| **Problema, em uma frase** | As informações de trânsito, transporte e segurança estão espalhadas em vários apps e não conversam, então o cidadão escolhe o caminho sem saber se ele é seguro |
| **Como funciona** | O app mostra um mapa com trânsito (TomTom), ocorrências dos usuários e áreas de risco; avalia a rota escolhida, alerta quando há risco no trajeto e permite registrar ocorrências (assalto, buraco, acidente, alagamento, trânsito, iluminação) |
| **O que cada parte faz** | `backend/`: API REST em Java (Spring Boot + Oracle) com login via Firebase, ocorrências, incidentes da TomTom, radares do OpenStreetMap, áreas de risco e avaliação de rotas · `frontend/`: app mobile em React Native (Expo) · `docs/`: documentação da entrega de Java, script do banco e DER |
| **Link do repositório** | https://github.com/PedroAlmeida232/SafeMove |
| **Indicador de impacto** | Número de ocorrências registradas e confirmadas por outros usuários ("eu também vi") e percentual de trajetos em que o usuário trocou para a rota mais segura sugerida |
| **ODS conectados** | ODS 11 (Cidades e comunidades sustentáveis, meta 11.2: transporte seguro) · ODS 3 (Saúde e bem-estar, meta 3.6: acidentes de trânsito) · ODS 16 (Paz, justiça e instituições eficazes, meta 16.1: redução da violência) |

## Integrantes

| Nome | RM |
|---|---|
| Alexandre Martins Lucas | 561732 |
| Gabriel Barbosa da Silva | 570133 |
| Pedro Rodrigues Almeida | 564711 |
| Vitor Carvalho Alexandre | 562298 |

## Estrutura do repositório

```
SafeMove/
├── backend/     API REST Java 21 + Spring Boot (entrega de Domain Driven Design - Java)
├── frontend/    App mobile Expo / React Native (veja frontend/README.md)
├── docs/        ENTREGA-JAVA.md (endpoints, DER, diagrama de classes) e safemove-oracle.sql
├── render.yaml  Deploy da API no Render
├── SETUP.md     Como rodar o projeto inteiro em outro computador
└── prompts.md   Registro do uso de IA
```

## Backend (Java) · resumo da entrega

- **Camadas:** `model` (entidades JPA) → `dao` (acesso ao Oracle) → `bo` (regras de negócio) → `controller` (REST), com `exception` para o tratamento de erros.
- **Requisições:** GET, POST, PUT e DELETE em `/usuarios/me` e `/ocorrencias`, além de `/auth/*`, `/incidentes`, `/radares`, `/areas-risco` e `POST /rotas/avaliar`. Tabela completa em [`docs/ENTREGA-JAVA.md`](docs/ENTREGA-JAVA.md).
- **Banco:** Oracle da FIAP (`oracle.fiap.com.br:1521:ORCL`). As tabelas são criadas pela aplicação; o script está em [`docs/safemove-oracle.sql`](docs/safemove-oracle.sql).
- **Deploy:** Render, pelo `render.yaml` da raiz. Link: https://safemove-sjiw.onrender.com (Swagger em `/swagger-ui/index.html`)
- **Documentação da API:** Swagger na raiz da URL (local: http://localhost:8080).

### Rodar a API

Ferramentas: JDK 21, IntelliJ IDEA (ou outra IDE Java). O Maven vem no wrapper (`mvnw`).

```bash
cd backend
cp .env.example .env      # preencha ORACLE_USER, ORACLE_PASSWORD, FIREBASE_*, TOMTOM_API_KEY
./mvnw spring-boot:run    # com Oracle FIAP
# ou, sem Oracle (H2 em memória):
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Testes: `./mvnw test`. Passo a passo completo, incluindo o deploy no Render, em [`docs/ENTREGA-JAVA.md`](docs/ENTREGA-JAVA.md).

## Frontend (app)

```bash
cd frontend
npm install
cp .env.example .env
npx expo start
```

Detalhes em [`frontend/README.md`](frontend/README.md).

## Dados

Nenhum dado pessoal real é usado. As ocorrências são cadastradas pelos próprios usuários (nos testes, dados simulados). Os incidentes de trânsito e as rotas vêm da TomTom (plano gratuito); os radares e o mapa base são do OpenStreetMap (© colaboradores do OpenStreetMap, licença ODbL).

## Uso de IA

A IA foi usada como copiloto para aprender, gerar e revisar código e documentação. Os prompts estão registrados em [`prompts.md`](prompts.md).
