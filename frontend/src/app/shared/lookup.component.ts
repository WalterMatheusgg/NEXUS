import { Component, DestroyRef, forwardRef, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api.service';
import { AppRecord, Page } from '../core/models';

@Component({
  selector: 'app-lookup', standalone: true,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => LookupComponent), multi: true }],
  template: `<div class="lookup"><input [value]="text()" [disabled]="disabled" (input)="search($event)" (focus)="open()" (blur)="blur()" autocomplete="off" role="combobox" aria-autocomplete="list" [attr.aria-expanded]="expanded()" [attr.aria-controls]="listId" [attr.aria-label]="label()" placeholder="Digite para pesquisar…" />
    @if (text()) { <button class="lookup-clear" type="button" (click)="clear()" aria-label="Limpar seleção">×</button> }
    @if (expanded()) { <div class="lookup-results" role="listbox" [id]="listId">
      @if (loading()) { <span class="lookup-message">Pesquisando…</span> }
      @for (item of items(); track item.id) { <button type="button" role="option" [attr.aria-selected]="item.id === value" (mousedown)="$event.preventDefault()" (click)="select(item)">{{ display(item) }} @if(item['email']) { <small>{{ item['email'] }}</small> }</button> }
      @if (!loading() && !items().length) { <span class="lookup-message">{{ error() || 'Nenhum resultado. Refine a pesquisa.' }}</span> }
    </div> }
  </div>`,
})
export class LookupComponent implements ControlValueAccessor {
  readonly endpoint = input.required<string>(); readonly label = input('Registro');
  readonly text = signal(''); readonly items = signal<AppRecord[]>([]); readonly expanded = signal(false); readonly loading = signal(false); readonly error = signal('');
  readonly listId = `lookup-${Math.random().toString(36).slice(2)}`;
  readonly api = inject(ApiService); private readonly destroy = inject(DestroyRef);
  disabled = false; value: number | null = null; private sequence = 0; private timer: ReturnType<typeof setTimeout> | undefined;
  private onChange: (value: number | null) => void = () => {}; private onTouched: () => void = () => {};
  constructor() { this.destroy.onDestroy(() => clearTimeout(this.timer)); }
  writeValue(value: unknown): void { this.value = value ? Number(value) : null; if(this.value) void this.resolve(this.value); else this.text.set(''); }
  registerOnChange(fn: (value: number | null) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(value: boolean): void { this.disabled = value; }
  display(item: AppRecord): string { return String(item['name'] ?? item['title'] ?? item['number'] ?? item.id); }
  async resolve(id: number): Promise<void> {
    try {
      if(this.endpoint() === '/admin/members' || this.endpoint().startsWith('/admin/')) {
        const result = await this.api.get<AppRecord[] | Page<AppRecord>>(this.endpoint(), {size:100, search:''});
        const row = (Array.isArray(result) ? result : result.content).find(item => item.id === id); this.text.set(row ? this.display(row) : `Registro ${id}`);
      } else this.text.set(this.display(await this.api.get<AppRecord>(`${this.endpoint()}/${id}`)));
    } catch { this.text.set(`Registro ${id}`); }
  }
  open(): void { if (!this.disabled) { this.expanded.set(true); void this.load(this.value ? '' : this.text()); } }
  blur(): void { this.onTouched(); setTimeout(() => this.expanded.set(false), 160); }
  search(event: Event): void {
    this.text.set((event.target as HTMLInputElement).value); this.value = null; this.onChange(null); this.expanded.set(true);
    clearTimeout(this.timer); this.timer = setTimeout(() => void this.load(this.text()), 250);
  }
  async load(search: string): Promise<void> {
    const sequence = ++this.sequence; this.loading.set(true); this.error.set('');
    try {
      const result = await this.api.get<AppRecord[] | Page<AppRecord>>(this.endpoint(), {search,size:30});
      const rows = Array.isArray(result) ? result.filter(item => this.display(item).toLocaleLowerCase().includes(search.toLocaleLowerCase())) : result.content;
      if(sequence === this.sequence) this.items.set(rows);
    } catch(error) { if(sequence === this.sequence) {this.items.set([]); this.error.set(errorMessage(error));} }
    finally { if(sequence === this.sequence) this.loading.set(false); }
  }
  select(item: AppRecord): void { this.value = item.id; this.text.set(this.display(item)); this.onChange(item.id); this.onTouched(); this.expanded.set(false); }
  clear(): void { this.value = null; this.text.set(''); this.onChange(null); this.onTouched(); }
}
