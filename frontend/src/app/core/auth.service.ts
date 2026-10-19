import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { SessionService } from './session.service';
import { UserView } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  readonly session = inject(SessionService);
  readonly status = signal({ setupRequired: false, setupEnabled: false });
  readonly ready = signal(false);
  async csrf(): Promise<void> { const value = await this.api.get<{ token: string }>('/auth/csrf'); this.session.csrf.set(value.token); }
  async restore(): Promise<void> {
    try {
      await this.csrf();
      this.status.set(await this.api.get<{ setupRequired: boolean; setupEnabled: boolean }>('/auth/status'));
      try { this.session.user.set(await this.api.get<UserView>('/auth/me')); } catch { this.session.user.set(null); }
    } catch { this.session.user.set(null); }
    finally { this.ready.set(true); }
  }
  async login(body: unknown): Promise<void> { await this.csrf(); this.session.user.set(await this.api.post<UserView>('/auth/login', body)); await this.csrf(); }
  async setup(body: unknown): Promise<void> { await this.csrf(); this.session.user.set(await this.api.post<UserView>('/auth/setup', body)); await this.csrf(); }
  async logout(): Promise<void> { await this.api.post('/auth/logout', {}); this.session.user.set(null); await this.csrf(); }
}
