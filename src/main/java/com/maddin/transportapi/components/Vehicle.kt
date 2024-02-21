package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.IdentifierImpl
import com.maddin.transportapi.utils.MaybeIdentifiable
import com.maddin.transportapi.utils.MaybeNamed
import com.maddin.transportapi.utils.Named

// TODO: there has to be an easier way of doing this
interface VehicleType {
    val supertypes: List<VehicleType>

    companion object {
        const val ACCESSIBILITY_NONE = 0
        const val ACCESSIBILITY_WHEELCHAIR = 1
    }

    fun isSubtypeOf(type: VehicleType) : Boolean {
        if (this == type) { return true }
        for (supertype in supertypes) {
            if (supertype.isSubtypeOf(type)) { return true }
        }
        return false
    }

    fun isMotorized(): Boolean = false
    fun getAccessibility(): Int = ACCESSIBILITY_NONE
}

enum class VehicleTypes(
    override val supertypes: List<VehicleType> = emptyList(),
    motorized: Boolean? = null
) : VehicleType {

    FOOT(motorized=false), // high likelihood that transport on foot will not be motorized
    RAIL(motorized=true),
    STREET,
    AIR(motorized=true),
    WATER,

    WALK(listOf(FOOT)),
    WALK_RUN(listOf(WALK)),
    WALK_LONG(listOf(WALK)),
    INTERCHANGE(listOf(WALK)),
    STAIRS(listOf(FOOT)),
    LIFT(listOf(FOOT), true),
    ESCALATOR(listOf(FOOT, STAIRS), true),
    CAR(listOf(STREET), true),
    BIKE(listOf(STREET, FOOT), false),
    BIKE_ELECTRIC(listOf(BIKE), true),
    SCOOTER(listOf(STREET, FOOT), false),
    SCOOTER_ELECTRIC(listOf(SCOOTER), true),
    SEGWAY(listOf(STREET, FOOT), true),
    BUS(listOf(STREET), true),
    BUS_NIGHT(listOf(BUS)),
    BUS_SCHOOL(listOf(BUS)),
    BUS_REGIONAL(listOf(BUS)),
    BUS_LONG_DISTANCE(listOf(BUS)),
    TROLLEYBUS(listOf(BUS)),
    TAXI(listOf(CAR)),
    TAXI_SHARED(listOf(TAXI, BUS)),
    TRAM(listOf(RAIL)),
    TRAIN(listOf(RAIL)),
    TRAIN_LONG_DISTANCE(listOf(TRAIN)),
    TRAIN_REGIONAL(listOf(TRAIN)),
    TRAIN_SUBURBAN(listOf(TRAIN_REGIONAL)),
    BUS_TRAIN_REPLACEMENT(listOf(BUS, TRAIN)),
    SUBWAY(listOf(RAIL)),
    FERRY(listOf(WATER), true),
    HOVERCRAFT(listOf(WATER), true),
    PLANE(listOf(AIR)),
    HELICOPTER(listOf(AIR)),
    CABLECAR(listOf(AIR));

    private val motorized by lazy { motorized?:supertypes.firstOrNull()?.isMotorized()?:super.isMotorized() }

    override fun isMotorized(): Boolean {
        return motorized
    }
}

interface LineVariantIdentifier : Identifier {
    val variantId: String
    val lineId: LineIdentifier?
    override val uid: String; get() = variantId
    override val uuid: String; get() = concat(lineId, variantId)
}
open class LineVariantIdentifierImpl(override val variantId: String, override val lineId: LineIdentifier?=null) : LineVariantIdentifier
fun String.toLiVaId(): LineVariantIdentifierImpl {
    val split = split(Identifier.SAFE_CONCAT, limit=2)
    if (split.size != 2) { return LineVariantIdentifierImpl(this) }
    return LineVariantIdentifierImpl(split[0], split[1].toLineId())
}

interface LineVariant : MaybeIdentifiable {
    override val id: LineVariantIdentifier?
    val direction: Direction?
    val defaultVehicleType: VehicleType?
}
open class LineVariantImpl(
    override var id: LineVariantIdentifier? = null,
    override var direction: Direction? = null,
    override var defaultVehicleType: VehicleType? = null
): LineVariant

interface LineIdentifier : Identifier
open class LineIdentifierImpl(uuid: String) : IdentifierImpl(uuid), LineIdentifier
fun String.toLineId() = LineIdentifierImpl(this)


interface Line : MaybeIdentifiable, MaybeNamed {
    override val id: LineIdentifier?
    override val name: String?
    val variants: List<LineVariant>
    val defaultVehicleType: VehicleType?
}
open class LineImpl(
    override var id: LineIdentifier? = null,
    override var name: String? = null,
    override val variants: List<LineVariant> = mutableListOf(),
    override var defaultVehicleType: VehicleType? = null
) : Line

interface Direction : Named {
    override val name: String
}

open class DirectionImpl(override var name: String) : Direction

interface StationDirection : Station, Direction
open class StationDirectionImpl(station: Station) : StationDirection, Station by station

interface VehicleIdentifier : Identifier
open class VehicleIdentifierImpl(uuid: String) : IdentifierImpl(uuid), VehicleIdentifier
fun String.toVehId() = VehicleIdentifierImpl(this)


interface Vehicle : MaybeIdentifiable {
    override val id: VehicleIdentifier?
    val type: VehicleType?
    val line: Line?
    val direction: Direction?
}

open class VehicleImpl(
    override var id: VehicleIdentifier? = null,
    override var type: VehicleType? = null,
    override var line: Line? = null,
    override var direction: Direction? = null
) : Vehicle