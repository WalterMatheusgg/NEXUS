import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { ApiService, errorMessage } from './api.service';
import { AuthService } from './auth.service';
import { SessionService } from './session.service';
import { Notice } from './models';
import { IconComponent } from '../shared/icon.component';
interface NavigationItem {path:string;label:string;icon:string;permission:string;group:string;}
@Component({standalone:true,imports:[RouterOutlet,RouterLink,RouterLinkActive,FormsModule,MatButtonModule,MatMenuModule,DatePipe,IconComponent],templateUrl:'./shell.component.html',styleUrl:'./shell.component.scss'})
export class ShellComponent implements OnInit {
  readonly session=inject(SessionService);readonly api=inject(ApiService);readonly auth=inject(AuthService);readonly router=inject(Router);
  readonly mobileOpen=signal(false);readonly notificationOpen=signal(false);readonly notices=signal<Notice[]>([]);readonly error=signal('');readonly notificationLoading=signal(false);search='';
  readonly groups=['PRINCIPAL','OPERAÇÕES','ADMINISTRAÇÃO'];
  readonly navigation:NavigationItem[]=[{path:'/dashboard',label:'Visão geral',icon:'dashboard',permission:'DASHBOARD_READ',group:'PRINCIPAL'},{path:'/customers',label:'Clientes',icon:'users',permission:'CUSTOMERS_READ',group:'PRINCIPAL'},{path:'/leads',label:'Leads',icon:'users',permission:'SALES_READ',group:'PRINCIPAL'},{path:'/opportunities',label:'Oportunidades',icon:'briefcase',permission:'SALES_READ',group:'PRINCIPAL'},{path:'/tickets',label:'Chamados',icon:'ticket',permission:'TICKETS_READ',group:'OPERAÇÕES'},{path:'/tasks',label:'Tarefas',icon:'tasks',permission:'TASKS_READ',group:'OPERAÇÕES'},{path:'/processes',label:'Processos',icon:'process',permission:'PROCESSES_READ',group:'OPERAÇÕES'},{path:'/reports',label:'Relatórios',icon:'chart',permission:'REPORTS_READ',group:'OPERAÇÕES'},{path:'/admin/users',label:'Usuários e equipes',icon:'users',permission:'USERS_MANAGE',group:'ADMINISTRAÇÃO'},{path:'/admin/roles',label:'Perfis e permissões',icon:'shield',permission:'ROLES_MANAGE',group:'ADMINISTRAÇÃO'},{path:'/admin/organization',label:'Empresa',icon:'settings',permission:'SETTINGS_MANAGE',group:'ADMINISTRAÇÃO'},{path:'/admin/audit',label:'Auditoria',icon:'clock',permission:'AUDIT_READ',group:'ADMINISTRAÇÃO'}];
  ngOnInit():void{void this.loadNotifications();}
  items(group:string):NavigationItem[]{return this.navigation.filter(item=>item.group===group&&this.session.can(item.permission));}
  initials():string{return(this.session.user()?.name??'N').split(' ').filter(Boolean).slice(0,2).map(word=>word[0]).join('').toUpperCase();}
  unread():number{return this.notices().filter(item=>!item.read).length;}
  async loadNotifications():Promise<void>{if(!this.session.can('NOTIFICATIONS_READ'))return;this.notificationLoading.set(true);try{this.notices.set(await this.api.get<Notice[]>('/notifications'));}catch(error){this.error.set(errorMessage(error));}finally{this.notificationLoading.set(false);}}
  async openNotice(notice:Notice):Promise<void>{try{if(!notice.read){await this.api.patch(`/notifications/${notice.id}/read`,{});this.notices.update(items=>items.map(item=>item.id===notice.id?{...item,read:true}:item));}this.notificationOpen.set(false);if(notice.link?.startsWith('/')&&!notice.link.startsWith('//'))await this.router.navigateByUrl(notice.link);}catch(error){this.error.set(errorMessage(error));}}
  find():void{if(this.search.trim()&&this.session.can('CUSTOMERS_READ'))void this.router.navigate(['/customers'],{queryParams:{search:this.search.trim()}});}
  async logout():Promise<void>{try{await this.auth.logout();await this.router.navigate(['/login']);}catch(error){this.error.set(errorMessage(error));}}
}
