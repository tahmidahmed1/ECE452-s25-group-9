package com.example.gooddeedfeed.domain.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val prettyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun String.toPrettyDate(): String {
    return try {
        OffsetDateTime.parse(this).format(prettyFormatter)
    } catch (e: Exception) {
        this
    }
} 