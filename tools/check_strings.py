#!/usr/bin/env python3
"""
Comprobador de traducciones de strings.xml (Android) para la asignatura
Localización de aplicaciones (Màster Tradumàtica, UAB).

Compara cada app/src/main/res/values-XX/strings.xml con el original en inglés
(app/src/main/res/values/strings.xml) y avisa de los problemas que suelen
romper la compilación o la app.

Uso:
    python3 tools/check_strings.py                 # revisa todos los idiomas del proyecto
    python3 tools/check_strings.py ruta/strings.xml  # revisa un archivo concreto
    python3 tools/check_strings.py --markdown      # salida en Markdown (para GitHub)

Devuelve el código 1 si hay ERRORES (cosas que impiden compilar o rompen la app).
Los AVISOS no bloquean, pero conviene revisarlos.
"""
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

RES_DIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"
SOURCE = RES_DIR / "values" / "strings.xml"
LOCALE_DIR = re.compile(r"^values-([a-z]{2,3}(-r[A-Z]{2})?|b\+[A-Za-z0-9+]+)$")
PLACEHOLDER = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([sSdfxXcbBeEgGoh%n])")
UNESCAPED_APOS = re.compile(r"(?<!\\)'")
MAX_CHARS = re.compile(r"max(?:imum|\.)?\s*(\d+)\s*(?:char|caract)", re.I)


class Resources:
    def __init__(self):
        self.strings = {}        # name -> dict(text, tags, translatable, comment, line)
        self.plurals = {}        # name -> {quantity: text}
        self.arrays = {}         # name -> [items]


def parse(path):
    """Lee un strings.xml conservando los comentarios (sirven de contexto)."""
    parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))
    raw = Path(path).read_text(encoding="utf-8")
    root = ET.fromstring(raw, parser=parser)
    res = Resources()
    last_comment = ""
    for el in root:
        if el.tag is ET.Comment:
            last_comment = (el.text or "").strip()
            continue
        name = el.get("name")
        if el.tag == "string":
            res.strings[name] = {
                "text": "".join(el.itertext()),
                "tags": sorted(child.tag for child in el.iter() if child is not el),
                "translatable": el.get("translatable", "true") != "false",
                "comment": last_comment,
            }
        elif el.tag == "plurals":
            res.plurals[name] = {item.get("quantity"): "".join(item.itertext())
                                 for item in el.findall("item")}
        elif el.tag == "string-array":
            res.arrays[name] = ["".join(item.itertext()) for item in el.findall("item")]
        last_comment = ""
    return res


def placeholders(text):
    return Counter(m.group(0) for m in PLACEHOLDER.finditer(text) if m.group(0) not in ("%%", "%n"))


def apostrophe_problem(text):
    stripped = text.strip()
    if len(stripped) >= 2 and stripped.startswith('"') and stripped.endswith('"'):
        return False
    return bool(UNESCAPED_APOS.search(text))


def stray_percent(text):
    cleaned = PLACEHOLDER.sub("", text)
    return "%" in cleaned


def check_text(where, src_text, tgt_text, errors, warnings, comment=""):
    if apostrophe_problem(tgt_text):
        errors.append(f"{where}: apóstrofo sin escapar. Escribe \\' (o usa el apóstrofo tipográfico ’). "
                      f"Así no compila.")
    if placeholders(src_text) != placeholders(tgt_text):
        exp = ", ".join(sorted(placeholders(src_text).elements())) or "ninguna"
        got = ", ".join(sorted(placeholders(tgt_text).elements())) or "ninguna"
        errors.append(f"{where}: las variables no coinciden. Original: {exp} · Traducción: {got}")
    elif stray_percent(tgt_text) and not stray_percent(src_text):
        warnings.append(f"{where}: hay un % suelto. Si la cadena lleva variables, escríbelo como %%.")
    if src_text.strip() and tgt_text.strip() == src_text.strip() and len(src_text.strip()) > 3:
        warnings.append(f"{where}: idéntica al original. ¿Se ha quedado sin traducir?")
    if src_text[:1].isspace() != tgt_text[:1].isspace() or src_text[-1:].isspace() != tgt_text[-1:].isspace():
        warnings.append(f"{where}: los espacios al principio o al final no coinciden con el original.")
    limit = MAX_CHARS.search(comment or "")
    if limit and len(tgt_text.strip()) > int(limit.group(1)):
        warnings.append(f"{where}: {len(tgt_text.strip())} caracteres; el comentario pide un máximo de "
                        f"{limit.group(1)}. Probablemente se cortará en pantalla.")


