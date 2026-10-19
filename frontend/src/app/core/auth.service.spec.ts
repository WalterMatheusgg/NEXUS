import { TestBed } from '@angular/core/testing';
import { provideHttpClient,withInterceptors } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter,Router } from '@angular/router';
import { AuthService } from './auth.service';
import { SessionService } from './session.service';
import { authInterceptor } from './auth.interceptor';
import { UserView } from './models';
import { ApiService } from './api.service';
const user:UserView={id:4,name:'Ana Silva',email:'ana@example.test',organizationId:2,organizationCode:'test',organizationName:'Empresa de teste',roleId:1,roleName:'Gestor',permissions:['CUSTOMERS_READ']};
describe('Session authentication',()=>{
  let http:HttpTestingController;let auth:AuthService;let session:SessionService;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);auth=TestBed.inject(AuthService);session=TestBed.inject(SessionService);});
  afterEach(()=>http.verify());
  it('obtains CSRF before login, sends credentials, stores the returned user and rotates CSRF',async()=>{
    const promise=auth.login({organizationCode:'test',email:'ana@example.test',password:'SecretPassword1!'});
    http.expectOne('/api/auth/csrf').flush({token:'before-login'});await Promise.resolve();await Promise.resolve();
    const request=http.expectOne('/api/auth/login');expect(request.request.withCredentials).toBe(true);expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('before-login');request.flush(user);
    await Promise.resolve();await Promise.resolve();http.expectOne('/api/auth/csrf').flush({token:'after-login'});await promise;
    expect(session.user()?.id).toBe(4);expect(session.csrf()).toBe('after-login');
  });
  it('clears expired sessions and redirects protected API requests to login',async()=>{
    session.user.set(user);const router=TestBed.inject(Router);const navigate=vi.spyOn(router,'navigate').mockResolvedValue(true);
    const promise=auth.session.user();expect(promise?.id).toBe(4);
    const api=TestBed.inject(AuthService);const requestPromise=TestBed.inject(ApiService).get('/customers').catch(()=>null);
    http.expectOne('/api/customers').flush({detail:'Session expired'},{status:401,statusText:'Unauthorized'});await requestPromise;
    expect(api.session.user()).toBeNull();expect(navigate).toHaveBeenCalledWith(['/login'],expect.objectContaining({queryParams:expect.objectContaining({expired:'1'})}));
  });
  it('does not replace login errors with a redirect',async()=>{
    const router=TestBed.inject(Router);const navigate=vi.spyOn(router,'navigate').mockResolvedValue(true);
    const result=auth.login({}).catch(error=>error);http.expectOne('/api/auth/csrf').flush({token:'csrf'});await Promise.resolve();await Promise.resolve();http.expectOne('/api/auth/login').flush({detail:'Invalid credentials'},{status:401,statusText:'Unauthorized'});await result;expect(navigate).not.toHaveBeenCalled();expect(session.user()).toBeNull();
  });
});
