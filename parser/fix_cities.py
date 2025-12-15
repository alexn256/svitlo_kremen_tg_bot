#!/usr/bin/env python3
"""
Скрипт для исправления названий населенных пунктов в addresses.json
- Нормализует регистр (все префиксы м./с./смт. в нижнем регистре)
- Исправляет неполные/обрывочные названия
"""

import json
from collections import defaultdict

INPUT_FILE = "addresses.json"
OUTPUT_FILE = "addresses_fixed.json"
BACKUP_FILE = "addresses_backup.json"

# Словарь для исправления неполных названий
# Будем пытаться восстановить из контекста
CITY_FIXES = {
    # Эти названия нужно будет заменить на основе контекста (улицы/филиалы)
}

def normalize_city_prefix(city):
    """Нормализует префикс населенного пункта к нижнему регистру"""
    if not city:
        return city

    # Заменяем заглавные префиксы на строчные
    if city.startswith('М.'):
        city = 'м.' + city[2:]
    elif city.startswith('С.'):
        city = 'с.' + city[2:]
    elif city.startswith('Смт.'):
        city = 'смт.' + city[4:]
    elif city.startswith('СМТ.'):
        city = 'смт.' + city[4:]

    return city

def is_short_city_name(city):
    """Проверяет, является ли название неполным (слишком коротким)"""
    if not city:
        return False

    # Убираем префикс и проверяем длину
    name = city.replace('м.', '').replace('с.', '').replace('смт.', '').strip()
    return len(name) <= 2

def find_correct_city_by_context(address, prev_addresses):
    """
    Пытается найти правильное название города по контексту:
    - По предыдущим адресам с той же улицей
    - По филиалу и черге
    """
    street = address['street']
    branch = address['branch']
    queue = address['queue_full']

    # Ищем в предыдущих адресах с той же улицей
    for prev in reversed(prev_addresses[-100:]):  # Смотрим последние 100 адресов
        if (prev['street'] == street and
            prev['branch'] == branch and
            prev['city'] and
            not is_short_city_name(prev['city'])):
            return prev['city']

    # Ищем по филиалу и черге с похожей улицей
    for prev in reversed(prev_addresses[-200:]):
        if (prev['branch'] == branch and
            prev['queue_full'] == queue and
            prev['city'] and
            not is_short_city_name(prev['city'])):
            # Если это соседние адреса (похожие номера домов на одной улице)
            if prev['street'] == street:
                return prev['city']

    return None

def main():
    print(f"Загрузка данных из {INPUT_FILE}...")
    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)

    print(f"Всего адресов: {len(data)}")

    # Создаем бэкап
    print(f"Создание бэкапа в {BACKUP_FILE}...")
    with open(BACKUP_FILE, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # Статистика
    stats = {
        'total': len(data),
        'normalized_prefix': 0,
        'fixed_short_names': 0,
        'unfixed_short_names': 0
    }

    short_city_examples = defaultdict(list)
    processed_addresses = []

    print("\nОбработка адресов...")
    for i, addr in enumerate(data):
        original_city = addr['city']

        # Нормализуем префикс
        normalized_city = normalize_city_prefix(addr['city'])
        if normalized_city != original_city:
            stats['normalized_prefix'] += 1
            addr['city'] = normalized_city

        # Проверяем на короткое название
        if is_short_city_name(addr['city']):
            # Пытаемся найти правильное название по контексту
            correct_city = find_correct_city_by_context(addr, processed_addresses)

            if correct_city:
                short_city_examples[original_city].append({
                    'old': addr['city'],
                    'new': correct_city,
                    'street': addr['street'],
                    'house': addr['house']
                })
                addr['city'] = correct_city
                stats['fixed_short_names'] += 1
            else:
                short_city_examples[original_city].append({
                    'old': addr['city'],
                    'new': None,
                    'street': addr['street'],
                    'house': addr['house'],
                    'branch': addr['branch']
                })
                stats['unfixed_short_names'] += 1

        processed_addresses.append(addr)

    # Сохраняем исправленные данные
    print(f"\nСохранение исправленных данных в {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(processed_addresses, f, ensure_ascii=False, indent=2)

    # Выводим статистику
    print("\n" + "="*60)
    print("СТАТИСТИКА ИСПРАВЛЕНИЙ")
    print("="*60)
    print(f"Всего адресов: {stats['total']}")
    print(f"Нормализовано префиксов (М.→м., С.→с.): {stats['normalized_prefix']}")
    print(f"Исправлено коротких названий: {stats['fixed_short_names']}")
    print(f"НЕ исправлено коротких названий: {stats['unfixed_short_names']}")

    if short_city_examples:
        print("\n" + "="*60)
        print("ПРИМЕРЫ КОРОТКИХ НАЗВАНИЙ:")
        print("="*60)
        for short_name, examples in sorted(short_city_examples.items()):
            print(f"\n{short_name}: ({len(examples)} адресов)")
            for ex in examples[:5]:  # Показываем первые 5
                if ex['new']:
                    print(f"  ✓ {ex['old']} → {ex['new']}: {ex['street']}, {ex['house']}")
                else:
                    print(f"  ✗ {ex['old']} (НЕ исправлено): {ex['street']}, {ex['house']} [{ex['branch']}]")

    print("\n" + "="*60)
    print(f"✓ Готово! Исправленные данные сохранены в {OUTPUT_FILE}")
    print(f"  Бэкап оригинала: {BACKUP_FILE}")
    print("="*60)

if __name__ == '__main__':
    main()