def check_file(src, path):
    errors, warnings = [], []
    missing = []
    try:
        tgt = parse(path)
    except ET.ParseError as exc:
        line, col = exc.position
        lines = Path(path).read_text(encoding="utf-8").splitlines()
        context = lines[line - 1].strip() if 0 < line <= len(lines) else ""
        hint = ""
        if "&" in context:
            hint = " Pista: el signo & se escribe &amp; dentro del XML."
        elif "<" in context:
            hint = " Pista: revisa que todas las etiquetas estén abiertas y cerradas."
        errors.append(f"El archivo no es un XML válido (línea {line}, columna {col}): {context}{hint}")
        return errors, warnings

    for name, s in src.strings.items():
        if not s["translatable"]:
            if name in tgt.strings:
                warnings.append(f"<string name=\"{name}\">: está marcada translatable=\"false\" en el "
                                f"original. Bórrala de la traducción.")
            continue
        if name not in tgt.strings:
            missing.append(name)
            continue
        t = tgt.strings[name]
        where = f"<string name=\"{name}\">"
        check_text(where, s["text"], t["text"], errors, warnings, s["comment"])
        if s["tags"] != t["tags"]:
            errors.append(f"{where}: las etiquetas de formato no coinciden "
                          f"(original: {s['tags'] or 'ninguna'} · traducción: {t['tags'] or 'ninguna'}).")

    for name in tgt.strings:
        if name not in src.strings:
            errors.append(f"<string name=\"{name}\">: esta clave no existe en el original. "
                          f"¿Se ha traducido o cambiado el atributo name?")

    for name, forms in src.plurals.items():
        if name not in tgt.plurals:
            missing.append(name)
            continue
        tforms = tgt.plurals[name]
        if "other" not in tforms:
            errors.append(f"<plurals name=\"{name}\">: falta la forma quantity=\"other\" (obligatoria).")
        if "one" not in tforms:
            warnings.append(f"<plurals name=\"{name}\">: falta la forma quantity=\"one\" "
                            f"(en español y catalán se necesita para el singular).")
        for qty, text in tforms.items():
            check_text(f"<plurals name=\"{name}\"> ({qty})", forms.get("other", ""), text, errors, warnings)
    for name in tgt.plurals:
        if name not in src.plurals:
            errors.append(f"<plurals name=\"{name}\">: esta clave no existe en el original.")

    for name, items in src.arrays.items():
        if name not in tgt.arrays:
            missing.append(name)
            continue
        if len(items) != len(tgt.arrays[name]):
            errors.append(f"<string-array name=\"{name}\">: el original tiene {len(items)} elementos y la "
                          f"traducción {len(tgt.arrays[name])}. El orden y el número deben coincidir.")
        for i, (a, b) in enumerate(zip(items, tgt.arrays[name]), 1):
            check_text(f"<string-array name=\"{name}\"> (elemento {i})", a, b, errors, warnings)
    for name in tgt.arrays:
        if name not in src.arrays:
            errors.append(f"<string-array name=\"{name}\">: esta clave no existe en el original.")

    if missing:
        warnings.insert(0, f"Faltan {len(missing)} recursos por traducir; la app los mostrará en inglés: "
                           + ", ".join(missing))
    return errors, warnings


def md(text):
    """Prepara un mensaje para Markdown: las etiquetas y los escapes se muestran como código."""
    text = re.sub(r"(<[^<>]+>)", r"`\1`", text)
    text = text.replace("\\'", "`\\'`")
    return text


def find_targets():
    return sorted(d / "strings.xml" for d in RES_DIR.iterdir()
                  if d.is_dir() and LOCALE_DIR.match(d.name) and (d / "strings.xml").exists())


def main(argv):
    markdown = "--markdown" in argv
    paths = [Path(a) for a in argv if not a.startswith("--")] or find_targets()
    src = parse(SOURCE)
    out = []
    total_errors = 0
    if not paths:
        out.append("No hay traducciones todavía: crea app/src/main/res/values-es/strings.xml "
                   "(o values-ca) y vuelve a ejecutar el comprobador.")
    for path in paths:
        errors, warnings = check_file(src, path)
        total_errors += len(errors)
        try:
            label = path.resolve().relative_to(RES_DIR.parent.parent.parent.parent)
        except ValueError:
            label = path
        if markdown:
            status = "❌" if errors else ("⚠️" if warnings else "✅")
            out.append(f"### {status} {label}")
            out.append(f"**{len(errors)} errores · {len(warnings)} avisos**\n")
            out += [f"- ❌ {md(e)}" for e in errors] + [f"- ⚠️ {md(w)}" for w in warnings]
            out.append("")
        else:
            out.append(f"== {label}: {len(errors)} errores, {len(warnings)} avisos")
            out += [f"  ERROR  {e}" for e in errors] + [f"  AVISO  {w}" for w in warnings]
            if not errors and not warnings:
                out.append("  Todo correcto.")
    print("\n".join(out))
    return 1 if total_errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
