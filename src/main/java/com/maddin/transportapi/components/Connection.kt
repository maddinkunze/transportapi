package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.IdentifierImpl
import com.maddin.transportapi.utils.MaybeHasArrivalDeparture
import com.maddin.transportapi.utils.MaybeIdentifiable
import com.maddin.transportapi.utils.Translatable
import java.time.LocalDateTime

interface UserReadableInformation : MaybeIdentifiable {
    companion object {
        const val FLAG_NONE = 0
        const val FLAG_TYPE_INFORMATION = 1
        const val FLAG_TYPE_WARNING = 2
        const val FLAG_TYPE_CRITICAL = 4
    }

    override val id: Identifier?
    val title: Translatable
    val information: Translatable
    val flags: Int

    fun getMostCriticalType(): Int {
        for (type in arrayOf(FLAG_TYPE_CRITICAL, FLAG_TYPE_WARNING, FLAG_TYPE_INFORMATION)) {
            if ((type and flags) > 0) { return type }
        }
        return FLAG_NONE
    }
}

@Suppress("NewApi")
interface Stop : MaybeHasArrivalDeparture {
    val poi: POI
    val station: Station?; get() = poi as? Station
    val platformPlanned: Platform?
    val platformActual: Platform?
    val flags: Int
    val additionalNotes: List<Note>

    companion object {
        const val FLAG_NONE = 0
        const val FLAG_CANCELLED = 1  // indicates that this stop has been cancelled
        const val FLAG_REALTIME_ARRIVAL = 2
        const val FLAG_REALTIME_DEPARTURE = 4
        const val FLAG_REALTIME = FLAG_REALTIME_ARRIVAL or FLAG_REALTIME_DEPARTURE  // indicates that this stops information is in realtime and not just from a timetable
    }

    class Note(
        override val id: Identifier? = null,
        override var title: Translatable,
        override var information: Translatable,
        override var flags: Int = UserReadableInformation.FLAG_NONE
    ) : UserReadableInformation

    val platform; get() = platformActualOrPlanned
    val platformPlannedOrActual; get() = platformPlanned ?: platformActual
    val platformActualOrPlanned; get() = platformActual ?: platformPlanned
    val platformActualIfNotPlanned; get() = if (platformPlanned != platformActual || platformPlanned?.id != platformActual?.id) { platformActual } else { null }

    val isCancelled: Boolean; get() = (flags and FLAG_CANCELLED) > 0
    val isRealtime: Boolean; get() = (flags and FLAG_REALTIME) > 0
    val isArrivalRealtime: Boolean; get() = (flags and FLAG_REALTIME_ARRIVAL) > 0
    val isDepartureRealtime: Boolean; get() = (flags and FLAG_REALTIME_DEPARTURE) > 0
}

open class StopImpl(
    override var poi: POI,
    override var platformPlanned: Platform? = null,
    override var platformActual: Platform? = null,
    override var departurePlanned: LocalDateTime? = null,
    override var departureActual: LocalDateTime? = null,
    override var arrivalPlanned: LocalDateTime? = null,
    override var arrivalActual: LocalDateTime? = null,
    override var flags: Int = Stop.FLAG_NONE,
    override val additionalNotes: MutableList<Stop.Note> = mutableListOf()
) : Stop

interface ConnectionIdentifier : Identifier
class ConnectionIdentifierImpl(uuid: String) : IdentifierImpl(uuid), ConnectionIdentifier
fun String.toConId() = ConnectionIdentifierImpl(this)

interface Connection : MaybeIdentifiable {
    override val id: ConnectionIdentifier?
    val stops: List<Stop>
    val vehicle: Vehicle?
    val path: List<LocationLatLon>?
    val flags: Int

    companion object {
        const val FLAG_NONE = 0
        const val FLAG_CANCELLED = 1
    }

    fun isCancelled(): Boolean {
        return (flags and FLAG_CANCELLED) > 0
    }
}

open class ConnectionImpl(
    override val id: ConnectionIdentifier? = null,
    override var stops: List<Stop>,
    override var vehicle: Vehicle? = null,
    override var path: List<LocationLatLon>? = null,
    override var flags: Int = Connection.FLAG_NONE
) : Connection