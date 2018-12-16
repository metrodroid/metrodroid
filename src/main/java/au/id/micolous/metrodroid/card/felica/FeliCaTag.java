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
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * FeliCa仕様に準拠した FeliCaタグクラスを提供します
 *
 * @author Kazzz
 * @date 2011/01/23
 * @since Android API Level 9
 */

public final class FeliCaTag {
    private static final String TAG = "FeliCaTag";
    private Tag nfcTag;
    private byte[] idm;
    private byte[] pmm;

    public FeliCaTag(Tag nfcTag) {
        this(nfcTag, null, null);
    }

    /**
     * コンストラクタ
     *
     * @param nfcTag NFCTagへの参照をセット
     * @param idm    FeliCa IDmをセット
     * @param pmm    FeliCa PMmをセット
     */
    private FeliCaTag(Tag nfcTag, byte[] idm, byte[] pmm) {
        this.nfcTag = nfcTag;
        this.idm = idm;
        this.pmm = pmm;
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
    public byte[] polling(int systemCode) throws IOException, TagLostException {
        if (this.nfcTag == null) {
            throw new IOException("tagService is null. no polling execution");
        }
        byte[]data = FeliCaLib.execute(this.nfcTag, FeliCaLib.COMMAND_POLLING
                , (byte) (systemCode >> 8)   // System code (upper byte)
                , (byte) (systemCode & 0xff) // System code (lower byte)
                , (byte) 0x01                // Request code (system code request)
                , (byte) 0x00);
        if (data != null && data.length >= 10) {
            this.idm = Arrays.copyOfRange(data, 2, 10);
        } else
            this.idm = null;
        if (data != null && data.length >= 18) {
            this.pmm = Arrays.copyOfRange(data, 10, 18);
        } else
            this.pmm = null;
        return data;
    }

    /**
     * カードデータをポーリングしてIDmを取得します
     *
     * @param systemCode 対象のシステムコードをセットします
     * @throws TagLostException if the tag went out of the field
     * @return IDm IDmが戻ります
     */
    public byte[] pollingAndGetIDm(int systemCode) throws IOException, TagLostException {
        this.polling(systemCode);
        return this.idm;
    }

    /**
     * FeliCa IDmを取得します
     *
     * @return IDm IDmが戻ります
     */
    public byte[] getIDm() {
        return this.idm;
    }

    /**
     * FeliCa PMmを取得します
     *
     * @return PMm PMmが戻ります
     */
    public byte[] getPMm() {
        return this.pmm;
    }

    /**
     * SystemCodeの一覧を取得します。
     *
     * @return SystemCode[] 検出された SystemCodeの一覧を返します。
     * @throws TagLostException if the tag went out of the field
     */
    public final List<FeliCaLib.SystemCode> getSystemCodeList() throws IOException, TagLostException {
        //request systemCode 
        byte[] retBytes = FeliCaLib.execute(this.nfcTag, FeliCaLib.COMMAND_REQUEST_SYSTEMCODE, idm);

        if (retBytes == null) {
            // No system codes were received from the card.
            return Collections.emptyList();
        }
        int num = (int) retBytes[10];
        //Log.d(TAG, "Num SystemCode: " + num);
        List<FeliCaLib.SystemCode> retCodeList = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            retCodeList.add(new FeliCaLib.SystemCode(Arrays.copyOfRange(retBytes, 11 + i * 2, 13 + i * 2)));
        }
        return retCodeList;
    }

    /**
     * Polling済みシステム領域のサービスの一覧を取得します。
     *
     * @return ServiceCode[] 検出された ServiceCodeの配列
     * @throws TagLostException if the tag went out of the field
     */
    public List<FeliCaLib.ServiceCode> getServiceCodeList() throws IOException, TagLostException {
        int index = 1; // 0番目は root areaなので1オリジンで開始する
        List<FeliCaLib.ServiceCode> serviceCodeList = new ArrayList<>();
        while (true) {
            byte[] bytes = doSearchServiceCode(index); // 1件1件 通信して聞き出します。
            if (bytes.length != 2 && bytes.length != 4)
                break; // 2 or 4 バイトじゃない場合は、とりあえず終了しておきます。正しい判定ではないかもしれません。
            if (bytes.length == 2) { // 2バイトは ServiceCode として扱っています。
                if (bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xff) break; // FFFF が終了コードのようです
                serviceCodeList.add(new FeliCaLib.ServiceCode(bytes));
            }
            index++;

            if (index > 0xffff) {
                // Invalid service code index
                break;
            }
        }
        return serviceCodeList;
    }

    /**
     * COMMAND_SEARCH_SERVICECODE を実行します。
     * 参考: http://wiki.osdev.info/index.php?PaSoRi%2FRC-S320#content_1_25
     *
     * @param index ？番目か
     * @return Response部分
     * @throws TagLostException if the tag went out of the field
     */
    protected byte[] doSearchServiceCode(int index) throws IOException, TagLostException {
        byte[] bytes = FeliCaLib.execute(this.nfcTag, FeliCaLib.COMMAND_SEARCH_SERVICECODE, idm
                , (byte) (index & 0xff), (byte) (index >> 8));
        if (bytes == null || bytes.length <= 0 || bytes[1] != (byte) 0x0b) { // 正常応答かどうか
            Log.w(TAG, "Response code is not 0x0b");
            // throw new FeliCaException("ResponseCode is not 0x0b");
            return new byte[0];
        }
        return Arrays.copyOfRange(bytes, 10, bytes.length);
    }

    /**
     * 認証不要領域のデータを読み込みます
     *
     * @param serviceCode サービスコードをセット
     * @param addr        読み込むブロックのアドレス (0オリジン)をセット
     * @return ReadResponse 読み込んだ結果が戻ります
     * @throws TagLostException if the tag went out of the field
     */
    public FeliCaLib.ReadResponse readWithoutEncryption(FeliCaLib.ServiceCode serviceCode,
                                                        byte addr) throws IOException, TagLostException {
        if (this.nfcTag == null) {
            throw new IOException("tagService is null. no read execution");
        }
        // read without encryption
        byte[] bytes = serviceCode.getBytes();
        byte[] resp = FeliCaLib.execute(this.nfcTag, FeliCaLib.COMMAND_READ_WO_ENCRYPTION, idm
                , (byte) 0x01         // サービス数
                , bytes[1]
                , bytes[0]             // サービスコード (little endian)
                , (byte) 0x01                 // 同時読み込みブロック数
                , (byte) 0x80, addr);
        return new FeliCaLib.ReadResponse(new FeliCaLib.CommandResponse (resp));
    }

}
