package com.maddin.transportapi.caches

import com.maddin.transportapi.components.POI
import com.maddin.transportapi.components.Station

typealias SearchPOICache = SearchCache<POI>
open class SearchPOICacheImpl : SearchCacheImpl<POI>()

typealias SearchStationCache = SearchCache<Station>
open class SearchStationCacheImpl : SearchCacheImpl<Station>(), SearchStationCache
