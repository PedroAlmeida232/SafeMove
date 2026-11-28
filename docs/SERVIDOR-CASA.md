# SafeMove: servidor no notebook (Ubuntu) com Tailscale Funnel

Hospeda a API em um notebook em casa e a publica na internet com HTTPS usando o
**Tailscale Funnel**: grátis, sem comprar domínio, sem abrir porta no roteador e funciona atrás de CGNAT.

**URL final (fixa):** `https://<nome-do-notebook>.<seu-tailnet>.ts.net`
**Custo:** só a luz (~R$ 7 a 10/mês). **Reserva:** Render Free (`render.yaml`).

## 1. Instalar Docker
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER      # saia e entre na sessão de novo
docker --version && docker compose version
```

## 2. Deixar o notebook ligado 24 h
```bash
sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target
sudo sed -i 's/^#\?HandleLidSwitch=.*/HandleLidSwitch=ignore/' /etc/systemd/logind.conf
sudo sed -i 's/^#\?HandleLidSwitchExternalPower=.*/HandleLidSwitchExternalPower=ignore/' /etc/systemd/logind.conf
sudo systemctl restart systemd-logind
sudo apt install -y unattended-upgrades && sudo dpkg-reconfigure -plow unattended-upgrades
```
Na **BIOS**, deixe *AC Power Recovery / Restore on AC Power Loss* em **Power On**.

## 3. Subir a API
```bash
git clone https://github.com/PedroAlmeida232/SafeMove.git safemove && cd safemove
cp servidor.env.example servidor.env
nano servidor.env            # Oracle, Firebase e TomTom (deixe TUNNEL_TOKEN vazio)
chmod 600 servidor.env
docker compose up -d --build   # a 1ª vez demora (compila o Maven)
curl http://127.0.0.1:8080/v3/api-docs   # deve devolver JSON
```
O contêiner reinicia sozinho (`restart: unless-stopped`) e sobe no boot.

## 4. Instalar o Tailscale e publicar
1. Crie a conta grátis em https://login.tailscale.com.
2. No notebook:
   ```bash
   curl -fsSL https://tailscale.com/install.sh | sh
   sudo tailscale up
   ```
   Abra o link que aparecer e autorize o notebook.
3. No painel (https://login.tailscale.com/admin/dns): ative **MagicDNS** e **HTTPS Certificates**.
4. Publique a porta 8080 na internet:
   ```bash
   sudo tailscale funnel --bg 8080
   ```
   Na primeira vez ele mostra um link para habilitar o Funnel na sua conta; abra, aceite e rode de novo.
5. Veja a URL e teste (de outro dispositivo, fora da sua rede, se puder):
   ```bash
   tailscale funnel status
   curl https://<nome>.<tailnet>.ts.net/v3/api-docs
   ```
O `--bg` mantém o Funnel ativo mesmo depois de reiniciar o notebook.

## 5. Apontar o app
Em `frontend/.env` e no `frontend/eas.json` (build da loja), use a URL
`https://<nome>.<tailnet>.ts.net` na variável de URL da API (nome em `frontend/.env.example`).
Para um nome mais curto, renomeie o notebook em https://login.tailscale.com/admin/machines.

## Operação do dia a dia
| O que | Comando |
|---|---|
| Ver se está rodando | `docker compose ps` e `tailscale funnel status` |
| Logs da API | `docker compose logs -f api` |
| Atualizar o código | `git pull && docker compose up -d --build` |
| Reiniciar a API | `docker compose restart api` |
| Parar a publicação | `sudo tailscale funnel reset` |

## Segurança
- Nenhuma porta é aberta no roteador. O Funnel só encaminha o tráfego HTTPS para `127.0.0.1:8080`.
- Mantenha o Ubuntu atualizado e o `servidor.env` com `chmod 600` (ele guarda segredos e não vai para o Git).
- Ative o firewall: `sudo ufw enable` (sem regras de entrada extras).

## Limites do Funnel
- O Tailscale limita a banda do Funnel (valor não publicado). Para um app pequeno é suficiente.
- A URL termina em `.ts.net`. Se quiser um domínio próprio, use o Cloudflare Tunnel (próxima seção).

## Plano B 1: Render
Se o notebook cair, faça o deploy no Render (`render.yaml`) e troque a URL do app. Como o app embutido
na loja aponta para uma URL fixa, **vale configurar um domínio próprio** (Plano B 2) para poder
trocar o servidor sem publicar uma nova versão.

## Plano B 2: Cloudflare Tunnel com domínio próprio (opcional)
1. Adicione o domínio na Cloudflare; em **Zero Trust > Networks > Tunnels**, crie o túnel e copie o token.
2. Coloque o token em `TUNNEL_TOKEN` no `servidor.env`.
3. Public Hostname: `api.seudominio.com.br` → **HTTP** `api:8080`.
4. `docker compose --profile cloudflare up -d`
