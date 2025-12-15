package com.svitlobot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.svitlobot.model.UserData
import com.svitlobot.model.UserState
import com.svitlobot.service.AddressService
import mu.KotlinLogging
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}

private val userStates = mutableMapOf<Long, UserData>()

private fun sendCitiesPage(
    bot: com.github.kotlintelegrambot.Bot,
    chatId: ChatId,
    page: Int,
    addressService: AddressService
) {
    val (cities, totalPages) = addressService.getCitiesPage(page, 20)
    val allCities = addressService.getCities()

    val citiesMessage = buildString {
        appendLine("🏙 Доступні міста та села (сторінка ${page + 1} з $totalPages):")
        appendLine()
        cities.forEach { city ->
            appendLine("• $city")
        }
        appendLine()
        appendLine("Всього міст/сіл: ${allCities.size}")
    }

    val buttons = mutableListOf<InlineKeyboardButton>()

    if (page > 0) {
        buttons.add(
            InlineKeyboardButton.CallbackData(
                text = "⬅️ Попередня",
                callbackData = "cities_page_${page - 1}"
            )
        )
    }

    if (page < totalPages - 1) {
        buttons.add(
            InlineKeyboardButton.CallbackData(
                text = "Наступна ➡️",
                callbackData = "cities_page_${page + 1}"
            )
        )
    }

    val keyboard = if (buttons.isNotEmpty()) {
        InlineKeyboardMarkup.create(buttons)
    } else {
        null
    }

    bot.sendMessage(
        chatId = chatId,
        text = citiesMessage,
        replyMarkup = keyboard
    )
}

private fun sendCitiesSelection(
    bot: com.github.kotlintelegrambot.Bot,
    chatId: ChatId,
    page: Int,
    addressService: AddressService
) {
    val (cities, totalPages) = addressService.getCitiesPage(page, 10)

    val message = buildString {
        appendLine("🏙 Оберіть ваше місто/село:")
        appendLine()
        appendLine("(Сторінка ${page + 1} з $totalPages)")
    }

    val buttons = mutableListOf<List<InlineKeyboardButton>>()

    // Добавляем кнопки городов (по 2 в ряд)
    cities.chunked(2).forEach { citiesInRow ->
        buttons.add(citiesInRow.map { city ->
            InlineKeyboardButton.CallbackData(
                text = city,
                callbackData = "select_city:$city"
            )
        })
    }

    // Добавляем навигацию
    val navigationButtons = mutableListOf<InlineKeyboardButton>()
    if (page > 0) {
        navigationButtons.add(
            InlineKeyboardButton.CallbackData(
                text = "⬅️ Попередня",
                callbackData = "select_city_page:${page - 1}"
            )
        )
    }
    if (page < totalPages - 1) {
        navigationButtons.add(
            InlineKeyboardButton.CallbackData(
                text = "Наступна ➡️",
                callbackData = "select_city_page:${page + 1}"
            )
        )
    }
    if (navigationButtons.isNotEmpty()) {
        buttons.add(navigationButtons)
    }

    // Кнопка отмены
    buttons.add(
        listOf(
            InlineKeyboardButton.CallbackData(
                text = "❌ Скасувати",
                callbackData = "cancel_selection"
            )
        )
    )

    val keyboard = InlineKeyboardMarkup.create(buttons)

    bot.sendMessage(
        chatId = chatId,
        text = message,
        replyMarkup = keyboard
    )
}

