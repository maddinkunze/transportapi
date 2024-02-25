package com.maddin.transportapi.components

import com.maddin.transportapi.utils.Identifier
import com.maddin.transportapi.utils.IdentifierImpl
import com.maddin.transportapi.utils.MaybeIdentifiable
import com.maddin.transportapi.utils.Named
import com.maddin.transportapi.utils.Searchable
import com.maddin.transportapi.utils.Translatable
import com.maddin.transportapi.utils.Translations
import java.io.Serializable
import java.util.Locale


interface POIIdentifier : Identifier
open class POIIdentifierImpl(uuid: String) : IdentifierImpl(uuid), POIIdentifier
fun String.toPoiId() = POIIdentifierImpl(this)

interface POI : Serializable, Searchable, Translatable, MaybeIdentifiable, Named {
    override val id: POIIdentifier?
    override val name: String
    val location: Location?

    override fun matches(search: String): Boolean {
        return name.contains(search, ignoreCase=true)
    }

    override fun translate(locale: Locale?, translations: Translations?): String = name
}

open class POIImpl(
    override var id: POIIdentifier? = null,
    override var name: String,
    override var location: Location? = null
) : POI

interface StreetIdentifier : POIIdentifier
open class StreetIdentifierImpl(uuid: String) : POIIdentifierImpl(uuid), StreetIdentifier
fun String.toStrId() = StreetIdentifierImpl(this)
interface Street : POI

class StreetImpl(
    override val id: StreetIdentifier? = null,
    override val name: String,
    override val location: Location? = null
) : Street

interface Building : POI