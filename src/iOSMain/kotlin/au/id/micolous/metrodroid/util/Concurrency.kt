package au.id.micolous.metrodroid.util

import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

actual class AtomicCounter {
    private val backer = AtomicInt(0)

    actual fun get() = backer.value
    actual fun getAndIncrement(): Int {
        while (true) {
            val ret = backer.value
            if (backer.compareAndSet(ret, ret + 1))
                return ret
        }
    }

    actual fun set(i: Int) {
        backer.value = i
    }
}

actual class AtomicRef<T> actual constructor(initial: T) {
    private val backer = AtomicReference(initial)

    actual var value: T
        get() = backer.value
        set(value) {
            backer.value = value
        }
}

actual fun Any?.nativeFreeze() = this.freeze()
