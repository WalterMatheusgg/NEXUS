import { Component,OnInit,inject,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ApiService,errorMessage } from '../../core/api.service';
import { AppRecord,Choice } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';
import { label } from '../../shared/labels';
import { stages } from '../sales/sales.config';
import { ticketStatuses } from '../tickets/tickets.config';
interface Report{columns:string[];rows:Record<string,unknown>[];total:number;}
@Component({standalone:true,imports:[FormsModule,MatButtonModule,IconComponent],templateUrl:'./reports.component.html'})
export class ReportsComponent implements OnInit{
  readonly api=inject(ApiService);readonly data=signal<Report|null>(null);readonly loading=signal(false);readonly exporting=signal(false);readonly error=signal('');readonly members=signal<AppRecord[]>([]);readonly label=label;
  kind='customers';from='';to='';status='';assignee='';page=0;readonly size=25;
  readonly kinds=[{value:'customers',label:'Clientes'},{value:'opportunities',label:'Oportunidades comerciais'},{value:'tickets',label:'Chamados'},{value:'productivity',label:'Produtividade'}];
  readonly titles:Record<string,string>={name:'Nome',title:'Título',document:'CPF / CNPJ',email:'E-mail',phone:'Telefone',type:'Tipo',active:'Ativo',status:'Situação',stage:'Etapa',amount:'Valor',probability:'Probabilidade (%)',customerName:'Cliente',assigneeName:'Responsável',createdAt:'Criado em',updatedAt:'Atualizado em',dueAt:'Prazo',dueDate:'Vencimento',number:'Número',priority:'Prioridade',category:'Categoria',userName:'Funcionário',completedTasks:'Tarefas concluídas',pendingTasks:'Tarefas pendentes',resolvedTickets:'Chamados resolvidos',openTickets:'Chamados abertos',overdueTasks:'Tarefas atrasadas',expectedCloseDate:'Previsão de fechamento',total:'Total',source:'Origem',taskCount:'Tarefas',ticketCount:'Chamados'};
  ngOnInit():void{void this.api.get<AppRecord[]>('/admin/members').then(members=>this.members.set(members)).catch(()=>{});void this.run();}
  statuses():Choice[]{return this.kind==='opportunities'?stages:this.kind==='tickets'?ticketStatuses:this.kind==='customers'?[{value:'ACTIVE',label:'Ativos'},{value:'INACTIVE',label:'Inativos'}]:[{value:'PENDING',label:'Pendentes'},{value:'COMPLETED',label:'Concluídos'}];}
  query():Record<string,string>{return{from:this.from,to:this.to,status:this.status,assigneeId:this.assignee};}
  change():void{this.status='';this.page=0;void this.run();}
  async run():Promise<void>{if(this.from&&this.to&&this.from>this.to){this.error.set('A data inicial deve ser anterior à data final.');return;}this.loading.set(true);this.error.set('');this.page=0;try{this.data.set(await this.api.get<Report>(`/reports/${this.kind}`,this.query()));}catch(error){this.error.set(errorMessage(error));this.data.set(null);}finally{this.loading.set(false);}}
  async export():Promise<void>{this.exporting.set(true);this.error.set('');try{await this.api.download(`/reports/${this.kind}/export`,`nexus-${this.kind}-${new Date().toISOString().slice(0,10)}.csv`,this.query());}catch(error){this.error.set(errorMessage(error));}finally{this.exporting.set(false);}}
  rows():Record<string,unknown>[]{return this.data()?.rows.slice(this.page*this.size,(this.page+1)*this.size)??[];}
  pages():number{return Math.max(1,Math.ceil((this.data()?.rows.length??0)/this.size));}
  cell(row:Record<string,unknown>,column:string):string{const value=row[column];if(value===null||value===undefined)return'—';if(typeof value==='boolean')return value?'Sim':'Não';if(column.toLowerCase().includes('amount')||column==='pipelineValue')return new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(Number(value));if(column.endsWith('At')||column.endsWith('Date'))return new Date(String(value)).toLocaleString('pt-BR');return label(value);}
}
