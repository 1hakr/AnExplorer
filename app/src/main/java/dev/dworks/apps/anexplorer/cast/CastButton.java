package dev.dworks.apps.anexplorer.cast;

import android.content.Context;
import android.util.AttributeSet;

import androidx.mediarouter.app.MediaRouteButton;
import dev.dworks.apps.anexplorer.AppFlavour;

public class CastButton extends MediaRouteButton {
    public CastButton(Context context) {
        super(context);
    }

    public CastButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CastButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean showDialog() {
        if(AppFlavour.isPurchased()) {
            return super.showDialog();
        } else {
            AppFlavour.openPurchaseActivity(getContext());
            return true;
        }
    }
}
