import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { ApiService, errorMessage } from '../../core/api.service';
import { IconComponent } from '../../shared/icon.component';
export function strongPassword(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  return value.length >= 12 && /[a-z]/.test(value) && /[A-Z]/.test(value) && /[0-9]/.test(value) && /[^a-zA-Z0-9]/.test(value) ? null : { strongPassword: true };
}
@Component({ standalone: true, imports: [ReactiveFormsModule, RouterLink, MatButtonModule, IconComponent], templateUrl: './auth-page.component.html', styleUrl: './auth-page.component.scss' })
export class AuthPageComponent {
  readonly auth = inject(AuthService); readonly api = inject(ApiService); readonly route = inject(ActivatedRoute); readonly router = inject(Router);
  readonly mode = this.route.snapshot.data['mode'] as 'login' | 'setup' | 'forgot' | 'reset';
  readonly busy = signal(false); readonly error = signal(''); readonly message = signal(''); readonly reveal = signal(false);
  readonly titles = { login: 'Bem-vindo de volta.', setup: 'Sua empresa começa aqui.', forgot: 'Recuperar acesso.', reset: 'Definir nova senha.' };
  readonly form = new FormGroup({organizationCode:new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.maxLength(50)]}),organizationName:new FormControl('',{nonNullable:true}),name:new FormControl('',{nonNullable:true}),email:new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.email]}),password:new FormControl('',{nonNullable:true,validators:[Validators.required]}),confirmPassword:new FormControl('',{nonNullable:true}),setupToken:new FormControl('',{nonNullable:true}),token:new FormControl(this.route.snapshot.queryParamMap.get('token')??'',{nonNullable:true})});
  constructor() {
    if(this.auth.session.user() && this.mode!=='reset')void this.router.navigate(['/dashboard']);
    if(this.mode==='forgot')this.form.controls.password.clearValidators();
    if(this.mode==='reset'){this.form.controls.organizationCode.clearValidators();this.form.controls.email.clearValidators();this.form.controls.token.setValidators(Validators.required);}
    if(this.mode==='setup'){this.form.controls.organizationName.setValidators(Validators.required);this.form.controls.name.setValidators(Validators.required);this.form.controls.setupToken.setValidators(Validators.required);}
    if(this.mode==='setup'||this.mode==='reset'){this.form.controls.password.setValidators([Validators.required,strongPassword]);this.form.controls.confirmPassword.setValidators(Validators.required);}
    Object.values(this.form.controls).forEach(control=>control.updateValueAndValidity());
  }
  invalid(key:keyof typeof this.form.controls):boolean{const control=this.form.controls[key];return control.touched&&control.invalid;}
  async submit():Promise<void>{
    this.form.markAllAsTouched();if(this.form.invalid||this.busy())return;const value=this.form.getRawValue();
    if((this.mode==='setup'||this.mode==='reset')&&value.password!==value.confirmPassword){this.error.set('As senhas precisam ser iguais.');return;}
    this.busy.set(true);this.error.set('');this.message.set('');
    try{
      if(this.mode==='login'){await this.auth.login({organizationCode:value.organizationCode.trim(),email:value.email.trim(),password:value.password});const target=this.route.snapshot.queryParamMap.get('returnUrl');await this.router.navigateByUrl(target?.startsWith('/')&&!target.startsWith('//')&&!target.startsWith('/login')?target:'/dashboard');}
      else if(this.mode==='setup'){const{confirmPassword:_confirm,token:_token,...body}=value;await this.auth.setup(body);await this.router.navigate(['/dashboard']);}
      else if(this.mode==='forgot'){await this.auth.csrf();const result=await this.api.post<{message:string}>('/auth/forgot-password',{organizationCode:value.organizationCode.trim(),email:value.email.trim()});this.message.set(result.message);}
      else{await this.auth.csrf();await this.api.post('/auth/reset-password',{token:value.token.trim(),password:value.password});this.message.set('Senha atualizada. Você já pode entrar com a nova senha.');}
    }catch(error){this.error.set(errorMessage(error));}finally{this.busy.set(false);}
  }
}
