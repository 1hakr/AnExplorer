package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

public class NativeAdViewHolder extends BaseHolder {

    public NativeAdViewHolder(DocumentsAdapter.Environment environment, Context context, ViewGroup parent) {
        this(inflateLayout(context, parent, getLayoutId(environment)));
    }

    public static int getLayoutId(DocumentsAdapter.Environment environment){
        return 0;
    }

    NativeAdViewHolder(View view) {
        super(view);
    }

    @Override
    public void setData(Cursor cursor, int position) {
    }
}