/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * <p>
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
     * @throws FeliCaException コマンドの発行に失敗した場合にスローされます
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
        final byte[] systemCode;

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
         * @return
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
        final byte[] serviceCode;

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
        protected final byte[] rawData;
        protected final int length;      //全体のデータ長 (FeliCaには無い)
        protected final byte responseCode;//コマンドレスポンスコード)
        protected final byte[] idm;          //FeliCa IDm
        protected final byte[] data;      //コマンドデータ

        /**
         * コンストラクタ
         *
         * @param response 他のレスポンスをセット
         */
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
        final int statusFlag1;
        final int statusFlag2;
        final int blockCount;
        final byte[] blockData;

        /**
         * コンストラクタ
         *
         * @param response コマンド実行結果で戻ったバイト列をセット
         */
        ReadResponse(CommandResponse response) {
            super(response);
            if (this.data == null) {
                // Tried to read a block which doesn't exist
                this.blockCount = 0;
                this.blockData = null;
                this.statusFlag1 = 0xffff;
                this.statusFlag2 = 0xffff;
                return;
            }

            this.statusFlag1 = this.data[0];
            this.statusFlag2 = this.data[1];
            if (this.getStatusFlag1() == 0) {
                this.blockCount = this.data[2];
                this.blockData = Arrays.copyOfRange(this.data, 3, data.length);
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
        public int getStatusFlag1() {
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
