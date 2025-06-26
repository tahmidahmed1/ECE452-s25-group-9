package com.example.gooddeedfeed.domain.model

/**
 * Domain-layer enumerations. These are intentionally **decoupled** from the DTO layer so that
 * changes in the remote API do not ripple into the domain / presentation layers.
 * Mapper extension functions convert between these enums and the DTO equivalents.
 */

enum class DomainUserType {
    VOLUNTEER,
    ORGANIZER,
    INSTITUTION,
}

enum class DomainSex {
    MALE,
    FEMALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY,
}

enum class DomainInstitutionName {
    INSTITUTION_1,
    INSTITUTION_2,
    INSTITUTION_3,
} 
