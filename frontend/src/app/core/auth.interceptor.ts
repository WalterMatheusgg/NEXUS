import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const router = inject(Router);
  if (!request.url.startsWith('/api/')) return next(request);
  let authenticated = request.clone({ withCredentials: true });
  if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method) && session.csrf()) authenticated = authenticated.clone({ setHeaders: { 'X-XSRF-TOKEN': session.csrf() } });
  return next(authenticated).pipe(catchError((error: unknown) => {
    if (error instanceof HttpErrorResponse && error.status === 401 && !request.url.startsWith('/api/auth/')) {
      session.user.set(null);
      void router.navigate(['/login'], { queryParams: { returnUrl: router.url, expired: '1' } });
    }
    return throwError(() => error);
  }));
};
