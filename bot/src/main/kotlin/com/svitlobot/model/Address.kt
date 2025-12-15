package com.svitlobot.model

data class Address(
    val branch: String,
    val queue: Int,
    val subqueue: Int,
    val queue_full: String,
    val city: String,
    val street: String,
    val house: String
) {
    fun toDisplayString(): String {
        return buildString {
            if (city.isNotBlank()) append("$city, ")
            append("$street, буд. $house")
        }
    }

    fun matches(searchCity: String, searchStreet: String, searchHouse: String): Boolean {
        return city.equals(searchCity, ignoreCase = true) &&
               street.equals(searchStreet, ignoreCase = true) &&
               house.equals(searchHouse, ignoreCase = true)
    }
}

enum class UserState {
    IDLE,
    WAITING_FOR_ADDRESS,
    CHOOSING_CITY,
    CHOOSING_STREET,
    CHOOSING_HOUSE
}

data class UserData(
    val userId: Long,
    var state: UserState = UserState.IDLE,
    var selectedCity: String? = null,
    var selectedStreet: String? = null
)
