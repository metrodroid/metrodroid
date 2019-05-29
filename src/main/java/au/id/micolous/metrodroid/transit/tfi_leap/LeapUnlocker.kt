/*
 * LeapUnlocker.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.tfi_leap

import android.util.Log

import au.id.micolous.metrodroid.util.Preferences
import com.google.protobuf.ByteString

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

import au.id.micolous.farebot.BuildConfig
import au.id.micolous.metrodroid.card.desfire.DesfireAuthLog
import au.id.micolous.metrodroid.card.desfire.DesfireUnlocker
import au.id.micolous.metrodroid.card.desfire.DesfireManufacturingData
import au.id.micolous.metrodroid.card.desfire.DesfireProtocol
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.proto.Leap
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable

class LeapUnlocker private constructor(private val mApplicationId: Int,
                                       private val mManufData: ImmutableByteArray) : DesfireUnlocker {
    private var mUnlocked1f: Boolean = false
    private var mUnlockedRest: Boolean = false
    private var mSessionId: String? = null
    private var mConfirmation: ByteArray? = null
    private var mReply1: Leap.LeapMessage? = null

    override fun getOrder(desfireTag: DesfireProtocol, fileIds: IntArray): IntArray {
        var skip = 0
        for (fileId in fileIds) {
            if (fileId == 1 || fileId == 0x1f)
                skip++
        }
        val ret = IntArray(fileIds.size - skip + 2)
        ret[0] = 1
        ret[1] = 0x1f
        var j = 2
        for (fileId in fileIds) {
            if (fileId == 1 || fileId == 0x1f)
                continue
            ret[j++] = fileId
        }

        return ret
    }

    @Throws(Exception::class)
    private suspend fun unlock1f(desfireTag: DesfireProtocol,
                                 files: Map<Int, RawDesfireFile>): DesfireAuthLog? {
        if (mUnlocked1f)
            return null

        val ze = ByteString.copyFrom(byteArrayOf(0))
        val af = ByteString.copyFrom(byteArrayOf(DesfireProtocol.ADDITIONAL_FRAME))

        val file1Desc = getFile(files, 1)
        if (file1Desc == null) {
            Log.e(TAG, "File 1 not found")
            return null
        }
        val file1 = file1Desc.data?.dataCopy
        val challenge = desfireTag.sendUnlock(0x0d)

        val request1 = Leap.LeapMessage.newBuilder()
                .setApplicationId(mApplicationId)
                .setSessionId(UUID.randomUUID().toString())
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(ByteString.copyFrom(byteArrayOf(DesfireProtocol.GET_MANUFACTURING_DATA)))
                        .setResponse(ze.concat(ByteString.copyFrom(mManufData.dataCopy)))
                )
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(ByteString.copyFrom(byteArrayOf(DesfireProtocol.READ_DATA, 1, 0, 0, 0, 0x20, 0, 0)))
                        .setResponse(ze.concat(ByteString.copyFrom(file1)))
                )
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(ByteString.copyFrom(byteArrayOf(DesfireProtocol.UNLOCK, 0x0d)))
                        .setResponse(af.concat(ByteString.copyFrom(challenge.dataCopy)))
                )
                .addKeyvalues(
                        Leap.LeapKeyValue.newBuilder()
                                .setKey("ASYNC_READS")
                                .setValue("true"))
                .build()
        val reply1 = communicate(request1)
        if (reply1.getCmds(0).query.byteAt(0) != DesfireProtocol.ADDITIONAL_FRAME) {
            Log.e(TAG, "CMD0 is not AF")
            return null
        }
        val response = reply1.getCmds(0).query.substring(1).toByteArray()
        mConfirmation = desfireTag.sendAdditionalFrame(response.toImmutable()).dataCopy

        mSessionId = reply1.sessionId
        mReply1 = reply1
        mUnlocked1f = true
        return DesfireAuthLog(0x0d, challenge,
                response.toImmutable(), mConfirmation!!.toImmutable())
    }

    @Throws(Exception::class)
    private suspend fun unlockRest(desfireTag: DesfireProtocol, files: Map<Int, RawDesfireFile>): DesfireAuthLog? {
        if (mUnlockedRest)
            return null

        val ze = ByteString.copyFrom(byteArrayOf(0))
        val af = ByteString.copyFrom(byteArrayOf(DesfireProtocol.ADDITIONAL_FRAME))
        val file1fDesc = getFile(files, 0x1f)
        if (file1fDesc == null) {
            Log.e(TAG, "File 1f not found")
            return null
        }

        val file1f = file1fDesc.data?.dataCopy

        val request2 = Leap.LeapMessage.newBuilder()
                .setApplicationId(mApplicationId)
                .setSessionId(mSessionId)
                .setStage("UPDATE_AUTHENTICATE_1")
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(mReply1!!.getCmds(0).query)
                        .setResponse(ze.concat(ByteString.copyFrom(mConfirmation!!)))
                        .setExpectedResponse(ByteString.copyFrom(byteArrayOf(0)))
                )
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(mReply1!!.getCmds(1).query)
                        .setResponse(ze.concat(ByteString.copyFrom(file1f)))
                )
                .addKeyvalues(
                        Leap.LeapKeyValue.newBuilder()
                                .setKey("ASYNC_READS")
                                .setValue("true"))
                .build()
        val reply2 = communicate(request2)
        val challenge = desfireTag.sendUnlock(0x03)
        val request3 = Leap.LeapMessage.newBuilder()
                .setApplicationId(mApplicationId)
                .setSessionId(mSessionId)
                .setStage("UPDATE_AUTHENTICATE_2")
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(reply2.getCmds(0).query)
                        .setResponse(af.concat(ByteString.copyFrom(challenge.dataCopy)))
                        .setExpectedResponse(af)
                )
                .addKeyvalues(
                        Leap.LeapKeyValue.newBuilder()
                                .setKey("ASYNC_READS")
                                .setValue("true"))
                .build()
        val reply3 = communicate(request3)
        if (reply3.getCmds(0).query.byteAt(0) != DesfireProtocol.ADDITIONAL_FRAME) {
            Log.e(TAG, "CMD0 is not AF")
            return null
        }
        val response = reply3.getCmds(0).query.substring(1).toByteArray()
        val confirm = desfireTag.sendAdditionalFrame(response.toImmutable())
        mUnlockedRest = true
        return DesfireAuthLog(0x03, challenge, response.toImmutable(), confirm)
    }

    override suspend fun unlock(desfireTag: DesfireProtocol,
                                files: Map<Int, RawDesfireFile>, fileId: Int,
                                authLog: MutableList<DesfireAuthLog>) {
        var cur: DesfireAuthLog? = null
        when (fileId) {
            1 -> return
            0x1f -> try {
                cur = unlock1f(desfireTag, files)
            } catch (e: Exception) {
                Log.e(TAG, "unlock failed")
                e.printStackTrace()
            }

            else -> try {
                cur = unlockRest(desfireTag, files)
            } catch (e: Exception) {
                Log.e(TAG, "unlock failed")
                e.printStackTrace()
            }

        }
        if (cur != null)
            authLog.add(cur)
    }

    companion object {
        private const val LEAP_API_URL = "https://tnfc.leapcard.ie//ReadCard/V0"
        private const val TAG = "LeapUnlocker"

        @Throws(IOException::class)
        private fun communicate(`in`: Leap.LeapMessage): Leap.LeapMessage {
            val url = URL(LEAP_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", null)

            conn.setRequestProperty("User-Agent", "Metrodroid/" + BuildConfig.VERSION_NAME)
            val send = conn.outputStream

            Log.d(TAG, "Sending " + `in`.toString())
            `in`.writeTo(send)
            val recv = conn.inputStream
            val reply = Leap.LeapMessage.parseFrom(recv)

            Log.d(TAG, "Received " + reply.toString())
            conn.disconnect()
            return reply
        }

        fun createUnlocker(applicationId: Int, manufData: ImmutableByteArray): LeapUnlocker? {
            val retrieveKeys = Preferences.retrieveLeapKeys
            if (!retrieveKeys) {
                Log.d(TAG, "Retrieving Leap keys not enabled")
                return null
            }
            Log.d(TAG, "Attempting unlock")
            return LeapUnlocker(applicationId, manufData)
        }

        private fun getFile(files: Map<Int, RawDesfireFile>, fileId: Int): RawDesfireFile? = files[fileId]
    }
}

internal actual fun createUnlockerDispatch(appId: Int, manufData: ImmutableByteArray): DesfireUnlocker? =
        LeapUnlocker.createUnlocker(appId, manufData)