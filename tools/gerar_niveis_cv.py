"""Gera a tabela de níveis por CV do app (composeResources/files/unit_levels.json).

Fonte: static_data.json do clashy.py (ClashKingInc, licença MIT), extraído dos arquivos do jogo:
https://raw.githubusercontent.com/ClashKingInc/clashy.py/master/coc/static/static_data.json

Uso (depois de cada atualização do jogo):
    py -I tools/gerar_niveis_cv.py <static_data.json baixado>

Para cada tropa, máquina, feitiço, herói, pet e equipamento da vila principal, grava o CV exigido
por nível (o máximo no CV N é quantos níveis pedem CV <= N) e, para equipamentos, o herói dono.
Só usa a biblioteca padrão do Python.
"""

import datetime
import json
import pathlib
import sys

CATEGORIES = ["troops", "spells", "heroes", "pets", "equipment"]
OUT = pathlib.Path(__file__).resolve().parent.parent / "composeApp/src/commonMain/composeResources/files/unit_levels.json"


def required_town_halls(levels):
    """CV exigido por nível, sem buracos e sem descer (nível sem CV herda o do anterior)."""
    result, last = [], 1
    for level in levels:
        th = level.get("required_townhall") or last
        last = max(last, th)
        result.append(last)
    return result


def main(source):
    data = json.loads(pathlib.Path(source).read_text(encoding="utf-8"))
    units, equipment_hero = {}, {}
    for category in CATEGORIES:
        for unit in data[category]:
            if unit.get("village", "home") != "home" or not unit.get("levels"):
                continue
            units[unit["name"]] = required_town_halls(unit["levels"])
            if category == "equipment" and unit.get("hero"):
                equipment_hero[unit["name"]] = unit["hero"]
    table = {
        "source": "clashy.py coc/static/static_data.json",
        "generated": datetime.date.today().isoformat(),
        "units": dict(sorted(units.items())),
        "equipmentHero": dict(sorted(equipment_hero.items())),
    }
    OUT.write_text(json.dumps(table, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{OUT.name}: {len(units)} unidades, {len(equipment_hero)} equipamentos, {OUT.stat().st_size} bytes")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
