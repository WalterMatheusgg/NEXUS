export interface UserView {
  id: number; name: string; email: string; organizationId: number;
  organizationCode: string; organizationName: string; roleId: number;
  roleName: string; permissions: string[];
}
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; page: number; size: number; }
export interface AppRecord { id: number; version?: number; [key: string]: unknown; }
export interface Choice { value: string | number; label: string; }
export interface Field {
  key: string; label: string; type?: 'text' | 'email' | 'password' | 'number' | 'textarea' | 'select' | 'checkbox' | 'date' | 'datetime-local' | 'permissions' | 'lookup';
  required?: boolean; min?: number; max?: number; minLength?: number; maxLength?: number;
  choices?: Choice[]; lookup?: string; hint?: string; createOnly?: boolean;
}
export interface Column { key: string; label: string; format?: 'money' | 'date' | 'datetime' | 'boolean' | 'label'; }
export interface FeatureConfig {
  key: string; title: string; singular: string; description: string; endpoint: string;
  permission: string; writePermission?: string; columns: Column[]; fields: Field[];
  statusKey?: string; statuses?: Choice[]; detail?: boolean; array?: boolean;
  board?: { key: string; choices: Choice[]; action: string };
}
export interface Notice { id: number; title: string; message: string; link: string; read: boolean; createdAt: string; }
