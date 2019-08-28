package au.id.micolous.metrodroid.util

expect object Collator {
    val collator: Comparator<in String>
}

inline fun <T> Iterable<T>.collatedBy(
    crossinline selector: (T) -> String
): List<T> {
    val collator = Collator.collator
    return this.sortedWith(Comparator { a, b ->
        collator.compare(selector(a), selector(b))
    })
}
