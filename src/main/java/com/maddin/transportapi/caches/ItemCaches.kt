package com.maddin.transportapi.caches

import com.maddin.transportapi.components.POI
import com.maddin.transportapi.components.Station

typealias POICache = ItemCache<POI>
open class POICacheImpl : ItemCacheImpl<POI>(), POICache

typealias StationCache = ItemCache<Station>
open class StationCacheImpl : ItemCacheImpl<Station>(), StationCache