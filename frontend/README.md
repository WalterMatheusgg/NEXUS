# Nexus CRM — frontend

Fundação Angular standalone com Angular Material 22.2.3. A tela de conectividade consulta a API real; os módulos do CRM ainda não foram implementados.

## Ambiente

Use a versão Node definida no `.nvmrc` da raiz. O projeto requer Node 24.15 ou superior na linha 24; a versão verificada é 24.22.0 com npm 11.19.1.

Na raiz do repositório:

```bash
. "$HOME/.nvm/nvm.sh"
nvm use
cd frontend
npm ci
```

## Desenvolvimento

Inicie Oracle e backend conforme o [README principal](../README.md). Em outro terminal, com Node 24 ativo:

```bash
npm start -- --host 127.0.0.1
```

Abra <http://localhost:4200>. A tela consulta `GET /api/actuator/health` e exibe o resultado da API.

O proxy em `proxy.conf.mjs` encaminha `/api/*` para `http://127.0.0.1:18080/*`, removendo `/api`. Ele é aplicado somente pelo servidor de desenvolvimento, na configuração `development`. Uma publicação do build precisa configurar o encaminhamento `/api` no servidor que hospeda o frontend.

## Validação

```bash
npm test -- --watch=false
npm run build
```

Os testes usam Vitest e verificam o componente raiz e o caminho HTTP de health. Eles não substituem a verificação da tela com o backend e o Oracle reais.

O build de produção gera `dist/nexus-crm/browser/` e não baixa fontes externas durante a compilação. Os links Google Fonts de `src/index.html` são carregados opcionalmente pelo navegador em runtime; sem acesso ao serviço, a interface usa as fontes alternativas disponíveis no sistema.
