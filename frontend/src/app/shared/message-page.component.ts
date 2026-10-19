import { Component,inject } from '@angular/core';
import { ActivatedRoute,RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
@Component({standalone:true,imports:[RouterLink,MatButtonModule],template:'<div class="empty message-page"><span class="error-code">{{code}}</span><h1>{{code===403?"Acesso restrito.":"Página não encontrada."}}</h1><p>{{code===403?"Seu perfil não tem permissão para acessar esta área. Procure o administrador da sua empresa.":"O endereço pode ter mudado. Use o menu para continuar."}}</p><a mat-flat-button routerLink="/profile">Ir para meu perfil</a></div>'})
export class MessagePageComponent{readonly code=Number(inject(ActivatedRoute).snapshot.data['code']??404);}
