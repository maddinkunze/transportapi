package com.maddin.transportapi.utils

import kotlin.reflect.KProperty

class SameNameDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = property.name
}

val SameName = SameNameDelegate()