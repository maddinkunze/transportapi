package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.IdentifierImpl
import com.maddin.transportapi.utils.MaybeHasStartAndEnd
import com.maddin.transportapi.utils.MaybeIdentifiable

interface TripConnection: Connection, MaybeHasStartAndEnd {
    val from: Stop; get() = stops.first()
    val to: Stop; get() = stops.last()
    val stopCount; get() = stops.size
    val stopCountBetween; get() = stops.size - 2

    override val startPlanned; get() = stops.firstNotNullOfOrNull { it.estimateDeparturePlanned() }
    override val startActual; get() = stops.firstNotNullOfOrNull { it.estimateDepartureActual() }
    override val endPlanned; get() = stops.asReversed().firstNotNullOfOrNull { it.estimateArrivalPlanned() }
    override val endActual; get() = stops.asReversed().firstNotNullOfOrNull { it.estimateArrivalActual() }
}

open class TripConnectionImpl(
    id: ConnectionIdentifier? = null,
    stops: List<Stop>,
    override var from: Stop = stops.first(),
    override var to: Stop = stops.last(),
    vehicle: Vehicle? = null,
    path: List<LocationLatLon>? = null,
    flags: Int = Connection.FLAG_NONE
) : ConnectionImpl(id=id, stops=stops, vehicle=vehicle, path=path, flags=flags), TripConnection

interface TripIdentifier : Identifier
open class TripIdentifierImpl(uuid: String) : IdentifierImpl(uuid), TripIdentifier
fun String.toTripId() = TripIdentifierImpl(this)


interface Trip : MaybeIdentifiable, MaybeHasStartAndEnd {
    override val id: TripIdentifier?
    val connections: List<TripConnection>

    val from; get() = connections.first().from
    val to; get() = connections.last().to
    override val startPlanned; get() = connections.firstNotNullOfOrNull { it.estimateStartPlanned() }
    override val startActual; get() = connections.firstNotNullOfOrNull { it.estimateStartActual() }
    override val endPlanned; get() = connections.asReversed().firstNotNullOfOrNull { it.estimateEndPlanned() }
    override val endActual; get() = connections.asReversed().firstNotNullOfOrNull { it.estimateEndActual() }
}

class TripImpl(
    override val id: TripIdentifier?=null,
    override val connections: List<TripConnection>
): Trip