/*
 * LeapUnlocker.java
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

package au.id.micolous.metrodroid.transit.tfi_leap;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import au.id.micolous.farebot.BuildConfig;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireAuthLog;
import au.id.micolous.metrodroid.card.desfire.DesfireUnlocker;
import au.id.micolous.metrodroid.card.desfire.DesfireManufacturingData;
import au.id.micolous.metrodroid.card.desfire.DesfireProtocol;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.proto.Leap;

public class LeapUnlocker implements DesfireUnlocker {
    private final static String LEAP_API_URL = "https://tnfc.leapcard.ie//ReadCard/V0";
    private static final String TAG = "LeapUnlocker";

    private final int mApplicationId;
    private final DesfireManufacturingData mManufData;
    private boolean mUnlocked1f;
    private boolean mUnlockedRest;
    private String mSessionId;
    private byte []mConfirmation;
    private Leap.LeapMessage mReply1;

    private LeapUnlocker(int applicationId, DesfireManufacturingData manufData) {
        mApplicationId = applicationId;
        mManufData = manufData;
    }

    private static Leap.LeapMessage communicate(Leap.LeapMessage in) throws IOException {
        URL url = new URL(LEAP_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", null);
        //noinspection StringConcatenation
        conn.setRequestProperty("User-Agent", "Metrodroid/" + BuildConfig.VERSION_NAME);
        OutputStream send = conn.getOutputStream();
        //noinspection StringConcatenation
        Log.d(TAG, "Sending " + in.toString());
        in.writeTo(send);
        InputStream recv = conn.getInputStream();
        Leap.LeapMessage reply = Leap.LeapMessage.parseFrom(recv);
        //noinspection StringConcatenation
        Log.d(TAG, "Received " + reply.toString());
        conn.disconnect();
        return reply;
    }

    public static LeapUnlocker createUnlocker(int applicationId, DesfireManufacturingData manufData) {
        final boolean retrieveKeys = MetrodroidApplication.retrieveLeapKeys();
        if (!retrieveKeys) {
            Log.d(TAG, "Retrieving Leap keys not enabled");
            return null;
        }
        Log.d(TAG, "Attempting unlock");
        return new LeapUnlocker(applicationId, manufData);
    }

    @Override
    public int[] getOrder(DesfireProtocol desfireTag, int[] fileIds) {
        int skip = 0;
        for (int fileId : fileIds) {
            if (fileId == 1 || fileId == 0x1f)
                skip++;
        }
        int ret[] = new int[fileIds.length-skip+2];
        ret[0] = 1;
        ret[1] = 0x1f;
        int j = 2;
        for (int fileId : fileIds) {
            if (fileId == 1 || fileId == 0x1f)
                continue;
            ret[j++] = fileId;
        }

        return ret;
    }

    private static DesfireFile getFile(List<DesfireFile> files, int fileId) {
        for (DesfireFile file : files) {
            if (file.getId() == fileId)
                return file;
        }
        return null;
    }

    private DesfireAuthLog unlock1f(DesfireProtocol desfireTag, List<DesfireFile> files) throws Exception {
        if (mUnlocked1f)
            return null;

        ByteString ze = ByteString.copyFrom(new byte[] {0});
        ByteString af = ByteString.copyFrom(new byte[] {DesfireProtocol.ADDITIONAL_FRAME});

        DesfireFile file1Desc = getFile(files, 1);
        if (file1Desc == null) {
            Log.e(TAG, "File 1 not found");
            return null;
        }
        byte[] file1 = file1Desc.getData().getDataCopy();
        byte[] challenge = desfireTag.sendUnlock(0x0d);

        Leap.LeapMessage request1 = Leap.LeapMessage.newBuilder()
                .setApplicationId(mApplicationId)
                .setSessionId(UUID.randomUUID().toString())
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(ByteString.copyFrom(new byte[]{
                                DesfireProtocol.GET_MANUFACTURING_DATA
                        }))
                        .setResponse(ze.concat(ByteString.copyFrom(mManufData.getRaw().getDataCopy())))
                )
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(ByteString.copyFrom(new byte[]{
                                DesfireProtocol.READ_DATA, 1, 0, 0, 0, 0x20, 0, 0
                        }))
                        .setResponse(ze.concat(ByteString.copyFrom(file1)))
                )
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(ByteString.copyFrom(new byte[]{
                                DesfireProtocol.UNLOCK, 0x0d
                        }))
                        .setResponse(af.concat(ByteString.copyFrom(challenge)))
                )
                .addKeyvalues(
                        Leap.LeapKeyValue.newBuilder()
                                .setKey("ASYNC_READS")
                                .setValue("true"))
                .build();
        Leap.LeapMessage reply1 = communicate(request1);
        if (reply1.getCmds(0).getQuery().byteAt(0) != DesfireProtocol.ADDITIONAL_FRAME) {
            Log.e(TAG, "CMD0 is not AF");
            return null;
        }
        byte[] response = reply1.getCmds(0).getQuery().substring(1).toByteArray();
        mConfirmation = desfireTag.sendAdditionalFrame(response);

        mSessionId = reply1.getSessionId();
        mReply1 = reply1;
        mUnlocked1f = true;
        return new DesfireAuthLog(0x0d, challenge, response, mConfirmation);
    }

    private DesfireAuthLog unlockRest(DesfireProtocol desfireTag, List<DesfireFile> files) throws Exception {
        if (mUnlockedRest)
            return null;

        ByteString ze = ByteString.copyFrom(new byte[] {0});
        ByteString af = ByteString.copyFrom(new byte[] {DesfireProtocol.ADDITIONAL_FRAME});
        DesfireFile file1fDesc = getFile(files, 0x1f);
        if (file1fDesc == null) {
            Log.e(TAG, "File 1f not found");
            return null;
        }

        byte[] file1f = file1fDesc.getData().getDataCopy();

        Leap.LeapMessage request2 = Leap.LeapMessage.newBuilder()
                .setApplicationId(mApplicationId)
                .setSessionId(mSessionId)
                .setStage("UPDATE_AUTHENTICATE_1")
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(mReply1.getCmds(0).getQuery())
                        .setResponse(ze.concat(ByteString.copyFrom(mConfirmation)))
                        .setExpectedResponse(ByteString.copyFrom(new byte[]{0}))
                )
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(mReply1.getCmds(1).getQuery())
                        .setResponse(ze.concat(ByteString.copyFrom(file1f)))
                )
                .addKeyvalues(
                        Leap.LeapKeyValue.newBuilder()
                                .setKey("ASYNC_READS")
                                .setValue("true"))
                .build();
        Leap.LeapMessage reply2 = communicate(request2);
        byte challenge[] = desfireTag.sendUnlock(0x03);
        Leap.LeapMessage request3 = Leap.LeapMessage.newBuilder()
                .setApplicationId(mApplicationId)
                .setSessionId(mSessionId)
                .setStage("UPDATE_AUTHENTICATE_2")
                .addCmds(Leap.LeapDesFireCommand.newBuilder()
                        .setQuery(reply2.getCmds(0).getQuery())
                        .setResponse(af.concat(ByteString.copyFrom(challenge)))
                        .setExpectedResponse(af)
                )
                .addKeyvalues(
                        Leap.LeapKeyValue.newBuilder()
                                .setKey("ASYNC_READS")
                                .setValue("true"))
                .build();
        Leap.LeapMessage reply3 = communicate(request3);
        if (reply3.getCmds(0).getQuery().byteAt(0) != DesfireProtocol.ADDITIONAL_FRAME) {
            Log.e(TAG, "CMD0 is not AF");
            return null;
        }
        byte[] response = reply3.getCmds(0).getQuery().substring(1).toByteArray();
        byte[] confirm = desfireTag.sendAdditionalFrame(response);
        mUnlockedRest = true;
        return new DesfireAuthLog(0x03, challenge, response, confirm);
    }

    @Override
    public void unlock(DesfireProtocol desfireTag, List<DesfireFile> files, int fileId, List<DesfireAuthLog> authLog) {
        DesfireAuthLog cur = null;
        switch (fileId) {
            case 1:
                return;
            case 0x1f:
                try {
                    cur = unlock1f(desfireTag, files);
                } catch (Exception e) {
                    Log.e(TAG, "unlock failed");
                    e.printStackTrace();
                }
                break;
            default:
                try {
                    cur = unlockRest(desfireTag, files);
                } catch (Exception e) {
                    Log.e(TAG, "unlock failed");
                    e.printStackTrace();
                }
                break;
        }
        if (cur != null)
            authLog.add(cur);
    }
}
