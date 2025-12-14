package com.svitlobot.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.svitlobot.model.Address
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Сервіс для роботи з адресами
 */
class AddressService(private val jsonFilePath: String) {

    private val addresses: List<Address>
    private val gson = Gson()

    init {
        logger.info { "Завантаження адрес з файлу: $jsonFilePath" }

        val file = File(jsonFilePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Файл не знайдено: $jsonFilePath")
        }

        val jsonString = file.readText()
        val addressListType = object : TypeToken<List<Address>>() {}.type
        addresses = gson.fromJson(jsonString, addressListType)

        logger.info { "Завантажено ${addresses.size} адрес" }
    }

    /**
     * Пошук черги за адресою
     */
    fun findQueue(city: String, street: String, house: String): Address? {
        return addresses.find { it.matches(city, street, house) }
    }

    /**
     * Отримати список всіх міст
     */
    fun getCities(): List<String> {
        return addresses
            .map { it.city }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    /**
     * Отримати список вулиць у місті
     */
    fun getStreets(city: String): List<String> {
        return addresses
            .filter { it.city.equals(city, ignoreCase = true) }
            .map { it.street }
            .distinct()
            .sorted()
    }

    /**
     * Отримати список будинків на вулиці
     */
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

    /**
     * Пошук адрес за частковим збігом
     */
    fun searchAddresses(query: String, limit: Int = 10): List<Address> {
        val normalizedQuery = query.trim().lowercase()

        return addresses.filter { addr ->
            addr.city.lowercase().contains(normalizedQuery) ||
            addr.street.lowercase().contains(normalizedQuery) ||
            addr.house.lowercase().contains(normalizedQuery)
        }.take(limit)
    }

    /**
     * Парсинг адреси з текстового рядка
     * Формати:
     * - "м.Полтава, вул. Грабчака, 10"
     * - "м.Полтава вул. Грабчака 10"
     * - "Полтава Грабчака 10"
     */
    fun parseAddressFromText(text: String): Triple<String, String, String>? {
        val normalized = text.trim()

        // Спроба 1: Формат з комами "м.Полтава, вул. Грабчака, 10"
        val commaPattern = Regex("""([^,]+),\s*([^,]+),\s*(.+)""")
        commaPattern.find(normalized)?.let { match ->
            val city = match.groupValues[1].trim()
            val street = match.groupValues[2].trim()
            val house = match.groupValues[3].trim()
            return Triple(city, street, house)
        }

        // Спроба 2: Формат з пробілами, останнє - номер будинку
        val parts = normalized.split(Regex("""\s+"""))
        if (parts.size >= 3) {
            val house = parts.last()
            val street = parts.dropLast(1).last()
            val city = parts.dropLast(2).joinToString(" ")

            // Перевіряємо, чи останній елемент схожий на номер будинку
            if (house.matches(Regex("""^\d+[а-яА-Яa-zA-Z]?/?[а-яА-Яa-zA-Z0-9]*$"""))) {
                return Triple(city, street, house)
            }
        }

        return null
    }

    /**
     * Розумний пошук адреси
     */
    fun smartSearch(text: String): Address? {
        // Спочатку пробуємо розпарсити як структуровану адресу
        parseAddressFromText(text)?.let { (city, street, house) ->
            // Шукаємо точний збіг
            findQueue(city, street, house)?.let { return it }

            // Якщо не знайдено, пробуємо додати префікси
            listOf("м.$city", "с.$city").forEach { cityVariant ->
                listOf("вул. $street", "пров. $street", street).forEach { streetVariant ->
                    findQueue(cityVariant, streetVariant, house)?.let { return it }
                }
            }
        }

        // Якщо не вдалося розпарсити, шукаємо за частковим збігом
        val results = searchAddresses(text, 1)
        return results.firstOrNull()
    }

    /**
     * Отримати статистику по чергах
     */
    fun getQueueStats(): Map<String, Int> {
        return addresses
            .groupBy { it.queue_full }
            .mapValues { it.value.size }
            .toSortedMap()
    }

    /**
     * Загальна кількість адрес
     */
    fun getTotalAddresses(): Int = addresses.size
}
