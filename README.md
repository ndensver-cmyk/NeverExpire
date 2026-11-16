# NeverExpire

Inventario de alimentos en casa. Registra vencimientos y te avisa qué productos debes consumir primero.

## Funciones
- Lista ordenada por fecha de vencimiento (lo que vence primero, arriba).
- Colores: rojo = vencido, naranja = ≤3 días, amarillo = ≤7 días, verde = OK.
- Tarjeta "Consume primero" con lo que vence en 3 días.
- Búsqueda y filtro por categoría.
- Botón ✓ para consumir una unidad; papelera para eliminar.
- Notificación diaria a las 9:00 a.m. con los productos por vencer.
- Datos guardados localmente en el teléfono.

## Compilar con GitHub Actions
1. Sube esta carpeta a un repositorio de GitHub (rama `main`).
2. Ve a la pestaña **Actions** → "Build NeverExpire APK".
3. Al terminar, descarga el artefacto **NeverExpire-apk** (ZIP con el APK).
4. Para crear un Release con el APK: `git tag v1.0 && git push origin v1.0`.

Requisitos: Android 8.0 (API 26) o superior.
