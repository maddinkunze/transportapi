package com.maddin.transportapi

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
    STAIRS(listOf(FOOT)),
    LIFT(listOf(FOOT), true),
    ESCALATOR(listOf(FOOT), true),
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
    TROLLEYBUS(listOf(BUS)),
    TAXI(listOf(CAR)),
    TRAM(listOf(RAIL)),
    TRAIN(listOf(RAIL)),
    TRAIN_LONG_DISTANCE(listOf(TRAIN)),
    TRAIN_REGIONAL(listOf(TRAIN)),
    TRAIN_SUBURBAN(listOf(TRAIN_REGIONAL)),
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

open class LineVariant(val id: String, val direction: Direction? = null, val defaultVehicleType: VehicleType? = null)

open class Line(val id: String, val name: String, val variants: List<LineVariant> = listOf(), val defaultVehicleType: VehicleType? = null)

open class Direction(open val name: String) {
    protected constructor() : this("")
}

open class StationDirection(val station: Station) : Direction() {
    override val name: String
        get() { return station.name }
}

open class Vehicle(val type: VehicleType?, val line: Line?, val direction: Direction?)