/**
 * CRM Agente de Carga — Backend en Google Apps Script
 * ----------------------------------------------------
 * La hoja de cálculo de Google (Drive) es la base de datos:
 *   - Hoja "Oportunidades": una fila por RO (mismas columnas que plantilla_oportunidades.xlsx)
 *   - Hoja "Usuarios": usuarios y contraseñas (guardadas como hash, nunca en texto plano)
 *   - Hoja "Config": parámetros compartidos (p. ej. PrimaMinima)
 *
 * Archivos HTML del proyecto:
 *   - Index  → contenido de control_oportunidades_crm.html
 *   - Gantt  → contenido de gantt_arribos.html
 */

const SHEET_OPS = 'Oportunidades';
const SHEET_USERS = 'Usuarios';
const SHEET_CFG = 'Config';

const MILESTONES = ['Booking confirmado', 'Gate-in', 'Loaded', 'In transit', 'Discharged', 'Delivered'];
const HEADERS = ['RO', 'Consignatario', 'Ruta', 'Servicio', 'Naviera', 'ETD', 'ETA', 'Estado', 'Prioridad', 'Semáforo',
  'Pendiente USD', 'Broker', 'Comisión USD', 'Seguro prima USD', 'Estado seguro', 'Cierre', 'Validez',
  'Agente/Coload', 'Ref cliente'].concat(MILESTONES).concat(['Alertas', 'Creado por', 'Actualizado por', 'Actualizado']);
const DATE_HEADERS = ['ETD', 'ETA', 'Cierre', 'Validez'].concat(MILESTONES);
const NUM_HEADERS = ['Pendiente USD', 'Comisión USD', 'Seguro prima USD'];
const USER_HEADERS = ['Usuario', 'Nombre', 'Rol', 'Activo', 'Salt', 'Hash', 'Último acceso'];
const ROLES = ['admin', 'editor', 'lector'];

const SESSION_SECONDS = 21600;     // 6 horas (máximo de CacheService)
const MAX_FAILED = 5;              // intentos fallidos antes de bloquear
const LOCK_SECONDS = 900;          // 15 minutos de bloqueo
const HASH_ROUNDS = 300;

// ===================== WEB APP =====================
function doGet(e) {
  const page = e && e.parameter && e.parameter.page === 'gantt' ? 'Gantt' : 'Index';
  return HtmlService.createHtmlOutputFromFile(page)
    .setTitle(page === 'Gantt' ? 'Calendario de Arribos' : 'Control de Oportunidades')
    .addMetaTag('viewport', 'width=device-width, initial-scale=1');
}

// ===================== MENÚ EN LA HOJA =====================
function onOpen() {
  SpreadsheetApp.getUi().createMenu('CRM')
    .addItem('1. Preparar hojas', 'setupSheets')
    .addItem('2. Crear usuario / cambiar contraseña', 'adminSetUser')
    .addItem('Activar / desactivar usuario', 'adminToggleUser')
    .addToUi();
}

function ss_() {
  const id = PropertiesService.getScriptProperties().getProperty('SHEET_ID');
  return id ? SpreadsheetApp.openById(id) : SpreadsheetApp.getActiveSpreadsheet();
}

function setupSheets() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  PropertiesService.getScriptProperties().setProperty('SHEET_ID', ss.getId());

  const ops = ss.getSheetByName(SHEET_OPS) || ss.insertSheet(SHEET_OPS);
  const lastCol = Math.max(ops.getLastColumn(), 1);
  const current = ops.getRange(1, 1, 1, lastCol).getValues()[0].map(String);
  HEADERS.forEach(h => {
    if (current.indexOf(h) === -1) {
      const col = current.filter(String).length + 1;
      ops.getRange(1, col).setValue(h);
      current[col - 1] = h;
    }
  });
  ops.getRange(1, 1, 1, ops.getLastColumn()).setFontWeight('bold').setBackground('#0f172a').setFontColor('#ffffff');
  ops.setFrozenRows(1);
  const heads = ops.getRange(1, 1, 1, ops.getLastColumn()).getValues()[0];
  DATE_HEADERS.forEach(h => {
    const c = heads.indexOf(h) + 1;
    if (c > 0) ops.getRange(2, c, ops.getMaxRows() - 1, 1).setNumberFormat('dd/mm/yyyy');
  });

  const users = ss.getSheetByName(SHEET_USERS) || ss.insertSheet(SHEET_USERS);
  if (users.getLastRow() === 0) users.appendRow(USER_HEADERS);
  users.getRange(1, 1, 1, USER_HEADERS.length).setFontWeight('bold').setBackground('#0f172a').setFontColor('#ffffff');
  users.setFrozenRows(1);

  const cfg = ss.getSheetByName(SHEET_CFG) || ss.insertSheet(SHEET_CFG);
  if (cfg.getLastRow() === 0) { cfg.appendRow(['Clave', 'Valor']); cfg.appendRow(['PrimaMinima', 60]); }

  SpreadsheetApp.getUi().alert('Hojas listas. Ahora crea tu usuario con: CRM → 2. Crear usuario / cambiar contraseña.');
}

