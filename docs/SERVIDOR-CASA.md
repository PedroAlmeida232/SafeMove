# SafeMove: servidor no notebook (Ubuntu)

Hospeda a API em um notebook em casa, com HTTPS público pelo **Cloudflare Tunnel**
(grátis, sem abrir porta no roteador, funciona atrás de CGNAT).

**Custo:** domínio (~R$ 40/ano) + luz (~R$ 7 a 10/mês). **Reserva:** o Render Free (`render.yaml`).

## 0. Pré-requisitos
- Notebook com Ubuntu, ligado na tomada, de preferência por cabo de rede.
- Um domínio (ex.: `safemove.com.br`, no Registro.br). Precisa usar o DNS da Cloudflare.
- Conta gratuita na Cloudflare (https://dash.cloudflare.com).

## 1. Instalar Docker
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # saia e entre na sessão de novo
docker --version && docker compose version
```

## 2. Deixar o notebook ligado 24 h
```bash
# Nunca suspender/hibernar
sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target
# Fechar a tampa não faz nada
sudo sed -i 's/^#\?HandleLidSwitch=.*/HandleLidSwitch=ignore/' /etc/systemd/logind.conf
sudo sed -i 's/^#\?HandleLidSwitchExternalPower=.*/HandleLidSwitchExternalPower=ignore/' /etc/systemd/logind.conf
sudo systemctl restart systemd-logind   # (a sessão gráfica pode reiniciar)
# Atualizações de segurança automáticas
sudo apt install -y unattended-upgrades && sudo dpkg-reconfigure -plow unattended-upgrades
```
- Na **BIOS**, procure *AC Power Recovery / Restore on AC Power Loss* e deixe **Power On**,
  para o notebook ligar sozinho quando a energia voltar.
- O Docker já sobe no boot e os contêineres usam `restart: unless-stopped`.

## 3. Baixar o projeto e configurar
```bash
git clone <URL-DO-SEU-REPOSITORIO> safemove && cd safemove
cp servidor.env.example servidor.env
nano servidor.env        # preencha Oracle, Firebase e TomTom
chmod 600 servidor.env
```
`servidor.env` guarda segredos: confira que está no `.gitignore`.

## 4. Criar o túnel na Cloudflare
1. Adicione o domínio na Cloudflare e troque os *nameservers* no Registro.br pelos que ela informar.
2. Painel **Zero Trust > Networks > Tunnels > Create a tunnel > Cloudflared**, nome `safemove`.
3. Copie o **token** do túnel e cole em `TUNNEL_TOKEN` no `servidor.env`.
4. Em **Public Hostname**: subdomínio `api`, domínio `seudominio.com.br`,
   serviço **HTTP** → `api:8080`.

## 5. Subir
```bash
docker compose up -d --build     # a 1ª vez demora (compila o Maven)
docker compose logs -f api       # Ctrl+C para sair
curl https://api.seudominio.com.br/v3/api-docs
```
Se o `curl` devolver JSON, a API está no ar com HTTPS.

## 6. Apontar o app
No `frontend/.env`, coloque `https://api.seudominio.com.br` na variável de URL da API
(o nome está em `frontend/.env.example`). Para o build da loja, use a mesma URL em `eas.json`.

## Operação do dia a dia
| O que | Comando |
|---|---|
| Ver se está rodando | `docker compose ps` |
| Logs | `docker compose logs -f api` |
| Atualizar o código | `git pull && docker compose up -d --build` |
| Reiniciar | `docker compose restart api` |
| Parar | `docker compose down` |

## Segurança
- Nenhuma porta é aberta no roteador; só o túnel (conexão de saída) expõe a API.
- A API não publica porta no host (`ports` comentado no compose).
- Mantenha o Ubuntu atualizado e o `servidor.env` com `chmod 600`.
- Mantenha o firewall ligado: `sudo ufw enable` (sem regras de entrada extras).

## Plano B (se o notebook cair)
Faça o deploy no Render (`render.yaml`) e, na Cloudflare, mude o *Public Hostname*/CNAME de `api`
para `safemove-api.onrender.com`. O app continua com a mesma URL.

## Alternativa sem domínio: Tailscale Funnel
Grátis e com URL fixa `nome.ts.net`, mas sem domínio próprio: instale o Tailscale
(`curl -fsSL https://tailscale.com/install.sh | sh`), rode `sudo tailscale up` e
`sudo tailscale funnel --bg 8080` (publique a porta 8080 no host descomentando `ports` no compose).
