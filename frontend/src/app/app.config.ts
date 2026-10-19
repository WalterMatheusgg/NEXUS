import { ApplicationConfig, inject, LOCALE_ID, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { AuthService } from './core/auth.service';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [{provide:LOCALE_ID,useValue:'pt-BR'}, provideBrowserGlobalErrorListeners(), provideHttpClient(withInterceptors([authInterceptor])), provideRouter(routes), provideAppInitializer(() => inject(AuthService).restore())],
};
