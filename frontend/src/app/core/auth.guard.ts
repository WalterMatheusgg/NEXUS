import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from './session.service';
export const authGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService); const router = inject(Router);
  return session.user() ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
export const permissionGuard: CanActivateFn = (route) => {
  const session = inject(SessionService); const router = inject(Router);
  return session.can(route.data['permission'] as string | undefined) ? true : router.createUrlTree(['/access-denied']);
};
