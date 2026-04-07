package org.akuatech.ksupatcher.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun formatToPlainEnglish(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Unknown"
        
        return try {
            val instant = Instant.parse(isoString)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val now = LocalDateTime.now()

            val diffDays = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())

            when {
                diffDays == 0L -> "Today at ${dateTime.format(timeFormatter)}"
                diffDays == 1L -> "Yesterday at ${dateTime.format(timeFormatter)}"
                diffDays < 7L -> "${dateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} at ${dateTime.format(timeFormatter)}"
                else -> dateTime.format(dateFormatter)
            }
        } catch (e: Exception) {
            isoString
        }
    }
}
