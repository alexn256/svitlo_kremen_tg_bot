# Налаштування Telegram Бота

## Крок 1: Створення бота в Telegram

1. **Відкрийте Telegram** і знайдіть бота [@BotFather](https://t.me/botfather)

2. **Створіть нового бота:**
   - Відправте команду `/newbot`
   - Введіть ім'я бота (наприклад, "Svitlo Kremen Bot")
   - Введіть username бота (має закінчуватися на "bot", наприклад, `svitlo_kremen_bot`)

3. **Збережіть токен:**
   - BotFather надасть вам токен, наприклад: `1234567890:ABCdefGHIjklMNOpqrsTUVwxyz`
   - **ВАЖЛИВО**: Зберігайте токен в секреті! Не публікуйте його в Git!

4. **Налаштуйте бота (опціонально):**
   - `/setdescription` - Опис бота
   - `/setabouttext` - Інформація про бота
   - `/setuserpic` - Аватар бота
   - `/setcommands` - Список команд (див. нижче)

### Рекомендовані команди для бота:

```
start - Почати роботу з ботом
help - Допомога та інструкції
cities - Список доступних міст
stats - Статистика по адресах
cancel - Скасувати поточну операцію
```

Відправте це BotFather через команду `/setcommands`

## Крок 2: Налаштування проекту

1. **Створіть конфігураційний файл:**
   ```bash
   cd bot/src/main/resources
   cp config.properties config.properties
   ```

2. **Відредагуйте `config.properties`:**
   ```properties
   bot.token=ВАШ_ТОКЕН_ВІД_BOTFATHER
   addresses.file.path=../parser/addresses.json
   ```

3. **Альтернативно: використовуйте змінну середовища**
   ```bash
   export BOT_TOKEN="ВАШ_ТОКЕН_ВІД_BOTFATHER"
   ```

## Крок 3: Запуск бота локально

### Встановіть Java 17 (якщо не встановлено):

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

**Windows:**
Завантажте з [adoptium.net](https://adoptium.net/)

### Запустіть бота:

```bash
cd bot

# Linux/Mac
./gradlew run

# Windows
gradlew.bat run
```

Бот запуститься і буде чекати на повідомлення!

## Крок 4: Тестування бота

1. Знайдіть вашого бота в Telegram за username
2. Відправте `/start`
3. Натисніть кнопку "Дізнатися чергу відключень"
4. Введіть адресу, наприклад: `м.Полтава, вул. Грабчака, 10`
5. Отримайте номер черги!

## Хостинг бота

Бот повинен працювати 24/7. Є кілька варіантів:

### Варіант 1: VPS (Digital Ocean, AWS EC2, Hetzner)

**Плюси:** Повний контроль, надійність
**Мінуси:** Платно (~$5-10/міс)

```bash
# На сервері:
git clone ваш_репозиторій
cd svitlo_kremen_tg_bot/bot

# Створіть config.properties або встановіть змінну BOT_TOKEN
export BOT_TOKEN="ваш_токен"

# Запустіть в фоні
nohup ./gradlew run &
```

**Краще: використовуйте systemd:**

Створіть `/etc/systemd/system/svitlobot.service`:
```ini
[Unit]
Description=Svitlo Kremen Telegram Bot
After=network.target

[Service]
Type=simple
User=your_user
WorkingDirectory=/home/your_user/svitlo_kremen_tg_bot/bot
Environment="BOT_TOKEN=your_token_here"
ExecStart=/usr/bin/java -jar build/libs/svitlo-kremen-bot-1.0.0.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

Запустіть:
```bash
sudo systemctl enable svitlobot
sudo systemctl start svitlobot
sudo systemctl status svitlobot
```

### Варіант 2: Railway.app (РЕКОМЕНДОВАНО - безкоштовно до певних лімітів)

**Покрокова інструкція деплою на Railway:**

#### 1. Підготовка репозиторія

Переконайтесь що у вас є:
- `Procfile` в корені проекту ✅
- `railway.json` з конфігурацією ✅
- `.railwayignore` для виключення зайвих файлів ✅
- Файл `parser/addresses.json` (має бути в репозиторії) ⚠️

**ВАЖЛИВО:** Перед деплоєм, перевірте що `addresses.json` існує:
```bash
ls parser/addresses.json
```

Якщо файлу немає, запустіть парсер:
```bash
cd parser
python3 parser.py
```

#### 2. Push в GitHub

```bash
# Додайте всі файли (крім config.properties з токеном!)
git add .
git commit -m "Підготовка до деплою на Railway"
git push origin main
```

#### 3. Створення проекту на Railway

1. **Відкрийте** [railway.app](https://railway.app)
2. **Зареєструйтесь** через GitHub
3. Натисніть **"New Project"**
4. Оберіть **"Deploy from GitHub repo"**
5. **Авторизуйте** Railway для доступу до ваших репозиторіїв
6. Оберіть репозиторій **`svitlo_kremen_tg_bot`**
7. Railway автоматично почне деплой

#### 4. Налаштування змінних середовища

1. У панелі Railway, відкрийте ваш проект
2. Перейдіть на вкладку **"Variables"**
3. Додайте змінну:
   - **Name:** `BOT_TOKEN`
   - **Value:** `ваш_токен_від_botfather`
4. Натисніть **"Add"**

Railway автоматично перезапустить бота з новими змінними.

#### 5. Налаштування Java версії (якщо потрібно)

Якщо виникає помилка з версією Java, додайте змінну:
- **Name:** `NIXPACKS_JDK_VERSION`
- **Value:** `21`

#### 6. Перевірка деплою

1. У панелі Railway перейдіть на вкладку **"Deployments"**
2. Перегляньте логи останнього деплою
3. Переконайтесь що немає помилок
4. Ви маєте побачити: `Бот запущено успішно!`

#### 7. Тестування

1. Відкрийте Telegram і знайдіть вашого бота
2. Відправте `/start`
3. Спробуйте пошукати адресу

#### Моніторинг на Railway

**Перегляд логів:**
1. Відкрийте ваш проект на Railway
2. Вкладка **"Deployments"** → оберіть активний деплой
3. Натисніть **"View Logs"**

**Перезапуск бота:**
- Вкладка **"Settings"** → **"Restart"**

#### Оновлення бота

Коли ви вносите зміни в код:

```bash
git add .
git commit -m "Оновлення бота"
git push origin main
```

Railway автоматично:
1. Визначить новий коміт
2. Пересобере проект
3. Задеплоїть нову версію

#### Переваги Railway:

✅ Безкоштовний план (500 годин на місяць)
✅ Автоматичний деплой з GitHub
✅ SSL сертифікати
✅ Логи в реальному часі
✅ Легке масштабування

#### Обмеження безкоштовного плану:

- 500 годин на місяць (~20 днів)
- 512 MB RAM
- 1 GB Disk

**Порада:** Для постійної роботи 24/7 потрібен платний план ($5/міс) або комбінація з іншими безкоштовними хостингами.

#### Troubleshooting Railway

**Бот не запускається:**
1. Перевірте логи на Railway
2. Переконайтесь що `BOT_TOKEN` встановлено
3. Перевірте що `addresses.json` є в репозиторії

**Помилка "Address file not found":**
```bash
# Переконайтесь що файл є:
git add parser/addresses.json
git commit -m "Додано addresses.json"
git push
```

**Бот працює але не відповідає:**
1. Перевірте токен у BotFather - можливо створили нового бота
2. Перевірте логи Railway - можливо є помилки
3. Перезапустіть деплой на Railway

### Варіант 3: Docker

**Створіть `Dockerfile` в папці `bot/`:**
```dockerfile
FROM gradle:7.6-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/../parser/addresses.json /app/addresses.json

ENV ADDRESSES_FILE_PATH=/app/addresses.json

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Запустіть:**
```bash
docker build -t svitlo-bot .
docker run -e BOT_TOKEN="your_token" svitlo-bot
```

### Варіант 4: Домашній комп'ютер

Якщо у вас є старий ПК або Raspberry Pi:

```bash
# Запустіть в screen або tmux
screen -S svitlobot
cd ~/svitlo_kremen_tg_bot/bot
./gradlew run

# Натисніть Ctrl+A, потім D щоб відключитися
# Щоб повернутися: screen -r svitlobot
```

## Моніторинг та логи

### Перегляд логів:
```bash
tail -f logs/bot.log
```

### Перевірка роботи:
- Відправте `/stats` в бот - має показати статистику
- Перевірте логи на помилки

## Оновлення бота

```bash
# Зупиніть бота
sudo systemctl stop svitlobot  # або Ctrl+C якщо в терміналі

# Отримайте нові зміни
git pull

# Пересоберіть
./gradlew build

# Запустіть знову
sudo systemctl start svitlobot
```

## Безпека

⚠️ **НІКОЛИ НЕ ПУБЛІКУЙТЕ ТОКЕН В GIT!**

Переконайтесь, що:
- `config.properties` в `.gitignore`
- Використовуєте змінні середовища на продакшені
- Регулярно оновлюєте залежності

## Troubleshooting

### Бот не відповідає:
1. Перевірте що він запущений: `systemctl status svitlobot`
2. Перевірте логи: `tail -f logs/bot.log`
3. Перевірте інтернет з'єднання
4. Перевірте що токен правильний

### Помилка "Address file not found":
- Перевірте шлях до `addresses.json` в `config.properties`
- Переконайтесь що ви запустили парсер

### OutOfMemoryError:
Збільште пам'ять JVM:
```bash
export JAVA_OPTS="-Xmx512m"
./gradlew run
```

## Підтримка

Якщо виникли проблеми:
1. Перевірте логи
2. Перезапустіть бота
3. Перевірте що addresses.json існує і не пустий
