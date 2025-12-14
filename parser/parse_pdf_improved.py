import pdfplumber
import re
import csv
import json

PDF_FILE = "HPV_25_26-zminy-hruden.pdf"
OUT_CSV = "result_improved.csv"
OUT_JSON = "result_improved.json"
UNPARSED = "unparsed_improved.txt"

# Паттерны для распознавания
QUEUE_PATTERN = re.compile(r'^(Перша|Друга|Третя|Четверта|П[.\'\u2019ʼ]ята|Шоста)\s+черга\s*$', re.IGNORECASE)
SUBQUEUE_PATTERN = re.compile(r'^(Перша|Друга|Третя|Четверта|П[.\'\u2019ʼ]ята|Шоста)\s+черга\s+(І|ІІ)\s+підчерга', re.IGNORECASE)
BRANCH_PATTERN = re.compile(r'(Полтавська|Кременчуцька|Лубенська|Миргородська|Хорольська|Гадяцька).*(філія|дільниця)', re.IGNORECASE)

# Мапинг названий черг на цифры
QUEUE_MAP = {
    'Перша': 1, 'Друга': 2, 'Третя': 3,
    'Четверта': 4, 'П.ята': 5, 'Пʼята': 5, "П'ята": 5, 'П\u2019ята': 5, 'Шоста': 6
}
SUBQUEUE_MAP = {'І': 1, 'ІІ': 2}

def parse_queue_number(text):
    """Извлекает номер черги из текста"""
    for name, num in QUEUE_MAP.items():
        if name in text:
            return num
    return None

def parse_subqueue_number(text):
    """Извлекает номер підчерги из текста"""
    for name, num in SUBQUEUE_MAP.items():
        if f'{name} підчерга' in text:
            return num
    return None

def expand_house_range(house_str):
    """Разворачивает диапазон домов: '45-64' -> ['45', '46', ..., '64']"""
    match = re.match(r'^(\d+)\s*-\s*(\d+)$', house_str.strip())
    if match:
        start, end = int(match.group(1)), int(match.group(2))
        return [str(i) for i in range(start, end + 1)]
    return [house_str.strip()]

def extract_houses(houses_text):
    """Извлекает список домов из текста, разворачивая диапазоны"""
    houses = []
    # Разбиваем по запятым и точкам с запятой
    parts = re.split(r'[,;]\s*', houses_text)
    for part in parts:
        part = part.strip()
        if not part:
            continue
        # Проверяем, не диапазон ли это
        expanded = expand_house_range(part)
        houses.extend(expanded)
    return houses

def parse_addresses(text, current_city=None):
    """
    Парсит адреса из текста.
    Возвращает список словарей: [{'city': ..., 'street': ..., 'houses': [...]}, ...]
    """
    results = []

    # Убираем лишние пробелы и переносы строк
    text = re.sub(r'\s+', ' ', text)

    # Ищем города/села - более гибкий паттерн
    city_pattern = re.compile(r'(м\.|с\.|смт\.?)\s*([^:,;\.]+?)(?:\s*:|;|(?=\s+вул\.|\s+пров\.|\s+просп\.))', re.IGNORECASE)

    # Улучшенный паттерн для улиц с домами
    street_pattern = re.compile(
        r'(вул\.|пров\.|просп\.|пл\.)\s*([^,;:]+?),\s*([0-9][^м\.с\.вул\.пров\.просп\.пл\.]+?)(?=(?:\s+вул\.|\s+пров\.|\s+просп\.|\s+пл\.|\s+м\.|\s+с\.|\s+смт\.)|$)',
        re.IGNORECASE
    )

    # Разбиваем текст на сегменты по городам
    segments = []
    city_matches = list(city_pattern.finditer(text))

    for i, city_match in enumerate(city_matches):
        city_prefix = city_match.group(1)
        city_name = city_match.group(2).strip().rstrip(':').rstrip()
        city = f"{city_prefix}{city_name}"

        # Находим начало следующего города или конец текста
        if i + 1 < len(city_matches):
            end_pos = city_matches[i + 1].start()
        else:
            end_pos = len(text)

        segment_text = text[city_match.end():end_pos]
        segments.append((city, segment_text))

    # Если не найдено городов, используем текущий город
    if not segments and current_city:
        segments = [(current_city, text)]

    # Если все еще нет сегментов, пропускаем
    if not segments:
        return results

    # Парсим каждый сегмент
    for city, segment_text in segments:
        # Очищаем начало сегмента от двоеточий и пробелов
        segment_text = segment_text.lstrip(': ')

        # Ищем улицы с префиксами
        street_matches = list(street_pattern.finditer(segment_text))

        for street_match in street_matches:
            street_prefix = street_match.group(1)
            street_name = street_match.group(2).strip()
            houses_text = street_match.group(3).strip()

            # Очищаем houses_text от точек в конце
            houses_text = houses_text.rstrip('.;,')

            street = f"{street_prefix} {street_name}"
            houses = extract_houses(houses_text)

            if houses:
                results.append({
                    'city': city,
                    'street': street,
                    'houses': houses
                })

    return results

