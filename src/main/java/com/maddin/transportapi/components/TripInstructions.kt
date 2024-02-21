package com.maddin.transportapi.components

import com.maddin.transportapi.utils.MutableTranslations
import com.maddin.transportapi.utils.SameName
import com.maddin.transportapi.utils.Translatable
import com.maddin.transportapi.utils.Translations
import com.maddin.transportapi.utils.toTranslatable
import java.util.Locale
import kotlin.math.absoluteValue

interface TripInstruction : Translatable {
    val path: List<Location>?; get() = null
}

interface WalkInstruction : TripInstruction

@Suppress("MemberVisibilityCanBePrivate")
abstract class WalkInstructionImpl : WalkInstruction {
    companion object {
        val defaultTranslations = mutableMapOf<String, String>()

        val DIRECTION_CLOCKWISE by SameName
        val DIRECTION_COUNTERCLOCKWISE by SameName

        val DIRECTION_SLIGHT_LEFT by SameName
        val DIRECTION_LEFT by SameName
        val DIRECTION_SHARP_LEFT by SameName
        val DIRECTION_SLIGHT_RIGHT by SameName
        val DIRECTION_RIGHT by SameName
        val DIRECTION_SHARP_RIGHT by SameName

        init {
            makeTranslations(defaultTranslations, "clockwise", "counterclockwise", "left", "right", "to the", "sharply", "slightly")
        }

        fun makeTranslations(translations: MutableTranslations, clockwise: String, counterclockwise: String, left: String, right: String, toThe: String, sharp: String, slight: String) {
            translations[DIRECTION_CLOCKWISE] = clockwise
            translations[DIRECTION_COUNTERCLOCKWISE] = counterclockwise

            translations[DIRECTION_LEFT] = "$toThe $left"
            translations[DIRECTION_SHARP_LEFT] = "$sharp $toThe $left"
            translations[DIRECTION_SLIGHT_LEFT] = "$slight $toThe $left"
            translations[DIRECTION_RIGHT] = "$toThe $right"
            translations[DIRECTION_SHARP_RIGHT] = "$sharp $toThe $right"
            translations[DIRECTION_SLIGHT_RIGHT] = "$slight $toThe $right"
        }

        fun verifyTranslations(translations: Translations): List<String> {
            return defaultTranslations.keys.filter { !translations.containsKey(it) }
        }

        @Suppress("UNUSED_PARAMETER")
        fun getTranslation(key: String, locale: Locale?, translations: Translations?): String {
            return translations?.get(key) ?: defaultTranslations[key] ?: "{$key}"
        }

        fun createTranslateFunction(key: String) = object : Translatable {
            override fun translate(locale: Locale?, translations: Translations?): String = getTranslation(key, locale, translations)
        }
        fun createTranslateUnitDistance(distanceInMeters: Int) = "${distanceInMeters}m".toTranslatable() // TODO
        fun createTranslateUnitDuration(durationInSeconds: Long) = "${durationInSeconds}s".toTranslatable() // TODO
        fun createTranslateUnitAngle(angleInDegrees: Int, includeDirection: Boolean = true): Translatable {
            // "clamp" angle between -180 and 180 degrees
            val angleMod = (angleInDegrees % 360).let { if (it > 180) { it-360 } else { it } }

            // determine direction of angle
            val dirKey = when {
                angleMod > 0 -> DIRECTION_CLOCKWISE
                angleMod < 0 -> DIRECTION_COUNTERCLOCKWISE
                else -> null
            }

            // since we now have the angle direction, we can ignore the sign
            val angle = angleMod.absoluteValue

            // return only the angle representation if wanted or if the direction is unknown
            if (!includeDirection || dirKey.isNullOrEmpty()) { return "${angle}°".toTranslatable() }

            // return the angle and its direction if known and wanted
            return object: Translatable {
                override fun translate(locale: Locale?, translations: Translations?): String {
                    val dir = getTranslation(dirKey, locale, translations)
                    return "${angle}° $dir"
                }
            }
        }
    }

