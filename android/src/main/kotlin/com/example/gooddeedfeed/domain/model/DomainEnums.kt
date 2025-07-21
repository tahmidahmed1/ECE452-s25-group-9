package com.example.gooddeedfeed.domain.model


enum class DomainUserType {
    VOLUNTEER,
    ORGANIZER,
}

enum class DomainSex {
    MALE,
    FEMALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY,
}

fun DomainSex.toDisplayString(): String = when (this) {
    DomainSex.MALE -> "Male"
    DomainSex.FEMALE -> "Female"
    DomainSex.NON_BINARY -> "Non-binary"
    DomainSex.PREFER_NOT_TO_SAY -> "Prefer not to say"
} 
