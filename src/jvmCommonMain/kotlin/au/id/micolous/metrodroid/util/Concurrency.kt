package au.id.micolous.metrodroid.util

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

actual class AtomicCounter {
    private val backer = AtomicInteger(0)

    actual fun get(): Int = backer.get()
    actual fun getAndIncrement(): Int = backer.incrementAndGet() - 1
    actual fun set(i: Int) {
        backer.set(i)
    }
}

@Suppress("UnknownNullness") // We specify nullness, but lint misbehaves
actual class AtomicRef<T> actual constructor(initial: T) {
    private val backer = AtomicReference(initial)

    actual var value: T
        get() = backer.get()
        set(value) { backer.set(value) }
}

actual fun Any?.nativeFreeze(): Any? = this
