package com.example.gooddeedfeed.data.mapper

import com.example.gooddeedfeed.data.remote.dto.LeaderboardEntryDto
import com.example.gooddeedfeed.data.remote.dto.LeaderboardResponseDto
import com.example.gooddeedfeed.domain.model.DomainLeaderboardEntry
import com.example.gooddeedfeed.domain.model.DomainLeaderboardResponse
import com.example.gooddeedfeed.domain.model.DomainUserType

fun LeaderboardEntryDto.toDomain(): DomainLeaderboardEntry {
    return DomainLeaderboardEntry(
        id = id,
        username = username,
        fullName = full_name,
        karmaPoints = karma_points,
        profilePictureUrl = profile_picture_url?.toEmulatorAccessibleUrl(),
        userType = user_type?.let { 
            try {
                DomainUserType.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        },
        rank = rank
    )
}

fun LeaderboardResponseDto.toDomain(): DomainLeaderboardResponse {
    return DomainLeaderboardResponse(
        entries = entries.map { it.toDomain() },
        page = page,
        pageSize = page_size,
        totalPages = total_pages,
        totalEntries = total_entries,
        hasNext = has_next,
        hasPrevious = has_previous
    )
}

private fun String.toEmulatorAccessibleUrl(): String {
    // More comprehensive emulator detection
    val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
        android.os.Build.FINGERPRINT.contains("unknown") ||
        android.os.Build.MODEL.contains("google_sdk") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK built for x86") ||
        android.os.Build.MODEL.contains("Android SDK built for arm64") ||
        android.os.Build.MANUFACTURER.contains("Genymotion") ||
        android.os.Build.BRAND.startsWith("generic") ||
        android.os.Build.DEVICE.startsWith("generic") ||
        "google_sdk" == android.os.Build.PRODUCT ||
        "sdk_gphone" in android.os.Build.PRODUCT ||
        "emulator" in android.os.Build.HARDWARE.lowercase()

    val host = if (isEmulator) "10.0.2.2" else "localhost"

    var mappedUrl = this.replace("http://localhost", "http://$host")
        .replace("http://127.0.0.1", "http://$host")
        .replace("http://minio:9000", "http://$host:9001")
        .replace("http://minio:9001", "http://$host:9001")
    
    // Force emulator URL if we detect we're still using localhost and it doesn't work
    if (mappedUrl.contains("localhost") && !isEmulator) {
        mappedUrl = mappedUrl.replace("http://localhost", "http://10.0.2.2")
            .replace("http://127.0.0.1", "http://10.0.2.2")
    }
    
    return mappedUrl
} 