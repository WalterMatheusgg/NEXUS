import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot,provideRouter,Router,RouterStateSnapshot,UrlTree } from '@angular/router';
import { authGuard,permissionGuard } from './auth.guard';
import { SessionService } from './session.service';
describe('Access guards',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideRouter([])]}));
  it('preserves a protected destination for unauthenticated users',()=>{
    const result=TestBed.runInInjectionContext(()=>authGuard({} as ActivatedRouteSnapshot,{url:'/customers/12'} as RouterStateSnapshot));expect(result).toBeInstanceOf(UrlTree);expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fcustomers%2F12');
  });
  it('refuses a feature when the permission is missing',()=>{
    const route=new ActivatedRouteSnapshot();route.data={permission:'USERS_MANAGE'};
    const result=TestBed.runInInjectionContext(()=>permissionGuard(route,{} as RouterStateSnapshot));expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/access-denied');
  });
  it('allows authenticated users to open their profile without business permissions',()=>{expect(TestBed.runInInjectionContext(()=>permissionGuard({data:{}} as ActivatedRouteSnapshot,{} as RouterStateSnapshot))).toBe(true);expect(TestBed.inject(SessionService).can('USERS_MANAGE')).toBe(false);});
});
