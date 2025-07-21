package com.example.gooddeedfeed.domain.model

data class OpportunityFilters(
    val selectedCategories: Set<OpportunityCategory> = emptySet(),
    val onlyAvailable: Boolean = false,
    val almostFull: Boolean = false,
    val minKarmaPoints: Int = 1,
    val maxKarmaPoints: Int = 200,
    val dateFilter: DateFilter = DateFilter.ALL,
    val radiusKm: Float = 50f,
    val useDistanceFilter: Boolean = false,
) 
