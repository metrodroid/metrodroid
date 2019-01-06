package au.id.micolous.metrodroid.util

import android.util.SparseArray
import java.util.NoSuchElementException

/**
 * SparseArrayIterator iterates over a [SparseArray], returning the key [Int] and value [T] as a
 * [Pair].
 *
 * @param sparseArray A [SparseArray] to iterate over
 * @param T The type of [SparseArray]
 */
class SparseArrayIterator<T>(
        private val sparseArray: SparseArray<T>
) : Iterator<Pair<Int, T>> {
    private var pos = -1

    override fun hasNext() = (pos + 1) < sparseArray.size()
    override fun next() : Pair<Int, T> {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val k = sparseArray.keyAt(++pos)
        val v = sparseArray.valueAt(pos)
        return Pair(k, v)
    }
}