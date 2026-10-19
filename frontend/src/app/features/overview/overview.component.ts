import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { ApiHealthService } from '../../shared/services/api-health.service';

type ConnectionState = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatCardModule, RouterLink],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.scss',
})
export class OverviewComponent implements OnInit {
  readonly state = signal<ConnectionState>('checking');
  readonly checkedAt = signal<Date | null>(null);
  readonly errorMessage = signal<string | null>(null);

  private readonly apiHealth = inject(ApiHealthService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.state.set('checking');
    this.errorMessage.set(null);

    this.apiHealth
      .check()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (health) => {
          this.state.set(health.status === 'UP' ? 'online' : 'offline');
          this.checkedAt.set(new Date());
        },
        error: (error: HttpErrorResponse) => {
          this.state.set('offline');
          this.errorMessage.set(error.status ? `Resposta HTTP ${error.status}` : 'API indisponível');
          this.checkedAt.set(new Date());
        },
      });
  }
}
