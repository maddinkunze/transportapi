package com.maddin.transportapi.impl.germany

import com.maddin.transportapi.components.MOTTypeIdentifier
import com.maddin.transportapi.components.PublicTransport
import com.maddin.transportapi.components.Speed
import com.maddin.transportapi.components.Train

interface SBahn : Train { override val areaClassification; get() = PublicTransport.AreaClassifications.REGIONAL }
open class SBahnImpl(
    override val id: MOTTypeIdentifier? = null,
    override val seats: Int? = null,
    override val stands: Int? = null,
    override val maximumSpeed: Speed? = null
) : SBahn

interface RB : Train { override val areaClassification; get() = PublicTransport.AreaClassifications.REGIONAL }
open class RBImpl(
    override val id: MOTTypeIdentifier? = null,
    override val seats: Int? = null,
    override val stands: Int? = null,
    override val maximumSpeed: Speed? = null
) : RB

interface RE : Train { override val areaClassification; get() = PublicTransport.AreaClassifications.REGIONAL }
open class REImpl(
    override val id: MOTTypeIdentifier? = null,
    override val seats: Int? = null,
    override val stands: Int? = null,
    override val maximumSpeed: Speed? = null
) : RE

interface IRE : Train { override val areaClassification; get() = PublicTransport.AreaClassifications.INTERREGIONAL }
open class IREImpl(
    override val id: MOTTypeIdentifier? = null,
    override val seats: Int? = null,
    override val stands: Int? = null,
    override val maximumSpeed: Speed? = null
) : IRE

interface IC : Train { override val areaClassification; get() = PublicTransport.AreaClassifications.INTERCITY }
open class ICImpl(
    override val id: MOTTypeIdentifier? = null,
    override val seats: Int? = null,
    override val stands: Int? = null,
    override val maximumSpeed: Speed? = null
) : IC

interface ICE : Train { override val areaClassification; get() = PublicTransport.AreaClassifications.INTERCITY }
open class ICEImpl(
    override val id: MOTTypeIdentifier? = null,
    override val seats: Int? = null,
    override val stands: Int? = null,
    override val maximumSpeed: Speed? = null
) : ICE

object VehicleTypes {
    val SBAHN = object : SBahn {}
    val RB = object : RB {}
    val RE = object : RE {}
    val IRE = object : IRE {}
    val IC = object : IC {}
    val ICE = object : ICE {}
}