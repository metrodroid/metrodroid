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
package au.id.micolous.metrodroid.card.felica;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcF;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * FeliCa、FeliCa Liteデバイスにアクセスするためのコマンドとデータ操作をライブラリィとして提供します
 * <pre>
 * ※ 「FeliCa」は、ソニー株式会社が開発した非接触ICカードの技術方式です。
 * ※ 「FeliCa」、「FeliCa Lite」、「FeliCa Plug」、「FeliCaポケット」、「FeliCaランチャー」は、ソニー株式会社の登録商標です。
 * ※ 「Suica」は東日本旅客鉄道株式会社の登録商標です。
 * ※ 「PASMO」は、株式会社パスモの登録商標です。
 *
 * 本ライブラリィはFeliCa、ソニー株式会社とはなんの関係もありません。
 * </pre>
 *
 * @author Kazzz
 * @since Android API Level 10
 */

@SuppressWarnings("DuplicateThrows")
public final class FeliCaLib {
    //polling
    public static final byte COMMAND_POLLING = 0x00;
    public static final byte RESPONSE_POLLING = 0x01;
    //request service
    public static final byte COMMAND_REQUEST_SERVICE = 0x02;
    public static final byte RESPONSE_REQUEST_SERVICE = 0x03;
    //request RESPONSE
    public static final byte COMMAND_REQUEST_RESPONSE = 0x04;
    public static final byte RESPONSE_REQUEST_RESPONSE = 0x05;
    //read without encryption
    public static final byte COMMAND_READ_WO_ENCRYPTION = 0x06;
    public static final byte RESPONSE_READ_WO_ENCRYPTION = 0x07;
    //write without encryption
    public static final byte COMMAND_WRITE_WO_ENCRYPTION = 0x08;
    public static final byte RESPONSE_WRITE_WO_ENCRYPTION = 0x09;
    //search service code
    public static final byte COMMAND_SEARCH_SERVICECODE = 0x0a;
    public static final byte RESPONSE_SEARCH_SERVICECODE = 0x0b;
    //request system code
    public static final byte COMMAND_REQUEST_SYSTEMCODE = 0x0c;
    public static final byte RESPONSE_REQUEST_SYSTEMCODE = 0x0d;
    //authentication 1
    public static final byte COMMAND_AUTHENTICATION1 = 0x10;
    public static final byte RESPONSE_AUTHENTICATION1 = 0x11;
    //authentication 2
    public static final byte COMMAND_AUTHENTICATION2 = 0x12;
    public static final byte RESPONSE_AUTHENTICATION2 = 0x13;
    //read
    public static final byte COMMAND_READ = 0x14;
    public static final byte RESPONSE_READ = 0x15;
    //write
    public static final byte COMMAND_WRITE = 0x16;
    public static final byte RESPONSE_WRITE = 0x17;
    // システムコード
    public static final int SYSTEMCODE_ANY = 0xffff;         // ANY
    public static final int SYSTEMCODE_FELICA_LITE = 0x88b4; // FeliCa Lite
    public static final int SYSTEMCODE_COMMON = 0xfe00;      // 共通領域
    public static final int SYSTEMCODE_CYBERNE = 0x0003;     // サイバネ領域

    public static final int SERVICE_FELICA_LITE_READONLY = 0x0b00;  // FeliCa Lite RO権限
    public static final int SERVICE_FELICA_LITE_READWRITE = 0x0900; // FeliCa Lite RW権限

    static byte[] execute(Tag tag, byte commandCode, byte[] idm, final byte... data)
            throws IOException, TagLostException {
        int length = idm.length + data.length + 2;
        ByteBuffer buff = ByteBuffer.allocate(length);
        buff.put((byte) length).put(commandCode).put(idm).put(data);
        return transceive(tag, buff.array());
    }

