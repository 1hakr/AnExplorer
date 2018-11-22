package dev.dworks.apps.anexplorer.directory;

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