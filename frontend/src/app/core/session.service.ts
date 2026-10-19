import { Injectable, signal } from '@angular/core';
import { UserView } from './models';
@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly user = signal<UserView | null>(null);
  readonly csrf = signal('');
  can(permission?: string): boolean { return !permission || !!this.user()?.permissions.includes(permission); }
}
