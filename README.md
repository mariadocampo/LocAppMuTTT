# TripNote · App de práctica de localización

App Android sencilla para la asignatura **Localización de aplicaciones** del Màster Universitari Tradumàtica (UAB).
Está pensada para traducirla, compilarla y **testearla**. Tiene errores de internacionalización a propósito: encontrarlos forma parte del ejercicio.

## Qué hay que traducir

Solo un archivo: `app/src/main/res/values/strings.xml` (inglés).
La traducción va en una carpeta nueva con el código del idioma:

| Idioma | Carpeta | Archivo |
|---|---|---|
| Español | `app/src/main/res/values-es/` | `strings.xml` |
| Catalán | `app/src/main/res/values-ca/` | `strings.xml` |

Reglas básicas:

- No cambies el atributo `name` de ninguna cadena.
- Mantén las variables (`%1$s`, `%2$s`, `%d`) y las etiquetas (`<b>…</b>`). Puedes cambiar su orden en la frase.
- Escapa los apóstrofos: `l\'aplicació`, o usa el apóstrofo tipográfico `’`.
- El signo `&` se escribe `&amp;`.
- No incluyas las cadenas marcadas con `translatable="false"`.
- Lee los comentarios `<!-- … -->`: dan contexto y límites de longitud.

## Cómo obtener el APK sin instalar nada

1. Haz un *fork* de este repositorio en tu cuenta de GitHub.
2. Activa las acciones: pestaña **Actions** > *I understand my workflows, go ahead and enable them*.
3. Sube tu `values-es/strings.xml` (o `values-ca`) con **Add file > Upload files**, respetando la ruta de la carpeta.
4. En **Actions** verás la ejecución *Compilar APK*. Tarda unos 4 o 5 minutos.
   - ✅ verde: entra en la ejecución y descarga **TripNote-apk** al final de la página.
   - ❌ roja: abre el paso que ha fallado y lee el error. Casi siempre es un apóstrofo o un `&` sin escapar.
5. En el resumen de la ejecución tienes también el informe del **comprobador de traducciones**.

## Cómo probar la app

- **En tu móvil Android**: descomprime el zip, abre `app-debug.apk` y permite instalar apps de origen desconocido.
- **En el emulador de Android Studio**: arrastra el APK a la ventana del emulador.
- Cambia el idioma del dispositivo (Ajustes > Sistema > Idiomas) para ver tu traducción.

### Pseudolocalización

Este APK incluye los pseudoidiomas de Android. Activa las *Opciones de desarrollador* (toca siete veces el *Número de compilación* en *Información del teléfono*). Después, en la lista de idiomas, aparecen dos opciones:

- **English (XA)**: acentúa y alarga los textos. Lo que no cambia está escrito en el código y no se puede traducir.
- **Arabic (XB)**: invierte la dirección (de derecha a izquierda) para comprobar el diseño.

## Comprobar la traducción en tu ordenador (opcional)

```
python3 tools/check_strings.py
```

## Compilar en tu ordenador (opcional)

Abre la carpeta con Android Studio y pulsa *Run*, o en una terminal:

```
./gradlew assembleDebug
```

Requiere JDK 17. El APK queda en `app/build/outputs/apk/debug/`.
