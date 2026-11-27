# SafeMove: API Java (Domain Driven Design)

Base para o PDF da entrega. Preencha o que está entre `<...>`.

- **Integrantes (ordem alfabética):**
  - Alexandre Martins Lucas · RM 561732
  - Gabriel Barbosa da Silva · RM 570133
  - Pedro Rodrigues Almeida · RM 564711
  - Vitor Carvalho Alexandre · RM 562298
- **GitHub:** https://github.com/PedroAlmeida232/SafeMove
- **Deploy (Render):** https://safemove-sjiw.onrender.com (Swagger em `/swagger-ui/index.html`)

## 1. Estrutura em camadas

Spring Boot 4.1 · Java 21 · Spring Data JPA · Oracle (FIAP) · Spring Security (token do Firebase) · springdoc (Swagger).

| Pacote (`backend/src/main/java/com/safemove/`) | Camada | Conteúdo |
|---|---|---|
| `model` | Modelo | Entidades JPA mapeadas para as tabelas Oracle: `Usuario`, `Ocorrencia`, `Confirmacao`, `Incidente`, `Radar` e os enums `TipoOcorrencia`, `CategoriaIncidente`, `ModoTransporte`, `Classificacao` |
| `dao` | DAO | Acesso ao banco com Spring Data JPA: `UsuarioDAO`, `OcorrenciaDAO`, `ConfirmacaoDAO`, `IncidenteDAO`, `RadarDAO` |
| `bo` | BO | Regras de negócio e transações: `AuthBO`, `UsuarioBO`, `OcorrenciaBO`, `IncidenteBO`, `RadarBO`, `AreaRiscoBO`, `RotaBO` |
| `controller` | Controller | Endpoints REST (GET, POST, PUT, DELETE): `AuthController`, `UsuarioController`, `OcorrenciaController`, `IncidenteController`, `RadarController`, `AreaRiscoController`, `RotaController` |
| `dto` | DTO | Corpos de requisição (com Bean Validation) e de resposta |
| `exception` | Exceções | `ApiException` + `ApiExceptionHandler` (`@RestControllerAdvice`) e `JsonErrorHandlers` (erros de segurança). Todo erro sai como `{ status, detail, campos? }` |
| `integracao` | Integrações | `FirebaseAuthClient` (login), `TomTomClient` (incidentes), `TomTomRotasClient` (rotas) e `OverpassClient` (radares do OpenStreetMap) |
| `agendamento` | Job | `IncidenteSincronizador` (incidentes da TomTom a cada 2 min) e `RadarSincronizador` (radares do OpenStreetMap 1 vez por dia) |
| `config`, `geo` | Apoio | Segurança, Swagger, fuso horário, cálculo de distância |

Fluxo de uma requisição: **Controller → BO → DAO → Oracle**. Os erros lançados em qualquer camada viram a resposta JSON no `ApiExceptionHandler`.

### Regras de negócio (BO)

- Ocorrência só pode ser registrada dentro da Grande São Paulo (422).
- Só o autor altera (PUT) ou exclui (DELETE) uma ocorrência (403).
- "Eu também vi": cada usuário confirma uma ocorrência uma única vez (409) e não pode confirmar a própria (422).
- Buscas por raio: até 50 km; ocorrências nas últimas 1 a 720 horas (400 fora disso).
- Excluir a conta apaga perfil, ocorrências e confirmações, e o usuário no Firebase, numa única transação.
- Avaliação de rota: origem e destino na Grande São Paulo (422) e a pelo menos 50 m um do outro (422). Cada rota começa com nota 100 e perde pontos pelo que está no caminho: incidente grave (−12), incidente leve (−4), assalto nas últimas 24 h (−12), outra ocorrência (−3), cada nível de risco médio acima de 1 (−8) e atraso por trânsito (−1 por minuto, até −10). Nota ≥ 70 = `SEGURA`, ≥ 45 = `ATENCAO`, abaixo = `EVITE`.
- Áreas de risco: cada ocorrência dos últimos 30 dias conta para o distrito mais próximo (até 1,5 km do centro). Peso: assalto 3; acidente, iluminação e alagamento 1; buraco 0,5; trânsito 0. Nível 1 a 5 (≥ 1, 3, 6 e 10 pontos).

## 2. Endpoints