function adminSetUser() {
  const ui = SpreadsheetApp.getUi();
  const ask = (title, msg) => {
    const r = ui.prompt(title, msg, ui.ButtonSet.OK_CANCEL);
    if (r.getSelectedButton() !== ui.Button.OK) throw new Error('Cancelado');
    return r.getResponseText().trim();
  };
  try {
    const usuario = ask('Usuario', 'Nombre de usuario (sin espacios), p. ej. mlopez:').toLowerCase();
    if (!/^[a-z0-9._-]{3,}$/.test(usuario)) return ui.alert('Usuario inválido: mínimo 3 caracteres, sin espacios.');
    const nombre = ask('Nombre', 'Nombre para mostrar, p. ej. María López:');
    let rol = ask('Rol', 'Rol: admin, editor o lector').toLowerCase();
    if (ROLES.indexOf(rol) === -1) rol = 'editor';
    const pass = ask('Contraseña', 'Contraseña (mínimo 8 caracteres). Se guarda cifrada (hash).');
    if (pass.length < 8) return ui.alert('La contraseña debe tener al menos 8 caracteres.');

    const sh = ss_().getSheetByName(SHEET_USERS);
    const salt = Utilities.getUuid();
    const row = [usuario, nombre, rol, 'SI', salt, hash_(salt, pass), ''];
    const idx = findUserRow_(sh, usuario);
    if (idx > 0) sh.getRange(idx, 1, 1, row.length).setValues([row]);
    else sh.appendRow(row);
    ui.alert('Usuario "' + usuario + '" guardado con rol ' + rol + '.');
  } catch (err) {
    if (err.message !== 'Cancelado') throw err;
  }
}

function adminToggleUser() {
  const ui = SpreadsheetApp.getUi();
  const r = ui.prompt('Activar / desactivar', 'Usuario:', ui.ButtonSet.OK_CANCEL);
  if (r.getSelectedButton() !== ui.Button.OK) return;
  const sh = ss_().getSheetByName(SHEET_USERS);
  const idx = findUserRow_(sh, r.getResponseText().trim().toLowerCase());
  if (idx < 0) return ui.alert('No existe ese usuario.');
  const cell = sh.getRange(idx, 4);
  const nuevo = String(cell.getValue()).toUpperCase() === 'SI' ? 'NO' : 'SI';
  cell.setValue(nuevo);
  ui.alert('Usuario ' + (nuevo === 'SI' ? 'activado' : 'desactivado') + '.');
}

// ===================== SEGURIDAD =====================
function hash_(salt, pass) {
  let h = salt + '|' + pass;
  for (let i = 0; i < HASH_ROUNDS; i++) {
    h = Utilities.base64Encode(Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, h + salt, Utilities.Charset.UTF_8));
  }
  return h;
}

function findUserRow_(sh, usuario) {
  const vals = sh.getRange(1, 1, Math.max(sh.getLastRow(), 1), 1).getValues();
  for (let i = 1; i < vals.length; i++) if (String(vals[i][0]).trim().toLowerCase() === usuario) return i + 1;
  return -1;
}

function login(usuario, password) {
  usuario = String(usuario || '').trim().toLowerCase();
  const cache = CacheService.getScriptCache();
  const failKey = 'fail_' + usuario;
  const fails = Number(cache.get(failKey) || 0);
  if (fails >= MAX_FAILED) throw new Error('Demasiados intentos fallidos. Espera 15 minutos.');

  const sh = ss_().getSheetByName(SHEET_USERS);
  const idx = sh ? findUserRow_(sh, usuario) : -1;
  let ok = false, row = null;
  if (idx > 0) {
    row = sh.getRange(idx, 1, 1, USER_HEADERS.length).getValues()[0];
    ok = String(row[3]).toUpperCase() === 'SI' && hash_(String(row[4]), String(password || '')) === String(row[5]);
  }
  if (!ok) {
    cache.put(failKey, String(fails + 1), LOCK_SECONDS);
    Utilities.sleep(800);
    throw new Error('Usuario o contraseña incorrectos.');
  }
  cache.remove(failKey);
  sh.getRange(idx, 7).setValue(new Date());
  const session = { usuario: usuario, nombre: String(row[1] || usuario), rol: String(row[2] || 'editor').toLowerCase() };
  const token = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '');
  cache.put('s_' + token, JSON.stringify(session), SESSION_SECONDS);
  return { token: token, usuario: session.usuario, nombre: session.nombre, rol: session.rol };
}

function logout(token) {
  CacheService.getScriptCache().remove('s_' + token);
  return true;
}

function session_(token) {
  const cache = CacheService.getScriptCache();
  const raw = token && cache.get('s_' + token);
  if (!raw) throw new Error('SESION_EXPIRADA');
  cache.put('s_' + token, raw, SESSION_SECONDS); // sesión deslizante
  return JSON.parse(raw);
}

