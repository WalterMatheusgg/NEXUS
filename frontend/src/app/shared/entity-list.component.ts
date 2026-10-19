import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe,DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute,Router,RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { ApiService,errorMessage } from '../core/api.service';
import { SessionService } from '../core/session.service';
import { AppRecord,Column,FeatureConfig,Page } from '../core/models';
import { EntityFormDialogComponent,FormDialogData } from './entity-form-dialog.component';
import { IconComponent } from './icon.component';
import { label } from './labels';

@Component({standalone:true,imports:[FormsModule,RouterLink,CurrencyPipe,DatePipe,MatButtonModule,IconComponent],templateUrl:'./entity-list.component.html'})
export class EntityListComponent implements OnInit{
  readonly route=inject(ActivatedRoute);readonly router=inject(Router);readonly api=inject(ApiService);readonly session=inject(SessionService);readonly dialog=inject(MatDialog);readonly snack=inject(MatSnackBar);readonly destroy=inject(DestroyRef);
  readonly config=this.route.snapshot.data['config'] as FeatureConfig;readonly rows=signal<AppRecord[]>([]);readonly members=signal<AppRecord[]>([]);readonly loading=signal(false);readonly error=signal('');readonly total=signal(0);readonly totalPages=signal(0);readonly board=signal(false);readonly label=label;
  search='';status='';assignee='';from='';to='';sort='createdAt';direction='desc';page=0;size=20;private dragging:number|null=null;private sequence=0;
  ngOnInit():void{
    if(this.config.key==='roles')void this.permissions();
    if(!this.config.endpoint.startsWith('/admin/'))void this.api.get<AppRecord[]>('/admin/members').then(members=>this.members.set(members)).catch(()=>{});
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(params=>{this.search=params.get('search')??'';void this.load();if(params.has('create')&&this.canWrite()){void this.edit();void this.router.navigate([],{relativeTo:this.route,queryParams:{create:null},queryParamsHandling:'merge',replaceUrl:true});}});
  }
  async permissions():Promise<void>{try{const permissions=await this.api.get<string[]>('/admin/permissions');const field=this.config.fields.find(item=>item.key==='permissions');if(field)field.choices=permissions.map(value=>({value,label:label(value)}));}catch(error){this.error.set(errorMessage(error));}}
  canWrite():boolean{return !!this.config.writePermission&&this.session.can(this.config.writePermission);}
  async load():Promise<void>{
    const sequence=++this.sequence;this.loading.set(true);this.error.set('');
    try{
      const result=await this.api.get<Page<AppRecord>|AppRecord[]>(this.config.endpoint,{page:this.page,size:this.size,search:this.search,status:this.config.statusKey==='active'?undefined:this.status,active:this.config.statusKey==='active'?this.status:undefined,assigneeId:this.assignee,from:this.from,to:this.to,sort:this.sort,direction:this.direction});
      if(sequence!==this.sequence)return;
      if(Array.isArray(result)){const filtered=result.filter(item=>Object.values(item).join(' ').toLocaleLowerCase().includes(this.search.toLocaleLowerCase()));this.rows.set(filtered);this.total.set(filtered.length);this.totalPages.set(1);}else{this.rows.set(result.content);this.total.set(result.totalElements);this.totalPages.set(result.totalPages);}
    }catch(error){if(sequence===this.sequence){this.error.set(errorMessage(error));this.rows.set([]);}}
    finally{if(sequence===this.sequence)this.loading.set(false);}
  }
  filter():void{this.page=0;void this.load();}
  clear():void{this.search='';this.status='';this.assignee='';this.from='';this.to='';this.filter();}
  go(page:number):void{this.page=page;void this.load();}
  toggleBoard(value:boolean):void{this.board.set(value);this.size=value?100:20;this.page=0;void this.load();}
  value(row:AppRecord,column:Column):string{const value=row[column.key];if(Array.isArray(value))return `${value.length} permissões`;if(value===null||value===undefined||value==='')return '—';return column.format==='label'?label(value):String(value);}
  amount(row:AppRecord):number{return Number(row['amount']??0);}
  date(row:AppRecord,key:string):string|null{return row[key]?String(row[key]):null;}
  boardRows(value:string|number):AppRecord[]{return this.rows().filter(row=>row[this.config.board?.key??'status']===value);}
  sum(rows:AppRecord[]):number{return rows.reduce((total,row)=>total+this.amount(row),0);}
  async edit(row?:AppRecord):Promise<void>{
    if(!this.canWrite())return;
    const data:FormDialogData={title:`${row?'Editar':'Novo'} ${this.config.singular}`,fields:this.config.fields,record:row,save:body=>row?this.api.put(`${this.config.endpoint}/${row.id}`,body):this.api.post(this.config.endpoint,body)};
    const saved=await firstValueFrom(this.dialog.open(EntityFormDialogComponent,{data,width:'720px',maxWidth:'95vw',autoFocus:'first-tabbable'}).afterClosed());
    if(saved){this.snack.open('Registro salvo com sucesso.','Fechar',{duration:3500});void this.load();}
  }
  drag(row:AppRecord,event:DragEvent):void{if(!this.canWrite())return;this.dragging=row.id;event.dataTransfer?.setData('text/plain',String(row.id));if(event.dataTransfer)event.dataTransfer.effectAllowed='move';}
  allow(event:DragEvent):void{if(this.canWrite())event.preventDefault();}
  drop(value:string|number,event:DragEvent):void{event.preventDefault();const id=this.dragging;this.dragging=null;const row=this.rows().find(item=>item.id===id);if(row)void this.move(row,String(value));}
  async move(row:AppRecord,value:string):Promise<void>{const board=this.config.board;if(!board||!this.canWrite())return;try{await this.api.patch(`${this.config.endpoint}/${row.id}/${board.action}`,{[board.key]:value});this.snack.open('Etapa atualizada.','Fechar',{duration:2500});await this.load();}catch(error){this.error.set(errorMessage(error));}}
  changeStage(row:AppRecord,event:Event):void{void this.move(row,(event.target as HTMLSelectElement).value);}
  async convert(row:AppRecord):Promise<void>{
    const saved=await firstValueFrom(this.dialog.open(EntityFormDialogComponent,{width:'600px',maxWidth:'95vw',data:{title:'Converter lead em cliente',fields:[{key:'type',label:'Tipo de cliente',type:'select',required:true,choices:[{value:'PERSON',label:'Pessoa física'},{value:'COMPANY',label:'Pessoa jurídica'}]},{key:'document',label:'CPF / CNPJ',required:true},{key:'name',label:'Nome / razão social',required:true}],record:{name:row['name']},save:body=>this.api.post(`/leads/${row.id}/convert`,body)} satisfies FormDialogData}).afterClosed());
    if(saved){this.snack.open('Lead convertido em cliente.','Fechar',{duration:3500});await this.load();}
  }
  async password(row:AppRecord):Promise<void>{const saved=await firstValueFrom(this.dialog.open(EntityFormDialogComponent,{width:'520px',data:{title:`Redefinir senha de ${String(row['name'])}`,fields:[{key:'password',label:'Nova senha',type:'password',required:true,minLength:12,hint:'Mínimo 12 caracteres, maiúscula, minúscula, número e símbolo.'}],save:body=>this.api.patch(`/admin/users/${row.id}/password`,body)} satisfies FormDialogData}).afterClosed());if(saved)this.snack.open('Senha redefinida.','Fechar',{duration:3500});}
}
