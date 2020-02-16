package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;


import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;

import java.util.ArrayList;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorInt;

public class NativeAdViewHolder extends BaseHolder {

    public static final String BUNDLE_AD_KEY = "bundle_ad_key";
    private UnifiedNativeAdView adView;
        private int mPosition;

        public NativeAdViewHolder(DocumentsAdapter.Environment environment, Context context, ViewGroup parent) {
            this(inflateLayout(context, parent, getLayoutId(environment)));
        }

        public static int getLayoutId(DocumentsAdapter.Environment environment){
            int layoutId = R.layout.item_doc_ad_list;
            if(environment.getDisplayState().derivedMode == MODE_GRID){
                layoutId = R.layout.item_doc_ad_grid;
            }
            return layoutId;
        }

        NativeAdViewHolder(View view) {
            super(view);
            adView = view.findViewById(R.id.ad_view);

            MediaView mediaView = adView.findViewById(R.id.ad_media);
            if(null != mediaView) {
                adView.setMediaView(mediaView);
            }

            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_icon));
            adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        }

        @Override
        public void setData(Cursor cursor, int position) {
            mPosition = position;
            int index = getCursorInt(cursor, DocumentsContract.Document.COLUMN_FLAGS);
            Bundle bundle = cursor.getExtras();
            ArrayList<UnifiedNativeAd> mNativeAds = (ArrayList<UnifiedNativeAd>) bundle.getSerializable(BUNDLE_AD_KEY);
            if(null == mNativeAds || index >= mNativeAds.size()){
                return;
            }
            UnifiedNativeAd nativeAd = mNativeAds.get(index);
            if(null == nativeAd){
                return;
            }
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

            NativeAd.Image icon = nativeAd.getIcon();

            if (icon == null) {
                adView.getIconView().setVisibility(View.INVISIBLE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }

            if (nativeAd.getStarRating() == null) {
                adView.getStarRatingView().setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) adView.getStarRatingView())
                        .setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }

            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }

            adView.setNativeAd(nativeAd);
        }
    }