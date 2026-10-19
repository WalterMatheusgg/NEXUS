export const LABELS: Record<string, string> = {
  PERSON: 'Pessoa física', COMPANY: 'Pessoa jurídica', NEW: 'Novo', CONTACTED: 'Contato iniciado', QUALIFIED: 'Qualificado', CONVERTED: 'Convertido', LOST: 'Perdido',
  NEW_LEAD: 'Novo lead', QUALIFICATION: 'Qualificação', PROPOSAL: 'Proposta', NEGOTIATION: 'Negociação', WON: 'Ganho',
  LOW: 'Baixa', MEDIUM: 'Média', HIGH: 'Alta', URGENT: 'Urgente', OPEN: 'Aberto', IN_PROGRESS: 'Em andamento', WAITING: 'Aguardando', RESOLVED: 'Resolvido', CLOSED: 'Fechado',
  TODO: 'A fazer', DONE: 'Concluída', COMPLETED: 'Concluído', CANCELLED: 'Cancelado', PENDING: 'Pendente', APPROVED: 'Aprovada', REJECTED: 'Rejeitada', ACTIVE: 'Ativa', INACTIVE: 'Inativa',
  DASHBOARD_READ: 'Visualizar dashboard', CUSTOMERS_READ: 'Consultar clientes', CUSTOMERS_WRITE: 'Gerenciar clientes', SALES_READ: 'Consultar comercial', SALES_WRITE: 'Gerenciar comercial',
  TICKETS_READ: 'Consultar chamados', TICKETS_WRITE: 'Gerenciar chamados', TASKS_READ: 'Consultar tarefas', TASKS_WRITE: 'Gerenciar tarefas', PROCESSES_READ: 'Consultar processos', PROCESSES_WRITE: 'Gerenciar processos',
  REPORTS_READ: 'Consultar relatórios', USERS_MANAGE: 'Gerenciar usuários', ROLES_MANAGE: 'Gerenciar perfis e permissões', SETTINGS_MANAGE: 'Configurações da empresa', AUDIT_READ: 'Consultar auditoria', NOTIFICATIONS_READ: 'Consultar notificações',
};
export function label(value: unknown): string { const text = String(value ?? '—'); return LABELS[text] ?? text; }
