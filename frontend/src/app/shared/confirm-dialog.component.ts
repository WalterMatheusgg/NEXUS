import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
@Component({ standalone: true, imports: [MatDialogModule, MatButtonModule], template: '<h2 mat-dialog-title>{{ data.title }}</h2><mat-dialog-content><p>{{ data.message }}</p></mat-dialog-content><mat-dialog-actions align="end"><button mat-button (click)="ref.close(false)">Cancelar</button><button mat-flat-button (click)="ref.close(true)">Confirmar</button></mat-dialog-actions>' })
export class ConfirmDialogComponent { readonly data = inject<{title: string; message: string}>(MAT_DIALOG_DATA); readonly ref = inject(MatDialogRef<ConfirmDialogComponent>); }
