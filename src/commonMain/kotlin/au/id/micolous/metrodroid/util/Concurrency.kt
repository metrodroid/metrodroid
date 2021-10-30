package au.id.micolous.metrodroid.util

expect class AtomicRef<T>(initial: T) {
    var value: T
}
expect class AtomicCounter() {
    fun get(): Int
    fun getAndIncrement(): Int
    fun set(i: Int)
}
expect fun Any?.nativeFreeze(): Any?