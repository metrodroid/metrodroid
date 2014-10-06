package com.codebutler.farebot.transit.itso;

import android.os.Parcel;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import net.kazzz.felica.lib.Util;

import java.util.Arrays;
import java.util.List;

public class ItsoTransitData extends TransitData {
	private long mSerialNumber;
	private long mISSN;
	private byte[] mDirectoryBytes;
	private byte[][] mLogs;
	private byte[] mShellBytes;
	private byte[][] mSectorData;
	private String mFirstName;
	private String mLastName;
	private String mDOB;

    public static Creator<ItsoTransitData> CREATOR = new Creator<ItsoTransitData>() {
        @Override public ItsoTransitData createFromParcel(Parcel source) {
            return new ItsoTransitData(source);
        }
        @Override public ItsoTransitData[] newArray(int size) {
            return new ItsoTransitData[size];
        }
    };

	public static boolean check(Card card) {
		if ((card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x1602a0) != null)) {
			// FIXME: Need to check IIN is 633597
			// if (Utils.getHexString(data, 2, 3) != "633597") {
			return true;
		}
		// TODO: Support Mifare classic etc. here
		return false;
	}

    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0x1602a0).getFile(0x0f).getData();
            return new TransitIdentity("ITSO", Utils.getHexString(data, 5, 2));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ITSO serial", ex);
        }
    }

	public ItsoTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
        mISSN = parcel.readLong();
        mDirectoryBytes = (byte[]) parcel.readSerializable();
        mLogs = (byte[][]) parcel.readSerializable();
        mShellBytes = (byte[]) parcel.readSerializable();
        mSectorData = (byte[][]) parcel.readSerializable();
        mFirstName = parcel.readString();
        mLastName = parcel.readString();
        mDOB = parcel.readString();
	}

	public ItsoTransitData(Card card) {
		if (card instanceof DesfireCard) {
			DesfireCard desfireCard = (DesfireCard) card;

			DesfireApplication application = desfireCard.getApplication(0x1602a0);

			// See page 100 of
			// http://www.itso.org.uk/content/Specification/Spec_v2.1.4/ITSO_TS_1000-10_V2_1_4_2010-02.pdf

			mDirectoryBytes = application.getFile(0x00).getData();
			mLogs = Utils.divideArray(application.getFile(0x01).getData(), 48);
			mShellBytes = application.getFile(0xf).getData();

			// We go via hex strings because these are binary coded decimal.
			mISSN = Long.parseLong(Utils.getHexString(mShellBytes, 5, 2));
			mSerialNumber = Long.parseLong(Utils.getHexString(mShellBytes, 7, 4));

			mSectorData = new byte[14][];
			for (int sector=0; sector<13; sector++) {
				mSectorData[sector] = application.getFile(14-sector).getData();

				byte[] thisSector = mSectorData[sector];
				if (Utils.getHexString(thisSector, 1, 3).equals("41ff00")) {
					byte firstNameLength = thisSector[29];
					mFirstName = new String (thisSector, 30, (int) firstNameLength);

                    byte lastNameLength = thisSector[30 + firstNameLength];
					mLastName = new String (thisSector, 31 + firstNameLength, lastNameLength);

                    mDOB = Util.getHexString(thisSector, 7, 2) + "-"
					    + Util.getHexString(thisSector, 9, 1) + "-"
					    + Util.getHexString(thisSector, 10, 1);
				}
			}
		}
	}

	@Override public String getBalanceString() {
		return null;
	}

	@Override public String getSerialNumber() {
		String stringSerialNo = String.format("%08d", mSerialNumber);
		return String.format("%04d", mISSN) + " " + stringSerialNo.substring(0, 4) + " " + stringSerialNo.substring(4);
	}

	@Override public Trip[] getTrips() {
		ItsoTrip[] trips = new ItsoTrip[mLogs.length];

		int tripCount = 0;

		for(byte[] logEntry : mLogs) {
			//Util.logEntry;
			long minutes = (0xFF & logEntry[4]) * 65536 + (0xFF & logEntry[5]) * 256 + (0xFF & logEntry[6]);
			if (minutes > 0) {
                long startTime = minutes * 60 + 852076800L; // 852076800L is the timestamp of 1st Jan 1997
                int agency = (0xFF & logEntry[21]) * 256 + (0xFF & logEntry[22]);
                int route = (0xFF & logEntry[23]) * 256 + (0xFF & logEntry[24]);
                trips[tripCount] = new ItsoTrip(startTime, agency, route);
				tripCount++;
			}
		}
		trips = Arrays.copyOfRange(trips, 0, tripCount);
		Arrays.sort(trips);
		return trips;
	}

	@Override public Refill[] getRefills() {
		return null;
	}

	@Override public String getCardName() {
		return "ITSO";
	}

	@Override public Subscription[] getSubscriptions() {
		return null;
	}

    @Override public List<ListItem> getInfo() {
        return null;
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber);
        parcel.writeLong(mISSN);
        parcel.writeSerializable(mDirectoryBytes);
        parcel.writeSerializable(mLogs);
        parcel.writeSerializable(mShellBytes);
        parcel.writeSerializable(mSectorData);
        parcel.writeString(mFirstName);
        parcel.writeString(mLastName);
        parcel.writeString(mDOB);
    }
}
