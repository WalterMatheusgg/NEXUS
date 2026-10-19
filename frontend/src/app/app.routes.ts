import { Routes } from '@angular/router';
import { authGuard,permissionGuard } from './core/auth.guard';
import { FeatureConfig } from './core/models';
import { customersConfig } from './features/customers/customers.config';
import { leadsConfig,opportunitiesConfig } from './features/sales/sales.config';
import { ticketsConfig } from './features/tickets/tickets.config';
import { tasksConfig } from './features/tasks/tasks.config';
import { processesConfig } from './features/processes/processes.config';
import { auditConfig,departmentsConfig,rolesConfig,teamsConfig,usersConfig } from './features/admin/admin.config';
const feature=(path:string,config:FeatureConfig):Routes=>[{path,loadComponent:()=>import('./shared/entity-list.component').then(module=>module.EntityListComponent),data:{config,permission:config.permission}},...(config.detail?[{path:`${path}/:id`,loadComponent:()=>import('./shared/entity-detail.component').then(module=>module.EntityDetailComponent),data:{config,permission:config.permission}}]:[])];
export const routes:Routes=[
  ...([{path:'login',mode:'login'},{path:'setup',mode:'setup'},{path:'forgot-password',mode:'forgot'},{path:'reset-password',mode:'reset'}].map(item=>({path:item.path,loadComponent:()=>import('./features/auth/auth-page.component').then(module=>module.AuthPageComponent),data:{mode:item.mode}}))),
  {path:'',loadComponent:()=>import('./core/shell.component').then(module=>module.ShellComponent),canActivate:[authGuard],canActivateChild:[permissionGuard],children:[
    {path:'',pathMatch:'full',redirectTo:'dashboard'},
    {path:'dashboard',loadComponent:()=>import('./features/dashboard/dashboard.component').then(module=>module.DashboardComponent),data:{permission:'DASHBOARD_READ'}},
    ...feature('customers',customersConfig),...feature('leads',leadsConfig),...feature('opportunities',opportunitiesConfig),...feature('tickets',ticketsConfig),...feature('tasks',tasksConfig),...feature('processes',processesConfig),
    {path:'reports',loadComponent:()=>import('./features/reports/reports.component').then(module=>module.ReportsComponent),data:{permission:'REPORTS_READ'}},
    ...feature('admin/users',usersConfig),...feature('admin/roles',rolesConfig),...feature('admin/departments',departmentsConfig),...feature('admin/teams',teamsConfig),...feature('admin/audit',auditConfig),
    {path:'admin/organization',loadComponent:()=>import('./features/admin/organization.component').then(module=>module.OrganizationComponent),data:{permission:'SETTINGS_MANAGE'}},
    {path:'profile',loadComponent:()=>import('./features/auth/profile.component').then(module=>module.ProfileComponent)},
    {path:'access-denied',loadComponent:()=>import('./shared/message-page.component').then(module=>module.MessagePageComponent),data:{code:403}},
    {path:'**',loadComponent:()=>import('./shared/message-page.component').then(module=>module.MessagePageComponent),data:{code:404}},
  ]},
];
