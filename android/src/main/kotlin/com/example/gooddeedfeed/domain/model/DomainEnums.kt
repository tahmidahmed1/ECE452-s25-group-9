package com.example.gooddeedfeed.domain.model

// Domain enums to avoid leaking DTO types into the domain layer

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

// Extension helpers
fun DomainSex.toDisplayString(): String = when (this) {
    DomainSex.MALE -> "Male"
    DomainSex.FEMALE -> "Female"
    DomainSex.NON_BINARY -> "Non-binary"
    DomainSex.PREFER_NOT_TO_SAY -> "Prefer not to say"
} 
