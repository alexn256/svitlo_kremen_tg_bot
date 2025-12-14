package com.svitlobot.model

/**
 * Модель адреса из парсера
 */
data class Address(
    val branch: String,        // Філія (Полтавська)
    val queue: Int,            // Номер черги (1-6)
    val subqueue: Int,         // Номер підчерги (1-2)
    val queue_full: String,    // Повний номер черги (1.1, 3.2)
    val city: String,          // Місто/село (м.Полтава, с.Омельник)
    val street: String,        // Вулиця (вул. Грабчака)
    val house: String          // Номер будинку (10, 12а)
) {
    /**
     * Форматований рядок адреси для відображення
     */
    fun toDisplayString(): String {
        return buildString {
            if (city.isNotBlank()) append("$city, ")
            append("$street, буд. $house")
        }
    }

    /**
     * Перевірка чи збігається адреса з пошуковим запитом
     */
    fun matches(searchCity: String, searchStreet: String, searchHouse: String): Boolean {
        return city.equals(searchCity, ignoreCase = true) &&
               street.equals(searchStreet, ignoreCase = true) &&
               house.equals(searchHouse, ignoreCase = true)
    }
}

/**
 * Стан користувача в боті
 */
enum class UserState {
    IDLE,              // Початковий стан
    WAITING_FOR_ADDRESS // Очікування введення адреси
}

/**
 * Дані користувача
 */
data class UserData(
    val userId: Long,
    var state: UserState = UserState.IDLE
)
