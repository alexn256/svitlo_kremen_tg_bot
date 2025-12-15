#!/usr/bin/env python3
"""
Парсер PDF файла с графиком отключений электроэнергии
Полтавской области для Telegram бота
"""

import pdfplumber
import re
import csv
import json
from typing import List, Dict, Tuple, Optional

PDF_FILE = "HPV_25_26-zminy-hruden.pdf"
OUT_CSV = "addresses.csv"
OUT_JSON = "addresses.json"
STATS_FILE = "parsing_stats.txt"

QUEUE_PATTERN = re.compile(r'^(Перша|Друга|Третя|Четверта|П[.\'\u2019ʼ]ята|Шоста)\s+черга\s*$', re.IGNORECASE)
SUBQUEUE_PATTERN = re.compile(r'^(Перша|Друга|Третя|Четверта|П[.\'\u2019ʼ]ята|Шоста)\s+черга\s+(І|ІІ)\s+підчерга', re.IGNORECASE)
BRANCH_PATTERN = re.compile(r'(Полтавська|Кременчуцька|Лубенська|Миргородська|Хорольська|Гадяцька).*(філія|дільниця)', re.IGNORECASE)

QUEUE_MAP = {
    'Перша': 1, 'Друга': 2, 'Третя': 3,
    'Четверта': 4, 'П.ята': 5, 'Пʼята': 5, "П'ята": 5, 'П\u2019ята': 5, 'Шоста': 6
}
SUBQUEUE_MAP = {'І': 1, 'ІІ': 2}

def parse_queue_number(text: str) -> Optional[int]:
    """Извлекает номер черги из текста"""
    for name, num in QUEUE_MAP.items():
        if name in text:
            return num
    return None

def parse_subqueue_number(text: str) -> Optional[int]:
    """Извлекает номер підчерги из текста"""
    for name in sorted(SUBQUEUE_MAP.keys(), key=len, reverse=True):
        if f'{name} підчерга' in text:
            return SUBQUEUE_MAP[name]
    return None

def expand_house_range(house_str: str) -> List[str]:
    """Разворачивает диапазон домов: '45-64' -> ['45', '46', ..., '64']"""
    house_str = house_str.strip()
    match = re.match(r'^(\d+)\s*-\s*(\d+)$', house_str)
    if match:
        start, end = int(match.group(1)), int(match.group(2))
        if end > start and end - start <= 100:
            return [str(i) for i in range(start, end + 1)]
    return [house_str]

def extract_houses_from_text(text: str) -> List[str]:
    """
    Извлекает номера домов из текста.
    Пример: "10, 12, 14-16, 18а" -> ['10', '12', '14', '15', '16', '18а']
    Обрабатывает сложные случаи: "1/49,12, 8" -> ['1/49', '12', '8']
    """
    houses = []

    text = text.rstrip(',;.: ')

    parts = re.split(r'[,;]\s*', text)

    for part in parts:
        part = part.strip().rstrip(',;.: ')
        if not part:
            continue

        if re.match(r'^\d', part):
            expanded = expand_house_range(part)
            houses.extend(expanded)

    return houses

def parse_address_line(text: str) -> List[Dict[str, any]]:
    """
    Парсит строку с адресами из таблицы PDF.
    Возвращает список словарей с распарсенными адресами.
    """
    results = []

    text = re.sub(r'\s+', ' ', text).strip()

    city_pattern = re.compile(r'(м\.|с\.|смт\.)\s*([А-ЯІЇЄҐа-яіїєґ\'\s-]+?)(?:\s*[:;.]|(?=\s+вул\.|\s+пров\.|\s+просп\.))', re.IGNORECASE)

    cities = list(city_pattern.finditer(text))

    current_city = None

    if not cities:
        results.extend(parse_streets_in_text(text, None))
        return results

    for i, city_match in enumerate(cities):
        city_prefix = city_match.group(1)
        city_name = city_match.group(2).strip()
        current_city = f"{city_prefix}{city_name}"

        start_pos = city_match.end()
        end_pos = cities[i + 1].start() if i + 1 < len(cities) else len(text)

        city_text = text[start_pos:end_pos]

        streets = parse_streets_in_text(city_text, current_city)
        results.extend(streets)

    return results

def parse_streets_in_text(text: str, city: Optional[str]) -> List[Dict[str, any]]:
    """
    Парсит улицы и дома из текста.
    Улучшенная версия - разбивает текст на сегменты по вулицям.
    """
    results = []

    text = text.lstrip(': ')

    street_pattern = re.compile(
        r'(вул\.|пров\.|просп\.|пл\.)\s*([А-ЯІЇЄҐа-яіїєґ\s\'\-]+?)(?=\s*,|\s*;|\s+\d|\s*\()',
        re.IGNORECASE
    )

    street_matches = list(street_pattern.finditer(text))

    if street_matches:
        for i, match in enumerate(street_matches):
            prefix = match.group(1)
            street_name = match.group(2).strip().rstrip(',;:').strip()
            street = f"{prefix} {street_name}"

            start_pos = match.end()

            if i + 1 < len(street_matches):
                end_pos = street_matches[i + 1].start()
            else:
                next_city = re.search(r'\s+(м\.|с\.|смт\.)\s*[А-ЯІЇЄҐ]', text[start_pos:])
                if next_city:
                    end_pos = start_pos + next_city.start()
                else:
                    end_pos = len(text)

            houses_segment = text[start_pos:end_pos].strip()

            houses_segment = houses_segment.lstrip(',;: ').rstrip(',;: ')

            houses_segment = re.sub(r'\(?\s*б\.?\s*', '', houses_segment, flags=re.IGNORECASE)
            houses_segment = re.sub(r'\)', '', houses_segment)

            houses = extract_houses_from_text(houses_segment)

            for house in houses:
                if house:
                    results.append({
                        'city': city,
                        'street': street,
                        'house': house
                    })

    if not street_matches:

        simple_street = re.compile(
            r'([А-ЯІЇЄҐ][а-яіїєґ\'\s-]+?)(?:\s*,|\s+)(\d[^м\.с\.смт\.]+)',
            re.IGNORECASE
        )

        for match in simple_street.finditer(text):
            street_name = match.group(1).strip().rstrip(',;:')
            houses_text = match.group(2).strip()

            if any(kw in street_name.lower() for kw in ['тов', 'пат', 'прат', 'ат ', 'філі', 'завод']):
                continue

            houses = extract_houses_from_text(houses_text)

            for house in houses:
                if house:
                    results.append({
                        'city': city,
                        'street': street_name,
                        'house': house
                    })

    return results

