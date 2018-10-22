package dev.dworks.apps.anexplorer.common;

import android.app.Dialog;
import android.content.Context;

public class DialogBuilder extends DialogCommonBuilder {

    public DialogBuilder(Context context) {
        super(context);
    }

    @Override
    public void showDialog() {
        show();
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public Dialog create() {
        return super.create();
    }
}
