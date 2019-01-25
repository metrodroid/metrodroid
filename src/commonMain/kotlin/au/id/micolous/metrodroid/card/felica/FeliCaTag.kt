/*
 * FeliCaTag.java
 *
 * Copyright 2011 Kazzz
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc
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
 *
 * This code was imported from nfc-felica-lib[0], formerly licensed under
 * Apache License v2.0. The Apache Software Foundation considers the license
 * to be compatible with the GPLv3+[1].
 *
 * As the upstream library[0] appears to be no longer maintained, and we're not
 * aware of other users of Metrodroid's fork[2] of the library, this has been
 * pulled into Metrodroid proper[3]. It has been relicensed as GPLv3+ to be
 * consistent with the remainder of the project.
 *
 * [0]: https://github.com/Kazzz/nfc-felica-lib
 * [1]: https://www.apache.org/licenses/GPL-compatibility.html
 * [2]: https://github.com/metrodroid/nfc-felica-lib
 * [3]: https://github.com/micolous/metrodroid/pull/255
 */
package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * FeliCa仕様に準拠した FeliCaタグクラスを提供します
 *
 * @author Kazzz
 * @since Android API Level 9
 */

class FeliCaTag(private val nfcTag: CardTransceiver) {
    /**
     * FeliCa IDmを取得します
     *
     * @return IDm IDmが戻ります
     */
    var iDm: ImmutableByteArray? = null
        private set
    /**
     * FeliCa PMmを取得します
     *
     * @return PMm PMmが戻ります
     */
    var pMm: ImmutableByteArray? = null
        private set

    /**
     * SystemCodeの一覧を取得します。
     *
     * @return SystemCode[] 検出された SystemCodeの一覧を返します。
     * @throws TagLostException if the tag went out of the field
     */
    //request systemCode
    // No system codes were received from the card.
    //Log.d(TAG, "Num SystemCode: " + num);
    suspend fun getSystemCodeList (): List<FeliCaLib.SystemCode>{
            val retBytes = FeliCaLib.execute(this.nfcTag, FelicaConsts.COMMAND_REQUEST_SYSTEMCODE, iDm!!)
            return (0 until retBytes[10].toInt()).map { i -> FeliCaLib.SystemCode(retBytes.sliceOffLen(11 + i * 2, 2)) }
        }

    /**
     * Polling済みシステム領域のサービスの一覧を取得します。
     *
     * @return ServiceCode[] 検出された ServiceCodeの配列
     * @throws TagLostException if the tag went out of the field
     */
    // 0番目は root areaなので1オリジンで開始する
    // 1件1件 通信して聞き出します。
    // 2 or 4 バイトじゃない場合は、とりあえず終了しておきます。正しい判定ではないかもしれません。
    // 2バイトは ServiceCode として扱っています。
    // FFFF が終了コードのようです
    // Invalid service code index
    suspend fun getServiceCodeList(): List<FeliCaLib.ServiceCode> {
            var index = 1
            val serviceCodeList = mutableListOf<FeliCaLib.ServiceCode>()
            while (true) {
                val bytes = doSearchServiceCode(index)
                if (bytes.size != 2 && bytes.size != 4)
                    break
                if (bytes.size == 2) {
                    if (bytes[0] == 0xff.toByte() && bytes[1] == 0xff.toByte()) break
                    serviceCodeList.add(FeliCaLib.ServiceCode(bytes))
                }
                index++

                if (index > 0xffff) {
                    break
                }
            }
            return serviceCodeList
        }

    init {
        this.iDm = null
        this.pMm = null
    }

    /**
     * カードデータをポーリングします
     *
     * Note: This does not check if we got the **same** IDm/PMm as last time.
     *
     * @param systemCode System code to request in Big Endian (ie: no byte swap needed)
     * @throws TagLostException if the tag went out of the field
     * @return byte[] システムコードの配列が戻ります
     */
    suspend fun polling(systemCode: Int): ImmutableByteArray? {
        val data = FeliCaLib.execute(this.nfcTag, FelicaConsts.COMMAND_POLLING, (systemCode shr 8).toByte()   // System code (upper byte)
                , (systemCode and 0xff).toByte() // System code (lower byte)
                , 0x01.toByte()                // Request code (system code request)
                , 0x00.toByte())
        if (data.size >= 10) {
            this.iDm = data.sliceOffLen(2, 8)
        } else
            this.iDm = null
        if (data.size >= 18) {
            this.pMm = data.sliceOffLen(10, 8)
        } else
            this.pMm = null
        return data
    }

    /**
     * カードデータをポーリングしてIDmを取得します
     *
     * @param systemCode 対象のシステムコードをセットします
     * @throws TagLostException if the tag went out of the field
     * @return IDm IDmが戻ります
     */
    suspend fun pollingAndGetIDm(systemCode: Int): ImmutableByteArray? {
        this.polling(systemCode)
        return this.iDm
    }

    /**
     * COMMAND_SEARCH_SERVICECODE を実行します。
     * 参考: http://wiki.osdev.info/index.php?PaSoRi%2FRC-S320#content_1_25
     *
     * @param index ？番目か
     * @return Response部分
     * @throws TagLostException if the tag went out of the field
     */
    private suspend fun doSearchServiceCode(index: Int): ImmutableByteArray {
        val bytes = FeliCaLib.execute(this.nfcTag, FelicaConsts.COMMAND_SEARCH_SERVICECODE, iDm!!, (index and 0xff).toByte(), (index shr 8).toByte())
        if (bytes.size <= 0 || bytes[1] != 0x0b.toByte()) { // 正常応答かどうか
            Log.w(TAG, "Response code is not 0x0b")
            // throw new FeliCaException("ResponseCode is not 0x0b");
            return ImmutableByteArray.empty()
        }
        return bytes.sliceOffLen(10, bytes.size - 10)
    }

    /**
     * 認証不要領域のデータを読み込みます
     *
     * @param serviceCode サービスコードをセット
     * @param addr        読み込むブロックのアドレス (0オリジン)をセット
     * @return ReadResponse 読み込んだ結果が戻ります
     * @throws TagLostException if the tag went out of the field
     */
    suspend fun readWithoutEncryption(serviceCode: FeliCaLib.ServiceCode,
                              addr: Byte): FeliCaLib.ReadResponse {
        // read without encryption
        val bytes = serviceCode.bytes
        val resp = FeliCaLib.execute(this.nfcTag, FelicaConsts.COMMAND_READ_WO_ENCRYPTION, iDm!!, 0x01.toByte()         // サービス数
                , bytes[1], bytes[0]             // サービスコード (little endian)
                , 0x01.toByte()                 // 同時読み込みブロック数
                , 0x80.toByte(), addr)
        return FeliCaLib.ReadResponse(FeliCaLib.CommandResponse(resp))
    }

    companion object {
        private const val TAG = "FeliCaTag"
    }
}
