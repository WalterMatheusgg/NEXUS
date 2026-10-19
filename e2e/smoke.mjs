#!/usr/bin/env node
/** Real-browser smoke checks. Requires Node 24 and local Chrome, without npm dependencies.
 * Read only by default: node e2e/smoke.mjs
 * Login: provide E2E_ORGANIZATION_CODE, E2E_EMAIL, E2E_PASSWORD through the environment.
 * Explicit test-fixture writes: add --write. Setup additionally requires --setup and
 * NEXUS_SETUP_TOKEN; only use a disposable test organization coordinated with its owner.
 * The script never deletes database records. Output artifacts live under /tmp.
 */
import { spawn } from 'node:child_process';
import { mkdtemp, readFile, writeFile, mkdir, readdir, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomBytes } from 'node:crypto';

const options = new Set(process.argv.slice(2));
if (options.has('--help')) {
  process.stdout.write('Nexus browser smoke: Node24 + Chrome. Default public-page checks. Credentials via E2E_ORGANIZATION_CODE/E2E_EMAIL/E2E_PASSWORD. --write enables coordinated fixture CRUD; --setup enables first-organization setup with NEXUS_SETUP_TOKEN. Artifacts under /tmp.\n');
  process.exit(0);
}
const origin = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4200';
const artifactDirectory = await mkdtemp(join(tmpdir(), 'nexus-browser-qa-'));
const profileDirectory = join(artifactDirectory, 'chrome-profile');
const downloadsDirectory = join(artifactDirectory, 'downloads');
await mkdir(downloadsDirectory);
const results = [], browserErrors = [], networkErrors = [];
let browser, client, currentPhase = 'initialization';
const pause = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
const safeUrl = value => { try { const url = new URL(value); return `${url.origin}${url.pathname}`; } catch { return '[unavailable]'; } };
function assert(condition, message) { if (!condition) throw new Error(message); }

class DevTools {
  constructor(socket) {
    this.socket = socket;
    this.sequence = 0;
    this.pending = new Map();
    this.listeners = new Map();
    socket.addEventListener('message', event => {
      const message = JSON.parse(String(event.data));
      if (message.id) {
        const pending = this.pending.get(message.id);
        if (!pending) return;
        this.pending.delete(message.id);
        clearTimeout(pending.timeout);
        message.error ? pending.reject(new Error(message.error.message)) : pending.resolve(message.result ?? {});
      } else for (const callback of this.listeners.get(message.method) ?? []) callback(message.params);
    });
  }
  static async connect(url) {
    const socket = new WebSocket(url);
    await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, { once: true }); socket.addEventListener('error', reject, { once: true }); });
    return new DevTools(socket);
  }
  on(method, callback) { this.listeners.set(method, [...(this.listeners.get(method) ?? []), callback]); }
  send(method, params = {}) {
    const id = ++this.sequence;
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => { this.pending.delete(id); reject(new Error(`DevTools timeout: ${method}`)); }, 15000);
      this.pending.set(id, { resolve, reject, timeout });
      this.socket.send(JSON.stringify({ id, method, params }));
    });
  }
  close() { this.socket.close(); }
}

