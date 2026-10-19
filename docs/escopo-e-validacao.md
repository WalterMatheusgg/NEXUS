# Escopo e critérios de aceite

A entrega amplia a fundação com os módulos abaixo. Arquivos existentes não comprovam todos os critérios testados; resultados ficam em `validacao-entrega.md`. O registro histórico da fundação não valida os novos fluxos.

| Área | Comportamento esperado |
| --- | --- |
| Autenticação | Setup com segredo local; login por empresa; sessão/CSRF/logout; recuperação por e-mail e troca de senha; limite de tentativas. |
| Administração | Contexto da empresa no backend; cinco perfis e permissões editáveis; usuários, departamentos e equipes. |
| Clientes | PF/PJ, CPF/CNPJ, unicidade por empresa, edição/inativação, contatos, interações, busca e paginação. |
| Atendimento | Cliente, prioridade, responsável, prazo, status, comentários, histórico e anexos privados. Atraso por prazo operacional. |
| Dashboard | Indicadores, gráficos e atividades calculados da API/Oracle; loading/erro/base vazia. |
| Comercial | Leads, conversão, oportunidades, etapas/Kanban, valores, probabilidade, interações e histórico. |
| Tarefas | Responsável, prazo, prioridade, vínculos e Kanban. |
| Processos | Etapas ordenadas, responsáveis, estados e aprovações simples com histórico. |
| Relatórios | Clientes, comercial, chamados e produtividade; filtros/CSV e limite operacional. |
| Operação | Migrations, testes Oracle, scripts, Docker/Nginx, probes, SMTP e procedimento de backup/restauração. |

## Validação antes da aceitação

1. Provisione a primeira empresa pelo setup. Repetição não pode criar outro administrador; token nunca aparece em respostas/logs.
2. Login/logout; mutações sem sessão, sem CSRF e com token incorreto; confirme 401/403 e cookies no navegador.
3. Perfis diferentes e permissões negativas por API direta; altere permissões/estado e confirme bloqueio imediato.
4. Em teste, duas empresas: unicidade de documento dentro de cada empresa e bloqueio de leitura/alteração/relacionamentos cruzados.
5. Cadastre, altere e inative cliente; crie contato/interação e confirme persistência após reiniciar.
6. Abra chamado, atribua, comente, mude status e baixe anexo; teste tamanho/formato inválido e download de outra empresa.
7. Confira mudanças reais no dashboard e relatórios, sem seed comercial para aparentar atividade.
8. Converta lead, mova oportunidade, vincule tarefa e execute processo com aprovação; teste versão desatualizada.
9. Recuperação existente/inexistente com resposta equivalente; Mailpit; expiração e uso único de token.
10. Builds, testes, proxy/SPA e navegação responsiva. Registre ensaio separado de restauração antes de uso empresarial.

## Evoluções e limites

PDF e tema escuro são posteriores conforme o pedido original. Não há editor BPMN, motor de workflow extensível, calendário contratual de SLA com feriados ou cadastro público de empresas. Sessões e limitador em memória não são distribuídos. Produção exige revisão de segurança, acessibilidade, carga, dependências e operação.

LGPD exige decisões do controlador sobre finalidade, base legal, retenção comercial, titulares, contratos e incidentes, além dos controles técnicos. Veja [privacidade.md](privacidade.md). Não se deve anunciar a implementação como certificação de conformidade.
