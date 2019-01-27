package au.id.micolous.metrodroid.transit.octopus

import au.id.micolous.metrodroid.transit.TransitData

expect class OctopusTransitData : TransitData {
    companion object {
        val SYSTEMCODE_OCTOPUS: Int
        val SYSTEMCODE_SZT: Int
        val SERVICE_OCTOPUS: Int
        val SERVICE_SZT: Int
    }
}