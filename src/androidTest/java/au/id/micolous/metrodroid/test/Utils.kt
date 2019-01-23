package au.id.micolous.metrodroid.test

import kotlin.math.abs
import kotlin.test.*

internal fun assertNear(expected: Double, actual: Double, tol: Double) {
    if (abs(actual - expected) < tol)
        return
    // Fall to normal assertEquals so it shows everything
    assertEquals(expected, actual)
}