async function evaluate(callback, ...arguments_) {
  const expression = `(${callback.toString()})(...${JSON.stringify(arguments_)})`;
  const result = await client.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true, userGesture: true });
  if (result.exceptionDetails) throw new Error('A browser evaluation failed; inspect the current screenshot.');
  return result.result.value;
}
async function until(callback, description, timeout = 15000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) { try { if (await callback()) return; } catch { /* Navigation can temporarily replace the JS execution context. */ } await pause(150); }
  throw new Error(`Timed out waiting for ${description}.`);
}
async function visible(selector) { return evaluate(selector_ => [...document.querySelectorAll(selector_)].some(element => element.getClientRects().length > 0), selector); }
async function waitSelector(selector) { await until(() => visible(selector), selector); }
async function click(selector) {
  await waitSelector(selector);
  const clicked = await evaluate(selector_ => { const element = [...document.querySelectorAll(selector_)].find(item => item.getClientRects().length > 0 && !item.disabled); if (!element) return false; element.click(); return true; }, selector);
  assert(clicked, `No clickable element: ${selector}`);
}
async function button(label, selector = 'button') {
  await until(() => evaluate((text, selector_) => [...document.querySelectorAll(selector_)].some(item => item.getClientRects().length > 0 && !item.disabled && item.textContent.trim() === text), label, selector), `button ${label}`);
  await evaluate((text, selector_) => [...document.querySelectorAll(selector_)].find(item => item.getClientRects().length > 0 && !item.disabled && item.textContent.trim() === text).click(), label, selector);
}
async function fill(selector, value) {
  await waitSelector(selector);
  await evaluate((selector_, value_) => {
    const element = [...document.querySelectorAll(selector_)].find(item => item.getClientRects().length > 0);
    const prototype = element.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    Object.getOwnPropertyDescriptor(prototype, 'value').set.call(element, value_);
    element.dispatchEvent(new Event('input', { bubbles: true })); element.dispatchEvent(new Event('change', { bubbles: true })); element.dispatchEvent(new Event('blur', { bubbles: true }));
  }, selector, String(value));
}
async function select(selector, label) {
  await waitSelector(selector);
  const selected = await evaluate((selector_, label_) => {
    const element = [...document.querySelectorAll(selector_)].find(item => item.getClientRects().length > 0);
    const option = [...element.options].find(item => item.textContent.trim() === label_ || item.value === label_);
    if (!option) return false;
    element.value = option.value; element.dispatchEvent(new Event('change', { bubbles: true })); return true;
  }, selector, label);
  assert(selected, `Option not found for ${selector}.`);
}
async function lookup(label, term) {
  const selector = `input[role="combobox"][aria-label="${label}"]`;
  await fill(selector, term);
  await until(() => evaluate(term_ => [...document.querySelectorAll('[role="option"]')].some(item => item.textContent.includes(term_)), term), `lookup ${label}`);
  await evaluate(term_ => [...document.querySelectorAll('[role="option"]')].find(item => item.textContent.includes(term_)).click(), term);
}
async function navigate(path, heading) {
  await client.send('Page.navigate', { url: new URL(path, origin).href });
  await until(() => evaluate(path_ => document.readyState === 'complete' && location.pathname === path_, new URL(path, origin).pathname), 'document ready');
  if (heading) await until(() => evaluate(text => [...document.querySelectorAll('h1,h2')].some(item => item.textContent.includes(text)), heading), `heading ${heading}`);
  await until(() => evaluate(() => !document.querySelector('.loading-state')), 'API-backed page');
  await pause(180);
}
async function screenshot(name) {
  const { data } = await client.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
  await writeFile(join(artifactDirectory, `${name}.png`), Buffer.from(data, 'base64'));
}
async function check(name, callback) {
  currentPhase = name;
  try { await callback(); results.push({ name, passed: true }); process.stdout.write(`PASS ${name}\n`); }
  catch (error) { await screenshot(`failure-${results.length}`).catch(() => {}); results.push({ name, passed: false, message: error.message }); throw error; }
}
async function saveDialog() {
  await button('Salvar', 'mat-dialog-container button');
  await until(() => evaluate(() => !document.querySelector('mat-dialog-container') || !!document.querySelector('mat-dialog-container [role="alert"]')), 'dialog save');
  assert(!await visible('mat-dialog-container [role="alert"]'), 'The API rejected the dialog submission.');
  await until(() => evaluate(() => !document.querySelector('mat-dialog-container')), 'dialog close');
  await until(() => evaluate(() => !document.querySelector('.loading-state')), 'list reload');
}
async function pageHas(text) { return evaluate(text_ => document.body.innerText.includes(text_), text); }
async function browserApi(path) { return evaluate(async path_ => { const response = await fetch(`/api${path_}`, { credentials: 'same-origin' }); if (!response.ok) return { status: response.status }; return { status: response.status, body: await response.json() }; }, path); }
function cpf() { const digits = [...randomBytes(9)].map(value => value % 10); for (let length = 9; length <= 10; length++) { const sum = digits.reduce((total, digit, index) => total + digit * (length + 1 - index), 0); const remainder = sum % 11; digits.push(remainder < 2 ? 0 : 11 - remainder); } return digits.join(''); }