    abstract val template: String
    open val values: Array<Translatable> = emptyArray()
    override fun translate(locale: Locale?, translations: Translations?): String {
        return getTranslation(template, locale, translations).format(locale, *values)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class WalkInstructionLeave(
    val what: Translatable,
    val where: Translatable? = null,
    val onto: Translatable? = null,
    override val path: List<Location>? = null
) : WalkInstructionImpl() {
    constructor(what: Translatable, where: Translatable?=null, onto: Translatable? = null, location: Location) : this(what, where, onto, listOf(location))

    companion object {
        val ACTION_LEAVE by SameName
        val ACTION_LEAVE_EXIT by SameName
        val ACTION_LEAVE_ONTO by SameName
        val ACTION_LEAVE_EXIT_ONTO by SameName

        init {
            defaultTranslations[ACTION_LEAVE] = "Leave %s"
            defaultTranslations[ACTION_LEAVE_EXIT] = "Leave %s (%s)"
            defaultTranslations[ACTION_LEAVE_ONTO] = "Leave %s onto \"%s\""
            defaultTranslations[ACTION_LEAVE_EXIT_ONTO] = "Leave %s (%s) onto \"%s\""
        }
    }

    private val whereValid = (where != null)
    private val ontoValid = (onto != null)

    private val mode = when {
        whereValid && ontoValid -> ACTION_LEAVE_EXIT_ONTO
        whereValid -> ACTION_LEAVE_EXIT
        ontoValid -> ACTION_LEAVE_ONTO
        else -> ACTION_LEAVE
    }

    override val template: String = mode
    override val values: Array<Translatable> = when (mode) {
        ACTION_LEAVE -> arrayOf(what)
        ACTION_LEAVE_EXIT -> arrayOf(what, where!!)
        ACTION_LEAVE_ONTO -> arrayOf(what, onto!!)
        ACTION_LEAVE_EXIT_ONTO -> arrayOf(what, where!!, onto!!)
        else -> arrayOf()
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class WalkInstructionEnter (
    val what: Translatable,
    val where: Translatable? = null,
    override val path: List<Location>? = null
) : WalkInstructionImpl() {
    constructor(what: Translatable, where: Translatable?=null, location: Location) : this(what, where, listOf(location))

    companion object {
        val ACTION_ENTER by SameName
        val ACTION_ENTER_ENTRY by SameName

        init {
            defaultTranslations[ACTION_ENTER] = "Enter %s"
            defaultTranslations[ACTION_ENTER_ENTRY] = "Enter %s (%s)"
        }
    }

    private val mode = when {
        (where != null) -> ACTION_ENTER_ENTRY
        else -> ACTION_ENTER
    }

    override val template: String = mode
    override val values: Array<Translatable> = when (mode) {
        ACTION_ENTER -> arrayOf(what)
        ACTION_ENTER_ENTRY -> arrayOf(what, where!!)
        else -> arrayOf()
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class WalkInstructionGo(
    val distanceInMeters: Int? = null,
    val durationInSeconds: Long? = null,
    val where: Translatable? = null,
    val target: Translatable? = null,
    override val path: List<Location>? = null
) : WalkInstructionImpl() {

    companion object {
        val ACTION_WALK_DISTANCE by SameName
        val ACTION_WALK_DURATION by SameName
        val ACTION_WALK_ON by SameName
        val ACTION_WALK_TO by SameName
        val ACTION_WALK_ON_TO by SameName
        val ACTION_WALK_DISTANCE_ON by SameName
        val ACTION_WALK_DURATION_ON by SameName
        val ACTION_WALK_DISTANCE_TO by SameName
        val ACTION_WALK_DURATION_TO by SameName
        val ACTION_WALK_DISTANCE_DURATION by SameName
        val ACTION_WALK_DISTANCE_ON_TO by SameName
        val ACTION_WALK_DURATION_ON_TO by SameName
        val ACTION_WALK_DISTANCE_DURATION_ON by SameName
        val ACTION_WALK_DISTANCE_DURATION_TO by SameName
        val ACTION_WALK_DISTANCE_DURATION_ON_TO by SameName

        init {
            makeTranslations(defaultTranslations, "Walk", "for", "until you reach", "on")
        }

        fun makeTranslations(translations: MutableTranslations, walk: String, forS: String, untilYouReach: String, on: String) {
            translations[ACTION_WALK_DISTANCE] = "$walk %s"
            translations[ACTION_WALK_DURATION] = "$walk $forS %s"
            translations[ACTION_WALK_DISTANCE_DURATION] = "$walk %s (%s)"
            translations[ACTION_WALK_TO] = "$walk $untilYouReach \"%s\""
            translations[ACTION_WALK_DISTANCE_TO] = "$walk %s $untilYouReach \"%s\""
            translations[ACTION_WALK_DURATION_TO] = "$walk $forS %s $untilYouReach \"%s\""
            translations[ACTION_WALK_DISTANCE_DURATION_TO] = "$walk %s (%s) $untilYouReach \"%s\""
            translations[ACTION_WALK_ON] = "$walk $on \"%s\""
            translations[ACTION_WALK_DISTANCE_ON] = "$walk %s $on \"%s\""
            translations[ACTION_WALK_DURATION_ON] = "$walk $forS %s $on \"%s\""
            translations[ACTION_WALK_DISTANCE_DURATION_ON] = "$walk %s (%s) $on \"%s\""
            translations[ACTION_WALK_ON_TO] = "$walk $on \"%s\" $untilYouReach \"%s\""
            translations[ACTION_WALK_DISTANCE_ON_TO] = "$walk %s $on \"%s\" $untilYouReach \"%s\""
            translations[ACTION_WALK_DURATION_ON_TO] = "$walk $forS %s $on \"%s\" $untilYouReach \"%s\""
            translations[ACTION_WALK_DISTANCE_DURATION_ON_TO] = "$walk %s (%s) $on \"%s\" $untilYouReach \"%s\""
        }
    }

    private val distanceValid = distanceInMeters != null
    private val durationValid = durationInSeconds != null
    private val whereValid = where != null
    private val targetValid = target != null

    private val mode = when {
        (distanceValid && durationValid && whereValid && targetValid) -> ACTION_WALK_DISTANCE_DURATION_ON_TO
        (distanceValid && durationValid && whereValid) -> ACTION_WALK_DISTANCE_DURATION_ON
        (distanceValid && targetValid && whereValid) -> ACTION_WALK_DISTANCE_ON_TO
        (durationValid && targetValid && whereValid) -> ACTION_WALK_DURATION_ON_TO
        (distanceValid && whereValid) -> ACTION_WALK_DISTANCE_ON
        (durationValid && whereValid) -> ACTION_WALK_DURATION_ON
        (whereValid && targetValid) -> ACTION_WALK_ON_TO
        (whereValid) -> ACTION_WALK_TO
        (distanceValid && durationValid && targetValid) -> ACTION_WALK_DISTANCE_DURATION_TO
        (distanceValid && durationValid) -> ACTION_WALK_DISTANCE_DURATION
        (distanceValid && targetValid) -> ACTION_WALK_DISTANCE_TO
        (durationValid && targetValid) -> ACTION_WALK_DURATION_TO
        (distanceValid) -> ACTION_WALK_DISTANCE
        (durationValid) -> ACTION_WALK_DURATION
        (targetValid) -> ACTION_WALK_TO
        else -> null
    }

    override val template: String = mode ?: "Invalid walk instruction"
    override val values: Array<Translatable> = when (mode) {
        ACTION_WALK_TO -> arrayOf(target!!)
        ACTION_WALK_DURATION -> arrayOf(createTranslateUnitDuration(durationInSeconds?:0L))
        ACTION_WALK_DISTANCE -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0))
        ACTION_WALK_DURATION_TO -> arrayOf(createTranslateUnitDuration(durationInSeconds?:0L), target!!)
        ACTION_WALK_DISTANCE_TO -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), target!!)
        ACTION_WALK_DISTANCE_DURATION -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), createTranslateUnitDuration(durationInSeconds?:0L))
        ACTION_WALK_DISTANCE_DURATION_TO -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), createTranslateUnitDuration(durationInSeconds?:0L), target!!)
        ACTION_WALK_ON -> arrayOf(where!!)
        ACTION_WALK_ON_TO -> arrayOf(where!!, target!!)
        ACTION_WALK_DURATION_ON -> arrayOf(createTranslateUnitDuration(durationInSeconds?:0L), where!!)
        ACTION_WALK_DISTANCE_ON -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), where!!)
        ACTION_WALK_DURATION_ON_TO -> arrayOf(createTranslateUnitDuration(durationInSeconds?:0L), where!!, target!!)
        ACTION_WALK_DISTANCE_ON_TO -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), where!!, target!!)
        ACTION_WALK_DISTANCE_DURATION_ON -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), createTranslateUnitDuration(durationInSeconds?:0L), where!!)
        ACTION_WALK_DISTANCE_DURATION_ON_TO -> arrayOf(createTranslateUnitDistance(distanceInMeters?:0), createTranslateUnitDuration(durationInSeconds?:0L), where!!, target!!)
        else -> arrayOf()
    }
}

