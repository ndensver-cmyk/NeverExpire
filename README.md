# NeverExpire 3.1

Inventario de alimentos en casa. Registra productos, controla sus fechas de vencimiento y recibe avisos para consumir primero lo que está próximo a caducar.

## Novedades de NeverExpire 3.1

- **Nueva identidad visual:** interfaz renovada para identificar la versión 3.1.
- **Escaneo de empaques:** utiliza la cámara para leer códigos de barras y texto del empaque mediante Google ML Kit.
- **Detección de fechas:** reconoce fechas junto a etiquetas como `Vence`, `Vencimiento`, `EXP` y equivalentes. Las fechas candidatas requieren confirmación.
- **Búsqueda de productos sin API de pago:** consulta primero Open Food Facts. Si no hay coincidencias, ofrece abrir una búsqueda pública en Google con el nombre o código del producto. No requiere claves, suscripciones ni secretos de GitHub.
- **Información del producto:** Open Food Facts puede aportar marca, imagen, ingredientes, alérgenos, Nutri-Score, grupo NOVA y datos nutricionales cuando estén disponibles.
- **Inventario visual:** contadores para productos vencidos, urgentes, próximos y al día; sección “Consume primero”, tarjetas con imagen y barra de vida útil.
- **Gestión rápida:** desliza para consumir o eliminar productos (con opción de deshacer), consulta el detalle y configura los avisos.

> **Importante:** la búsqueda externa abre resultados en el navegador; no importa automáticamente datos desde cualquier página. Verifica siempre que el resultado corresponda exactamente al producto. El OCR puede equivocarse y la fecha de vencimiento debe confirmarse leyendo el empaque. Nunca se debe deducir a partir de la fecha de fabricación ni inventar una fecha.

## Descargar e instalar

1. Descarga el archivo **NeverExpire.apk** desde el artefacto **NeverExpire-apk** de la ejecución más reciente de GitHub Actions.
2. Extrae el APK del ZIP descargado.
3. Abre **NeverExpire.apk** en tu dispositivo Android y autoriza la instalación desde esa fuente si Android lo solicita.

## Compilar con GitHub Actions

El workflow está en `.github/workflows/build.yml` y se ejecuta al:
- hacer push a `main`;
- abrir o actualizar un pull request;
- ejecutar manualmente el workflow desde GitHub Actions;
- publicar un tag que comience por `v`.

El proceso ejecuta las pruebas unitarias, compila la variante release y publica el artefacto **NeverExpire-apk**. Los tags `v*` también publican un GitHub Release con el APK.

### Firma

Si se configuran los secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` y `KEY_ALIAS`, el workflow prepara el keystore para la compilación. Configura esos secretos con una misma llave de firma para poder actualizar instalaciones existentes con futuras versiones.

## Requisitos

- Android 8.0 o superior.
- Google Play Services para las funciones de cámara y reconocimiento con ML Kit.
- Conexión a Internet para consultar Open Food Facts y abrir búsquedas externas. El inventario local puede utilizarse sin conexión.
