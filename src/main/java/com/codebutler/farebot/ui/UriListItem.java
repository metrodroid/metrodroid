package com.codebutler.farebot.ui;

import android.net.Uri;

/**
 * ListItem which supports directing to a website.
 */
public class UriListItem extends ListItem {
    private Uri mUri;

    public UriListItem(String name, String value, Uri uri) {
        super(name, value);
        this.mUri = uri;
    }

    public UriListItem(int nameResource, int valueResource, Uri uri) {
        super(nameResource, valueResource);
        this.mUri = uri;
    }

    public Uri getUri() { return mUri; }
}