private fun sendStreetsSelection(
    bot: com.github.kotlintelegrambot.Bot,
    chatId: ChatId,
    city: String,
    page: Int,
    addressService: AddressService
) {
    val (streets, totalPages) = addressService.getStreetsPage(city, page, 10)

    val message = buildString {
        appendLine("📍 Місто: $city")
        appendLine()
        appendLine("Оберіть вашу вулицю:")
        appendLine()
        appendLine("(Сторінка ${page + 1} з $totalPages)")
    }

    val buttons = mutableListOf<List<InlineKeyboardButton>>()

    // Добавляем кнопки улиц (по 2 в ряд)
    streets.chunked(2).forEach { streetsInRow ->
        buttons.add(streetsInRow.map { street ->
            InlineKeyboardButton.CallbackData(
                text = street,
                callbackData = "select_street:$street"
            )
        })
    }

    // Добавляем навигацию
    val navigationButtons = mutableListOf<InlineKeyboardButton>()
    if (page > 0) {
        navigationButtons.add(
            InlineKeyboardButton.CallbackData(
                text = "⬅️ Попередня",
                callbackData = "select_street_page:${page - 1}"
            )
        )
    }
    if (page < totalPages - 1) {
        navigationButtons.add(
            InlineKeyboardButton.CallbackData(
                text = "Наступна ➡️",
                callbackData = "select_street_page:${page + 1}"
            )
        )
    }
    if (navigationButtons.isNotEmpty()) {
        buttons.add(navigationButtons)
    }

    // Кнопки назад и отмены
    buttons.add(
        listOf(
            InlineKeyboardButton.CallbackData(
                text = "⬅️ Назад до міст",
                callbackData = "back_to_cities"
            ),
            InlineKeyboardButton.CallbackData(
                text = "❌ Скасувати",
                callbackData = "cancel_selection"
            )
        )
    )

    val keyboard = InlineKeyboardMarkup.create(buttons)

    bot.sendMessage(
        chatId = chatId,
        text = message,
        replyMarkup = keyboard
    )
}

private fun sendHousesSelection(
    bot: com.github.kotlintelegrambot.Bot,
    chatId: ChatId,
    city: String,
    street: String,
    page: Int,
    addressService: AddressService
) {
    val (houses, totalPages) = addressService.getHousesPage(city, street, page, 15)

    val message = buildString {
        appendLine("📍 Місто: $city")
        appendLine("🏘 Вулиця: $street")
        appendLine()
        appendLine("Оберіть номер будинку:")
        appendLine()
        appendLine("(Сторінка ${page + 1} з $totalPages)")
    }

    val buttons = mutableListOf<List<InlineKeyboardButton>>()

    // Добавляем кнопки домов (по 4 в ряд)
    houses.chunked(4).forEach { housesInRow ->
        buttons.add(housesInRow.map { house ->
            InlineKeyboardButton.CallbackData(
                text = house,
                callbackData = "select_house:$house"
            )
        })
    }

    // Добавляем навигацию
    val navigationButtons = mutableListOf<InlineKeyboardButton>()
    if (page > 0) {
        navigationButtons.add(
            InlineKeyboardButton.CallbackData(
                text = "⬅️ Попередня",
                callbackData = "select_house_page:${page - 1}"
            )
        )
    }
    if (page < totalPages - 1) {
        navigationButtons.add(
            InlineKeyboardButton.CallbackData(
                text = "Наступна ➡️",
                callbackData = "select_house_page:${page + 1}"
            )
        )
    }
    if (navigationButtons.isNotEmpty()) {
        buttons.add(navigationButtons)
    }

    // Кнопки назад и отмены
    buttons.add(
        listOf(
            InlineKeyboardButton.CallbackData(
                text = "⬅️ Назад до вулиць",
                callbackData = "back_to_streets"
            ),
            InlineKeyboardButton.CallbackData(
                text = "❌ Скасувати",
                callbackData = "cancel_selection"
            )
        )
    )

    val keyboard = InlineKeyboardMarkup.create(buttons)

    bot.sendMessage(
        chatId = chatId,
        text = message,
        replyMarkup = keyboard
    )
}

