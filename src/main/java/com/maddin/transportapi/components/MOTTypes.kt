package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifier
import org.json.JSONWriter

// Just some more interfaces describing the potential types of vehicles

interface Walk : Foot, IndividualMOTType
val MOTType.isWalk; get() = this is Walk
val MOTType.isRunning; get() = (this as? Walk)?.suggestedSpeed?.isRunning ?: false
interface Hike : Walk
val MOTType.isHiking; get() = this is Hike
interface Interchange : Walk
interface Bike : Vehicle, ManualMOTType, IndividualMOTType, StreetMOT {
    override val seats: Int?; get() = 1
    override val stands: Int?; get() = 0
}
interface EBike : Bike, MotorizedMOTType
interface Scooter : Vehicle, ManualMOTType, IndividualMOTType, StreetMOT {
    override val seats: Int?; get() = 0
    override val stands: Int?; get() = 1
}
interface EScooter : Scooter, MotorizedMOTType
interface Car : Vehicle, MotorizedMOTType, StreetMOT {
    override val stands: Int?; get() = 0
}
interface PrivateCar : Car, IndividualMOTType, ManualMOTType
interface Taxi : Car, IndividualMOTType, ServicedMOTType
interface SharedTaxi : Car, SharedMOTType, ServicedMOTType
interface Bus : Vehicle, PublicTransport, StreetMOT {
    companion object {
        const val BUS_MODE_NONE = 0
        const val BUS_MODE_NIGHT = 1
        const val BUS_MODE_SCHOOL = 2
    }
    val busMode: Int; get() = BUS_MODE_NONE
}
val Bus.isNightBus; get() = (busMode and Bus.BUS_MODE_NIGHT) > 0
val Bus.isSchoolBus; get() = (busMode and Bus.BUS_MODE_SCHOOL) > 0
interface TrolleyBus : Bus
interface Tram : Vehicle, PublicTransport, RailMOT
interface Train : Vehicle, PublicTransport, RailMOT
interface Subway : Vehicle, PublicTransport, RailMOT
interface Boat : Vehicle, WaterMOT
interface Ferry : Boat, SharedMOTType, ServicedMOTType, WaterMOT
interface PublicFerry : Ferry, PublicTransport
interface Hovercraft : Vehicle, WaterMOT
interface PlaneIdentifier : Identifier {
    override val uuid: String; get() = "$prefix-$suffix"
    val prefix: String
    val suffix: String
}
interface Plane : Vehicle, ServicedMOTType, AirMOT {
    val planeId: PlaneIdentifier?; get() = null
}
interface PrivatePlane : Plane, IndividualMOTType
interface CommercialPlane : Plane, PublicTransport
interface Helicopter : Vehicle, ServicedMOTType, AirMOT
interface CableCar : Vehicle, ServicedMOTType, SharedMOTType, AirMOT


// Actual implementations of these potential modes of transport

open class WalkImpl(
    override var suggestedSpeed: Speed? = null,
) : Walk

open class HikeImpl(
    override var suggestedSpeed: Speed? = null
) : Hike

open class InterchangeImpl(
    override var suggestedSpeed: Speed? = null
) : Interchange

open class PrivateCarImpl(
    override var suggestedSpeed: Speed? = null,
    override var maximumSpeed: Speed? = null,
    override var seats: Int? = null
) : PrivateCar

open class TaxiImpl(
    override var seats: Int? = null
) : Taxi

open class BikeImpl(
    override var suggestedSpeed: Speed? = null,
) : Bike {
    override var seats: Int = 1
    override var stands: Int = 0
}

open class EBikeImpl(
    override var suggestedSpeed: Speed? = null,
    override var maximumSpeed: Speed? = null
) : EBike, MotorizedMOTType

open class ScooterImpl(
    override var suggestedSpeed: Speed? = null
) : Scooter {
    override var seats: Int = 0
    override var stands: Int = 1
}

open class EScooterImpl(
    override var suggestedSpeed: Speed? = null,
    override var maximumSpeed: Speed? = null
) : EScooter

open class SharedTaxiImpl(
    override var id: MOTTypeIdentifier? = null,
    override var seats: Int? = null
) : SharedTaxi

open class BusImpl(
    override var id: MOTTypeIdentifier?=null,
    override var seats: Int? = null,
    override var stands: Int? = null,
    override var busMode: Int = Bus.BUS_MODE_NONE,
    override var areaClassification: PublicTransport.AreaClassification? = null
) : Bus
open class TrolleyBusImpl(
    override var id: MOTTypeIdentifier?=null,
    override var seats: Int? = null,
    override var stands: Int? = null,
    override var busMode: Int = Bus.BUS_MODE_NONE,
    override var areaClassification: PublicTransport.AreaClassification? = null
) : TrolleyBus
open class TramImpl(
    override var id: MOTTypeIdentifier?=null,
    override var seats: Int? = null,
    override var stands: Int? = null,
    override var areaClassification: PublicTransport.AreaClassification? = null
) : Tram
open class TrainImpl(
    override var id: MOTTypeIdentifier?=null,
    override var seats: Int? = null,
    override var stands: Int? = null,
    override var areaClassification: PublicTransport.AreaClassification? = null
) : Train
open class SubwayImpl(
    override var id: MOTTypeIdentifier?=null,
    override var seats: Int? = null,
    override var stands: Int? = null,
    override var areaClassification: PublicTransport.AreaClassification? = null
) : Subway
open class FerryImpl(
    override var id: MOTTypeIdentifier?=null,
    override var seats: Int? = null,
    override var stands: Int? = null,
    override var areaClassification: PublicTransport.AreaClassification? = null
) : PublicFerry

@Suppress("SpellCheckingInspection", "ClassName")
object MOTTypes {
    object WALK : Walk
    object HIKE : Hike
    object INTERCHANGE : Interchange
    object BIKE : Bike
    object EBIKE : EBike
    object SCOOTER : Scooter
    object ESCOOTER : EScooter
    object CAR_PRIVATE : PrivateCar { override val seats: Int = 5 }
    object TAXI : Taxi
    object TAXI_SHARED : SharedTaxi
    object BUS : Bus
    object BUS_NIGHT : Bus { override val busMode = Bus.BUS_MODE_NIGHT }
    object BUS_SCHOOL : Bus { override val busMode = Bus.BUS_MODE_SCHOOL }
    object BUS_REGIONAL : Bus { override val areaClassification = PublicTransport.AreaClassifications.REGIONAL }
    object TROLLEYBUS : TrolleyBus
    object TRAM : Tram
    object TRAIN : Train
    object SUBWAY : Subway
    object FERRY : PublicFerry
    object PLANE : CommercialPlane
    object HELICOPTER : Helicopter
    object CABLECAR : CableCar

    object BUS_REPLACES_TRAM : Bus, ReplacementMOT { override val replaces: MOTType = TRAM }
    object BUS_REPLACES_TRAIN : Bus, ReplacementMOT { override val replaces: MOTType = TRAIN }
}