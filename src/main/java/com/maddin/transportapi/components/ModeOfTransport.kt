package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.IdentifierImpl
import com.maddin.transportapi.utils.MaybeIdentifiable
import com.maddin.transportapi.utils.MaybeNamed
import com.maddin.transportapi.utils.Named
import java.io.Serializable

/*// TODO: there has to be an easier way of doing this
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
}*/

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

interface LineVariant : Serializable, MaybeIdentifiable {
    override val id: LineVariantIdentifier?
    val name: String?
    val direction: Direction?
    val defaultMOT: MOTType?
}
open class LineVariantImpl(
    override var id: LineVariantIdentifier? = null,
    override var name: String? = null,
    override var direction: Direction? = null,
    override var defaultMOT: MOTType? = null
): LineVariant

interface LineIdentifier : Identifier
open class LineIdentifierImpl(uuid: String) : IdentifierImpl(uuid), LineIdentifier
fun String.toLineId() = LineIdentifierImpl(this)


interface Line : Serializable, MaybeIdentifiable, MaybeNamed {
    override val id: LineIdentifier?
    override val name: String?
    val variants: List<LineVariant>?
    val defaultMOTType: MOTType?
}
open class LineImpl(
    override var id: LineIdentifier? = null,
    override var name: String? = null,
    override val variants: List<LineVariant>? = null,
    override var defaultMOTType: MOTType? = null
) : Line

interface Direction : Named, Serializable {
    override val name: String
}

open class DirectionImpl(override var name: String) : Direction

interface StationDirection : Station, Direction
open class StationDirectionImpl(station: Station) : StationDirection, Station by station

interface ModeOfTransportIdentifier : Identifier
open class ModeOfTransportIdentifierImpl(uuid: String) : IdentifierImpl(uuid), ModeOfTransportIdentifier

interface ModeOfTransport : Serializable {
    val id: ModeOfTransportIdentifier?
    val motType: MOTType?
}
typealias MOT = ModeOfTransport
open class MOTImpl(
    override val id: ModeOfTransportIdentifier? = null,
    override val motType: MOTType? = null
) : ModeOfTransport

interface LineMOT : ModeOfTransport {
    val line: Line?
    val variant: LineVariant?

    val symbol; get() = variant?.name ?: line?.name
    val direction; get() = variant?.direction?.name
}
open class LineMOTImpl(
    id: ModeOfTransportIdentifier? = null,
    motType: MOTType? = null,
    override val line: Line? = null,
    override val variant: LineVariant? = null
) : LineMOT, MOTImpl(id=id, motType=motType)


interface MOTTypeIdentifier : Identifier
open class MOTTypeIdentifierImpl(uuid: String) : IdentifierImpl(uuid), MOTTypeIdentifier
fun String.toMOTTId() = MOTTypeIdentifierImpl(this)

interface ModeOfTransportType : MaybeIdentifiable, Serializable {
    override val id: MOTTypeIdentifier?; get() = null
}
typealias MOTType = ModeOfTransportType

interface IndividualMOTType : MOTType

interface SharedMOTType : MOTType {
    val sharedWithAtLeast: Int?; get() = null
    val sharedWithExactly: Int?; get() = null
    val sharedWithAtMost: Int?;  get() = null
}

interface ManualMOTType : MOTType {
    val suggestedSpeed: Speed?; get() = null
}
interface MotorizedMOTType : MOTType {
    val maximumSpeed: Speed?; get() = null
}
interface ServicedMOTType : MotorizedMOTType // serviced as in you are not the driver
interface AutomatedMOTType : ServicedMOTType // automated as in there is no driver

interface Foot : ManualMOTType
interface Vehicle : MOTType {
    val seats: Int?; get() = null
    val stands: Int?; get() = null
}

interface PublicTransport : SharedMOTType, ServicedMOTType {
    interface AreaClassification
    enum class AreaClassifications : AreaClassification {
        CITY,
        REGIONAL,
        INTERREGIONAL,
        INTERCITY,
        INTERSTATE,
        INTERNATIONAL,
        INTERCONTINENTAL,
        INTERPLANETARY,
        INTERSTELLAR // who knows what the future brings i guess
    }
    val areaClassification: AreaClassification?; get() = null
}
val MOTType.isPublicTransport; get() = this is PublicTransport

interface ReplacementMOT : MOTType {
    val replaces: MOTType?
}
val MOTType.isReplacement; get() = this is ReplacementMOT

interface StreetMOT : MOTType
interface RailMOT : MOTType
interface AirMOT : MOTType {
    val maximumAirHeight: Distance?; get() = null
}
interface WaterMOT : MOTType