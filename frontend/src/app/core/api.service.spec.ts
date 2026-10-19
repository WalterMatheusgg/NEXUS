import { HttpErrorResponse } from '@angular/common/http';
import { FormControl } from '@angular/forms';
import { errorMessage } from './api.service';
import { strongPassword } from '../features/auth/auth-page.component';
describe('User-facing validation and errors',()=>{
  it('shows server problem details without dumping the HTTP payload',()=>{expect(errorMessage(new HttpErrorResponse({status:409,error:{detail:'CPF já cadastrado.',debug:'internal'}}))).toBe('CPF já cadastrado.');});
  it('explains connection failures in Portuguese',()=>{expect(errorMessage(new HttpErrorResponse({status:0}))).toContain('Não foi possível acessar o servidor');});
  it('rejects weak passwords and accepts long passwords with all required classes',()=>{expect(strongPassword(new FormControl('short'))).toEqual({strongPassword:true});expect(strongPassword(new FormControl('OnlyLettersLongEnough'))).toEqual({strongPassword:true});expect(strongPassword(new FormControl('LongPassword2026!'))).toBeNull();});
});
