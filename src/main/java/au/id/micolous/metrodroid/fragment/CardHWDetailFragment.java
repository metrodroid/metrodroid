/*
 * CardHWDetailActivity.java
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.fragment;

import android.app.ListFragment;
import android.os.Bundle;

import com.neovisionaries.i18n.CountryCode;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoCard;
import au.id.micolous.metrodroid.card.calypso.CalypsoData;
import au.id.micolous.metrodroid.card.calypso.CalypsoFile;
import au.id.micolous.metrodroid.card.calypso.CalypsoRecord;
import au.id.micolous.metrodroid.card.cepas.CEPASCard;
import au.id.micolous.metrodroid.card.cepas.CEPASPurse;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireManufacturingData;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Serializer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import au.id.micolous.metrodroid.MetrodroidApplication;

public class CardHWDetailFragment extends ListFragment {
    private Card mCard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));

        List<ListItem> items = new ArrayList<>();

        if (mCard.getCardType() == CardType.MifareDesfire) {
            DesfireManufacturingData data = ((DesfireCard) mCard).getManufacturingData();
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

            items.add(new HeaderListItem("General Information"));
            items.add(new ListItem("Serial Number", Integer.toString(data.uid)));
            items.add(new ListItem("Batch Number", Integer.toString(data.batchNo)));
            items.add(new ListItem("Week of Production", Integer.toString(data.weekProd)));
            items.add(new ListItem("Year of Production", Integer.toString(data.yearProd)));

        } else if (mCard.getCardType() == CardType.CEPAS) {
            CEPASCard card = (CEPASCard) mCard;

            // FIXME: What about other purses?
            CEPASPurse purse = card.getPurse(3);

            items.add(new HeaderListItem(R.string.cepas_purse_info));

            if (!purse.isValid()) {
                if (purse.getErrorMessage() != null && !purse.getErrorMessage().equals("")) {
                    items.add(new ListItem(R.string.error, purse.getErrorMessage()));
                } else {
                    items.add(new ListItem(R.string.error, R.string.unknown));
                }
            } else {
                items.add(new ListItem(R.string.cepas_version, Byte.toString(purse.getCepasVersion())));
                items.add(new ListItem(R.string.cepas_purse_id, Integer.toString(purse.getId())));
                items.add(new ListItem(R.string.cepas_purse_status, Byte.toString(purse.getPurseStatus())));
                items.add(new ListItem(R.string.cepas_purse_balance, NumberFormat.getCurrencyInstance(Locale.US).format(purse.getPurseBalance() / 100.0)));

                items.add(new ListItem(R.string.cepas_purse_creation_date,
                        Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(purse.getPurseCreationDate()))));
                items.add(new ListItem(R.string.cepas_expiry_date,
                        Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(purse.getPurseExpiryDate()))));
                items.add(new ListItem(R.string.cepas_autoload_amount, Integer.toString(purse.getAutoLoadAmount())));
                items.add(new ListItem("CAN", Utils.getHexString(purse.getCAN(), "<Error>")));
                items.add(new ListItem("CSN", Utils.getHexString(purse.getCSN(), "<Error>")));

                items.add(new HeaderListItem(R.string.cepas_last_txn_info));
                items.add(new ListItem("TRP", Integer.toString(purse.getLastTransactionTRP())));
                items.add(new ListItem("Credit TRP", Integer.toString(purse.getLastCreditTransactionTRP())));
                items.add(new ListItem(R.string.cepas_credit_header, Utils.getHexString(purse.getLastCreditTransactionHeader(), "<Error>")));
                items.add(new ListItem(R.string.cepas_debit_options, Byte.toString(purse.getLastTransactionDebitOptionsByte())));

                items.add(new HeaderListItem(R.string.cepas_other_purse_info));
                items.add(new ListItem(R.string.cepas_logfile_record_count, Byte.toString(purse.getLogfileRecordCount())));
                items.add(new ListItem(R.string.cepas_issuer_data_length, Integer.toString(purse.getIssuerDataLength())));
                items.add(new ListItem(R.string.cepas_issuer_data, Utils.getHexString(purse.getIssuerSpecificData(), "<Error>")));
            }
        } else if (mCard.getCardType() == CardType.FeliCa) {
            FelicaCard card = (FelicaCard) mCard;
            items.add(new ListItem(R.string.felica_idm, Utils.getHexString(card.getIDm().getBytes(), "err")));
            items.add(new ListItem(R.string.felica_pmm, Utils.getHexString(card.getPMm().getBytes(), "err")));
        } else if (mCard.getCardType() == CardType.Calypso) {
            CalypsoCard card = (CalypsoCard) mCard;
            CalypsoFile iccFile = card.getFile(CalypsoCard.File.ICC);
            CalypsoRecord iccRecord = null;
            if (iccFile != null) {
                iccRecord = iccFile.getRecord(1);
            }

            if (iccRecord != null) {
                // https://github.com/zoobab/mobib-extractor/blob/master/MOBIB-Extractor.py#L324
                byte[] data = iccRecord.getData();
                int countryCode = 0;

                // The country code is a ISO 3166-1 numeric in base16. ie: bytes(0x02,0x40) = 240
                try {
                    countryCode = Integer.parseInt(Utils.getHexString(data, 20, 2), 10);
                } catch (NumberFormatException ignored) {}

                // This shows a country name if it's known, or "unknown (number)" if not.
                String countryName;
                if (countryCode > 0) {
                    countryName = CountryCode.getByCode(countryCode).toLocale().getDisplayCountry();
                } else {
                    countryName = Utils.localizeString(R.string.unknown_format, countryCode);
                }

                CalypsoData.Manufacturer manufacturer = CalypsoData.Manufacturer.get(data[22]);
                String manufacturerHex = "0x" + Integer.toHexString((int)data[22] & 0xff);
                String manufacturerName;
                if (manufacturer != null) {
                    manufacturerName = String.format(Locale.ENGLISH, "%s (%s)",
                            Utils.localizeString(manufacturer.getCompanyName()),
                            manufacturerHex);
                } else {
                    manufacturerName = Utils.localizeString(R.string.unknown_format,
                            manufacturerHex);
                }

                GregorianCalendar manufactureDate = new GregorianCalendar(CalypsoData.TIME_ZONE);
                manufactureDate.setTimeInMillis(CalypsoData.MANUFACTURE_EPOCH.getTimeInMillis());
                manufactureDate.add(Calendar.DATE, Utils.byteArrayToInt(data, 25, 2));

                items.add(new HeaderListItem("ICC"));
                items.add(new ListItem(R.string.calypso_serial_number, Utils.getHexString(data, 12, 8)));
                items.add(new ListItem(R.string.calypso_manufacture_country, countryName));
                items.add(new ListItem(R.string.calypso_manufacturer, manufacturerName));
                items.add(new ListItem(R.string.calypso_manufacture_date, Utils.longDateFormat(manufactureDate)));
            }
        }

        setListAdapter(new ListItemAdapter(getActivity(), items));
    }


}
