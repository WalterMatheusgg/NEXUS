import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { ApiService, errorMessage } from '../core/api.service';
import { AppRecord, Field } from '../core/models';
import { LookupComponent } from './lookup.component';
import { label } from './labels';
export interface FormDialogData { title: string; fields: Field[]; record?: Partial<AppRecord>; save: (body: Record<string, unknown>) => Promise<unknown>; }
@Component({ standalone: true, imports: [MatDialogModule, MatButtonModule, ReactiveFormsModule, LookupComponent], templateUrl: './entity-form-dialog.component.html' })
export class EntityFormDialogComponent {
  readonly data = inject<FormDialogData>(MAT_DIALOG_DATA); readonly ref = inject(MatDialogRef<EntityFormDialogComponent>);
  readonly api = inject(ApiService); readonly busy = signal(false); readonly error = signal(''); readonly label = label;
  readonly fields = this.data.fields.filter(field => !field.createOnly || !this.data.record?.id);
  readonly form = new FormGroup<Record<string, FormControl<unknown>>>({});
  constructor() {
    for(const field of this.fields) {
      const validators: ValidatorFn[] = [];
      if(field.required) validators.push(Validators.required);
      if(field.type === 'email') validators.push(Validators.email);
      if(field.min !== undefined) validators.push(Validators.min(field.min));
      if(field.max !== undefined) validators.push(Validators.max(field.max));
      if(field.minLength !== undefined) validators.push(Validators.minLength(field.minLength));
      if(field.maxLength !== undefined) validators.push(Validators.maxLength(field.maxLength));
      let value: unknown = this.data.record?.[field.key] ?? (field.type === 'checkbox' ? field.key === 'active' : field.type === 'permissions' ? [] : field.type === 'select' ? field.choices?.[0]?.value ?? null : field.type === 'number' ? 0 : null);
      if(field.type === 'datetime-local' && typeof value === 'string') { const date = new Date(value); value = new Date(date.getTime()-date.getTimezoneOffset()*60000).toISOString().slice(0,16); }
      this.form.addControl(field.key, new FormControl<unknown>(value, validators));
    }
  }
  invalid(key: string): boolean { const control = this.form.controls[key]; return !!control?.invalid && !!control.touched; }
  checked(key: string, value: string | number): boolean { const current = this.form.controls[key]?.value; return Array.isArray(current) && current.includes(value); }
  toggle(key: string, value: string | number, event: Event): void { const control = this.form.controls[key]; if(!control)return; const current: unknown[] = Array.isArray(control.value) ? control.value : []; control.setValue((event.target as HTMLInputElement).checked ? [...current,value] : current.filter(item=>item!==value)); control.markAsTouched(); }
  async submit(): Promise<void> {
    this.form.markAllAsTouched(); if(this.form.invalid || this.busy()) return;
    this.busy.set(true); this.error.set(''); this.ref.disableClose = true;
    const body = this.form.getRawValue();
    for(const field of this.fields) {
      if(typeof body[field.key] === 'string') body[field.key] = (body[field.key] as string).trim() || null;
      if(field.type === 'datetime-local' && body[field.key]) body[field.key] = new Date(String(body[field.key])).toISOString();
    }
    if(this.data.record?.version !== undefined) body['version'] = this.data.record.version;
    try { await this.data.save(body); this.ref.close(true); }
    catch(error) { this.error.set(errorMessage(error)); }
    finally { this.busy.set(false); this.ref.disableClose = false; }
  }
}
