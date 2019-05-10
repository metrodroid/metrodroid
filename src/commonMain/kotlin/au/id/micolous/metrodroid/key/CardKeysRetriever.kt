
package au.id.micolous.metrodroid.key

import au.id.micolous.metrodroid.util.ImmutableByteArray

interface CardKeysRetriever {
    fun forTagID(tagID: ImmutableByteArray): CardKeys?

    fun forClassicStatic(): ClassicStaticKeys?

    fun forID(id: Int): CardKeys?
}