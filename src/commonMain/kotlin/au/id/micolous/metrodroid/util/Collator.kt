package au.id.micolous.metrodroid.util

expect object Collator {
    val collator: Comparator<in String>
}
