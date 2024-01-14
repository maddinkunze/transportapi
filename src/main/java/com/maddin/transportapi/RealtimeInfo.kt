package com.maddin.transportapi

import java.time.LocalDateTime

open class RealtimeConnection : Connection {
    val stop: Stop
    constructor(id: String, stop: Stop, stops: List<Stop> = listOf(stop), vehicle: Vehicle) : super(id, listOf(stop), vehicle) {
        this.stop = stop
    }
    constructor(id: String, stop: Stop, stops: List<Stop> = listOf(stop), vehicle: Vehicle, flags: Int) : super(id, listOf(stop), vehicle, flags) {
        this.stop = stop
    }

    companion object {
        fun fromExisting(c: Connection, s: Station): RealtimeConnection? {
            if (c is RealtimeConnection) { return c }
            val stop = c.stops.find { it.station.id == s.id } ?: return null
            return RealtimeConnection(c.id, stop, c.stops, c.vehicle, c.flags)
        }
    }

    fun departsIn(from: LocalDateTime) : Long {
        return stop.departsIn(from)
    }
    fun departsIn() : Long {
        return stop.departsIn()
    }

    fun isStopCancelled(): Boolean {
        return super.isCancelled() || stop.isCancelled()
    }
}

@Suppress("NewApi") // can be suppressed since the actual project should either support java 1.8 or be built with desugaring enabled
open class RealtimeInfo(val requestTime: LocalDateTime, val connections: List<RealtimeConnection>) {
    constructor(connections: List<RealtimeConnection>) : this(LocalDateTime.now(), connections)
    constructor(station: Station, requestTime: LocalDateTime, connections: List<Connection>) : this(requestTime, connections.mapNotNull { RealtimeConnection.fromExisting(it, station) })
    constructor(station: Station, connections: List<Connection>) : this(station, LocalDateTime.now(), connections)
}