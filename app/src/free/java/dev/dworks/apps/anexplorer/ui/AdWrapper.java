package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import dev.dworks.apps.anexplorer.AppPaymentFlavour;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;


/**
 * A Wrapper which wraps AdView along with loading the view aswell
 */
public class AdWrapper extends FrameLayout {

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private boolean showInterstiatial = true;

    public AdWrapper(Context context) {
        super(context);
        init(context);
    }

    public AdWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AdWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        //Ads
        if(!isTelevision()){
            LayoutInflater.from(context).inflate(R.layout.ads_wrapper, this, true);
            initAd();
            mInterstitialAd = new InterstitialAd(context);
            initInterstitialAd();
        }
    }

    public void initInterstitialAd(){
        mInterstitialAd.setAdUnitId("ca-app-pub-6407484780907805/9134520474");
        requestNewInterstitial();
    }

    public void initAd(){
        mAdView = (AdView) findViewById(R.id.adView);
        mAdView.setAdListener(adListener);
    }

    private void requestNewInterstitial() {
        if(AppPaymentFlavour.isPurchased()){
            return;
        }
        if(null != mInterstitialAd){
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }
    }

    private void showInterstitial() {
        if(AppPaymentFlavour.isPurchased()){
            return;
        }
        if(showInterstiatial && null != mInterstitialAd){
            if(mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        showInterstitial();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        showAd();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        showInterstiatial = false;
        return super.onSaveInstanceState();
    }

    private void showAd(){
        if(AppPaymentFlavour.isPurchased()){
            return;
        }
        if(isInEditMode()){
            return;
        }
        //Fixes GPS AIOB Exception
        try {
            if(null != mAdView){
                mAdView.loadAd(new AdRequest.Builder().build());
            }
        } catch (Exception e){
            CrashReportingManager.logException(e);
        }
    }

    AdListener adListener = new AdListener() {
        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            mAdView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            super.onAdFailedToLoad(errorCode);
            mAdView.setVisibility(View.GONE);
        }
    };
}