async function publicPages() {
  await check('public login page', async () => { await navigate('/login', 'Bem-vindo'); await waitSelector('#password'); await screenshot('login-desktop'); });
  await check('empty login validation', async () => { await button('Entrar na plataforma'); assert(await visible('.field-error'), 'Empty login should show field validation.'); });
  await check('public recovery page', async () => { await navigate('/forgot-password', 'Recuperar acesso'); await waitSelector('#email'); });
  await check('protected route guard', async () => { await client.send('Page.navigate', { url: new URL('/customers', origin).href }); await until(() => evaluate(() => location.pathname === '/login' && !!document.querySelector('#password')), 'login redirect'); assert(await evaluate(() => location.pathname === '/login'), 'Customers page should require login.'); });
  await check('mobile login layout', async () => { await client.send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true }); await navigate('/login', 'Bem-vindo'); assert(await evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), 'Mobile login has horizontal overflow.'); await screenshot('login-mobile'); await client.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false }); });
}
async function authenticate() {
  const code = process.env.E2E_ORGANIZATION_CODE, email = process.env.E2E_EMAIL, password = process.env.E2E_PASSWORD;
  if (!code || !email || !password) { assert(!options.has('--setup') && !options.has('--write'), 'Fixture writes require E2E_ORGANIZATION_CODE, E2E_EMAIL and E2E_PASSWORD.'); return false; }
  await check(options.has('--setup') ? 'initial organization setup through UI' : 'login through UI', async () => {
    await navigate(options.has('--setup') ? '/setup' : '/login', options.has('--setup') ? 'Sua empresa' : 'Bem-vindo');
    await fill('#organizationCode', code); await fill('#email', email); await fill('#password', password);
    if (options.has('--setup')) { assert(options.has('--write'), 'Setup requires explicit --write.'); assert(process.env.NEXUS_SETUP_TOKEN, 'NEXUS_SETUP_TOKEN is required for setup.'); await fill('#organizationName', process.env.E2E_ORGANIZATION_NAME ?? `QA temporário ${code}`); await fill('#name', 'Operador QA temporário'); await fill('#confirmPassword', password); await fill('#setupToken', process.env.NEXUS_SETUP_TOKEN); await button('Criar empresa'); }
    else await button('Entrar na plataforma');
    await until(() => evaluate(() => location.pathname === '/dashboard'), 'authenticated dashboard');
    await waitSelector('h1'); assert((await browserApi('/auth/me')).status === 200, 'Session was not established.');
    await screenshot('dashboard-desktop');
  });
  return true;
}
async function authenticatedPages() {
  for (const [path, title] of [['/customers','Clientes'],['/leads','Leads'],['/opportunities','Oportunidades'],['/tickets','Chamados'],['/tasks','Tarefas'],['/processes','Processos'],['/reports','Relatórios'],['/admin/users','Usuários'],['/admin/roles','Perfis'],['/admin/organization','empresa'],['/admin/audit','Auditoria']]) {
    await check(`authenticated page ${path}`, async () => { await navigate(path, title); assert(!await visible('main [role="alert"]'), 'Page displayed an API error.'); });
  }
  await check('mobile authenticated layout', async () => { await client.send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true }); await navigate('/dashboard'); assert(await evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), 'Mobile dashboard has horizontal overflow.'); await screenshot('dashboard-mobile'); await client.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false }); });
}
async function fixtureFlows() {
  const marker = `QA_BROWSER_${Date.now()}`;
  const fixture = { marker };
  await writeFile(join(artifactDirectory, 'fixture.json'), JSON.stringify(fixture, null, 2));
  await check('create customer through UI', async () => {
    await navigate('/customers','Clientes'); await button('Novo cliente'); await select('#field-type','Pessoa física'); await fill('#field-name',marker); await fill('#field-document',cpf()); await fill('#field-email',`${marker.toLowerCase()}@example.invalid`); await saveDialog(); assert(await pageHas(marker), 'Created customer was not displayed.');
    const response = await browserApi(`/customers?search=${encodeURIComponent(marker)}`); fixture.customerId = response.body?.content?.find(item => item.name === marker)?.id; assert(fixture.customerId, 'Customer was not persisted.'); await writeFile(join(artifactDirectory,'fixture.json'),JSON.stringify(fixture,null,2));
  });
  await check('edit persisted customer through UI', async () => { await evaluate(marker_ => [...document.querySelectorAll('tbody tr')].find(row => row.innerText.includes(marker_)).querySelector('button').click(), marker); await fill('#field-phone','11999990000'); await saveDialog(); assert((await browserApi(`/customers/${fixture.customerId}`)).body?.phone === '11999990000', 'Edited phone was not persisted.'); });
  await check('customer interaction through UI', async () => { await navigate(`/customers/${fixture.customerId}`,marker); await button('Registrar interação'); await fill('#field-note',`Verificação de relacionamento ${marker}`); await saveDialog(); assert(await pageHas(`Verificação de relacionamento ${marker}`), 'Interaction was not displayed.'); });
  await check('create ticket through UI', async () => {
    await navigate('/tickets','Chamados'); await button('Novo chamado'); await fill('#field-title',`${marker} chamado`); await lookup('Cliente',marker); await fill('#field-category','QA temporário'); await select('#field-priority','Alta'); await select('#field-status','Aberto'); await fill('#field-description','Teste automatizado de navegador com persistência Oracle.'); await saveDialog();
    const response = await browserApi(`/tickets?search=${encodeURIComponent(marker)}`); fixture.ticketId = response.body?.content?.find(item => item.title === `${marker} chamado`)?.id; assert(fixture.ticketId, 'Ticket was not persisted.'); await writeFile(join(artifactDirectory,'fixture.json'),JSON.stringify(fixture,null,2));
  });
  await check('ticket status and comment through UI', async () => { await navigate(`/tickets/${fixture.ticketId}`,`${marker} chamado`); await select('#detail-status','Em andamento'); await until(async () => (await browserApi(`/tickets/${fixture.ticketId}`)).body?.status === 'IN_PROGRESS','persisted ticket status'); await button('Adicionar comentário'); await fill('#field-body',`Comentário ${marker}`); await saveDialog(); assert(await pageHas(`Comentário ${marker}`),'Comment was not displayed.'); });
  await check('opportunity Kanban persists stage', async () => {
    await navigate('/opportunities','Oportunidades'); await button('Novo oportunidade'); await fill('#field-title',`${marker} oportunidade`); await select('#field-stage','Proposta'); await fill('#field-amount','1234.56'); await fill('#field-probability','60'); await saveDialog();
    const response = await browserApi(`/opportunities?search=${encodeURIComponent(marker)}`); fixture.opportunityId = response.body?.content?.find(item => item.title === `${marker} oportunidade`)?.id; assert(fixture.opportunityId,'Opportunity was not persisted.'); await writeFile(join(artifactDirectory,'fixture.json'),JSON.stringify(fixture,null,2));
    await button('Kanban'); await until(() => visible('.kanban-card'),'Kanban card'); await evaluate(marker_ => { const card=[...document.querySelectorAll('.kanban-card')].find(item=>item.innerText.includes(marker_)); if(!card) throw new Error('QA card missing'); const select_=card.querySelector('select'); select_.value='WON'; select_.dispatchEvent(new Event('change',{bubbles:true})); },marker); await until(async () => (await browserApi(`/opportunities/${fixture.opportunityId}`)).body?.stage === 'WON','persisted Kanban stage');
  });
  await check('real report and CSV download', async () => { await navigate('/reports','Relatórios'); await select('#report-type','Clientes'); await button('Gerar relatório'); await until(() => pageHas(marker),'customer report result'); await button('Exportar CSV'); await until(async () => (await readdir(downloadsDirectory)).some(name => name.endsWith('.csv')),'CSV file download'); const file=(await readdir(downloadsDirectory)).find(name => name.endsWith('.csv')); const text=await readFile(join(downloadsDirectory,file),'utf8'); assert(text.includes(marker),'Downloaded CSV does not include the persisted customer.'); });
  await writeFile(join(artifactDirectory, 'fixture.json'), JSON.stringify(fixture, null, 2));
  process.stdout.write(`Fixture identifiers saved in ${join(artifactDirectory,'fixture.json')}; coordinate their removal separately.\n`);
}

