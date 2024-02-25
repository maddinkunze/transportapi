package com.maddin.transportapi.components

import com.maddin.transportapi.utils.MaybeHasArrivalDeparture


// TODO: refactor/redo
interface RealtimeConnection : Connection, MaybeHasArrivalDeparture {
    val stop: Stop

    override val departurePlanned; get() = stop.departurePlanned
    override val departureActual; get() = stop.departureActual
    override val arrivalPlanned; get() = stop.arrivalPlanned
    override val arrivalActual; get() = stop.arrivalActual

    val isStopCancelled; get() = super.isCancelled || stop.isCancelled
}

open class RealtimeConnectionImpl(
    id: ConnectionIdentifier? = null,
    override var stop: Stop,
    stops: List<Stop> = listOf(stop),
    modeOfTransport: ModeOfTransport? = null,
    path: List<LocationLatLon>? = null,
    flags: Int = Connection.FLAG_NONE
) : ConnectionImpl(id=id, stops=stops, modeOfTransport=modeOfTransport, path=path, flags=flags), RealtimeConnection {
    companion object {
        fun fromExisting(c: Connection, s: POI): RealtimeConnection? {
            if (c is RealtimeConnection) { return c }
            val stop = c.stops.find { it.poi.id == s.id } ?: return null
            return RealtimeConnectionImpl(c.id, stop, c.stops, c.modeOfTransport, c.path, c.flags)
        }
    }
}