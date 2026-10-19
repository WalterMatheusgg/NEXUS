import { Component, input } from '@angular/core';
const PATHS: Record<string, string> = {
  dashboard: 'M3 3h7v8H3z M14 3h7v5h-7z M3 15h7v6H3z M14 12h7v9h-7z',
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8',
  briefcase: 'M3 7h18v14H3z M8 7V3h8v4 M3 12h18 M10 12v3h4v-3',
  ticket: 'M3 3h18v6a3 3 0 0 0 0 6v6H3v-6a3 3 0 0 0 0-6z M14 3v18',
  tasks: 'M9 5h12 M9 12h12 M9 19h12 M2 5l2 2 3-4 M2 12l2 2 3-4 M2 19l2 2 3-4',
  process: 'M3 3h6v6H3z M15 15h6v6h-6z M6 9v9h9 M15 3h6v6h-6z M9 6h6 M18 9v6',
  chart: 'M3 3v18h18 M7 16v-5 M12 16V7 M17 16V3',
  settings: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1z',
  shield: 'M12 2l9 4v6c0 6-9 10-9 10S3 18 3 12V6z M8 12l3 3 5-6',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9 M10 21h4',
  search: 'M21 21l-5-5 M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14',
  menu: 'M3 6h18 M3 12h18 M3 18h18',
  plus: 'M12 5v14 M5 12h14',
  arrow: 'M5 12h14 M14 7l5 5-5 5',
  logout: 'M9 3H3v18h6 M9 12h12 M16 7l5 5-5 5',
  chevron: 'M9 5l7 7-7 7',
  clock: 'M12 8v4l3 2 M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20',
  check: 'M5 12l4 4L19 6',
  download: 'M12 3v12 M7 10l5 5 5-5 M3 16v5h18v-5',
};
@Component({ selector: 'app-icon', standalone: true, template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="path()" /></svg>', styles: [':host{display:inline-flex;width:20px;height:20px;flex:0 0 20px}svg{width:100%;height:100%}'] })
export class IconComponent { readonly name = input('dashboard'); path(): string { return PATHS[this.name()] ?? PATHS['dashboard'] ?? ''; } }
