package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import dev.dworks.apps.anexplorer.AppFlavour;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;

public class ProWrapper extends FrameLayout {

    public ProWrapper(Context context) {
        super(context);
        init(context);
    }

    public ProWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.pro_wrapper, this, true);
        view.findViewById(R.id.action_layout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DocumentsApplication.openPurchaseActivity(getContext());
            }
        });
        setVisibility(AppFlavour.isPurchased() ? GONE : VISIBLE);
    }

}