try {
  const ready = await fetch(origin);
  assert(ready.ok, `Frontend HTTP status ${ready.status}.`);
  browser = spawn(process.env.CHROME_BIN ?? '/usr/bin/google-chrome', ['--headless=new','--disable-gpu','--disable-dev-shm-usage','--no-first-run','--no-default-browser-check','--remote-debugging-address=127.0.0.1','--remote-debugging-port=0',`--user-data-dir=${profileDirectory}`,'about:blank'], { stdio: ['ignore','ignore','ignore'] });
  let debuggingPort;
  await until(async () => { try { debuggingPort = Number((await readFile(join(profileDirectory,'DevToolsActivePort'),'utf8')).split('\n')[0]); return !!debuggingPort; } catch { return false; } },'Chrome debugger');
  const target = await (await fetch(`http://127.0.0.1:${debuggingPort}/json/new?about:blank`, { method: 'PUT' })).json();
  client = await DevTools.connect(target.webSocketDebuggerUrl);
  client.on('Runtime.exceptionThrown', event => browserErrors.push({ phase: currentPhase, kind: 'exception', description: event.exceptionDetails.text }));
  client.on('Runtime.consoleAPICalled', event => { if(event.type === 'error') browserErrors.push({ phase: currentPhase, kind: 'console', description: event.args.map(argument => argument.description ?? argument.value ?? '').join(' ').slice(0,300) }); });
  client.on('Network.responseReceived', event => { const { status,url } = event.response; if(status >= 400 && !(status === 401 && new URL(url).pathname === '/api/auth/me')) networkErrors.push({ phase: currentPhase,status,url:safeUrl(url) }); });
  client.on('Network.loadingFailed', event => { if(!event.canceled && event.errorText !== 'net::ERR_ABORTED') networkErrors.push({ phase:currentPhase,status:'failed',description:event.errorText }); });
  await client.send('Runtime.enable'); await client.send('Page.enable'); await client.send('Network.enable');
  await client.send('Browser.setDownloadBehavior',{behavior:'allow',downloadPath:downloadsDirectory,eventsEnabled:true});
  await client.send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
  await publicPages();
  if(await authenticate()) { await authenticatedPages(); if(options.has('--write')) await fixtureFlows(); }
  assert(browserErrors.length === 0,'Browser emitted JavaScript errors.'); assert(networkErrors.length === 0,'Browser emitted unexpected network errors.');
} catch(error) {
  process.exitCode = 1; process.stderr.write(`FAIL ${currentPhase}: ${error.message}\n`);
} finally {
  await writeFile(join(artifactDirectory,'results.json'),JSON.stringify({origin,results,browserErrors,networkErrors},null,2));
  client?.close(); browser?.kill('SIGTERM');
  if(browser) await Promise.race([new Promise(resolve => browser.once('exit',resolve)),pause(3000)]);
  await rm(profileDirectory,{recursive:true,force:true}).catch(() => {});
  process.stdout.write(`Browser QA artifacts: ${artifactDirectory}\n`);
}
