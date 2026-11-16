# NeverExpire 3.0

Inventario de alimentos en casa. Registra productos, controla sus fechas de vencimiento y recibe avisos para consumir primero lo que está próximo a caducar.

## Novedades de NeverExpire 3.0

- **Escaneo de empaques:** utiliza la cámara para leer códigos de barras y texto del empaque mediante Google ML Kit.
- **Detección de fechas mejorada (OCR):** reconoce fechas de vencimiento junto a etiquetas como `Vence`, `Vencimiento`, `EXP` y equivalentes; contempla formatos habituales como `DD/MM/AAAA` y mes/año.
- **Confirmación antes de guardar:** las fechas detectadas por OCR se muestran como candidatas. Debes seleccionar la fecha correcta o introducirla manualmente antes de guardar un producto escaneado.
- **Menos confusiones:** el analizador reduce la prioridad de fechas cercanas a etiquetas de fabricación o producción y prioriza el formato día/mes/año.
- **Información del producto:** consulta [Open Food Facts](https://world.openfoodfacts.org) mediante el código de barras o el nombre para completar datos disponibles como marca, imagen, ingredientes, alérgenos, Nutri-Score, grupo NOVA y tabla nutricional.
- **Inventario visual:** contadores para productos vencidos, urgentes, próximos y al día; sección “Consume primero”, tarjetas con imagen y barra de vida útil.
- **Gestión rápida:** desliza para consumir o eliminar productos (con opción de deshacer), consulta el detalle y configura los avisos.
- **Privacidad del escaneo:** el reconocimiento de texto se realiza en el dispositivo con ML Kit. La consulta de información comercial se conecta a Open Food Facts.

> **Importante:** el OCR puede equivocarse por el diseño, desgaste o impresión del empaque. Verifica siempre la fecha sugerida. El código de barras normalmente identifica el producto, pero no contiene la fecha de vencimiento ni la cantidad que tienes en casa.

## Descargar e instalar

1. Descarga el archivo **NeverExpire.apk** desde el artefacto **NeverExpire-apk** de la ejecución más reciente de GitHub Actions.
2. Extrae el APK del ZIP descargado.
3. Abre `NeverExpire.apk` en tu dispositivo Android y autoriza la instalación desde esa fuente si Android lo solicita.

La compilación de GitHub Actions conserva el artefacto durante el periodo de retención configurado por GitHub. Para instalar una versión nueva, descarga el APK de la ejecución más reciente.

## Compilar con GitHub Actions

El workflow está en `.github/workflows/build.yml` y se ejecuta al:
- hacer push a `main`;
- abrir o actualizar un pull request;
- ejecutar manualmente el workflow desde GitHub Actions;
- publicar un tag que comience por `v`.

El proceso ejecuta `gradle testDebugUnitTest`, compila la variante release y publica el artefacto **NeverExpire-apk**. Los tags `v*` también publican un GitHub Release con el APK.

### Firma

Si se configuran los secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` y `KEY_ALIAS`, el workflow prepara el keystore para la compilación. Configura esos secretos con una misma llave de firma para poder actualizar instalaciones existentes con futuras versiones.

## Requisitos

- Android 8.0 o superior.
- Google Play Services para las funciones de cámara y reconocimiento con ML Kit.
- Conexión a internet para consultar Open Food Facts. El inventario local y las funciones que no requieren consulta externa pueden utilizarse sin esa búsqueda.
