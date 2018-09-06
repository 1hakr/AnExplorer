package dev.dworks.apps.anexplorer.directory;

import android.view.View;
import android.view.ViewGroup;

public abstract class Footer {
    protected int mIcon;
    protected String mMessage;
    protected DocumentsAdapter.Environment mEnv;
    private final int mItemViewType;

    public Footer(int itemViewType) {
        mItemViewType = itemViewType;
    }

    public int getItemViewType() {
        return mItemViewType;
    }

}