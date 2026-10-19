# Dados pessoais e responsabilidades

Nexus CRM trata clientes, contatos, funcionários, documentos, interações, chamados e anexos. Separação por organização, autorização, auditoria e storage privado são medidas técnicas; o uso adequado depende de decisões e procedimentos de cada empresa.

Antes de dados reais, documente finalidades, categorias de dados, acesso, bases legais, fornecedores, retenção e descarte com os responsáveis da empresa. Evite dados sensíveis desnecessários. Demonstrações e capturas de portfólio devem usar exemplos claramente identificados, sem informações reais de clientes.

Inativar clientes mantém vínculos/histórico; não anonimiza nem exclui automaticamente backups e outras cópias. Pedidos de titulares precisam de verificação de identidade e análise pelo controlador, execução técnica auditada e atenção a anexos, exportações, logs, e-mail e backups. Um botão de exclusão indiscriminada não substitui esse processo.

## Retenção

| Categoria | Padrão técnico | Responsabilidade adicional |
| --- | --- | --- |
| Sessão | 30 minutos de inatividade; memória da instância. | Revogação, monitoramento e política de dispositivos. |
| Reset | Token de 30 minutos, uso único e limpeza de expirados/usados. | Proteger caixa postal; não registrar token/link. |
| Fila SMTP | Corpo com link temporário até envio/limpeza; processamento assíncrono no Oracle. | Restringir acesso à tabela; o hash de reset não cifra o link na fila. |
| Auditoria | `NEXUS_AUDIT_RETENTION_DAYS`, 365 dias. | Aprovar prazo e exceções com os responsáveis. |
| Notificações lidas | `NEXUS_NOTIFICATION_RETENTION_DAYS`, 90 dias. | Revisar não lidas e conteúdo. |
| Logs app | 5 arquivos de até 10 MiB por serviço. | Revisar agregador e Oracle; tamanho não é prazo em dias. |
| Mailpit dev | Até 200 mensagens/um dia, sem volume persistente configurado. | Não expor UI ou links de reset. |
| Comercial, anexos e backups | Sem exclusão automática universal. | Política por finalidade/empresa, rotina de descarte e evidências. |

Os números não são prazos legais prescritos. Configuração e limpeza precisam ser validadas em homologação. Políticas não devem guardar segredos nem apagar evidências obrigatórias sem análise apropriada.

A limpeza agendada executa às 03:00 UTC (00:00 de São Paulo no fuso atual). Remove reset após um dia da expiração e fila SMTP após um dia da criação. Auditoria usa no mínimo 30 dias e notificações lidas no mínimo 7 quando a respectiva retenção é positiva; zero/negativo desabilita a exclusão dessa categoria. Esses limites precisam estar na política operacional, inclusive para registros de e-mail que falharam.

## Operação

Mantenha `.env`, chaves, dumps e anexos fora do Git. Proteja CSV, capturas e logs. Conceda acesso necessário, revise desligamentos, use HTTPS antes de acesso remoto e registre restaurações. Mantenha responsáveis por incidentes e titulares. MFA, SSO, DLP, antivírus e auditoria externa podem ser necessários conforme o risco e não são garantidos pela entrega local.

Orientação institucional: [ANPD](https://www.gov.br/anpd/pt-br) e [materiais educativos e publicações](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes). Esta documentação descreve comportamento e lacunas técnicas; definições jurídicas cabem aos responsáveis da empresa.
