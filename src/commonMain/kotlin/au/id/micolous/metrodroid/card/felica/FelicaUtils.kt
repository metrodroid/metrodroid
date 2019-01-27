package au.id.micolous.metrodroid.card.felica

expect object FelicaUtils {
    fun getFriendlySystemName(systemCode: Int): String
    fun getFriendlyServiceName(systemCode: Int, serviceCode: Int): String
}