package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifiable
import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.IdentifierImpl
import com.maddin.transportapi.utils.MaybeIdentifiable
import com.maddin.transportapi.utils.Named
import com.maddin.transportapi.utils.Searchable
import com.maddin.transportapi.utils.Translatable
import com.maddin.transportapi.utils.Translations
import java.io.Serializable
import java.util.Locale

interface PlatformIdentifier : Identifier {
    val platformId: String
    val stationId: StationIdentifier?
    override val uid: String; get() = platformId
    override val uuid: String; get() = concat(stationId, platformId)
}
open class PlatformIdentifierImpl(
    override val platformId: String,
    override val stationId: StationIdentifier?=null,
) : PlatformIdentifier
fun String.toPlatId(): PlatformIdentifierImpl {
    val split = split(Identifier.SAFE_CONCAT, limit=2)
    if (split.size != 2) { return PlatformIdentifierImpl(this) }
    return PlatformIdentifierImpl(split[0], split[1].toStaId())
}

interface Platform: MaybeIdentifiable, Named, Translatable {
    override val id: PlatformIdentifier?
    override val name: String
    override fun translate(locale: Locale?, translations: Translations?): String = name
}

open class PlatformImpl(
    override var id: PlatformIdentifier? = null,
    override var name: String
) : Platform

interface StationIdentifier : POIIdentifier
open class StationIdentifierImpl(uuid: String) : POIIdentifierImpl(uuid), StationIdentifier
fun String.toStaId() = StationIdentifierImpl(this)

interface Station : POI, Identifiable {
    override val id: StationIdentifier
    val lines: List<Line>?
    val platforms: List<Platform>?
}

open class StationImpl(
    override var id: StationIdentifier,
    override var name: String,
    override var location: Location? = null,
    override var lines: List<Line>? = null,
    override var platforms: List<Platform>? = null
) : Station