// TODO
/*open class WalkInstructionStairs : WalkInstructionImpl() {
    override val template: String = when {
        false -> ""
        else -> ""
    }
}
open class WalkInstructionEscalator : WalkInstructionImpl() {
    override val template: String = when {
        false -> ""
        else -> ""
    }
}
open class WalkInstructionElevator : WalkInstructionImpl() {
    override val template: String = when {
        false -> ""
        else -> ""
    }
}
open class WalkInstructionWait(val until: LocalDateTime? = null, val event: TranslateFun? = null, val duration: Long? = null) : WalkInstructionImpl() {
    override val template: String = when {
        false -> ""
        else -> ""
    }
}*/

@Suppress("MemberVisibilityCanBePrivate")
open class WalkInstructionTurn(
    val direction: String? = null,
    val angle: Int? = null,
    val onto: Translatable? = null,
    val target: Translatable? = null,
    override val path: List<Location>? = null
): WalkInstructionImpl() {
    constructor(direction: String? = null, angle: Int? = null, onto: Translatable? = null, target: Translatable? = null, location: Location) : this(direction, angle, onto, target, listOf(location))

    companion object {
        val ACTION_TURN_DIRECTION by SameName
        val ACTION_TURN_DIRECTION_ANGLE by SameName
        val ACTION_TURN_ANGLE by SameName
        val ACTION_TURN_TO by SameName
        val ACTION_TURN_TO_ANGLE by SameName
        val ACTION_TURN_DIRECTION_TO by SameName
        val ACTION_TURN_DIRECTION_TO_ANGLE by SameName
        val ACTION_TURN_ONTO by SameName
        val ACTION_TURN_DIRECTION_ONTO by SameName
        val ACTION_TURN_DIRECTION_ANGLE_ONTO by SameName
        val ACTION_TURN_ANGLE_ONTO by SameName
        val ACTION_TURN_ONTO_TO by SameName
        val ACTION_TURN_TO_ANGLE_ONTO by SameName
        val ACTION_TURN_DIRECTION_ONTO_TO by SameName
        val ACTION_TURN_DIRECTION_ONTO_TO_ANGLE by SameName

        init {
            makeTranslations(defaultTranslations, "Turn", "to", "until you are looking at", "onto")
        }

        fun makeTranslations(translations: MutableTranslations, turn: String, to: String, untilLookingAt: String, onto: String) {
            translations[ACTION_TURN_DIRECTION] = "$turn %s"
            translations[ACTION_TURN_ANGLE] = "$turn %s"
            translations[ACTION_TURN_TO] = "$turn $to \"%s\""
            translations[ACTION_TURN_DIRECTION_TO] = "$turn %s $untilLookingAt \"%s\""
            translations[ACTION_TURN_DIRECTION_ANGLE] = "$turn %s (%s)"
            translations[ACTION_TURN_TO_ANGLE] = "$turn $to \"%s\" (%s)"
            translations[ACTION_TURN_DIRECTION_TO_ANGLE] = "$turn %s (%s) $untilLookingAt \"%s\""

            translations[ACTION_TURN_DIRECTION_ONTO] = "$turn %s $onto %s"
            translations[ACTION_TURN_ANGLE_ONTO] = "$turn %s $onto %s"
            translations[ACTION_TURN_ONTO_TO] = "$turn $onto %s $untilLookingAt \"%s\""
            translations[ACTION_TURN_DIRECTION_ONTO_TO] = "$turn %s $onto %s $untilLookingAt \"%s\""
            translations[ACTION_TURN_DIRECTION_ANGLE_ONTO] = "$turn %s (%s) $onto %s"
            translations[ACTION_TURN_TO_ANGLE_ONTO] = "$turn $onto %s $untilLookingAt \"%s\" (%s)"
            translations[ACTION_TURN_DIRECTION_ONTO_TO_ANGLE] = "$turn %s (%s) $onto %s $untilLookingAt \"%s\""
        }
    }

    private val directionValid = direction != null
    private val angleValid = angle != null
    private val ontoValid = onto != null
    private val targetValid = target != null

    private val mode = when {
        (ontoValid && directionValid && angleValid && targetValid) -> ACTION_TURN_DIRECTION_ONTO_TO_ANGLE
        (ontoValid && directionValid && angleValid) -> ACTION_TURN_DIRECTION_ANGLE_ONTO
        (ontoValid && directionValid && targetValid) -> ACTION_TURN_DIRECTION_ONTO_TO
        (ontoValid && angleValid && targetValid) -> ACTION_TURN_TO_ANGLE_ONTO
        (ontoValid && angleValid) -> ACTION_TURN_ANGLE_ONTO
        (ontoValid && targetValid) -> ACTION_TURN_ONTO_TO
        (ontoValid && directionValid) -> ACTION_TURN_DIRECTION_ONTO
        (ontoValid) -> ACTION_TURN_ONTO
        (directionValid && angleValid && targetValid) -> ACTION_TURN_DIRECTION_TO_ANGLE
        (directionValid && angleValid) -> ACTION_TURN_DIRECTION_ANGLE
        (directionValid && targetValid) -> ACTION_TURN_DIRECTION_TO
        (angleValid && targetValid) -> ACTION_TURN_TO_ANGLE
        (angleValid) -> ACTION_TURN_ANGLE
        (targetValid) -> ACTION_TURN_TO
        (directionValid) -> ACTION_TURN_DIRECTION
        else -> null
    }

    override val template: String = mode ?: "Invalid turn instruction"

    override val values: Array<Translatable> = when (mode) {
        ACTION_TURN_ONTO_TO -> arrayOf(onto!!, target!!)
        ACTION_TURN_DIRECTION_ONTO -> arrayOf(createTranslateFunction(direction?:""), onto!!)
        ACTION_TURN_ANGLE_ONTO -> arrayOf(createTranslateUnitAngle(angle?:0), onto!!)
        ACTION_TURN_TO_ANGLE_ONTO -> arrayOf(onto!!, target!!, createTranslateUnitAngle(angle?:0))
        ACTION_TURN_DIRECTION_ONTO_TO -> arrayOf(createTranslateFunction(direction?:""), onto!!, target!!)
        ACTION_TURN_DIRECTION_ANGLE_ONTO -> arrayOf(createTranslateFunction(direction?:""), createTranslateUnitAngle(angle?:0), onto!!)
        ACTION_TURN_DIRECTION_ONTO_TO_ANGLE -> arrayOf(createTranslateFunction(direction?:""), createTranslateUnitAngle(angle?:0), onto!!, target!!)
        ACTION_TURN_TO -> arrayOf(target!!)
        ACTION_TURN_DIRECTION -> arrayOf(createTranslateFunction(direction?:""))
        ACTION_TURN_ANGLE -> arrayOf(createTranslateUnitAngle(angle?:0))
        ACTION_TURN_TO_ANGLE -> arrayOf(target!!, createTranslateUnitAngle(angle?:0))
        ACTION_TURN_DIRECTION_TO -> arrayOf(createTranslateFunction(direction?:""), target!!)
        ACTION_TURN_DIRECTION_ANGLE -> arrayOf(createTranslateFunction(direction?:""), createTranslateUnitAngle(angle?:0))
        ACTION_TURN_DIRECTION_TO_ANGLE -> arrayOf(createTranslateFunction(direction?:""), createTranslateUnitAngle(angle?:0), target!!)
        else -> emptyArray()
    }
}

open class InstructedTripConnection(
    id: ConnectionIdentifier? = null,
    stops: List<Stop>,
    val instructions: List<TripInstruction>? = null,
    vehicle: Vehicle? = null,
    path: List<LocationLatLon>? = null,
    flags: Int = Connection.FLAG_NONE
) : TripConnectionImpl(id=id, stops=stops, vehicle=vehicle, path=path, flags=flags)

class TripWalkConnection(
    id: ConnectionIdentifier? = null,
    stops: List<Stop>,
    instructions: List<WalkInstruction>? = null,
    vehicle: Vehicle? = DEFAULT_VEHICLE_WALK,
    path: List<LocationLatLon>? = null,
    flags: Int = Connection.FLAG_NONE
) : InstructedTripConnection(id=id, stops=stops, instructions=instructions, vehicle=vehicle, path=path, flags=flags) {
    companion object {
        val DEFAULT_VEHICLE_WALK = VehicleImpl(type=VehicleTypes.WALK)
        val DEFAULT_VEHICLE_INTERCHANGE = VehicleImpl(type=VehicleTypes.INTERCHANGE)
    }
}
