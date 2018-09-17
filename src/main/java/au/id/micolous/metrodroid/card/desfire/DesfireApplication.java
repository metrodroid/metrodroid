/*
 * DesfireApplication.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.InvalidDesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.StandardDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
@Root(name = "application")
public class DesfireApplication {
    @Attribute(name = "id")
    private String mId;
    @ElementList(name = "files")
    private List<DesfireFile> mFiles;
    @SuppressWarnings("unused")
    @ElementList(name = "auth-log", required = false)
    private List<DesfireAuthLog> mAuthLog;

    private DesfireApplication() { /* For XML Serializer */ }

    public DesfireApplication(int id, DesfireFile[] files, List<DesfireAuthLog> authLog) {
        mId = String.valueOf(id);
        mFiles = Arrays.asList(files);
        mAuthLog = authLog;
    }

    public int getId() {
        return Integer.parseInt(mId);
    }

    public List<DesfireFile> getFiles() {
        return mFiles;
    }

    public DesfireFile getFile(int fileId) {
        for (DesfireFile file : mFiles) {
            if (file.getId() == fileId)
                return file;
        }
        return null;
    }

    public List<ListItem> getRawData() {
        List<ListItem> ali = new ArrayList<>();

        for (DesfireFile file : getFiles()) {
            if ((file instanceof InvalidDesfireFile) && !(file instanceof UnauthorizedDesfireFile)) {
                ali.add(new ListItem(Utils.localizeString(R.string.invalid_file_title_format,
                        "0x" + Integer.toHexString(file.getId()),
                        ((InvalidDesfireFile) file).getErrorMessage()), null));
                continue;
            }

            String title = Utils.localizeString(R.string.file_title_format,
                    "0x" + Integer.toHexString(file.getId()));
            String subtitle;

            if (file instanceof UnauthorizedDesfireFile) {
                title = Utils.localizeString(R.string.unauthorized_file_title_format,
                        "0x" + Integer.toHexString(file.getId()));
            }

            if (file.getFileSettings() instanceof StandardDesfireFileSettings) {
                StandardDesfireFileSettings fileSettings = (StandardDesfireFileSettings) file.getFileSettings();
                subtitle = Utils.localizePlural(R.plurals.desfire_standard_format,
                        fileSettings.getFileSize(),
                        Utils.localizeString(fileSettings.getFileTypeString()),
                        fileSettings.getFileSize());
            } else if (file.getFileSettings() instanceof RecordDesfireFileSettings) {
                RecordDesfireFileSettings fileSettings = (RecordDesfireFileSettings) file.getFileSettings();
                subtitle = Utils.localizePlural(R.plurals.desfire_record_format,
                        fileSettings.getCurRecords(),
                        Utils.localizeString(fileSettings.getFileTypeString()),
                        fileSettings.getCurRecords(),
                        fileSettings.getMaxRecords(),
                        fileSettings.getRecordSize());
            } else if (file.getFileSettings() instanceof ValueDesfireFileSettings) {
                ValueDesfireFileSettings fileSettings = (ValueDesfireFileSettings) file.getFileSettings();

                subtitle = Utils.localizeString(R.string.desfire_value_format,
                        Utils.localizeString(fileSettings.getFileTypeString()),
                        fileSettings.getLowerLimit(),
                        fileSettings.getUpperLimit(),
                        fileSettings.getLimitedCreditValue(),
                        Utils.localizeString(fileSettings.getLimitedCreditEnabled() ? R.string.enabled : R.string.disabled));
            } else {
                subtitle = Utils.localizeString(R.string.desfire_unknown_file);
            }

            String data = null;
            if (!(file instanceof UnauthorizedDesfireFile))
                data = Utils.getHexString(file.getData());
            ali.add(ListItemRecursive.collapsedValue(title, subtitle, data));
        }
        return ali;
    }
}


