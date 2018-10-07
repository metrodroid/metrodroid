/*
 * DesfireCard.java
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.desfire;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.InvalidDesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.StandardDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData;
import au.id.micolous.metrodroid.transit.hsl.HSLTransitData;
import au.id.micolous.metrodroid.transit.istanbulkart.IstanbulKartTransitData;
import au.id.micolous.metrodroid.transit.myki.MykiTransitData;
import au.id.micolous.metrodroid.transit.opal.OpalTransitData;
import au.id.micolous.metrodroid.transit.orca.OrcaTransitData;
import au.id.micolous.metrodroid.transit.stub.AdelaideMetrocardStubTransitData;
import au.id.micolous.metrodroid.transit.stub.AtHopStubTransitData;
import au.id.micolous.metrodroid.transit.tfi_leap.LeapTransitData;
import au.id.micolous.metrodroid.transit.tfi_leap.LeapUnlocker;
import au.id.micolous.metrodroid.transit.trimethop.TrimetHopTransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedDesfireTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

@Root(name = "card")
public class DesfireCard extends Card {
    private static final String TAG = "DesfireCard";

    @Element(name = "manufacturing-data")
    private DesfireManufacturingData mManfData;
    @ElementList(name = "applications")
    private List<DesfireApplication> mApplications;

    private DesfireCard() { /* For XML Serializer */ }

    public DesfireCard(byte[] tagId, Calendar scannedAt, DesfireManufacturingData manfData, DesfireApplication[] apps) {
        super(CardType.MifareDesfire, tagId, scannedAt);
        mManfData = manfData;
        mApplications = Arrays.asList(apps);
    }

    /**
     * Dumps a DESFire tag in the field.
     * @param tag Tag to dump.
     * @return DesfireCard of the card contents. Returns null if an unsupported card is in the
     *         field.
     * @throws Exception On communication errors.
     */
    public static DesfireCard dumpTag(Tag tag, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        List<DesfireApplication> apps = new ArrayList<>();

        IsoDep tech = IsoDep.get(tag);

        tech.connect();

        DesfireManufacturingData manufData;
        DesfireApplication[] appsArray;

        try {
            DesfireProtocol desfireTag = new DesfireProtocol(tech);

            try {
                manufData = desfireTag.getManufacturingData();
            } catch (IllegalArgumentException e) {
                // Credit cards tend to fail at this point.
                Log.w(TAG, "Card responded with invalid response, may not be DESFire?", e);
                return null;
            }

            feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfd_reading));
            feedbackInterface.updateProgressBar(0, 1);

            int[] appIds = desfireTag.getAppList();
            int maxProgress = appIds.length;
            int progress = 0;

            CardInfo i = parseEarlyCardInfo(appIds);
            if (i != null) {
                Log.d(TAG, String.format(Locale.ENGLISH, "Early Card Info: %s", i.getName()));
                feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type, i.getName()));
                feedbackInterface.showCardType(i);
            }

            // Uncomment this to test the card type display.
            //Thread.sleep(5000);

            for (int appId : appIds) {
                feedbackInterface.updateProgressBar(progress, maxProgress);
                desfireTag.selectApp(appId);
                progress++;

                List<DesfireFile> files = new ArrayList<>();

                DesfireUnlocker unlocker = null;
                if(LeapTransitData.earlyCheck(appId))
                    unlocker = LeapUnlocker.createUnlocker(appId, manufData);
                int[] fileIds = desfireTag.getFileList();
                if (unlocker != null) {
                    fileIds = unlocker.getOrder(desfireTag, fileIds);
                }
                maxProgress += fileIds.length * (unlocker == null ? 1 : 2);
                List<DesfireAuthLog> authLog = new ArrayList<>();
                for (int fileId : fileIds) {
                    feedbackInterface.updateProgressBar(progress, maxProgress);
                    if (unlocker != null) {
                        if (i != null) {
                            feedbackInterface.updateStatusText(
                                    Utils.localizeString(R.string.mfd_unlocking, i.getName()));
                        }
                        unlocker.unlock(desfireTag, files, fileId, authLog);
                        feedbackInterface.updateProgressBar(++progress, maxProgress);
                    }

                    DesfireFileSettings settings = null;
                    try {
                        settings = desfireTag.getFileSettings(fileId);
                        byte[] data;
                        if (settings instanceof StandardDesfireFileSettings) {
                            data = desfireTag.readFile(fileId);
                        } else if (settings instanceof ValueDesfireFileSettings) {
                            data = desfireTag.getValue(fileId);
                        } else {
                            data = desfireTag.readRecord(fileId);
                        }
                        files.add(DesfireFile.create(fileId, settings, data));
                    } catch (AccessControlException ex) {
                        files.add(new UnauthorizedDesfireFile(fileId, ex.getMessage(), settings));
                    } catch (IOException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        files.add(new InvalidDesfireFile(fileId, ex.toString(), settings));
                    }
                    progress++;
                }

                DesfireFile[] filesArray = new DesfireFile[files.size()];
                files.toArray(filesArray);

                apps.add(new DesfireApplication(appId, filesArray, authLog));
            }

            appsArray = new DesfireApplication[apps.size()];
            apps.toArray(appsArray);
        } finally {
            if (tech.isConnected())
                tech.close();
        }

        return new DesfireCard(tag.getId(), GregorianCalendar.getInstance(), manufData, appsArray);
    }

    /**
     * DESFire has well-known application IDs.  If those application IDs are sufficient to detect
     * a particular type of card (or at least have a really good guess at it), then we should send
     * back a CardInfo.
     *
     * If we have no idea, then send back "null".
     *
     * Each of these checks should be really cheap to run, because this blocks further card
     * reads.
     * @param appIds An array of DESFire application IDs that are present on the card.
     * @return A CardInfo about the card, or null if we have no idea.
     */
    static CardInfo parseEarlyCardInfo(int[] appIds) {
        if (OrcaTransitData.earlyCheck(appIds))
            return OrcaTransitData.CARD_INFO;
        if (ClipperTransitData.earlyCheck(appIds))
            return ClipperTransitData.CARD_INFO;
        if (HSLTransitData.earlyCheck(appIds))
            return HSLTransitData.CARD_INFO;
        if (OpalTransitData.earlyCheck(appIds))
            return OpalTransitData.CARD_INFO;
        if (MykiTransitData.earlyCheck(appIds))
            return MykiTransitData.CARD_INFO;
        if (IstanbulKartTransitData.earlyCheck(appIds))
            return IstanbulKartTransitData.CARD_INFO;
        if (LeapTransitData.earlyCheck(appIds))
            return LeapTransitData.CARD_INFO;
        if (TrimetHopTransitData.earlyCheck(appIds))
            return TrimetHopTransitData.CARD_INFO;

        return null;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (OrcaTransitData.check(this))
            return OrcaTransitData.parseTransitIdentity(this);
        if (ClipperTransitData.check(this))
            return ClipperTransitData.parseTransitIdentity(this);
        if (HSLTransitData.check(this))
            return HSLTransitData.parseTransitIdentity(this);
        if (OpalTransitData.check(this))
            return OpalTransitData.parseTransitIdentity(this);
        if (MykiTransitData.check(this))
            return MykiTransitData.parseTransitIdentity(this);
        if (LeapTransitData.check(this))
            return LeapTransitData.parseTransitIdentity(this);

        // Stub card types go last
        if (AdelaideMetrocardStubTransitData.check(this))
            return AdelaideMetrocardStubTransitData.parseTransitIdentity(this);
        if (AtHopStubTransitData.check(this))
            return AtHopStubTransitData.parseTransitIdentity(this);
        if (IstanbulKartTransitData.check(this))
            return IstanbulKartTransitData.parseTransitIdentity(this);
        if (TrimetHopTransitData.check(this))
            return TrimetHopTransitData.parseTransitIdentity(this);

        if (UnauthorizedDesfireTransitData.check(this))
            return UnauthorizedDesfireTransitData.parseTransitIdentity(this);
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        if (OrcaTransitData.check(this))
            return new OrcaTransitData(this);
        if (ClipperTransitData.check(this))
            return new ClipperTransitData(this);
        if (HSLTransitData.check(this))
            return new HSLTransitData(this);
        if (OpalTransitData.check(this))
            return new OpalTransitData(this);
        if (MykiTransitData.check(this))
            return new MykiTransitData(this);
        if (LeapTransitData.check(this))
            return new LeapTransitData(this);

        // Stub card types go last
        if (IstanbulKartTransitData.check(this))
            return new IstanbulKartTransitData(this);
        if (TrimetHopTransitData.check(this))
            return new TrimetHopTransitData(this);
        if (AdelaideMetrocardStubTransitData.check(this))
            return new AdelaideMetrocardStubTransitData(this);
        if (AtHopStubTransitData.check(this))
            return new AtHopStubTransitData(this);

        if (UnauthorizedDesfireTransitData.check(this))
            return new UnauthorizedDesfireTransitData();
        return null;
    }

    @Override
    public List<ListItem> getManufacturingInfo() {
        List<ListItem> items = new ArrayList<>();
        DesfireManufacturingData data = getManufacturingData();
        items.add(new HeaderListItem(R.string.hardware_information));
        items.add(new ListItem("Vendor ID", Integer.toString(data.hwVendorID)));
        items.add(new ListItem("Type", Integer.toString(data.hwType)));
        items.add(new ListItem("Subtype", Integer.toString(data.hwSubType)));
        items.add(new ListItem("Major Version", Integer.toString(data.hwMajorVersion)));
        items.add(new ListItem("Minor Version", Integer.toString(data.hwMinorVersion)));
        items.add(new ListItem("Storage Size", Integer.toString(data.hwStorageSize)));
        items.add(new ListItem("Protocol", Integer.toString(data.hwProtocol)));

        items.add(new HeaderListItem(R.string.software_information));
        items.add(new ListItem("Vendor ID", Integer.toString(data.swVendorID)));
        items.add(new ListItem("Type", Integer.toString(data.swType)));
        items.add(new ListItem("Subtype", Integer.toString(data.swSubType)));
        items.add(new ListItem("Major Version", Integer.toString(data.swMajorVersion)));
        items.add(new ListItem("Minor Version", Integer.toString(data.swMinorVersion)));
        items.add(new ListItem("Storage Size", Integer.toString(data.swStorageSize)));
        items.add(new ListItem("Protocol", Integer.toString(data.swProtocol)));

        if (!MetrodroidApplication.hideCardNumbers()) {
            items.add(new HeaderListItem("General Information"));
            items.add(new ListItem("Serial Number", Integer.toHexString(data.uid)));
            items.add(new ListItem("Batch Number", Integer.toHexString(data.batchNo)));
            items.add(new ListItem("Week of Production", Integer.toHexString(data.weekProd)));
            items.add(new ListItem("Year of Production", Integer.toHexString(data.yearProd)));
        }

        return items;
    }

    public List<DesfireApplication> getApplications() {
        return mApplications;
    }

    public DesfireApplication getApplication(int appId) {
        for (DesfireApplication app : mApplications) {
            if (app.getId() == appId)
                return app;
        }
        return null;
    }

    public DesfireManufacturingData getManufacturingData() {
        return mManfData;
    }

    @Override
    public List<ListItem> getRawData() {
        List<ListItem> li = new ArrayList<>();
        for (DesfireApplication app : mApplications) {
            List<ListItem> ali = app.getRawData();
            li.add(new ListItemRecursive(
                    Utils.localizeString(R.string.application_title_format,
                            "0x" + Integer.toHexString(app.getId())),
                    null, ali));
        }
        return li;
    }
}
