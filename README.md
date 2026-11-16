# NeverExpire 2.0

Inventario de alimentos en casa. Registra vencimientos, te avisa qué consumir primero y lee los empaques con la cámara.

## Novedades 2.0
- **Escáner de empaques**: la cámara lee el código de barras y el texto del empaque (fecha de vencimiento, ingredientes) directamente en el teléfono con ML Kit.
- **Información automática**: con el código de barras (o el nombre) busca en [Open Food Facts](https://world.openfoodfacts.org) el nombre, la marca, la imagen, los ingredientes, los alérgenos, el Nutri-Score, el grupo NOVA y la tabla nutricional.
- **Interfaz nueva**: encabezado con contadores (vencidos, urgentes, pronto, al día) que funcionan como filtros, carrusel "Consume primero", tarjetas con imagen y barra de vida útil, deslizar para consumir o eliminar (con deshacer), pantalla de detalle y pantalla de ajustes.
- **Ajustes**: días de anticipación del aviso y hora de la notificación diaria.
- **APK optimizado**: ~3 MB, firmado con una llave fija (las actualizaciones se instalan encima).

## Compilar con GitHub Actions
- Cada push a `main` compila el APK (artefacto **NeverExpire-apk**).
- Cada tag `v*` publica un Release con el APK: `git tag v2.1 && git push origin v2.1`.
- La firma usa los secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` y `KEY_ALIAS`.

Requisitos: Android 8.0 o superior con Google Play Services (para la lectura con cámara).
