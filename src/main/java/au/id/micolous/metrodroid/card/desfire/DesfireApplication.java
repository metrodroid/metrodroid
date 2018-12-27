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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.ui.ListItem;

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

    public DesfireApplication(int id, DesfireFile[] files) {
	this(id, files, null);
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
            ali.add(file.getRawData());
        }
        if (mAuthLog != null) {
            for (DesfireAuthLog authLog : mAuthLog) {
                ali.add(authLog.getRawData());
            }
        }
        return ali;
    }
}


