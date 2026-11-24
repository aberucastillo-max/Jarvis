# Explorador (Android) — estilo Explorador de Windows

App de exploración de archivos para tablet (pensada para Galaxy Tab S6 Lite en horizontal).

## Funciones
- **Vista Detalles** con columnas Nombre / Fecha de modificación / Tipo / Tamaño (y Ubicación al buscar).
  Toca un encabezado para ordenar (otra vez = invertir), arrastra el borde izquierdo de una columna para redimensionarla.
- **Ordenar** por nombre (orden natural: archivo2 < archivo10), fecha, tipo, tamaño; carpetas siempre primero.
- **Agrupar** por nombre (A-H, I-P…), fecha (Hoy, Ayer, La semana pasada…), tipo o tamaño (Diminuto, Pequeño…),
  con cabeceras fijas que se pliegan/despliegan.
- **Vistas**: Detalles, Lista, Mosaicos, Iconos medianos y grandes (miniaturas de fotos y vídeos).
- **Densidad de filas** compacta/normal/cómoda; columnas configurables; paneles redimensionables.
- **Panel de navegación**: Acceso rápido (Descargas, Documentos, Cámara…), carpetas ancladas,
  árbol de carpetas del almacenamiento interno y tarjeta SD con espacio libre.
- **Panel de vista previa**: imágenes con zoom, vídeo, PDF por páginas, texto/código, carátula de audio,
  y propiedades (tamaño exacto, fecha, dimensiones, duración, contenido de carpetas…).
- Barra de direcciones con migas de pan (toca para escribir una ruta), Atrás/Adelante/Subir/Actualizar.
- Búsqueda instantánea en la carpeta; la lupa (o Intro) busca también en subcarpetas.
- Cortar / Copiar / Pegar / Cambiar nombre / Eliminar / Compartir / Nueva carpeta, selección múltiple,
  menú contextual con pulsación larga.
- Atajos de teclado (Book Cover Keyboard): Ctrl+A/C/X/V, Supr, F2, F5, Retroceso, Alt+←/→/↑, flechas, Intro, Ctrl+Shift+N.

## Compilar
GitHub Actions compila el APK en cada push (workflow «Explorador Android (APK)»): descarga el artefacto
`Explorador-apk`, descomprímelo e instala `Explorador.apk` en la tablet (permite «Instalar apps desconocidas»).
Localmente: abrir `explorador_android/` en Android Studio, o `./gradlew assembleRelease`.

Al abrirla por primera vez pide «Acceso a todos los archivos» (necesario para un explorador completo).
El keystore `app/explorador.keystore` es solo para firmar compilaciones personales
(permite instalar actualizaciones encima sin desinstalar).
