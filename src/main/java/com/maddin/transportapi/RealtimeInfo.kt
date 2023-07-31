package com.maddin.transportapi

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("NewApi") // can be suppressed since the actual project should either support java 1.8 or be built with desugaring enabled
data class RealtimeInfo(val station: Station, val time: LocalDateTime, val connections: List<RealtimeConnection>) {
    constructor(station: Station, connections: List<RealtimeConnection>)
            : this(station, LocalDateTime.now(), connections)
}

@Suppress("NewApi") // can be suppressed since the actual project should either support java 1.8 or be built with desugaring enabled
data class RealtimeConnection(val station: Station, val timeArrive: LocalDateTime, val timeDepart: LocalDateTime, val vehicle: Vehicle) {
    constructor(station: Station, timeArriveDepart: LocalDateTime, vehicle: Vehicle)
            : this(station, timeArriveDepart, timeArriveDepart, vehicle)
    constructor(station: Station, arrivesIn: Long, departsIn: Long, vehicle: Vehicle)
            : this(station, LocalDateTime.now().plusSeconds(arrivesIn), LocalDateTime.now().plusSeconds(departsIn), vehicle)
    constructor(station: Station, arrivesDepartsIn: Long, vehicle: Vehicle)
            : this(station, arrivesDepartsIn, arrivesDepartsIn, vehicle)

    fun arrivesIn(now: LocalDateTime) : Long {
        return ChronoUnit.SECONDS.between(now, timeArrive)
    }
    fun arrivesIn() : Long {
        return arrivesIn(LocalDateTime.now())
    }

    fun departsIn(now: LocalDateTime) : Long {
        return ChronoUnit.SECONDS.between(now, timeDepart)
    }
    fun departsIn() : Long {
        return departsIn(LocalDateTime.now())
    }
}