function requireEditor_(s) {
  if (s.rol === 'lector') throw new Error('Tu usuario es de solo lectura.');
}

// ===================== DATOS =====================
function opsSheet_() {
  const sh = ss_().getSheetByName(SHEET_OPS);
  if (!sh) throw new Error('No existe la hoja "' + SHEET_OPS + '". Ejecuta CRM → 1. Preparar hojas.');
  return sh;
}

function headers_(sh) {
  return sh.getRange(1, 1, 1, sh.getLastColumn()).getValues()[0].map(h => String(h).trim());
}

function fmtCell_(v) {
  if (v instanceof Date) return Utilities.formatDate(v, Session.getScriptTimeZone(), 'dd/MM/yyyy');
  return v;
}

function toCell_(header, v) {
  if (v === null || v === undefined) return '';
  if (DATE_HEADERS.indexOf(header) !== -1) {
    const m = String(v).match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    return m ? new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1])) : '';
  }
  if (NUM_HEADERS.indexOf(header) !== -1) return Number(v) || 0;
  return String(v);
}

function getData(token) {
  const s = session_(token);
  const sh = opsSheet_();
  const heads = headers_(sh);
  const last = sh.getLastRow();
  const values = last > 1 ? sh.getRange(2, 1, last - 1, heads.length).getValues() : [];
  const roCol = heads.indexOf('RO');
  const rows = values.filter(r => String(r[roCol]).trim()).map(r => {
    const o = {};
    heads.forEach((h, i) => { if (h) o[h] = fmtCell_(r[i]); });
    return o;
  });
  return {
    rows: rows,
    config: getConfig_(),
    user: s,
    sheetUrl: s.rol === 'admin' ? ss_().getUrl() : '',
    appUrl: ScriptApp.getService().getUrl()
  };
}

function findRoRow_(sh, heads, ro) {
  const roCol = heads.indexOf('RO') + 1;
  const last = sh.getLastRow();
  if (last < 2) return -1;
  const vals = sh.getRange(2, roCol, last - 1, 1).getValues();
  const key = String(ro).trim().toLowerCase();
  for (let i = 0; i < vals.length; i++) if (String(vals[i][0]).trim().toLowerCase() === key) return i + 2;
  return -1;
}

function createRO(token, obj) {
  const s = session_(token);
  requireEditor_(s);
  const ro = String(obj && obj.RO || '').trim();
  if (!ro) throw new Error('El RO es obligatorio.');
  const lock = LockService.getScriptLock();
  lock.waitLock(20000);
  try {
    const sh = opsSheet_();
    const heads = headers_(sh);
    if (findRoRow_(sh, heads, ro) > 0) throw new Error('Ya existe el RO ' + ro + '.');
    obj['Creado por'] = s.usuario;
    obj['Actualizado por'] = s.usuario;
    const row = heads.map(h => h === 'Actualizado' ? new Date() : (h in obj ? toCell_(h, obj[h]) : ''));
    sh.appendRow(row);
  } finally {
    lock.releaseLock();
  }
  return { ok: true };
}

function updateRO(token, ro, patch) {
  const s = session_(token);
  requireEditor_(s);
  const lock = LockService.getScriptLock();
  lock.waitLock(20000);
  try {
    const sh = opsSheet_();
    const heads = headers_(sh);
    const r = findRoRow_(sh, heads, ro);
    if (r < 0) throw new Error('No se encontró el RO ' + ro + '.');
    patch['Actualizado por'] = s.usuario;
    const range = sh.getRange(r, 1, 1, heads.length);
    const row = range.getValues()[0];
    heads.forEach((h, i) => {
      if (h === 'RO') return; // el RO no se cambia
      if (h === 'Actualizado') row[i] = new Date();
      else if (h in patch) row[i] = toCell_(h, patch[h]);
    });
    range.setValues([row]);
  } finally {
    lock.releaseLock();
  }
  return { ok: true };
}

function getConfig_() {
  const sh = ss_().getSheetByName(SHEET_CFG);
  const cfg = {};
  if (sh && sh.getLastRow() > 1) sh.getRange(2, 1, sh.getLastRow() - 1, 2).getValues().forEach(r => { if (r[0]) cfg[String(r[0])] = r[1]; });
  return cfg;
}

function setConfig(token, key, value) {
  const s = session_(token);
  requireEditor_(s);
  if (['PrimaMinima'].indexOf(key) === -1) throw new Error('Parámetro no permitido.');
  const sh = ss_().getSheetByName(SHEET_CFG) || ss_().insertSheet(SHEET_CFG);
  const last = sh.getLastRow();
  const keys = last > 0 ? sh.getRange(1, 1, last, 1).getValues().map(r => String(r[0])) : [];
  const idx = keys.indexOf(key);
  if (idx >= 0) sh.getRange(idx + 1, 2).setValue(value);
  else sh.appendRow([key, value]);
  return { ok: true };
}
