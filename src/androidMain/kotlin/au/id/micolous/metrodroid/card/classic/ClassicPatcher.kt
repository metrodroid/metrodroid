/*
 * ClassicPatcher.java
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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
package au.id.micolous.metrodroid.card.classic

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Bundle
import android.os.Parcel
import android.util.Log

object ClassicPatcher {
    private const val TAG = "ClassicPatcher"

    fun getTech(tag: Tag): MifareClassic? {
        try {
            return MifareClassic.get(tag)
        } catch (e: NullPointerException) {
            Log.d(TAG, "Working around broken Android NFC on HTC devices (and others)", e)
            return MifareClassic.get(patchTag(tag))
        }
    }

    /**
     * Patch the broken Tag object of HTC One (m7/m8) devices with Android 5.x.
     *
     *
     * Also observed on Galaxy Nexus running Cyanogenmod 13.
     *
     *
     * "It seems, the reason of this bug is TechExtras of NfcA is null.
     * However, TechList contains MIFAREClassic." -- bildin.
     *
     *
     * This patch will fix this. For more information please refer to
     * https://github.com/ikarus23/MIFAREClassicTool/issues/52
     *
     *
     * This patch was provided by bildin (https://github.com/bildin).
     *
     * @param tag The broken tag.
     * @return The fixed tag.
     */
    private fun patchTag(tag: Tag): Tag {
        val sTechList = tag.techList
        val oldParcel = Parcel.obtain()
        tag.writeToParcel(oldParcel, 0)
        oldParcel.setDataPosition(0)

        val len = oldParcel.readInt()
        var id = ByteArray(0)
        if (len >= 0) {
            id = ByteArray(len)
            oldParcel.readByteArray(id)
        }
        val oldTechList = IntArray(oldParcel.readInt())
        oldParcel.readIntArray(oldTechList)
        val oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR)
        val serviceHandle = oldParcel.readInt()
        val isMock = oldParcel.readInt()
        val tagService = if (isMock == 0) {
            oldParcel.readStrongBinder()
        } else {
            null
        }
        oldParcel.recycle()

        var nfcaIdx = -1
        var mcIdx = -1
        for (idx in sTechList.indices) {
            if (sTechList[idx] == NfcA::class.java.name) {
                nfcaIdx = idx
            } else if (sTechList[idx] == MifareClassic::class.java.name) {
                mcIdx = idx
            }
        }

        if (nfcaIdx >= 0 && mcIdx >= 0 && oldTechExtras!![mcIdx] == null) {
            oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx]
        } else {
            return tag
        }

        val newParcel = Parcel.obtain()
        newParcel.writeInt(id.size)
        newParcel.writeByteArray(id)
        newParcel.writeInt(oldTechList.size)
        newParcel.writeIntArray(oldTechList)
        newParcel.writeTypedArray(oldTechExtras, 0)
        newParcel.writeInt(serviceHandle)
        newParcel.writeInt(isMock)
        if (isMock == 0) {
            newParcel.writeStrongBinder(tagService)
        }
        newParcel.setDataPosition(0)
        val newTag = Tag.CREATOR.createFromParcel(newParcel)
        newParcel.recycle()

        return newTag
    }
}
