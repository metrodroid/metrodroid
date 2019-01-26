/*
 * FeliCaLib.java
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
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable

/**
 * FeliCa、FeliCa Liteデバイスにアクセスするためのコマンドとデータ操作をライブラリィとして提供します
 * <pre>
 * ※ 「FeliCa」は、ソニー株式会社が開発した非接触ICカードの技術方式です。
 * ※ 「FeliCa」、「FeliCa Lite」、「FeliCa Plug」、「FeliCaポケット」、「FeliCaランチャー」は、ソニー株式会社の登録商標です。
 * ※ 「Suica」は東日本旅客鉄道株式会社の登録商標です。
 * ※ 「PASMO」は、株式会社パスモの登録商標です。
 *
 * 本ライブラリィはFeliCa、ソニー株式会社とはなんの関係もありません。
</pre> *
 *
 * @author Kazzz
 * @since Android API Level 10
 */

object FeliCaLib {
    internal suspend fun execute(tag: CardTransceiver, commandCode: Byte, idm: ImmutableByteArray, vararg data: Byte): ImmutableByteArray {
        val length = idm.size + data.size + 2
        val buff = ImmutableByteArray.ofB(length, commandCode) + idm + data.toImmutable()
        return transceive(tag, buff)
    }

    internal suspend fun execute(tag: CardTransceiver, commandCode: Byte, vararg data: Byte): ImmutableByteArray {
        val length = data.size + 2
        val buff = ImmutableByteArray.ofB(length, commandCode) + data.toImmutable()

        return transceive(tag, buff)
    }

    internal suspend fun execute(tag: CardTransceiver, commandCode: Byte, data: ImmutableByteArray): ImmutableByteArray {
        val length = data.size + 2
        val buff = ImmutableByteArray.ofB(length, commandCode) + data

        return transceive(tag, buff)
    }

    /**
     * INfcTag#transceiveを実行します
     *
     * @param tag  Tagクラスの参照をセットします
     * @param data 実行するコマンドパケットをセットします
     * @return byte[] コマンドの実行結果バイト列で戻ります
     * @throws TagLostException if the tag went out of the field
     * @throws IOException if any I/O errors occur
     */
    private suspend fun transceive(tag: CardTransceiver, data: ImmutableByteArray) =
    //NfcFはFeliCa
            tag.transceive(data)

    /**
     * FeliCa SystemCodeクラスを提供します
     *
     * @author Kazzz
     * @since Android API Level 9
     */
    /**
     * Create a new system code.
     *
     * @param bytes Bytes passed in little endian
     */
    class SystemCode (bytes: ImmutableByteArray) {
        val bytes: ImmutableByteArray = ImmutableByteArray.of(bytes[1], bytes[0])

        /**
         * Returns the system code in Big Endian
         *
         * @return System code as integer
         */
        val code: Int
            get() = (this.bytes[0].toInt() and 0xff) + (this.bytes[1].toInt() and 0xff shl 8)

        constructor(systemCode: Int) : this(ImmutableByteArray.ofB((systemCode shr 8), (systemCode and 0xff))) // Pass in a system code in "big endian" form
    }

    /**
     * FeliCa ServiceCodeクラスを提供します
     *
     * @author Kazzz
     * @since Android API Level 9
     */
    /**
     * コンストラクタ
     *
     * @param bytes バイト列をセット
     */
    /*
         * サービスコードをバイト列として返します。
         * @return サービスコードのバイト列表現
         */
    class ServiceCode (val bytes: ImmutableByteArray) {
        constructor(serviceCode: Int) : this(ImmutableByteArray.ofB((serviceCode and 0xff), (serviceCode shr 8)))
    }

    /**
     * FeliCa コマンドレスポンスクラスを提供します
     *
     * @author Kazz
     * @since Android API Level 9
     */
    /**
     * コンストラクタ
     *
     * @param data コマンド実行結果で戻ったバイト列をセット
     */
    open class CommandResponse {
        /**
         * バイト列表現を戻します
         *
         * @return byte[] このデータのバイト列表現を戻します
         */
        val bytes: ImmutableByteArray?
        private val length: Int      //全体のデータ長 (FeliCaには無い)
        private val responseCode: Byte//コマンドレスポンスコード)
        private val iDm: ImmutableByteArray?          //FeliCa IDm
        val data: ImmutableByteArray?      //コマンドデータ

        /**
         * コンストラクタ
         *
         * @param response 他のレスポンスをセット
         */
        internal constructor(response: CommandResponse?) : this(response?.bytes)

        internal constructor(data: ImmutableByteArray?) {
            this.bytes = data
            if (data != null) {
                this.length = data[0].toInt() and 0xff
                this.responseCode = data[1]
            } else {
                this.length = 0
                this.responseCode = 0
            }
            this.iDm = data?.sliceOffLen(2, 8)
            this.data = data?.sliceOffLen(10, data.size - 10)
        }
    }

    /**
     * Read コマンドのレスポンスを抽象化したクラスを提供します
     *
     * @author Kazzz
     * @since Android API Level 9
     */

    /**
     * コンストラクタ
     *
     * @param response コマンド実行結果で戻ったバイト列をセット
     */
    class ReadResponse internal constructor(response: CommandResponse) : CommandResponse(response) {
        /**
         * statusFlag1を取得します
         *
         * @return int statusFlag1が戻ります
         */
        val statusFlag1: Int
        /**
         * statusFlag2を取得します
         *
         * @return int statusFlag2が戻ります
         */
        private val statusFlag2: Int
        /**
         * blockCountを取得します
         *
         * @return int blockCountが戻ります
         */
        private val blockCount: Int
        /**
         * blockDataを取得します
         *
         * @return byte[] blockDataが戻ります
         */
        val blockData: ImmutableByteArray?

        init {
            if (this.data == null) {
                // Tried to read a block which doesn't exist
                this.blockCount = 0
                this.blockData = null
                this.statusFlag1 = 0xffff
                this.statusFlag2 = 0xffff
            } else {
                this.statusFlag1 = data[0].toInt()
                this.statusFlag2 = data[1].toInt()
                if (this.statusFlag1 == 0) {
                    this.blockCount = data[2].toInt()
                    this.blockData = data.sliceOffLen(3, data.size - 3)
                } else {
                    this.blockCount = 0
                    this.blockData = null
                }
            }
        }
    }
}
