# Швидкий старт - Svitlo Kremen Bot

## За 5 хвилин до запуску бота! ⚡

### Крок 1: Підготовка (2 хв)

```bash
# 1. Встановіть необхідні пакети
sudo apt install python3-venv openjdk-17-jdk

# 2. Клонуйте проект
git clone https://github.com/yourusername/svitlo_kremen_tg_bot.git
cd svitlo_kremen_tg_bot
```

### Крок 2: Парсинг адрес (1 хв)

```bash
cd parser
python3 -m venv ../venv
source ../venv/bin/activate
pip install pdfplumber
python3 parse_pdf_v2.py
```

✅ Готово! Створено файл `addresses.json` з 3551+ адресами

### Крок 3: Створіть Telegram бота (1 хв)

1. Відкрийте Telegram
2. Знайдіть [@BotFather](https://t.me/botfather)
3. Відправте: `/newbot`
4. Введіть ім'я: `Svitlo Kremen Bot`
5. Введіть username: `svitlo_kremen_bot` (або інший, що закінчується на `bot`)
6. **Збережіть токен!** (виглядає як `1234567890:ABCdefGHIjklMNOpqrsTUVwxyz`)

### Крок 4: Налаштуйте бота (1 хв)

```bash
cd ../bot/src/main/resources
cp config.properties.example config.properties

# Відредагуйте config.properties
nano config.properties
```

Вставте ваш токен:
```properties
bot.token=ВАШ_ТОКЕН_ВІД_BOTFATHER
addresses.file.path=../parser/addresses.json
```

Збережіть (Ctrl+O, Enter, Ctrl+X)

### Крок 5: Запустіть бота! (30 сек)

```bash
cd ../../..
chmod +x gradlew  # Тільки для Linux/Mac
./gradlew run
```

**Windows:**
```bash
gradlew.bat run
```

### Готово! 🎉

Тепер знайдіть вашого бота в Telegram та відправте `/start`

---

## Що далі?

- 📖 Прочитайте повну документацію: [README.md](README.md)
- 🤖 Налаштування бота: [bot/BOT_SETUP.md](bot/BOT_SETUP.md)
- 🔧 Інструкція парсера: [parser/USAGE.md](parser/USAGE.md)

---

## Проблеми?

### Бот не відповідає
```bash
# Перевірте логи
tail -f bot/logs/bot.log
```

### Адреси не знайдені
```bash
# Перевірте що файл існує
ls -lh parser/addresses.json
```

### Gradle помилки
```bash
# Перевірте Java
java -version  # Має бути 17+

# Очистіть кеш
cd bot
./gradlew clean build
```

---

**Потрібна допомога?** Відкрийте Issue на GitHub або перегляньте повну документацію!