def main():
    rows = []
    unparsed_lines = []

    current_queue = None
    current_subqueue = None
    current_branch = None
    current_city = None

    total_addresses = 0

    with pdfplumber.open(PDF_FILE) as pdf:
        print(f"Обработка {len(pdf.pages)} страниц...")

        for page_num, page in enumerate(pdf.pages):
            tables = page.extract_tables()

            if not tables:
                continue

            for table in tables:
                for row in table:
                    if not row or not row[0]:
                        continue

                    cell_text = row[0].strip() if row[0] else ""

                    # Проверяем, это черга?
                    if QUEUE_PATTERN.match(cell_text):
                        current_queue = parse_queue_number(cell_text)
                        current_subqueue = None
                        print(f"\n[Страница {page_num + 1}] Черга: {current_queue}")
                        continue

                    # Проверяем, это підчерга?
                    if SUBQUEUE_PATTERN.match(cell_text):
                        queue_num = parse_queue_number(cell_text)
                        sub_num = parse_subqueue_number(cell_text)
                        if queue_num:
                            current_queue = queue_num
                        current_subqueue = sub_num
                        print(f"[Страница {page_num + 1}] Підчерга: {current_queue}.{current_subqueue}")
                        continue

                    # Проверяем, это филиал?
                    branch_match = BRANCH_PATTERN.search(cell_text)
                    if branch_match:
                        current_branch = branch_match.group(1)
                        print(f"[Страница {page_num + 1}] Філія: {current_branch}")
                        continue

                    # Обрабатываем только Полтавську філію
                    if current_branch != 'Полтавська':
                        continue

                    # Пропускаем строки без очереди или подочереди
                    if current_queue is None or current_subqueue is None:
                        continue

                    # Проверяем, есть ли данные во второй колонке
                    if len(row) < 2 or not row[1]:
                        continue

                    address_text = row[1].strip()

                    # Пропускаем строки с названиями организаций (без адресов)
                    if not any(marker in address_text.lower() for marker in ['вул.', 'пров.', 'просп.', 'пл.', 'м.', 'с.', 'смт']):
                        continue

                    # Парсим адреса
                    parsed_addresses = parse_addresses(address_text, current_city)

                    if not parsed_addresses:
                        unparsed_lines.append(f"[{current_queue}.{current_subqueue}] {address_text}")
                        continue

                    # Добавляем адреса в результат
                    for addr in parsed_addresses:
                        city = addr['city']
                        street = addr['street']

                        # Обновляем текущий город
                        if city:
                            current_city = city

                        for house in addr['houses']:
                            rows.append({
                                'branch': current_branch,
                                'queue': current_queue,
                                'subqueue': current_subqueue,
                                'queue_full': f"{current_queue}.{current_subqueue}",
                                'city': city,
                                'street': street,
                                'house': house
                            })
                            total_addresses += 1

    # Сохраняем в CSV
    print(f"\nСохранение результатов...")
    with open(OUT_CSV, "w", newline="", encoding="utf-8") as f:
        fieldnames = ['branch', 'queue', 'subqueue', 'queue_full', 'city', 'street', 'house']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    # Сохраняем в JSON (для бота удобнее)
    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)

    # Сохраняем необработанные строки
    with open(UNPARSED, "w", encoding="utf-8") as f:
        for line in unparsed_lines:
            f.write(line + "\n")

    print(f"\n✓ Готово!")
    print(f"  Всего адресов: {total_addresses}")
    print(f"  Сохранено в {OUT_CSV}")
    print(f"  Сохранено в {OUT_JSON}")
    print(f"  Необработанных строк: {len(unparsed_lines)} (см. {UNPARSED})")

if __name__ == "__main__":
    main()
