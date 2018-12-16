/*
 * IteratorTransformer.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.util

/**
 * IteratorTransformer converts an [Iterator] of type [T] into one of type [R].
 *
 * This implements similar functionality to Guice's Iterators.transform method.
 *
 * @param iterator The iterator to consume
 * @param transform A [FunctionalInterface] that transforms type [T] into [R].
 * @param T The source type
 * @param R The destination type
 */
class IteratorTransformer<T, out R>(
        private val iterator: Iterator<T>,
        private val transform: Function<T, R>
) : Iterator<R> {
    // j.u.function.Function<T, R> is not available before API 24
    @FunctionalInterface
    interface Function<in T, out R> {
        fun apply(input: T) : R
    }

    override fun hasNext() = iterator.hasNext()
    override fun next() = transform.apply(iterator.next())
}