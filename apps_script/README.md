# CRM en Google Drive: instalación (15 minutos, una sola vez)

La **hoja de Google Sheets** en tu Drive es la base de datos.
Un **Google Apps Script** pegado dentro de esa hoja publica las dos páginas (CRM y Calendario de arribos) con **usuario y contraseña**.

```
Google Sheet (Drive)                    Apps Script (dentro de la hoja)            Navegador / celular
┌─────────────────────┐   lee/escribe   ┌──────────────────────────────┐   URL    ┌────────────────────┐
│ Oportunidades       │ ◄─────────────► │ Code.gs  (login + datos)     │ ◄──────► │ Login → CRM        │
│ Usuarios (hash)     │                 │ Index    (CRM)               │          │        → Calendario│
│ Config (PrimaMinima)│                 │ Gantt    (Calendario)        │          └────────────────────┘
└─────────────────────┘                 └──────────────────────────────┘
```

## 1. La hoja en Drive

Abre la hoja **"CRM Oportunidades"** en tu Google Drive.
Si no la tienes, sube `plantilla_oportunidades.xlsx` a Drive y ábrela con Google Sheets (Archivo → Guardar como Hojas de cálculo de Google).

Debe tener tres pestañas: **Oportunidades**, **Usuarios** y **Config**.

## 2. Pegar el código

1. En la hoja: **Extensiones → Apps Script**.
2. Archivo `Código.gs`: borra lo que haya y pega el contenido de `apps_script/Code.gs`.
3. Botón **＋ → HTML**, nómbralo exactamente **`Index`**. Pega el contenido completo de `control_oportunidades_crm.html`.
4. Botón **＋ → HTML**, nómbralo exactamente **`Gantt`**. Pega el contenido completo de `gantt_arribos.html`.
5. Guarda (💾).

## 3. Preparar hojas y crear usuarios

1. Vuelve a la hoja y **recárgala**. Aparece el menú **CRM**.
2. **CRM → 1. Preparar hojas**. La primera vez Google pide autorizar el script: acepta con tu cuenta.
   - Si sale "Google no verificó esta app": *Configuración avanzada → Ir a (nombre del proyecto)*. Es tu propio script.
3. **CRM → 2. Crear usuario / cambiar contraseña**. Crea tu usuario con rol **admin**.
   - Repite para cada persona: rol **editor** (puede registrar y editar RO) o **lector** (solo ver).
   - Las contraseñas se guardan cifradas (hash con sal). En la pestaña Usuarios nunca verás la contraseña.
   - Para quitar el acceso a alguien: **CRM → Activar / desactivar usuario**.

## 4. Publicar la aplicación

1. En Apps Script: **Implementar → Nueva implementación → tipo "Aplicación web"**.
2. **Ejecutar como:** *Yo*.
3. **Quién tiene acceso:**
   - *Cualquier usuario*: cualquiera con la URL ve la pantalla de login; sin usuario y contraseña no ve datos.
   - *Cualquier usuario con cuenta de Google*: además exige iniciar sesión en Google (más seguro).
4. **Implementar** y copia la **URL de la aplicación web** (termina en `/exec`). Esa es la dirección que compartes con tu equipo.

> Cada vez que cambies el código: **Implementar → Gestionar implementaciones → ✏️ → Versión: Nueva → Implementar**. La URL no cambia.

## Uso diario

| Qué | Cómo |
|---|---|
| Registrar un RO nuevo | Botón **＋ Nuevo RO** → completa la ventana de registro → *Registrar RO*. Se agrega una fila en la hoja. |
| Avanzar el timeline | Abre el RO → **✏️ Editar** → pon la fecha del hito cumplido (Gate-in, Loaded, In transit…). También puedes escribir la fecha directo en la hoja. |
| Prioridad, seguro, prima mínima | Se guardan en la hoja al cambiarlos en pantalla. La prima mínima es la misma para todos (pestaña Config). |
| Ver la hoja | El admin tiene el botón **Abrir Google Sheet** en la cabecera. |
| Calendario | Botón **📅 Calendario de arribos**: usa los mismos datos y la misma sesión. |

## Seguridad: qué protege y qué no

- El usuario y la contraseña se validan **en el servidor de Google**. Sin una sesión válida el servidor no entrega ningún dato.
- Tras 5 intentos fallidos el usuario queda bloqueado 15 minutos.
- La sesión dura 6 horas sin actividad. El botón **Salir** la cierra.
- El rol **lector** no puede crear ni modificar nada (el servidor lo rechaza).
- Mantén la **hoja** compartida solo contigo (o con quien administre). Quien tenga acceso de edición a la hoja ve y modifica todos los datos sin pasar por el login.

## Modo local

Si abres `control_oportunidades_crm.html` directamente (sin Apps Script), funciona en **modo local**: sin login, con datos de ejemplo o importados desde Excel, guardados solo en ese navegador. Sirve para pruebas; para el uso real del equipo usa la URL de Apps Script.
