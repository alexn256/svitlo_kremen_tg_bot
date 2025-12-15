package com.example.svitlobot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class Address(
    val branch: String,
    val queue: Int,
    val subqueue: Int,
    val queue_full: String,
    val city: String,
    val street: String,
    val house: String
)

class AddressLookup(private val jsonFilePath: String) {
    private val addresses: List<Address>

    init {
        val jsonString = File(jsonFilePath).readText()
        val gson = Gson()
        val addressListType = object : TypeToken<List<Address>>() {}.type
        addresses = gson.fromJson(jsonString, addressListType)

        println("Загружено ${addresses.size} адресов")
    }

    fun findQueue(city: String, street: String, house: String): String? {
        val normalizedCity = city.trim()
        val normalizedStreet = street.trim()
        val normalizedHouse = house.trim()

        val found = addresses.find { addr ->
            addr.city.equals(normalizedCity, ignoreCase = true) &&
            addr.street.equals(normalizedStreet, ignoreCase = true) &&
            addr.house.equals(normalizedHouse, ignoreCase = true)
        }

        return found?.queue_full
    }

    fun getCities(): List<String> {
        return addresses
            .map { it.city }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getStreets(city: String): List<String> {
        return addresses
            .filter { it.city.equals(city, ignoreCase = true) }
            .map { it.street }
            .distinct()
            .sorted()
    }

    fun getHouses(city: String, street: String): List<String> {
        return addresses
            .filter {
                it.city.equals(city, ignoreCase = true) &&
                it.street.equals(street, ignoreCase = true)
            }
            .map { it.house }
            .distinct()
            .sortedWith(compareBy({ it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }, { it }))
    }

    fun searchAddresses(query: String): List<Address> {
        val normalizedQuery = query.trim().lowercase()

        return addresses.filter { addr ->
            addr.city.lowercase().contains(normalizedQuery) ||
            addr.street.lowercase().contains(normalizedQuery) ||
            addr.house.lowercase().contains(normalizedQuery)
        }.take(20)
    }

    fun getQueueStats(): Map<String, Int> {
        return addresses
            .groupBy { it.queue_full }
            .mapValues { it.value.size }
            .toSortedMap()
    }
}

fun main() {
    val lookup = AddressLookup("../../parser/addresses.json")
    println("\n=== Пример 1: Поиск очереди по адресу ===")
    val queue = lookup.findQueue("м.Полтава", "вул. Грабчака", "10")
    if (queue != null) {
        println("Адрес: м.Полтава, вул. Грабчака, 10")
        println("Черга: $queue")
    } else {
        println("Адрес не найден")
    }

    println("\n=== Пример 2: Список городов ===")
    val cities = lookup.getCities()
    println("Всего городов: ${cities.size}")
    cities.take(10).forEach { println("  - $it") }

    println("\n=== Пример 3: Улицы в м.Полтава ===")
    val streets = lookup.getStreets("м.Полтава")
    println("Всего улиц: ${streets.size}")
    streets.take(10).forEach { println("  - $it") }

    println("\n=== Пример 4: Дома на вул. Грабчака ===")
    val houses = lookup.getHouses("м.Полтава", "вул. Грабчака")
    println("Всего домов: ${houses.size}")
    houses.forEach { println("  - $it") }

    println("\n=== Пример 5: Поиск по запросу ===")
    val results = lookup.searchAddresses("Полтава")
    println("Найдено адресов: ${results.size}")
    results.take(5).forEach {
        println("  - ${it.city}, ${it.street}, ${it.house} -> Черга ${it.queue_full}")
    }

    println("\n=== Пример 6: Статистика по очередям ===")
    val stats = lookup.getQueueStats()
    stats.forEach { (queue, count) ->
        println("  Черга $queue: $count адресов")
    }
}

class TelegramBotHandler(private val lookup: AddressLookup) {

    fun handleStart(): String {
        return """
            Привіт! Я бот для перевірки графіка відключень електроенергії.

            Оберіть місто, вулицю та будинок, щоб дізнатися вашу чергу відключень.

            Команди:
            /cities - Список міст
            /search <адреса> - Пошук адреси
            /queue <місто> <вулиця> <будинок> - Дізнатися чергу
        """.trimIndent()
    }

    fun handleCities(): String {
        val cities = lookup.getCities()
        return "Доступні міста та села:\n" +
               cities.joinToString("\n") { "• $it" }
    }

    fun handleQueue(city: String, street: String, house: String): String {
        val queue = lookup.findQueue(city, street, house)

        return if (queue != null) {
            """
                📍 Адреса: $city, $street, $house
                ⚡ Черга: $queue
            """.trimIndent()
        } else {
            """
                ❌ Адресу не знайдено.
                Перевірте правильність написання міста, вулиці та будинку.
            """.trimIndent()
        }
    }

    fun handleSearch(query: String): String {
        val results = lookup.searchAddresses(query)

        return if (results.isEmpty()) {
            "Нічого не знайдено за запитом: $query"
        } else {
            val resultsList = results.take(10).joinToString("\n") { addr ->
                "• ${addr.city}, ${addr.street}, ${addr.house} → Черга ${addr.queue_full}"
            }
            "Знайдено адрес: ${results.size}\n\n$resultsList"
        }
    }
}
