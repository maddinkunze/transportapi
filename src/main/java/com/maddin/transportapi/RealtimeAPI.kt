package com.maddin.transportapi

interface RealtimeAPI {
    fun getRealtimeInformation(station: Station) : RealtimeInfo
}