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
                    Мене створила мила дівчинка Сонечка Мармеладова (@M_AHTS) 💫

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

                    📝 Формати введення адреси:
                    • м.Полтава, вул. Грабчака, 10
                    • Полтава вул. Грабчака 10
                    • Полтава Грабчака 10

                    ℹ️ Команди:
                    /start - Початок роботи
                    /help - Ця довідка
                    /cities - Список міст
                    /stats - Статистика
                    /cancel - Скасувати поточну операцію
                """.trimIndent()

                bot.sendMessage(chatId = chatId, text = helpMessage)
            }

            command("cities") {
                val chatId = ChatId.fromId(message.chat.id)
                val cities = addressService.getCities()

                val citiesMessage = buildString {
                    appendLine("🏙 Доступні міста та села:")
                    appendLine()
                    cities.take(50).forEach { city ->
                        appendLine("• $city")
                    }
                    if (cities.size > 50) {
                        appendLine()
                        appendLine("... і ще ${cities.size - 50} міст/сіл")
                    }
                }

                bot.sendMessage(chatId = chatId, text = citiesMessage)
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

                userStates[userId] = UserData(userId, UserState.WAITING_FOR_ADDRESS)

                val message = """
                    📍 Введіть вашу адресу

                    Формати:
                    • м.Полтава, вул. Грабчака, 10
                    • Полтава вул. Грабчака 10
                    • Полтава Грабчака 10

                    Для скасування введіть /cancel
                """.trimIndent()

                bot.sendMessage(chatId = chatId, text = message)
                bot.answerCallbackQuery(callbackQuery.id)
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
            • м.Полтава, вул. Грабчака, 10
            • с.Омельник, вул. Шкільна, 5
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

    return properties
}
