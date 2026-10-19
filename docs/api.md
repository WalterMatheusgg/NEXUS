# Contrato HTTP

API direta: `http://127.0.0.1:18080`. Angular: `http://127.0.0.1:4200/api`. Full stack: `http://127.0.0.1:8081/api`. O prefixo `/api` é removido pelo proxy antes do Spring. [api.http](api.http) contém exemplos manuais, sem credenciais nem dados seed.

## Sessão e CSRF

1. `GET /auth/csrf` retorna `{token}` e cookie CSRF.
2. Mutações enviam cookie e header `X-XSRF-TOKEN`, inclusive login/setup/reset. Este token não é o segredo de provisionamento.
3. Login/setup retornam usuário e estabelecem `NEXUSSESSION`. Renove CSRF após autenticação/logout. Clientes HTTP precisam manter cookie jar.
4. `GET /auth/me` verifica sessão; `POST /auth/logout` encerra-a. `401` exige login; `403` exige revisar permissão ou CSRF.

| Método/rota | Entrada ou resultado |
| --- | --- |
| `GET /auth/status` | `{setupRequired,setupEnabled}`, sem segredo. |
| `POST /auth/setup` | `{setupToken,organizationCode,organizationName,name,email,password}`; primeira instalação. |
| `POST /auth/login` | `{organizationCode,email,password}`. |
| `GET /auth/me` | Usuário, organização, perfil e permissões. |
| `POST /auth/logout` | Encerra sessão. |
| `POST /auth/forgot-password` | `{organizationCode,email}`; resposta genérica, token por e-mail. |
| `POST /auth/reset-password` | `{token,password}`; token temporário de uso único. |
| `POST /auth/change-password` | `{currentPassword,password}`; autenticado. |

Login, setup e recuperação têm limite de requisições por instância. `429` indica que o cliente deve aguardar.

## Convenções

Listagens: `?page=0&size=20&search=&sort=createdAt&direction=desc`; resposta `{content,totalElements,totalPages,page,size}`. `size` máximo 100. Ordenação validada por domínio. Datas de filtro: `YYYY-MM-DD`; instantes: ISO 8601 com offset. Enums são maiúsculos no JSON, traduzidos na UI.

Entidades retornam `id`, `version`, `createdAt`, `updatedAt`. Edição devolve `version` para detectar conflito. Referências devem pertencer à organização autenticada; ID numérico não é autorização.

Erros Problem Detail: `{status,title,detail,instance,errors?}`. `400`: validação; `401`: sessão ausente; `403`: permissão/CSRF; `404`: recurso não acessível; `409`: duplicidade/concorrência; `413`: upload grande; `429`: limite; `500`: falha sem stack trace para cliente.

## Negócios

Todos exigem sessão e permissão. Recursos principais usam GET lista/detalhe, POST criação e PUT edição por ID.

| Recurso | Campos / operações adicionais |
| --- | --- |
| `/customers` | `type` PERSON/COMPANY, `name,document,email,phone,address,active`; CPF/CNPJ com dígitos verificadores e unicidade por empresa. |
| `/customers/{id}/contacts` | Listar/criar; DELETE `/{contactId}`; `name,email,phone,position`. |
| `/customers/{id}/interactions` | Listar/criar `note`, autor/data. |
| `/leads` | `name,email,phone,source,status,assigneeId,notes`; POST `/{id}/convert` com `document,type,name?` retorna cliente. |
| `/opportunities` | `title,customerId,leadId,assigneeId,stage,amount,probability,expectedCloseDate,description`; PATCH `/{id}/stage`, `/interactions`, `/history`. |
| `/tickets` | `number,title,description,customerId,category,priority,status,assigneeId,dueAt`; PATCH `/{id}/status`, `/comments`, `/history`, `/attachments`. |
| `/tasks` | `title,description,priority,status,assigneeId,dueDate,customerId,ticketId,opportunityId`; PATCH `/{id}/status`. |
| `/processes` | `title,description,status,assigneeId`; `/steps` lista/cria, PATCH `/steps/{stepId}` altera estado; `/history`. |
| `/dashboard` | Indicadores, séries e atividades calculados da empresa atual no Oracle. |
| `/reports/{customers,opportunities,tickets,productivity}` | Filtros `from,to,status,assigneeId`; `columns,rows,total`; `/export` CSV protegido contra fórmulas. Limite 10 mil linhas. |

Funil: `NEW_LEAD,CONTACTED,QUALIFICATION,PROPOSAL,NEGOTIATION,WON,LOST`. Chamados: `OPEN,IN_PROGRESS,WAITING,RESOLVED,CLOSED`. Tarefas: `TODO,IN_PROGRESS,DONE`. Prioridades: `LOW,MEDIUM,HIGH,URGENT`. Processos: `OPEN,IN_PROGRESS,COMPLETED,CANCELLED`; etapas: `PENDING,IN_PROGRESS,APPROVED,REJECTED,COMPLETED`.

Anexos: POST `/tickets/{id}/attachments`, multipart campo `file`, até 10 MiB; PDF, PNG, JPEG, TXT e CSV, com validação de conteúdo. GET `/tickets/{id}/attachments/{attachmentId}` exige autorização; storage é privado. HTML/SVG/executáveis não são aceitos. Validação de formato não substitui antivírus.

## Administração

| Rotas | Finalidade |
| --- | --- |
| `/admin/users` | Listar/criar/editar; senha na criação; PATCH `/{id}/password` para alteração administrativa. |
| `/admin/members` | Usuários ativos para atribuições. |
| `/admin/roles` | Listar/criar/editar nome e permissões. |
| `/admin/permissions` | Catálogo válido de permissões. |
| `/admin/departments`, `/admin/teams` | Listar/criar/editar estrutura empresarial. |
| `/admin/organization` | Consultar/editar empresa atual. |
| `/admin/audit` | Trilha de auditoria. |
| `/notifications` | Notificações do usuário; PATCH `/{id}/read`. |

Perfis iniciais: Administrador, Gestor, Vendedor, Atendimento e Funcionário. Administrador começa com todo o catálogo; consulte os perfis atuais na API, pois suas permissões são editáveis. A UI não é fonte de autorização.

OpenAPI exige sessão válida. Na API direta, `/swagger-ui/index.html` e `/v3/api-docs`; `NEXUS_API_DOCS_ENABLED=false` desabilita. O prefixo externo pode exigir ajuste da URL do JSON no Swagger; use a API direta no desenvolvimento quando necessário. Documentação instalada não significa API pública.
