import { Component,OnInit,inject,signal } from '@angular/core';
import { CurrencyPipe,DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { ApiService,errorMessage } from '../core/api.service';
import { SessionService } from '../core/session.service';
import { AppRecord,FeatureConfig,Field } from '../core/models';
import { EntityFormDialogComponent,FormDialogData } from './entity-form-dialog.component';
import { ConfirmDialogComponent } from './confirm-dialog.component';
import { IconComponent } from './icon.component';
import { label } from './labels';
interface DetailSection{key:string;title:string;action?:string;fields?:Field[];}
@Component({standalone:true,imports:[RouterLink,FormsModule,CurrencyPipe,DatePipe,MatButtonModule,IconComponent],templateUrl:'./entity-detail.component.html'})
export class EntityDetailComponent implements OnInit{
  readonly route=inject(ActivatedRoute);readonly api=inject(ApiService);readonly session=inject(SessionService);readonly dialog=inject(MatDialog);readonly snack=inject(MatSnackBar);
  readonly config=this.route.snapshot.data['config'] as FeatureConfig;readonly id=Number(this.route.snapshot.paramMap.get('id'));readonly record=signal<AppRecord|null>(null);readonly loading=signal(true);readonly error=signal('');readonly nested=signal<Record<string,AppRecord[]>>({});readonly uploading=signal(false);readonly label=label;readonly sections:DetailSection[]=this.buildSections();
  ngOnInit():void{void this.load();}
  canWrite():boolean{return!!this.config.writePermission&&this.session.can(this.config.writePermission);}
  buildSections():DetailSection[]{
    switch(this.config.key){
      case'customers':return[{key:'contacts',title:'Contatos',action:'Adicionar contato',fields:[{key:'name',label:'Nome',required:true},{key:'email',label:'E-mail',type:'email'},{key:'phone',label:'Telefone'},{key:'position',label:'Cargo'}]},{key:'interactions',title:'Histórico de relacionamento',action:'Registrar interação',fields:[{key:'note',label:'Interação',type:'textarea',required:true}]}];
      case'opportunities':return[{key:'interactions',title:'Interações comerciais',action:'Registrar interação',fields:[{key:'note',label:'Interação',type:'textarea',required:true}]},{key:'history',title:'Histórico de alterações'}];
      case'tickets':return[{key:'comments',title:'Comentários',action:'Adicionar comentário',fields:[{key:'body',label:'Comentário',type:'textarea',required:true}]},{key:'attachments',title:'Anexos'},{key:'history',title:'Histórico do atendimento'}];
      case'processes':return[{key:'steps',title:'Etapas e aprovações',action:'Adicionar etapa',fields:[{key:'title',label:'Título da etapa',required:true},{key:'assigneeId',label:'Responsável',type:'lookup',lookup:'/admin/members'},{key:'requiresApproval',label:'Exige aprovação',type:'checkbox'}]},{key:'history',title:'Histórico de movimentações'}];
      default:return[];
    }
  }
  async load():Promise<void>{this.loading.set(true);this.error.set('');try{this.record.set(await this.api.get<AppRecord>(`${this.config.endpoint}/${this.id}`));await this.loadNested();}catch(error){this.error.set(errorMessage(error));}finally{this.loading.set(false);}}
  async loadNested():Promise<void>{await Promise.all(this.sections.map(async section=>{try{const rows=await this.api.get<AppRecord[]>(`${this.config.endpoint}/${this.id}/${section.key}`);this.nested.update(value=>({...value,[section.key]:rows}));}catch(error){this.error.set(errorMessage(error));}}));}
  title():string{return String(this.record()?.['name']??this.record()?.['title']??`Registro #${this.id}`);}
  value(key:string):string{return label(this.record()?.[key]);}
  date(value:unknown):string|null{return value?String(value):null;}
  amount():number{return Number(this.record()?.['amount']??0);}
  async edit():Promise<void>{const record=this.record();if(!record)return;const saved=await firstValueFrom(this.dialog.open(EntityFormDialogComponent,{width:'720px',maxWidth:'95vw',data:{title:`Editar ${this.config.singular}`,fields:this.config.fields,record,save:body=>this.api.put(`${this.config.endpoint}/${this.id}`,body)} satisfies FormDialogData}).afterClosed());if(saved){this.snack.open('Registro atualizado.','Fechar',{duration:3000});await this.load();}}
  async add(section:DetailSection):Promise<void>{const saved=await firstValueFrom(this.dialog.open(EntityFormDialogComponent,{width:'640px',maxWidth:'95vw',data:{title:section.action??'Adicionar registro',fields:section.fields??[],save:body=>this.api.post(`${this.config.endpoint}/${this.id}/${section.key}`,body)} satisfies FormDialogData}).afterClosed());if(saved){this.snack.open('Registro adicionado.','Fechar',{duration:3000});await this.loadNested();}}
  async removeContact(row:AppRecord):Promise<void>{const confirmed=await firstValueFrom(this.dialog.open(ConfirmDialogComponent,{width:'420px',data:{title:'Remover contato?',message:`O contato ${String(row['name'])} será removido deste cliente.`}}).afterClosed());if(confirmed)try{await this.api.delete(`/customers/${this.id}/contacts/${row.id}`);await this.loadNested();this.snack.open('Contato removido.','Fechar',{duration:3000});}catch(error){this.error.set(errorMessage(error));}}
  async status(event:Event):Promise<void>{const select=event.target as HTMLSelectElement;const value=select.value;const key=this.config.key==='opportunities'?'stage':'status';if(this.record()?.[key]===value)return;try{await this.api.patch(`${this.config.endpoint}/${this.id}/${key}`,{[key]:value});await this.load();}catch(error){select.value=String(this.record()?.[key]??'');this.error.set(errorMessage(error));}}
  async step(row:AppRecord,event:Event):Promise<void>{try{await this.api.patch(`/processes/${this.id}/steps/${row.id}`,{status:(event.target as HTMLSelectElement).value});await this.load();}catch(error){(event.target as HTMLSelectElement).value=String(row['status']);this.error.set(errorMessage(error));}}
  history(row:AppRecord):string{if(row['detail']||row['note']||row['body'])return String(row['detail']??row['note']??row['body']);const from=row['fromStatus']??row['fromStage']??row['oldStatus']??row['oldValue'];const to=row['toStatus']??row['toStage']??row['newStatus']??row['newValue'];return from||to?`${label(from)} → ${label(to)}`:label(row['action']??row['status']??'Registro atualizado');}
  async upload(event:Event):Promise<void>{const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;if(file.size>10*1024*1024){this.error.set('O arquivo deve ter no máximo 10 MB.');input.value='';return;}this.uploading.set(true);this.error.set('');try{const data=new FormData();data.append('file',file);await this.api.post(`/tickets/${this.id}/attachments`,data);await this.loadNested();this.snack.open('Anexo enviado.','Fechar',{duration:3000});}catch(error){this.error.set(errorMessage(error));}finally{this.uploading.set(false);input.value='';}}
  async download(row:AppRecord):Promise<void>{try{await this.api.download(`/tickets/${this.id}/attachments/${row.id}`,String(row['fileName']??'anexo'));}catch(error){this.error.set(errorMessage(error));}}
  size(row:AppRecord):string{const size=Number(row['size']??0);return size>1024*1024?`${(size/1024/1024).toFixed(1)} MB`:`${Math.ceil(size/1024)} KB`;}
}
