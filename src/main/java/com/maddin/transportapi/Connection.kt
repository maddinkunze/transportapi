package com.maddin.transportapi

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

interface UserReadableInformation {
    companion object {
        const val FLAG_NONE = 0
        const val FLAG_TYPE_INFORMATION = 1
        const val FLAG_TYPE_WARNING = 2
        const val FLAG_TYPE_CRITICAL = 4
    }

    val id: String?
    var title: String
    var information: String
    var flags: Int

    fun getMostCriticalType(): Int {
        for (type in arrayOf(FLAG_TYPE_CRITICAL, FLAG_TYPE_WARNING, FLAG_TYPE_INFORMATION)) {
            if ((type and flags) > 0) { return type }
        }
        return FLAG_NONE
    }
}

@Suppress("NewApi")
open class Stop(
    val station: Station,
    var departurePlanned: LocalDateTime,
    var departureActual: LocalDateTime = departurePlanned,
    var arrivalPlanned: LocalDateTime = departurePlanned,
    var arrivalActual: LocalDateTime = arrivalPlanned,
    var flags: Int = FLAG_NONE,
    val additionalNotes: MutableList<Note> = mutableListOf()
) {
    companion object {
        const val FLAG_NONE = 0
        const val FLAG_REALTIME =
            1   // indicates that this stops information is in realtime and not just from a timetable
        const val FLAG_CANCELLED = 2  // indicates that this stop has been cancelled
    }

    class Note(
        override val id: String? = null,
        override var title: String,
        override var information: String,
        override var flags: Int = UserReadableInformation.FLAG_NONE
    ) : UserReadableInformation

    open fun departsIn(): Long {
        return departsIn(LocalDateTime.now())
    }

    open fun departsIn(from: LocalDateTime): Long {
        return ChronoUnit.SECONDS.between(from, departureActual)
    }

    open fun arrivesIn(): Long {
        return arrivesIn(LocalDateTime.now())
    }

    open fun arrivesIn(from: LocalDateTime): Long {
        return ChronoUnit.SECONDS.between(from, arrivalActual)
    }

    open fun isCancelled(): Boolean {
        return (flags and FLAG_CANCELLED) > 0
    }

    open fun isRealtime(): Boolean {
        return (flags and FLAG_REALTIME) > 0
    }
}

open class Connection(
    val stops: List<Stop>,
    var vehicle: Vehicle,
    var flags: Int = FLAG_NONE)
{
    companion object {
        const val FLAG_NONE = 0
        const val FLAG_CANCELLED = 2
    }

    open fun isCancelled(): Boolean {
        return (flags and FLAG_CANCELLED) > 0
    }
}