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

import au.id.micolous.metrodroid.card.desfire.DesfireAuthLog
import au.id.micolous.metrodroid.card.desfire.DesfireUnlocker
import au.id.micolous.metrodroid.card.desfire.DesfireProtocol
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LeapDesFireCommand constructor(
    @ProtoNumber(1)
    @Serializable(with = ImmutableByteArray.RawSerializer::class)
    val query: ImmutableByteArray? = null,
    @ProtoNumber(2)
    @Serializable(with = ImmutableByteArray.RawSerializer::class)
    val response: ImmutableByteArray? = null,
    @ProtoNumber(3)
    @Serializable(with = ImmutableByteArray.RawSerializer::class)
    val expectedResponse: ImmutableByteArray? = null,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LeapKeyValue(
    @ProtoNumber(3)
    val key: String? = null,
    @ProtoNumber(4)
    val value: String? = null
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LeapMessage(
    @ProtoNumber(1)
    val applicationId: Int? = null,
    @ProtoNumber(2)
    val sessionId: String? = null,
    @ProtoNumber(3)
    val stage: String? = null,
    @ProtoNumber(4)
    val cmds: List<LeapDesFireCommand> = emptyList(),
    @ProtoNumber(5)
    val keyValues: List<LeapKeyValue> = emptyList()
)

class LeapUnlocker private constructor(private val mApplicationId: Int,
                                       private val mManufData: ImmutableByteArray) : DesfireUnlocker {
    private var mUnlocked1f: Boolean = false
    private var mUnlockedRest: Boolean = false
    private var mSessionId: String? = null
    private var mConfirmation: ImmutableByteArray? = null
    private var mReply1: LeapMessage? = null

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
    private fun unlock1f(desfireTag: DesfireProtocol,
                                 files: Map<Int, RawDesfireFile>): DesfireAuthLog? {
        if (mUnlocked1f)
            return null

        val ze = ImmutableByteArray.of(0)
        val af = ImmutableByteArray.of(DesfireProtocol.ADDITIONAL_FRAME)

        val file1Desc = getFile(files, 1)
        if (file1Desc == null) {
            Log.e(TAG, "File 1 not found")
            return null
        }
        val file1 = file1Desc.data ?: ImmutableByteArray.empty()
        val challenge = desfireTag.sendUnlock(0x0d)

        val request1 = LeapMessage(
            applicationId = mApplicationId,
            sessionId = networkHelper.randomUUID(),
            cmds = listOf(
                LeapDesFireCommand(
                    query = ImmutableByteArray.of(DesfireProtocol.GET_MANUFACTURING_DATA),
                    response = ze + mManufData
                ),
                LeapDesFireCommand(
                    query = ImmutableByteArray.of(DesfireProtocol.READ_DATA, 1, 0, 0, 0, 0x20, 0, 0),
                    response = ze + file1
                ),
                LeapDesFireCommand(
                    query = ImmutableByteArray.of(DesfireProtocol.UNLOCK, 0x0d),
                    response = af + challenge
                )
            ),
            keyValues = listOf(
                LeapKeyValue(key = "ASYNC_READS", value = "true")
            )
        )
        val reply1 = communicate(request1)
        if (reply1.cmds[0].query?.get(0) != DesfireProtocol.ADDITIONAL_FRAME) {
            Log.e(TAG, "CMD0 is not AF")
            return null
        }
        val response = reply1.cmds[0].query?.let {
            it.copyOfRange(1, it.size)
        }
        mConfirmation = desfireTag.sendAdditionalFrame(response!!)

        mSessionId = reply1.sessionId
        mReply1 = reply1
        mUnlocked1f = true
        return DesfireAuthLog(0x0d, challenge, response, mConfirmation!!)
    }

    @Throws(Exception::class)
    private fun unlockRest(desfireTag: DesfireProtocol, files: Map<Int, RawDesfireFile>): DesfireAuthLog? {
        if (mUnlockedRest)
            return null

        val ze = ImmutableByteArray.of(0)
        val af = ImmutableByteArray.of(DesfireProtocol.ADDITIONAL_FRAME)
        val file1fDesc = getFile(files, 0x1f)
        if (file1fDesc == null) {
            Log.e(TAG, "File 1f not found")
            return null
        }

        val file1f = file1fDesc.data ?: ImmutableByteArray.empty()

        val request2 = LeapMessage(
            applicationId = mApplicationId,
            sessionId = mSessionId,
            stage = "UPDATE_AUTHENTICATE_1",
            cmds = listOf(
                LeapDesFireCommand(
                    query = mReply1!!.cmds[0].query,
                    response = ze + mConfirmation!!,
                    expectedResponse = ImmutableByteArray.of(0)
                ),
                LeapDesFireCommand(
                    query = mReply1!!.cmds[1].query,
                    response = ze + file1f
                )
            ),
            keyValues = listOf(
                LeapKeyValue(key="ASYNC_READS", value="true")
            )
        )
        val reply2 = communicate(request2)
        val challenge = desfireTag.sendUnlock(0x03)
        val request3 = LeapMessage(
            applicationId = mApplicationId,
            sessionId = mSessionId,
            stage = "UPDATE_AUTHENTICATE_2",
            cmds = listOf(
                LeapDesFireCommand(
                    query = reply2.cmds[0].query,
                    response = af + challenge,
                    expectedResponse = af)
            ),
            keyValues = listOf(
                LeapKeyValue(key="ASYNC_READS", value="true")
            )
        )
        val reply3 = communicate(request3)
        if (reply3.cmds[0].query?.get(0) != DesfireProtocol.ADDITIONAL_FRAME) {
            Log.e(TAG, "CMD0 is not AF")
            return null
        }
        val response = reply3.cmds[0].query?.let {
            it.copyOfRange(1, it.size)
        }!!
        val confirm = desfireTag.sendAdditionalFrame(response)
        mUnlockedRest = true
        return DesfireAuthLog(0x03, challenge, response, confirm)
    }

    override fun unlock(desfireTag: DesfireProtocol,
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

        @OptIn(ExperimentalSerializationApi::class)
        private fun communicate(inputPb: LeapMessage): LeapMessage {
            Log.d(TAG, "Sending $inputPb")
            val input = ProtoBuf.encodeToByteArray(LeapMessage.serializer(), inputPb)
            val reply = networkHelper.sendPostRequest(LEAP_API_URL, input)
            val replyPb = ProtoBuf.decodeFromByteArray(LeapMessage.serializer(),
                reply!!)
            Log.d(TAG, "Received $replyPb")
            return replyPb
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

internal fun createUnlockerDispatch(appId: Int, manufData: ImmutableByteArray): DesfireUnlocker? =
        LeapUnlocker.createUnlocker(appId, manufData)