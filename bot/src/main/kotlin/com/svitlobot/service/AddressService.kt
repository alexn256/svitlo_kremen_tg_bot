package com.svitlobot.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.svitlobot.model.Address
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}


class AddressService(private val jsonFilePath: String) {

    private val addresses: List<Address>
    private val gson = Gson()

    init {
        logger.info { "Loading addresses from file: $jsonFilePath" }
        val file = File(jsonFilePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Файл не знайдено: $jsonFilePath")
        }
        val jsonString = file.readText()
        val addressListType = object : TypeToken<List<Address>>() {}.type
        addresses = gson.fromJson(jsonString, addressListType)
        logger.info { "Loaded ${addresses.size} addresses" }
    }

    fun findQueue(city: String, street: String, house: String): Address? {
        return addresses.find { it.matches(city, street, house) }
    }

    fun getCities(): List<String> {
        return addresses
            .map { it.city }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getCitiesPage(page: Int, pageSize: Int = 20): Pair<List<String>, Int> {
        val allCities = getCities()
        val totalPages = (allCities.size + pageSize - 1) / pageSize
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, allCities.size)
        val citiesOnPage = if (startIndex < allCities.size) {
            allCities.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        return Pair(citiesOnPage, totalPages)
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

    fun getStreetsPage(city: String, page: Int, pageSize: Int = 20): Pair<List<String>, Int> {
        val allStreets = getStreets(city)
        val totalPages = (allStreets.size + pageSize - 1) / pageSize
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, allStreets.size)
        val streetsOnPage = if (startIndex < allStreets.size) {
            allStreets.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        return Pair(streetsOnPage, totalPages)
    }

    fun getHousesPage(city: String, street: String, page: Int, pageSize: Int = 20): Pair<List<String>, Int> {
        val allHouses = getHouses(city, street)
        val totalPages = (allHouses.size + pageSize - 1) / pageSize
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, allHouses.size)
        val housesOnPage = if (startIndex < allHouses.size) {
            allHouses.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        return Pair(housesOnPage, totalPages)
    }

    fun searchAddresses(query: String, limit: Int = 10): List<Address> {
        val normalizedQuery = query.trim().lowercase()
        return addresses.filter { addr ->
            addr.city.lowercase().contains(normalizedQuery) ||
            addr.street.lowercase().contains(normalizedQuery) ||
            addr.house.lowercase().contains(normalizedQuery)
        }.take(limit)
    }

    fun parseAddressFromText(text: String): Triple<String, String, String>? {
        val normalized = text.trim()
        val separatorPattern = Regex("""([^,/\*\-]+)[,/\*\-]\s*([^,/\*\-]+)[,/\*\-]\s*(.+)""")
        separatorPattern.find(normalized)?.let { match ->
            val city = match.groupValues[1].trim()
            val street = match.groupValues[2].trim()
            val house = match.groupValues[3].trim()
            return Triple(city, street, house)
        }
        val parts = normalized.split(Regex("""\s+"""))
        if (parts.size >= 3) {
            val house = parts.last()
            val street = parts.dropLast(1).last()
            val city = parts.dropLast(2).joinToString(" ")

            if (house.matches(Regex("""^\d+[а-яА-Яa-zA-Z]?/?[а-яА-Яa-zA-Z0-9]*$"""))) {
                return Triple(city, street, house)
            }
        }
        return null
    }

    fun smartSearch(text: String): Address? {
        parseAddressFromText(text)?.let { (city, street, house) ->
            findQueue(city, street, house)?.let { return it }
            listOf("м.$city", "с.$city").forEach { cityVariant ->
                listOf("вул. $street", "пров. $street", street).forEach { streetVariant ->
                    findQueue(cityVariant, streetVariant, house)?.let { return it }
                }
            }
        }
        val results = searchAddresses(text, 1)
        return results.firstOrNull()
    }

    fun getQueueStats(): Map<String, Int> {
        return addresses
            .groupBy { it.queue_full }
            .mapValues { it.value.size }
            .toSortedMap()
    }

    fun getTotalAddresses(): Int = addresses.size
}