fun main() {
    logger.info { "Запуск Svitlo Kremen Telegram Bot..." }

    val config = loadConfig()
    val botToken = config.getProperty("bot.token")
        ?: throw IllegalStateException("BOT_TOKEN не знайдено в config.properties або змінних середовища")

    val addressesFilePath = config.getProperty("addresses.file.path", "../parser/addresses.json")

    val addressService = AddressService(addressesFilePath)
    logger.info { "Сервіс адрес ініціалізовано. Всього адрес: ${addressService.getTotalAddresses()}" }

    val bot = bot {
        token = botToken

        dispatch {
            command("start") {
                val chatId = ChatId.fromId(message.chat.id)
                val userId = message.from?.id ?: return@command

                userStates[userId] = UserData(userId)

                val welcomeMessage = """
                    👋 Вітаємо!

                    Я бот для перевірки черги відключень електроенергії у Полтавській області за адресою.
                    👧 Мене створила мила дівчинка Сонечка Мармеладова (@M_AHTS).

                    📍 Я можу допомогти вам дізнатися вашу чергу відключень за адресою.

                    Всього в базі: ${addressService.getTotalAddresses()} адрес

                    Натисніть кнопку нижче, щоб почати 👇
                """.trimIndent()

                val keyboard = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "🔍 Дізнатися чергу відключень",
                            callbackData = "find_queue"
                        )
                    )
                )

                bot.sendMessage(
                    chatId = chatId,
                    text = welcomeMessage,
                    replyMarkup = keyboard
                )
            }

            command("help") {
                val chatId = ChatId.fromId(message.chat.id)

                val helpMessage = """
                    📖 Допомога

                    Щоб дізнатися вашу чергу відключень:
                    1️⃣ Натисніть кнопку "Дізнатися чергу"
                    2️⃣ Введіть вашу адресу

                    📝 Приклади введення адреси:
                    • Горішні Плавні/Портова/1
                    • м.Полтава*Індустріальна*10А
                    • Кременчук-Перемоги-12
                    • м.Лубни, Київська, 15

                    ℹ️ Розділювачі: / * - або ,
                    Регістр букв не має значення

                    ℹ️ Команди:
                    /start - Початок роботи
                    /help - Ця довідка
                    /cities - Список міст (з навігацією)
                    /stats - Статистика
                    /cancel - Скасувати поточну операцію
                """.trimIndent()

                bot.sendMessage(chatId = chatId, text = helpMessage)
            }

            command("cities") {
                val chatId = ChatId.fromId(message.chat.id)
                sendCitiesPage(bot, chatId, 0, addressService)
            }

            command("stats") {
                val chatId = ChatId.fromId(message.chat.id)
                val stats = addressService.getQueueStats()

                val statsMessage = buildString {
                    appendLine("📊 Статистика адрес по чергах:")
                    appendLine()
                    stats.forEach { (queue, count) ->
                        appendLine("Черга $queue: $count адрес")
                    }
                    appendLine()
                    appendLine("Всього адрес: ${addressService.getTotalAddresses()}")
                }

                bot.sendMessage(chatId = chatId, text = statsMessage)
            }

            command("cancel") {
                val chatId = ChatId.fromId(message.chat.id)
                val userId = message.from?.id ?: return@command

                userStates[userId] = UserData(userId, UserState.IDLE)

                bot.sendMessage(
                    chatId = chatId,
                    text = "❌ Операцію скасовано. Натисніть /start щоб почати знову."
                )
            }

            callbackQuery("find_queue") {
                val chatId = ChatId.fromId(callbackQuery.message?.chat?.id ?: return@callbackQuery)
                val userId = callbackQuery.from.id

                userStates[userId] = UserData(userId, UserState.CHOOSING_CITY)

                sendCitiesSelection(bot, chatId, 0, addressService)
                bot.answerCallbackQuery(callbackQuery.id)
            }

            callbackQuery {
                val data = callbackQuery.data
                val chatId = ChatId.fromId(callbackQuery.message?.chat?.id ?: return@callbackQuery)
                val userId = callbackQuery.from.id

                when {
                    // Пагинация для команды /cities
                    data.startsWith("cities_page_") -> {
                        val page = data.removePrefix("cities_page_").toIntOrNull() ?: 0
                        sendCitiesPage(bot, chatId, page, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Пагинация для выбора города
                    data.startsWith("select_city_page:") -> {
                        val page = data.removePrefix("select_city_page:").toIntOrNull() ?: 0
                        sendCitiesSelection(bot, chatId, page, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Выбор города
                    data.startsWith("select_city:") -> {
                        val city = data.removePrefix("select_city:")
                        val userData = userStates[userId] ?: UserData(userId)
                        userData.selectedCity = city
                        userData.state = UserState.CHOOSING_STREET
                        userStates[userId] = userData

                        sendStreetsSelection(bot, chatId, city, 0, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Пагинация для выбора улицы
                    data.startsWith("select_street_page:") -> {
                        val page = data.removePrefix("select_street_page:").toIntOrNull() ?: 0
                        val userData = userStates[userId]
                        val city = userData?.selectedCity ?: return@callbackQuery

                        sendStreetsSelection(bot, chatId, city, page, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Выбор улицы
                    data.startsWith("select_street:") -> {
                        val street = data.removePrefix("select_street:")
                        val userData = userStates[userId] ?: return@callbackQuery
                        val city = userData.selectedCity ?: return@callbackQuery

                        userData.selectedStreet = street
                        userData.state = UserState.CHOOSING_HOUSE
                        userStates[userId] = userData

                        sendHousesSelection(bot, chatId, city, street, 0, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Пагинация для выбора дома
                    data.startsWith("select_house_page:") -> {
                        val page = data.removePrefix("select_house_page:").toIntOrNull() ?: 0
                        val userData = userStates[userId]
                        val city = userData?.selectedCity ?: return@callbackQuery
                        val street = userData.selectedStreet ?: return@callbackQuery

                        sendHousesSelection(bot, chatId, city, street, page, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Выбор дома - показываем результат
                    data.startsWith("select_house:") -> {
                        val house = data.removePrefix("select_house:")
                        val userData = userStates[userId] ?: return@callbackQuery
                        val city = userData.selectedCity ?: return@callbackQuery
                        val street = userData.selectedStreet ?: return@callbackQuery

                        val foundAddress = addressService.findQueue(city, street, house)

                        if (foundAddress != null) {
                            val resultMessage = """
                                ✅ Адресу знайдено!

                                📍 ${foundAddress.toDisplayString()}
                                ⚡ Черга відключень: ${foundAddress.queue_full}

                                ---
                                Філія: ${foundAddress.branch}
                            """.trimIndent()

                            val keyboard = InlineKeyboardMarkup.create(
                                listOf(
                                    InlineKeyboardButton.CallbackData(
                                        text = "🔍 Шукати іншу адресу",
                                        callbackData = "find_queue"
                                    )
                                )
                            )

                            bot.sendMessage(
                                chatId = chatId,
                                text = resultMessage,
                                replyMarkup = keyboard
                            )

                            userStates[userId] = UserData(userId, UserState.IDLE)
                        } else {
                            bot.sendMessage(
                                chatId = chatId,
                                text = "❌ Помилка: адресу не знайдено в базі даних"
                            )
                        }

                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Кнопка "Назад до міст"
                    data == "back_to_cities" -> {
                        val userData = userStates[userId] ?: UserData(userId)
                        userData.selectedCity = null
                        userData.selectedStreet = null
                        userData.state = UserState.CHOOSING_CITY
                        userStates[userId] = userData

                        sendCitiesSelection(bot, chatId, 0, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Кнопка "Назад до вулиць"
                    data == "back_to_streets" -> {
                        val userData = userStates[userId] ?: return@callbackQuery
                        val city = userData.selectedCity ?: return@callbackQuery

                        userData.selectedStreet = null
                        userData.state = UserState.CHOOSING_STREET
                        userStates[userId] = userData

                        sendStreetsSelection(bot, chatId, city, 0, addressService)
                        bot.answerCallbackQuery(callbackQuery.id)
                    }

                    // Кнопка отмены
                    data == "cancel_selection" -> {
                        userStates[userId] = UserData(userId, UserState.IDLE)

                        bot.sendMessage(
                            chatId = chatId,
                            text = "❌ Операцію скасовано. Натисніть /start щоб почати знову."
                        )
                        bot.answerCallbackQuery(callbackQuery.id)
                    }
                }
            }

            message {
                val chatId = ChatId.fromId(message.chat.id)
                val userId = message.from?.id ?: return@message
                val text = message.text ?: return@message

                if (text.startsWith("/")) return@message

                val userData = userStates[userId] ?: UserData(userId)

                when (userData.state) {
                    UserState.WAITING_FOR_ADDRESS -> {
                        handleAddressInput(bot, chatId, userId, text, addressService)
                    }

                    UserState.IDLE -> {
                        bot.sendMessage(
                            chatId = chatId,
                            text = "Для пошуку адреси натисніть /start і оберіть 'Дізнатися чергу'"
                        )
                    }

                    UserState.CHOOSING_CITY,
                    UserState.CHOOSING_STREET,
                    UserState.CHOOSING_HOUSE -> {
                        bot.sendMessage(
                            chatId = chatId,
                            text = "Будь ласка, використовуйте кнопки для вибору. Або натисніть /cancel для скасування."
                        )
                    }
                }
            }
        }
    }

    logger.info { "Бот запущено успішно!" }
    bot.startPolling()
}

private fun handleAddressInput(
    bot: com.github.kotlintelegrambot.Bot,
    chatId: ChatId,
    userId: Long,
    addressText: String,
    addressService: AddressService
) {
    logger.info { "Користувач $userId шукає адресу: $addressText" }

    bot.sendMessage(chatId = chatId, text = "🔍 Шукаю адресу...")

    val foundAddress = addressService.smartSearch(addressText)

    if (foundAddress != null) {
        val resultMessage = """
            ✅ Адресу знайдено!

            📍 ${foundAddress.toDisplayString()}
            ⚡ Черга відключень: ${foundAddress.queue_full}

            ---
            Філія: ${foundAddress.branch}
        """.trimIndent()

        val keyboard = InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(
                    text = "🔍 Шукати іншу адресу",
                    callbackData = "find_queue"
                )
            )
        )

        bot.sendMessage(
            chatId = chatId,
            text = resultMessage,
            replyMarkup = keyboard
        )

        userStates[userId] = UserData(userId, UserState.IDLE)

    } else {
        val errorMessage = """
            ❌ Адресу не знайдено

            Можливо, ви ввели:
            • Неправильну назву міста
            • Неправильну назву вулиці
            • Неіснуючий номер будинку

            💡 Спробуйте ще раз або перегляньте список міст: /cities

            Приклади правильного формату:
            • Горішні Плавні/Портова/1
            • м.Полтава*Індустріальна*10А
            • Кременчук-Перемоги-12
            • м.Лубни, Київська, 15

            Ви можете використовувати розділювачі: / * - або ,
        """.trimIndent()

        bot.sendMessage(chatId = chatId, text = errorMessage)
    }
}

private fun loadConfig(): Properties {
    val properties = Properties()

    val configFile = File("src/main/resources/config.properties")
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
        logger.info { "Конфігурацію завантажено з файлу" }
    } else {
        logger.warn { "Файл config.properties не знайдено, використовуємо змінні середовища" }
    }

    if (!properties.containsKey("bot.token")) {
        val tokenFromEnv = System.getenv("BOT_TOKEN")
        if (tokenFromEnv != null) {
            properties.setProperty("bot.token", tokenFromEnv)
            logger.info { "Токен бота завантажено зі змінної середовища BOT_TOKEN" }
        }
    }

    if (!properties.containsKey("addresses.file.path")) {
        val addressesPathFromEnv = System.getenv("ADDRESSES_FILE_PATH")
        if (addressesPathFromEnv != null) {
            properties.setProperty("addresses.file.path", addressesPathFromEnv)
            logger.info { "Шлях до файлу адрес завантажено зі змінної середовища ADDRESSES_FILE_PATH" }
        }
    }

    return properties
}