def main():
    with open('skipped_lines.txt', 'w', encoding='utf-8') as f:
        f.write("=== Пропущені рядки при парсингу ===\n\n")

    rows = []
    stats = {
        'total_pages': 0,
        'processed_lines': 0,
        'skipped_lines': 0,
        'total_addresses': 0,
        'by_queue': {}
    }

    current_queue = None
    current_subqueue = None
    current_branch = None
    current_city = None

    print(f"Открываем PDF файл: {PDF_FILE}")

    with pdfplumber.open(PDF_FILE) as pdf:
        stats['total_pages'] = len(pdf.pages)
        print(f"Всего страниц: {stats['total_pages']}")
        print("\nНачинаем обработку...\n")

        for page_num, page in enumerate(pdf.pages, 1):
            tables = page.extract_tables()

            if not tables:
                continue

            for table in tables:
                for row in table:
                    if not row or not row[0]:
                        continue

                    cell_text = str(row[0]).strip()

                    if QUEUE_PATTERN.match(cell_text):
                        current_queue = parse_queue_number(cell_text)
                        current_subqueue = None
                        print(f"[Стр. {page_num}] Черга: {current_queue}")
                        continue

                    if SUBQUEUE_PATTERN.match(cell_text):
                        queue_num = parse_queue_number(cell_text)
                        sub_num = parse_subqueue_number(cell_text)
                        if queue_num:
                            current_queue = queue_num
                        current_subqueue = sub_num
                        print(f"[Стр. {page_num}] Підчерга: {current_queue}.{current_subqueue}")
                        continue

                    branch_match = BRANCH_PATTERN.search(cell_text)
                    if branch_match:
                        current_branch = branch_match.group(1)
                        print(f"[Стр. {page_num}] Філія: {current_branch}")
                        continue

                    if current_queue is None or current_subqueue is None:
                        continue

                    if len(row) < 2 or not row[1]:
                        continue

                    address_text = str(row[1]).strip()

                    if not address_text or len(address_text) < 5:
                        continue

                    stats['processed_lines'] += 1

                    parsed = parse_address_line(address_text)

                    if not parsed:
                        stats['skipped_lines'] += 1

                        with open('skipped_lines.txt', 'a', encoding='utf-8') as f:
                            f.write(f"[Стр. {page_num}] Черга {current_queue}.{current_subqueue} - {current_branch}\n")
                            f.write(f"  {address_text}\n\n")
                        continue

                    queue_key = f"{current_queue}.{current_subqueue}"

                    for addr in parsed:
                        if addr['city']:
                            current_city = addr['city']

                        city = addr['city'] if addr['city'] else current_city

                        rows.append({
                            'branch': current_branch,
                            'queue': current_queue,
                            'subqueue': current_subqueue,
                            'queue_full': queue_key,
                            'city': city or '',
                            'street': addr['street'],
                            'house': addr['house']
                        })

                        stats['total_addresses'] += 1
                        stats['by_queue'][queue_key] = stats['by_queue'].get(queue_key, 0) + 1

    print(f"\n\nСохранение результатов в {OUT_CSV}...")
    with open(OUT_CSV, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['branch', 'queue', 'subqueue', 'queue_full', 'city', 'street', 'house']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Сохранение результатов в {OUT_JSON}...")
    with open(OUT_JSON, 'w', encoding='utf-8') as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)

    with open(STATS_FILE, 'w', encoding='utf-8') as f:
        f.write("=== Статистика парсинга ===\n\n")
        f.write(f"Всего страниц: {stats['total_pages']}\n")
        f.write(f"Обработано строк: {stats['processed_lines']}\n")
        f.write(f"Пропущено строк: {stats['skipped_lines']}\n")
        f.write(f"Всего адресов: {stats['total_addresses']}\n\n")
        f.write("Распределение по очередям:\n")
        for queue_key, count in sorted(stats['by_queue'].items()):
            f.write(f"  {queue_key}: {count} адресов\n")

    print("\n" + "="*50)
    print("✓ Парсинг завершен!")
    print("="*50)
    print(f"Всего адресов: {stats['total_addresses']}")
    print(f"Обработано строк: {stats['processed_lines']}")
    print(f"Пропущено строк: {stats['skipped_lines']}")
    print("\nРаспределение по очередям:")
    for queue_key, count in sorted(stats['by_queue'].items()):
        print(f"  {queue_key}: {count} адресов")
    print(f"\nРезультаты сохранены в:")
    print(f"  - {OUT_CSV}")
    print(f"  - {OUT_JSON}")
    print(f"  - {STATS_FILE}")
    print("="*50)

if __name__ == '__main__':
    main()
