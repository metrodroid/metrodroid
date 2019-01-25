package au.id.micolous.metrodroid.card.classic;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import au.id.micolous.metrodroid.key.*;
import au.id.micolous.metrodroid.multi.Localizer;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import static au.id.micolous.metrodroid.key.CardKeysEmbedKt.CardKeysEmbed;

public final class ClassicAndroidReader {
    private final static String TAG = "ClassicAndroidReader";

    private ClassicAndroidReader() {}

    public static CardKeysFromFiles getKeyRetrieverEmbed(Context context) {
        return CardKeysEmbed(context, "keys");
    }
    private static final Set<String> devicesMifareWorks = new HashSet<>();
    private static final Set<String> devicesMifareNotWorks = new HashSet<>();

    static {
        devicesMifareWorks.add("Pixel 2");
        devicesMifareWorks.add("Find7");
    }

    private static Boolean mMifareClassicSupport = null;

    private static void detectMfcSupport() {
        if (devicesMifareNotWorks.contains(android.os.Build.MODEL)) {
            mMifareClassicSupport = false;
            return;
        }

        if (devicesMifareWorks.contains(android.os.Build.MODEL)) {
            mMifareClassicSupport = true;
            return;
        }

        // TODO: Some devices report MIFARE Classic support, when they actually don't have it.
        //
        // Detecting based on libraries and device nodes doesn't work great either. There's edge
        // cases, and it's still vulnerable to vendors doing silly things.

        // Fallback: Look for com.nxp.mifare feature.
        mMifareClassicSupport = MetrodroidApplication.getInstance().getPackageManager().hasSystemFeature("com.nxp.mifare");
        //noinspection StringConcatenation
        Log.d(TAG, "Falling back to com.nxp.mifare feature detection "
                + (mMifareClassicSupport ? "(found)" : "(missing)"));
    }

    public static boolean getMifareClassicSupport() {
        try {
            if (mMifareClassicSupport == null)
                detectMfcSupport();
        } catch (Exception e) {
            Log.w(TAG, "Detecting nfc support failed", e);
        }

        return mMifareClassicSupport;
    }

    public static CardKeysMerged getKeyRetriever(Context context) {
        return new CardKeysMerged(Arrays.asList(
                getKeyRetrieverEmbed(context),
                new CardKeysDB(context)
        ));
    }

    public static ClassicCard dumpTag(ImmutableByteArray tagId, Tag tag, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        feedbackInterface.updateStatusText(Localizer.INSTANCE.localizeString(R.string.mfc_reading));
        feedbackInterface.showCardType(null);

        MifareClassic tech = null;

        try {
            try {
                tech = MifareClassic.get(tag);
            } catch (NullPointerException e) {
                Log.d(TAG, "Working around broken Android NFC on HTC devices (and others)", e);
                tech = MifareClassic.get(patchTag(tag));
            }
            tech.connect();

            ClassicCardTechAndroid techWrapper = new ClassicCardTechAndroid(tech, tagId);

            CardKeysMerged keyRetriever = getKeyRetriever(MetrodroidApplication.getInstance());

            return ClassicReader.INSTANCE.readCard(
                    keyRetriever, techWrapper, feedbackInterface);
        } finally {
            if (tech != null && tech.isConnected()) {
                tech.close();
            }
        }
    }


    /**
     * Patch the broken Tag object of HTC One (m7/m8) devices with Android 5.x.
     * <p>
     * Also observed on Galaxy Nexus running Cyanogenmod 13.
     * <p>
     * "It seems, the reason of this bug is TechExtras of NfcA is null.
     * However, TechList contains MIFAREClassic." -- bildin.
     * <p>
     * This patch will fix this. For more information please refer to
     * https://github.com/ikarus23/MIFAREClassicTool/issues/52
     * <p>
     * This patch was provided by bildin (https://github.com/bildin).
     *
     * @param tag The broken tag.
     * @return The fixed tag.
     */
    private static Tag patchTag(Tag tag) {
        if (tag == null) {
            return null;
        }

        @NonNls String[] sTechList = tag.getTechList();
        Parcel oldParcel;
        Parcel newParcel;
        oldParcel = Parcel.obtain();
        tag.writeToParcel(oldParcel, 0);
        oldParcel.setDataPosition(0);

        int len = oldParcel.readInt();
        byte[] id = new byte[0];
        if (len >= 0) {
            id = new byte[len];
            oldParcel.readByteArray(id);
        }
        int[] oldTechList = new int[oldParcel.readInt()];
        oldParcel.readIntArray(oldTechList);
        Bundle[] oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oldParcel.readInt();
        int isMock = oldParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oldParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oldParcel.recycle();

        int nfcaIdx = -1;
        int mcIdx = -1;
        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx].equals(NfcA.class.getName())) {
                nfcaIdx = idx;
            } else if (sTechList[idx].equals(MifareClassic.class.getName())) {
                mcIdx = idx;
            }
        }

        if (nfcaIdx >= 0 && mcIdx >= 0 && oldTechExtras[mcIdx] == null) {
            oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx];
        } else {
            return tag;
        }

        newParcel = Parcel.obtain();
        newParcel.writeInt(id.length);
        newParcel.writeByteArray(id);
        newParcel.writeInt(oldTechList.length);
        newParcel.writeIntArray(oldTechList);
        newParcel.writeTypedArray(oldTechExtras, 0);
        newParcel.writeInt(serviceHandle);
        newParcel.writeInt(isMock);
        if (isMock == 0) {
            newParcel.writeStrongBinder(tagService);
        }
        newParcel.setDataPosition(0);
        Tag newTag = Tag.CREATOR.createFromParcel(newParcel);
        newParcel.recycle();

        return newTag;
    }
}
