#!/usr/bin/env python3
"""
Финальная очистка данных:
- Удаляем адреса с некорректными/непонятными названиями городов
- Оставляем только валидные записи
"""

import json

INPUT_FILE = "addresses.json"
OUTPUT_FILE = "addresses_clean.json"
REMOVED_FILE = "addresses_removed.json"

# Города которые нужно удалить (некорректный парсинг)
CITIES_TO_REMOVE = [
    'с.В',  # Невозможно определить что это за село
    'м.І',  # Остатки после парсинга
]

def is_short_city_name(city):
    """Проверяет, является ли название слишком коротким (вероятно некорректным)"""
    if not city:
        return True
    name = city.replace('м.', '').replace('с.', '').replace('смт.', '').strip()
    return len(name) <= 2

def main():
    print(f"Загрузка данных из {INPUT_FILE}...")
    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)

    print(f"Всего адресов: {len(data)}")

    # Фильтруем данные
    valid_data = []
    removed_data = []

    for addr in data:
        # Удаляем адреса из списка проблемных городов
        if addr['city'] in CITIES_TO_REMOVE:
            removed_data.append(addr)
        # Удаляем адреса с очень короткими названиями (скорее всего ошибки парсинга)
        elif is_short_city_name(addr['city']):
            removed_data.append(addr)
        else:
            valid_data.append(addr)

    # Статистика
    print(f"\nРезультат фильтрации:")
    print(f"  Валидных адресов: {len(valid_data)}")
    print(f"  Удалено адресов: {len(removed_data)}")

    # Показываем что удалили
    if removed_data:
        from collections import Counter
        removed_cities = Counter([d['city'] for d in removed_data])
        print(f"\nУдаленные записи по городам:")
        for city, count in sorted(removed_cities.items()):
            print(f"  {city}: {count} адресов")

    # Сохраняем очищенные данные
    print(f"\nСохранение очищенных данных в {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(valid_data, f, ensure_ascii=False, indent=2)

    # Сохраняем удаленные данные для справки
    if removed_data:
        print(f"Сохранение удаленных данных в {REMOVED_FILE}...")
        with open(REMOVED_FILE, 'w', encoding='utf-8') as f:
            json.dump(removed_data, f, ensure_ascii=False, indent=2)

    # Итоговая статистика
    from collections import Counter
    cities = Counter([d['city'] for d in valid_data if d['city']])

    print("\n" + "="*60)
    print("ИТОГОВАЯ СТАТИСТИКА")
    print("="*60)
    print(f"Всего адресов: {len(valid_data)}")
    print(f"Уникальных населенных пунктов: {len(cities)}")
    print(f"\nТоп-10 населенных пунктов:")
    for city, count in cities.most_common(10):
        print(f"  {count:5d} {city}")

    # Проверяем на дубликаты и короткие названия
    duplicates = {}
    for city in set([d['city'] for d in valid_data if d['city']]):
        lower = city.lower()
        if lower not in duplicates:
            duplicates[lower] = []
        duplicates[lower].append(city)

    dup_count = sum(1 for variants in duplicates.values() if len(variants) > 1)
    short_count = sum(1 for city in cities.keys() if is_short_city_name(city))

    print(f"\nПроверка качества данных:")
    print(f"  Дубликаты по регистру: {dup_count}")
    print(f"  Короткие/некорректные названия: {short_count}")

    print("\n" + "="*60)
    print(f"✓ Готово!")
    print(f"  Очищенные данные: {OUTPUT_FILE}")
    print(f"  Удаленные данные: {REMOVED_FILE}")
    print("="*60)

if __name__ == '__main__':
    main()
