package au.id.micolous.metrodroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.unnamed.b.atv.model.TreeNode;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;

public class ListItemRecursive extends ListItem {
    private final List<ListItem> mSubTree;

    public ListItemRecursive(String text1, String text2, List<ListItem> subTree) {
        super(text1, text2);
        mSubTree = subTree;
    }

    public ListItemRecursive(int text1Res, String text2, List<ListItem> subTree) {
        super(text1Res, text2);
        mSubTree = subTree;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup root, boolean attachToRoot) {
        View view = inflater.inflate(R.layout.list_recursive, root, attachToRoot);
        adjustView(view);
        return view;
    }

    public List<ListItem> getSubTree() {
        return mSubTree;
    }

    public static ListItem collapsedValue(String name, String value) {
        return collapsedValue(name, null, value);
    }

    public static ListItem collapsedValue(int nameRes, String value) {
        return new ListItemRecursive(nameRes, null,
                value != null ? Collections.singletonList(new ListItem(null, value)) : null);
    }

    public static ListItem collapsedValue(String title, String subtitle, String value) {
        return new ListItemRecursive(title, subtitle,
                value != null ? Collections.singletonList(new ListItem(null, value)) : null);
    }
}
