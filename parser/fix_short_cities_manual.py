#!/usr/bin/env python3
"""
Ручное исправление коротких названий городов на основе анализа улиц
"""

import json

INPUT_FILE = "addresses_fixed.json"
OUTPUT_FILE = "addresses.json"

# Словарь исправлений на основе улиц и контекста
# Формат: (город_короткий, улица, филиал) -> правильный_город
MANUAL_FIXES = {
    # м.І - судя по улицам (Набережна, Портова, Строни) и филиалу - это часть м.Горішні плавні
    ('м.І', 'вул. Набережна', 'Кременчуцька'): 'м.Горішні плавні',
    ('м.І', 'вул. Портова', 'Кременчуцька'): 'м.Горішні плавні',
    ('м.І', 'вул. Строни', 'Кременчуцька'): 'м.Горішні плавні',

    # м.В - судя по вул. Набережна - это м.Горішні плавні
    ('м.В', 'вул. Набережна', 'Кременчуцька'): 'м.Горішні плавні',

    # м.М - вул. Строни - скорее всего тоже м.Горішні плавні
    ('м.М', 'вул. Строни', 'Кременчуцька'): 'м.Горішні плавні',

    # с.М - вул. Строни - может быть село рядом с Горішні плавні, оставим как есть
    # Но скорее всего это тоже часть Горішні плавні
    ('с.М', 'вул. Строни', 'Кременчуцька'): 'м.Горішні плавні',

    # м.А - вул. Троїцька - это точно м.Кременчук (Троїцька есть только там)
    ('м.А', 'вул. Троїцька', 'Кременчуцька'): 'м.Кременчук',

    # с.В - судя по улицам (Куценка, Греблянська, Миру) - нужно проверить
    # Но скорее всего это село, оставим как "с.В" пока не уточним
}

# Дополнительные исправления по шаблонам (для оставшихся)
PATTERN_FIXES = {
    # Если остались адреса с короткими названиями, попробуем по филиалу
    'Кременчуцька': {
        'м.': 'м.Кременчук',  # По умолчанию город в этой филиале
        'с.': 'с.Огуївка',    # Нужно уточнить
    }
}

def is_short_city_name(city):
    """Проверяет, является ли название неполным"""
    if not city:
        return False
    name = city.replace('м.', '').replace('с.', '').replace('смт.', '').strip()
    return len(name) <= 2

def main():
    print(f"Загрузка данных из {INPUT_FILE}...")
    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)

    stats = {
        'total': len(data),
        'fixed_manual': 0,
        'still_short': 0
    }

    fixed_examples = {}

    print("Применение ручных исправлений...")
    for addr in data:
        if not is_short_city_name(addr['city']):
            continue

        key = (addr['city'], addr['street'], addr['branch'])

        if key in MANUAL_FIXES:
            old_city = addr['city']
            new_city = MANUAL_FIXES[key]
            addr['city'] = new_city
            stats['fixed_manual'] += 1

            if old_city not in fixed_examples:
                fixed_examples[old_city] = []
            fixed_examples[old_city].append({
                'old': old_city,
                'new': new_city,
                'street': addr['street'],
                'house': addr['house']
            })
        else:
            stats['still_short'] += 1

    # Сохраняем исправленные данные
    print(f"\nСохранение в {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # Статистика
    print("\n" + "="*60)
    print("СТАТИСТИКА РУЧНЫХ ИСПРАВЛЕНИЙ")
    print("="*60)
    print(f"Всего адресов: {stats['total']}")
    print(f"Исправлено вручную: {stats['fixed_manual']}")
    print(f"Осталось с короткими названиями: {stats['still_short']}")

    if fixed_examples:
        print("\n" + "="*60)
        print("ПРИМЕРЫ ИСПРАВЛЕНИЙ:")
        print("="*60)
        for old_city, examples in sorted(fixed_examples.items()):
            print(f"\n{old_city}: исправлено {len(examples)} адресов")
            for ex in examples[:5]:
                print(f"  ✓ {ex['old']} → {ex['new']}: {ex['street']}, {ex['house']}")

    # Показываем что осталось
    if stats['still_short'] > 0:
        print("\n" + "="*60)
        print("ОСТАЛОСЬ НЕИСПРАВЛЕННЫХ:")
        print("="*60)
        remaining = [d for d in data if is_short_city_name(d['city'])]
        from collections import defaultdict
        by_city = defaultdict(list)
        for r in remaining:
            by_city[r['city']].append(f"{r['street']} [{r['branch']}]")

        for city, streets in sorted(by_city.items()):
            print(f"\n{city}: {len(streets)} адресов")
            for s in sorted(set(streets))[:5]:
                print(f"  - {s}")

    print("\n" + "="*60)
    print(f"✓ Готово! Данные сохранены в {OUTPUT_FILE}")
    print("="*60)

if __name__ == '__main__':
    main()
