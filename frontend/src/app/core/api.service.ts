import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  get<T>(path: string, query: Record<string, string | number | boolean | null | undefined> = {}): Promise<T> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) if (value !== null && value !== undefined && value !== '') params = params.set(key, value);
    return firstValueFrom(this.http.get<T>(`/api${path}`, { params }));
  }
  post<T>(path: string, body: unknown): Promise<T> { return firstValueFrom(this.http.post<T>(`/api${path}`, body)); }
  put<T>(path: string, body: unknown): Promise<T> { return firstValueFrom(this.http.put<T>(`/api${path}`, body)); }
  patch<T>(path: string, body: unknown): Promise<T> { return firstValueFrom(this.http.patch<T>(`/api${path}`, body)); }
  delete(path: string): Promise<void> { return firstValueFrom(this.http.delete<void>(`/api${path}`)); }
  async download(path: string, filename: string, query: Record<string, string> = {}): Promise<void> {
    const response = await firstValueFrom(this.http.get(`/api${path}`, { params: query, observe: 'response', responseType: 'blob' }));
    const blob = response.body;
    if (!blob) throw new Error('O servidor não retornou um arquivo.');
    const disposition = response.headers.get('content-disposition');
    const match = disposition?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)/i);
    const name = match?.[1] ? decodeURIComponent(match[1]).replace(/[\\/]/g, '_') : filename;
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = name;
    anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (!error.status) return 'Não foi possível acessar o servidor. Verifique sua conexão e tente novamente.';
    const body: unknown = error.error;
    if (body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string') return body.detail;
    if (error.status === 403) return 'Você não tem permissão para realizar esta ação.';
    if (error.status === 409) return 'O registro foi alterado ou já existe. Atualize a página e tente novamente.';
    if (error.status === 429) return 'Muitas tentativas. Aguarde um momento e tente novamente.';
    return `Não foi possível concluir a solicitação (HTTP ${error.status}).`;
  }
  return error instanceof Error ? error.message : 'Ocorreu um erro inesperado. Tente novamente.';
}
