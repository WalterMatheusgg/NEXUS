import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { ApiService,errorMessage } from '../../core/api.service';
import { SessionService } from '../../core/session.service';
import { IconComponent } from '../../shared/icon.component';
import { label } from '../../shared/labels';
interface Activity{id:number;action:string;entityType:string;detail:string;actorName:string;createdAt:string;}
interface Dashboard{totalCustomers:number;newCustomersThisMonth:number;openOpportunities:number;pipelineValue:number;openTickets:number;resolvedTickets:number;pendingTasks:number;overdueTickets:number;overdueTasks:number;conversionRate:number;opportunitiesByStage:{label:string;count:number;value:number}[];ticketsByStatus:{label:string;count:number}[];recentActivities:Activity[];}
@Component({standalone:true,imports:[CurrencyPipe,DatePipe,DecimalPipe,RouterLink,MatButtonModule,IconComponent],templateUrl:'./dashboard.component.html'})
export class DashboardComponent implements OnInit{
  readonly api=inject(ApiService);readonly session=inject(SessionService);readonly data=signal<Dashboard|null>(null);readonly loading=signal(true);readonly error=signal('');readonly label=label;readonly now=new Date();
  ngOnInit():void{void this.load();}
  async load():Promise<void>{this.loading.set(true);this.error.set('');try{this.data.set(await this.api.get<Dashboard>('/dashboard'));}catch(error){this.error.set(errorMessage(error));}finally{this.loading.set(false);}}
  firstName():string{return this.session.user()?.name.split(' ')[0]??'';}
  width(value:number,rows:{count:number}[]):number{const maximum=Math.max(...rows.map(row=>row.count),1);return value/maximum*100;}
}
