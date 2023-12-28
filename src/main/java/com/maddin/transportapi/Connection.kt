package com.maddin.transportapi

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("NewApi")
open class Stop(val station: Station, val arrivalPlanned: LocalDateTime, val departurePlanned: LocalDateTime) {
    constructor(station: Station, departurePlanned: LocalDateTime) : this(station, departurePlanned, departurePlanned)

    open fun departsIn() : Long {
        return departsIn(LocalDateTime.now())
    }

    open fun departsIn(from: LocalDateTime) : Long {
        return ChronoUnit.SECONDS.between(from, departurePlanned)
    }
}

@Suppress("NewApi")
open class RealtimeStop(station: Station, arrivalPlanned: LocalDateTime, departurePlanned: LocalDateTime, var arrivalActual: LocalDateTime, var departureActual: LocalDateTime) : Stop(station, arrivalPlanned, departurePlanned) {
    constructor(station: Station, departurePlanned: LocalDateTime, departureActual: LocalDateTime) : this(station, departurePlanned, departurePlanned, departureActual, departureActual)

    override fun departsIn(from: LocalDateTime): Long {
        return ChronoUnit.SECONDS.between(from, departureActual)
    }
}

open class Connection(val stops: List<Stop>, val vehicle: Vehicle)

enum class ConnectionStatus {
    NONE,
    ACTIVE,
    CANCELLED
}

interface CancellableConnection {
    var status: ConnectionStatus
}