🔒 = exige `Authorization: Bearer <token>` (o `token` devolvido por `/auth/login`).

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/auth/register` | Cria conta (`nome`, `email`, `senha`) | 201 | 400, 409 |
| POST | `/auth/login` | Entra (`email`, `senha`) e devolve `token`, `refreshToken`, `usuario` | 200 | 400, 401 |
| POST | `/auth/refresh` | Renova o token (`refreshToken`) | 200 | 401 |
| POST | `/auth/recuperar-senha` | Envia e-mail de redefinição (`email`) | 204 | 400 |
| GET 🔒 | `/usuarios/me` | Perfil de quem está logado | 200 | 401 |
| PUT 🔒 | `/usuarios/me` | Altera o nome (`nome`) | 200 | 400, 401 |
| DELETE 🔒 | `/usuarios/me` | Exclui a conta | 204 | 401 |
| GET | `/ocorrencias?lat&lng[&raio][&horas][&tipo]` | Ocorrências próximas (padrão 2 km, 24 h) | 200 | 400 |
| GET | `/ocorrencias/{id}` | Uma ocorrência | 200 | 404 |
| POST 🔒 | `/ocorrencias` | Registra (`tipo`, `descricao?`, `latitude`, `longitude`) | 201 | 400, 401, 422 |
| PUT 🔒 | `/ocorrencias/{id}` | Altera (mesmo corpo do POST; só o autor) | 200 | 400, 401, 403, 404, 422 |
| DELETE 🔒 | `/ocorrencias/{id}` | Exclui (só o autor) | 204 | 401, 403, 404 |
| POST 🔒 | `/ocorrencias/{id}/confirmar` | "Eu também vi" | 200 | 401, 404, 409, 422 |
| GET | `/incidentes[?lat&lng&raio][&categoria]` | Incidentes de trânsito (TomTom) | 200 | 400 |
| GET | `/incidentes/{id}` | Um incidente | 200 | 404 |
| GET | `/radares[?lat&lng&raio]` | Radares de velocidade (OpenStreetMap, padrão 2 km) | 200 | 400 |
| GET | `/areas-risco` | Nível de risco (1 a 5) de cada distrito, calculado pelas ocorrências | 200 | — |
| POST | `/rotas/avaliar` | Até 3 rotas (TomTom) com nota de segurança, da melhor para a pior (`origem`, `destino`, `modo?`) | 200 | 400, 422, 503 |

Exemplo de corpo de `POST /ocorrencias`:

```json
{ "tipo": "BURACO", "descricao": "Buraco grande na faixa da direita", "latitude": -23.5613, "longitude": -46.6565 }
```

Exemplo de corpo de `POST /rotas/avaliar` (`modo`: `CARRO`, `A_PE` ou `BICICLETA`; padrão `CARRO`):

```json
{ "origem": { "latitude": -23.5613, "longitude": -46.6565 }, "destino": { "latitude": -23.5505, "longitude": -46.6333 }, "modo": "CARRO" }
```

Exemplo de erro:

```json
{ "status": 400, "detail": "Escolha o tipo da ocorrência.", "campos": { "tipo": "Escolha o tipo da ocorrência." } }
```

## 3. DER

Script completo em [`safemove-oracle.sql`](safemove-oracle.sql).

```mermaid
erDiagram
    PERFIS_USUARIO ||--o{ OCORRENCIAS : registra
    PERFIS_USUARIO ||--o{ CONFIRMACOES_OCORRENCIA : confirma
    OCORRENCIAS ||--o{ CONFIRMACOES_OCORRENCIA : recebe

    PERFIS_USUARIO {
        varchar2 id_usuario PK "uid do Firebase"
        varchar2 nome
        varchar2 email UK
        timestamp criado_em
    }
    OCORRENCIAS {
        number id_ocorrencia PK
        varchar2 id_usuario FK
        varchar2 tipo
        varchar2 descricao
        binary_double lat
        binary_double lng
        timestamp criado_em
        number confirmacoes
    }
    CONFIRMACOES_OCORRENCIA {
        number id_confirmacao PK
        number id_ocorrencia FK
        varchar2 id_usuario FK
        timestamp criado_em
    }
    INCIDENTES {
        number id_incidente PK
        varchar2 id_tomtom UK
        varchar2 categoria
        number gravidade
        varchar2 via
        varchar2 descricao
        number atraso
        binary_double lat
        binary_double lng
        timestamp atualizado_em
    }
    RADARES {
        number id_radar PK
        number id_osm UK
        binary_double lat
        binary_double lng
        number velocidade_max
        timestamp atualizado_em
    }
```

`INCIDENTES` e `RADARES` não têm relacionamento: são cópias dos dados da TomTom e do OpenStreetMap, trocadas a cada sincronização. As áreas de risco não têm tabela: são calculadas a partir de `OCORRENCIAS`.

## 4. Diagrama de classes

```mermaid
classDiagram
    direction LR
    class Usuario {
        -String uid
        -String nome
        -String email
        -OffsetDateTime criadoEm
        +alterarNome(String)
    }
    class Ocorrencia {
        -Long id
        -Usuario autor
        -TipoOcorrencia tipo
        -String descricao
        -double latitude
        -double longitude
        -OffsetDateTime criadoEm
        -int confirmacoes
        +atualizar(tipo, descricao, lat, lng)
        +registrarConfirmacao()
        +pertenceA(String uid) boolean
    }
    class Confirmacao {
        -Long id
        -Ocorrencia ocorrencia
        -Usuario usuario
        -OffsetDateTime criadoEm
    }
    class Incidente {
        -Long id
        -String idTomtom
        -CategoriaIncidente categoria
        -int gravidade
        -String via
        -String descricao
        -Integer atrasoSegundos
        -double latitude
        -double longitude
        -OffsetDateTime atualizadoEm
        +atualizar(...)
    }
    class Radar {
        -Long id
        -Long idOsm
        -double latitude
        -double longitude
        -Integer velocidadeMax
        -OffsetDateTime atualizadoEm
        +atualizar(lat, lng, velocidadeMax)
    }
    class TipoOcorrencia {
        <<enumeration>>
        ASSALTO
        BURACO
        ACIDENTE
        ALAGAMENTO
        TRANSITO
        ILUMINACAO
    }
    class CategoriaIncidente {
        <<enumeration>>
        ACIDENTE
        CONGESTIONAMENTO
        OBRAS
        ...
    }
    Ocorrencia "*" --> "1" Usuario : autor
    Confirmacao "*" --> "1" Ocorrencia
    Confirmacao "*" --> "1" Usuario
    Ocorrencia --> TipoOcorrencia
    Incidente --> CategoriaIncidente

    class OcorrenciaController
    class OcorrenciaBO {
        +listar(lat, lng, raio, horas, tipo)
        +buscar(id)
        +criar(jwt, req)
        +atualizar(jwt, id, req)
        +excluir(jwt, id)
        +confirmar(jwt, id)
    }
    class OcorrenciaDAO {
        <<interface>>
    }
    class ConfirmacaoDAO {
        <<interface>>
    }
    class UsuarioController
    class UsuarioBO {
        +perfilDoToken(jwt) Usuario
        +buscar(jwt)
        +atualizar(jwt, req)
        +excluir(jwt)
    }
    class UsuarioDAO {
        <<interface>>
    }
    class IncidenteController
    class IncidenteBO {
        +listar(lat, lng, raio, categoria)
        +buscar(id)
        +substituir(novos)
    }
    class IncidenteDAO {
        <<interface>>
    }
    class RadarController
    class RadarBO {
        +listar(lat, lng, raio)
        +substituir(novos)
    }
    class RadarDAO {
        <<interface>>
    }
    class AreaRiscoController
    class AreaRiscoBO {
        +listar() List~AreaRiscoResponse~
    }
    class RotaController
    class RotaBO {
        +avaliar(req) List~RotaAvaliada~
    }
    class TomTomRotasClient {
        +calcularRotas(origem, destino, modo)
    }
    class ApiException {
        -HttpStatus status
    }
    class ApiExceptionHandler

    RadarController --> RadarBO
    RadarBO --> RadarDAO
    AreaRiscoController --> AreaRiscoBO
    AreaRiscoBO --> OcorrenciaDAO
    RotaController --> RotaBO
    RotaBO --> TomTomRotasClient
    RotaBO --> IncidenteDAO
    RotaBO --> OcorrenciaDAO
    RotaBO --> RadarDAO
    RotaBO --> AreaRiscoBO
    OcorrenciaController --> OcorrenciaBO
    OcorrenciaBO --> OcorrenciaDAO
    OcorrenciaBO --> ConfirmacaoDAO
    OcorrenciaBO --> UsuarioBO
    UsuarioController --> UsuarioBO
    UsuarioBO --> UsuarioDAO
    IncidenteController --> IncidenteBO
    IncidenteBO --> IncidenteDAO
    ApiExceptionHandler ..> ApiException : trata
```

Os diagramas em Mermaid podem ser exportados como imagem em <https://mermaid.live> para colar no PDF.

## 5. Como executar

### Ferramentas e versões

| Ferramenta | Versão |
|---|---|
| Java (JDK) | 21 |
| Maven | wrapper incluso (`./mvnw`), não precisa instalar |
| Spring Boot | 4.1.2 |
| Banco | Oracle da FIAP (`oracle.fiap.com.br:1521:ORCL`); H2 em memória no perfil `dev` |
| IDE | IntelliJ IDEA (ou qualquer IDE Java) |
| Docker | opcional, só para testar a imagem do deploy |

### Passo a passo (local)

1. Clone o repositório e entre em `backend/`.
2. Copie `.env.example` para `.env` e preencha `ORACLE_USER`, `ORACLE_PASSWORD` (seu RM e senha do Oracle FIAP), `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY` e `TOMTOM_API_KEY`.
3. Rode com Oracle:

   ```bash
   ./mvnw spring-boot:run
   ```

   ou sem Oracle (H2 em memória):

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. Abra <http://localhost:8080> (redireciona para o Swagger). Para as rotas 🔒: faça `POST /auth/login`, copie o `token` e clique em **Authorize**.
5. Testes: `./mvnw test`.

As tabelas são criadas automaticamente na primeira execução (`ddl-auto=update`).

### Deploy no Render

1. No Render: **New › Blueprint** e selecione o repositório no GitHub. O arquivo `render.yaml` da raiz cria o serviço `safemove-api` (Docker, plano free) a partir de `backend/Dockerfile`.
2. Preencha as variáveis pedidas: `ORACLE_USER`, `ORACLE_PASSWORD`, `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`, `TOMTOM_API_KEY`.
3. Aguarde o build. A URL pública (`https://<nome>.onrender.com`) abre o Swagger.

No plano free o serviço "dorme" após 15 min sem uso; a primeira requisição depois disso demora ~1 min.
