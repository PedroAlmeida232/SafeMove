# Registro de prompts (uso de IA)

A regra da GS pede que o uso de IA seja registrado. Ferramenta principal: Claude (Anthropic). Os prompts abaixo estão resumidos, na ordem em que foram usados. Cada integrante deve acrescentar os prompts que usou em outras ferramentas (Cursor, Copilot, Codex etc.).

| Data | Para quê | Prompt (resumo) | O que fizemos com a resposta |
|---|---|---|---|
| 05/11/2026 | Ideia e diagramas | "Crie um diagrama do SafeMove: o web só mostra as informações; o app mostra, avisa se a rota é boa e segura e registra ocorrências de trânsito e assalto." | Diagrama de casos de uso e de tecnologias no FigJam, revisado pelo grupo |
| 05/11/2026 | Documento de UX | "Faça o documento da entrega com a identidade visual do SafeMove, com espaço para o protótipo e o link do YouTube." | PDF de UX; personas, backlog e MVP conferidos e ajustados |
| 05/11/2026 | Pitch | "Faça o roteiro do vídeo pitch de até 3 minutos para 2 pessoas falarem." | Roteiro gravado pelo grupo |
| 05/11/2026 | Protótipo | "Faça no Figma o protótipo das telas que encaixem com o texto do pitch." / "Deixe o design parecido com o da Apple, tema branco." | 7 telas do app usadas no vídeo e no PDF |
| 06/11/2026 | Fontes de dados | "Fui reprovado no Waze for Cities; me mande opções open source para acidentes, assaltos por região, buracos e trânsito." | Escolha da TomTom Traffic API, OpenStreetMap e dados abertos da SSP-SP |
| 06/11/2026 | Arquitetura | "Monte uma arquitetura simples, sem overengineering, usando o Firebase Auth." | Arquitetura App/Site → API Java → Oracle, login pelo Firebase |
| 06/11/2026 | Backend | "Comece a desenvolver o código Java." / "Crie módulos do backend para eu desenvolver." / "Divida isso em sprints." | Projeto de referência e plano de sprints; o grupo implementou os módulos no repositório |
| 06/11/2026 | Frontend | "Mande o prompt para desenvolver o frontend igual ao protótipo." | Prompt usado para gerar a base do app em Expo, depois ajustada pelo grupo |
| 06/11/2026 | Firebase | "Como posso configurar o meu Firebase?" / "Onde fica a API key?" | Configuração do Firebase Authentication (e-mail e senha) |
| 17/11/2026 | Entrega de Java | "Ajuste o projeto e o repositório para a entrega de Java (sem fazer commit)." | README com a Ficha da CitizenTech, organização das pastas e revisão das camadas Model/DAO/BO/Controller |

## Como usamos a IA

- Para aprender e revisar: cada trecho gerado foi lido, testado e ajustado pelo grupo antes de entrar no repositório.
- Nenhuma chave, senha ou dado pessoal real foi enviado nos prompts.
- Todos os integrantes sabem explicar as camadas do backend, o fluxo de login com o Firebase e como os incidentes da TomTom são sincronizados.