    static byte[] execute(Tag tag, byte commandCode, final byte... data)
            throws IOException, TagLostException {
        int length = data.length + 2;
        ByteBuffer buff = ByteBuffer.allocate(length);
        buff.put((byte) length).put(commandCode).put(data);

        return transceive(tag, buff.array());
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
    private static byte[] transceive(Tag tag, byte[] data) throws IOException, TagLostException {
        //NfcFはFeliCa
        NfcF nfcF = NfcF.get(tag);
        if (nfcF == null) throw new IOException("tag is not FeliCa(NFC-F) ");
        nfcF.connect();
        try {
            return nfcF.transceive(data);
        } finally {
            nfcF.close();
        }
    }

    /**
     * FeliCa SystemCodeクラスを提供します
     *
     * @author Kazzz
     * @since Android API Level 9
     */
    public static class SystemCode {
        private final byte[] systemCode;

        /**
         * Create a new system code.
         *
         * @param bytes Bytes passed in little endian
         */
        public SystemCode(byte[] bytes) {
            this.systemCode = new byte[]{bytes[1], bytes[0]};
        }

        public SystemCode(int systemCode) {
            // Pass in a system code in "big endian" form
            this(new byte[]{(byte) (systemCode >> 8), (byte) (systemCode & 0xff)});
        }

        public byte[] getBytes() {
            return this.systemCode;
        }

        /**
         * Returns the system code in Big Endian
         *
         * @return System code as integer
         */
        public int getCode() {
            return (this.systemCode[0] & 0xff) + ((this.systemCode[1] & 0xff) << 8);
        }
    }

    /**
     * FeliCa ServiceCodeクラスを提供します
     *
     * @author Kazzz
     * @since Android API Level 9
     */
    public static class ServiceCode {
        private final byte[] serviceCode;

        /**
         * コンストラクタ
         *
         * @param bytes バイト列をセット
         */
        public ServiceCode(byte[] bytes) {
            this.serviceCode = bytes;
        }

        public ServiceCode(int serviceCode) {
            this(new byte[]{(byte) (serviceCode & 0xff), (byte) (serviceCode >> 8)});
        }

        /*
         * サービスコードをバイト列として返します。
         * @return サービスコードのバイト列表現
         */
        public byte[] getBytes() {
            return this.serviceCode;
        }
    }

    /**
     * FeliCa コマンドレスポンスクラスを提供します
     *
     * @author Kazz
     * @since Android API Level 9
     */
    public static class CommandResponse {
        private final byte[] rawData;
        private final int length;      //全体のデータ長 (FeliCaには無い)
        private final byte responseCode;//コマンドレスポンスコード)
        private final byte[] idm;          //FeliCa IDm
        private final byte[] data;      //コマンドデータ

        /**
         * コンストラクタ
         *
         * @param response 他のレスポンスをセット
         */
        @SuppressWarnings("CopyConstructorMissesField")
        CommandResponse(CommandResponse response) {
            this(response != null ? response.getBytes() : null);
        }

        /**
         * コンストラクタ
         *
         * @param data コマンド実行結果で戻ったバイト列をセット
         */
        CommandResponse(byte[] data) {
            if (data != null) {
                this.rawData = data;
                this.length = data[0] & 0xff;
                this.responseCode = data[1];
                this.idm = Arrays.copyOfRange(data, 2, 10);
                this.data = Arrays.copyOfRange(data, 10, data.length);
            } else {
                this.rawData = null;
                this.length = 0;
                this.responseCode = 0;
                this.idm = null;
                this.data = null;
            }
        }

        public byte[] getIDm() {
            return this.idm;
        }

        /**
         * バイト列表現を戻します
         *
         * @return byte[] このデータのバイト列表現を戻します
         */
        public byte[] getBytes() {
            return this.rawData;
        }

        public byte[] getData() {
            return data;
        }
    }

    /**
     * Read コマンドのレスポンスを抽象化したクラスを提供します
     *
     * @author Kazzz
     * @since Android API Level 9
     */

    public static class ReadResponse extends CommandResponse {
        private final int statusFlag1;
        private final int statusFlag2;
        private final int blockCount;
        private final byte[] blockData;

        /**
         * コンストラクタ
         *
         * @param response コマンド実行結果で戻ったバイト列をセット
         */
        ReadResponse(CommandResponse response) {
            super(response);
            if (this.getData() == null) {
                // Tried to read a block which doesn't exist
                this.blockCount = 0;
                this.blockData = null;
                this.statusFlag1 = 0xffff;
                this.statusFlag2 = 0xffff;
                return;
            }

            byte[] data = getData();
            this.statusFlag1 = data[0];
            this.statusFlag2 = data[1];
            if (this.statusFlag1 == 0) {
                this.blockCount = data[2];
                this.blockData = Arrays.copyOfRange(data, 3, data.length);
            } else {
                this.blockCount = 0;
                this.blockData = null;
            }
        }

        /**
         * statusFlag1を取得します
         *
         * @return int statusFlag1が戻ります
         */
        public final int getStatusFlag1() {
            return this.statusFlag1;
        }

        /**
         * statusFlag2を取得します
         *
         * @return int statusFlag2が戻ります
         */
        public int getStatusFlag2() {
            return this.statusFlag2;
        }

        /**
         * blockDataを取得します
         *
         * @return byte[] blockDataが戻ります
         */
        public byte[] getBlockData() {
            return this.blockData;
        }

        /**
         * blockCountを取得します
         *
         * @return int blockCountが戻ります
         */
        public int getBlockCount() {
            return this.blockCount;
        }
    }
}
