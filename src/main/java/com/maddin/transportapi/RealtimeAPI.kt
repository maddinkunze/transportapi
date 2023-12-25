package com.maddin.transportapi

import java.time.LocalDateTime

interface RealtimeAPI {
    fun getRealtimeInformation(station: Station) : RealtimeInfo
}

interface FutureRealtimeAPI : RealtimeAPI {
    fun getRealtimeInformation(station: Station, from: LocalDateTime) : RealtimeInfo
}