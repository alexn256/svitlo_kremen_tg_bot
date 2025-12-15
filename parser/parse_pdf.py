import pdfplumber
import re
import csv

PDF_FILE = "HPV_25_26-zminy-hruden.pdf"
OUT_CSV = "result.csv"
UNPARSED = "unparsed.txt"

queue_pattern = re.compile(r"Черга\s*(\d)")
street_pattern = re.compile(
    r"(вул\.|пров\.|просп\.|пл\.)\s*([^,]+),\s*(.+)",
    re.IGNORECASE
)

def expand_houses(house_part: str):
    result = []

    parts = re.split(r",\s*", house_part)
    for p in parts:
        p = p.strip()

        if re.match(r"\d+\s*-\s*\d+", p):
            start, end = map(int, re.split(r"\s*-\s*", p))
            result.extend(str(i) for i in range(start, end + 1))

        else:
            result.append(p)

    return result


rows = []
unparsed_lines = []

current_queue = None
current_city = None

with pdfplumber.open(PDF_FILE) as pdf:
    for page in pdf.pages:
        text = page.extract_text()
        if not text:
            continue

        for line in text.split("\n"):
            line = line.strip()

            q_match = queue_pattern.search(line)
            if q_match:
                current_queue = int(q_match.group(1))
                continue

            if line.startswith(("м.", "с.", "смт")):
                current_city = line
                continue

            s_match = street_pattern.search(line)
            if s_match and current_queue and current_city:
                street_type, street_name, houses = s_match.groups()
                houses_list = expand_houses(houses)

                for house in houses_list:
                    rows.append({
                        "city": current_city,
                        "street": f"{street_type} {street_name}",
                        "house": house,
                        "queue": current_queue
                    })
            else:
                if any(x in line for x in ["вул.", "просп.", "пров."]):
                    unparsed_lines.append(line)

with open(OUT_CSV, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(
        f,
        fieldnames=["city", "street", "house", "queue"]
    )
    writer.writeheader()
    writer.writerows(rows)

with open(UNPARSED, "w", encoding="utf-8") as f:
    for l in unparsed_lines:
        f.write(l + "\n")

print(f"Complete!")
print(f"Addresses: {len(rows)}")
print(f"Unparsed lines: {len(unparsed_lines)}")
