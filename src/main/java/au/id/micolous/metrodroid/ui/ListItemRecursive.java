package au.id.micolous.metrodroid.ui;

import android.support.annotation.StringRes;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;

public class ListItemRecursive extends ListItem {
    @Nullable
    private final List<ListItem> mSubTree;

    public ListItemRecursive(String text1, String text2,
                             @Nullable List<ListItem> subTree) {
        super(text1, text2);
        mSubTree = subTree;
    }

    public ListItemRecursive(@StringRes int text1Res, String text2,
                             @Nullable List<ListItem> subTree) {
        super(text1Res, text2);
        mSubTree = subTree;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup root, boolean attachToRoot) {
        View view = inflater.inflate(R.layout.list_recursive, root, attachToRoot);
        adjustView(view);
        return view;
    }

    @Nullable
    public List<ListItem> getSubTree() {
        if (mSubTree == null)
            return null;
        return Collections.unmodifiableList(mSubTree);
    }

    public static ListItem collapsedValue(String name, Spanned value) {
        return collapsedValue(name, null, value);
    }

    public static ListItem collapsedValue(int nameRes, Spanned value) {
        return new ListItemRecursive(nameRes, null,
                value != null ? Collections.singletonList(new ListItem(null, value)) : null);
    }

    public static ListItem collapsedValue(String title, String subtitle, Spanned value) {
        return new ListItemRecursive(title, subtitle,
                value != null ? Collections.singletonList(new ListItem(null, value)) : null);
    }
}
