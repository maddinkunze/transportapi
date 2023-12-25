package com.maddin.transportapi

import java.time.LocalDateTime

open class RealtimeConnection(val vehicle: Vehicle, val stop: Stop) {
    fun departsIn(from: LocalDateTime) : Long {
        return stop.departsIn(from)
    }
    fun departsIn() : Long {
        return stop.departsIn()
    }
}

@Suppress("NewApi") // can be suppressed since the actual project should either support java 1.8 or be built with desugaring enabled
open class RealtimeInfo(val requestTime: LocalDateTime, val connections: List<RealtimeConnection>) {
    constructor(connections: List<RealtimeConnection>) : this(LocalDateTime.now(), connections)
    constructor(station: Station, requestTime: LocalDateTime, connections: List<Connection>) : this(requestTime, connections.mapNotNull { connection -> val stop = connection.stops.find { stop -> stop.station.id == station.id }; if (stop == null) { null } else { RealtimeConnection(connection.vehicle, stop) } })
    constructor(station: Station, connections: List<Connection>) : this(station, LocalDateTime.now(), connections)
}