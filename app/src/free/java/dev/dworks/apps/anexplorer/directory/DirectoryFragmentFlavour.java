package dev.dworks.apps.anexplorer.directory;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.util.ArrayList;

import dev.dworks.apps.anexplorer.common.RecyclerFragment;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.RootCursorWrapper;
import dev.dworks.apps.anexplorer.model.DirectoryResult;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.directory.NativeAdViewHolder.BUNDLE_AD_KEY;
import static dev.dworks.apps.anexplorer.misc.Utils.NATIVE_APP_UNIT_ID;
import static dev.dworks.apps.anexplorer.misc.Utils.NATIVE_BIG_APP_UNIT_ID;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorInt;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorLong;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorString;

public abstract class DirectoryFragmentFlavour extends RecyclerFragment {
    public static final int AD_POSITION = 5;
    private AdLoader adLoader;
    private ArrayList<UnifiedNativeAd> mNativeAds = new ArrayList<>();

    public void loadNativeAds(final DirectoryResult result) {
        int cursorCount = result.cursor != null ? result.cursor.getCount() : 0;
        if(cursorCount <= 5){
            showData(result);
            return;
        }
        String appUnitId = result.mode == MODE_GRID ? NATIVE_BIG_APP_UNIT_ID : NATIVE_APP_UNIT_ID;
        AdLoader.Builder builder = new AdLoader.Builder(getActivity(), appUnitId);
        mNativeAds = new ArrayList<>();
        adLoader = builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
            @Override
            public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                mNativeAds.add(unifiedNativeAd);
                if (null != adLoader && !adLoader.isLoading()) {
                    insertNativeAds(result);
                }
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                if (null != adLoader && !adLoader.isLoading()) {
                    insertNativeAds(result);
                }
            }
        }).build();

        boolean dontLoadAds = null == adLoader;
        if (dontLoadAds){
            showData(result);
            return;
        }

        int numberOfAds = cursorCount / AD_POSITION;
        adLoader.loadAds(new AdRequest.Builder().build(), numberOfAds);
    }

    private void insertNativeAds(DirectoryResult result) {
        int cursorCount = result.cursor != null ? result.cursor.getCount() : 0;
        int adsCount = mNativeAds.size();
        if (adsCount <= 0 || cursorCount <= AD_POSITION) {
            return;
        }
        int offset = (cursorCount / mNativeAds.size()) + 1;
        int index = 0;
        MatrixCursor matrixCursor = new MatrixCursor(result.cursor.getColumnNames());
        Bundle bundle = new Bundle();
        bundle.putSerializable(BUNDLE_AD_KEY, mNativeAds);
        matrixCursor.respond(bundle);
        result.cursor.moveToPosition(-1);
        for (int i = 0; i < (cursorCount + adsCount); i++) {
            if (i % offset == 0) {
                final MatrixCursor.RowBuilder row = matrixCursor.newRow();
                row.add(RootCursorWrapper.COLUMN_AUTHORITY, "");
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, "");
                row.add(DocumentsContract.Document.COLUMN_FLAGS, index);
                index++;
            } else {
                result.cursor.moveToNext();
                String authority =  getCursorString(result.cursor, RootCursorWrapper.COLUMN_AUTHORITY);
                if(TextUtils.isEmpty(authority)){
                    continue;
                }
                final MatrixCursor.RowBuilder row = matrixCursor.newRow();
                row.add(RootCursorWrapper.COLUMN_AUTHORITY, authority);
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_MIME_TYPE));
                row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, getCursorLong(result.cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED));
                row.add(DocumentsContract.Document.COLUMN_FLAGS, getCursorInt(result.cursor, DocumentsContract.Document.COLUMN_FLAGS));
                row.add(DocumentsContract.Document.COLUMN_SUMMARY, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_SUMMARY));
                row.add(DocumentsContract.Document.COLUMN_SIZE, getCursorLong(result.cursor, DocumentsContract.Document.COLUMN_SIZE));
                row.add(DocumentsContract.Document.COLUMN_ICON, getCursorInt(result.cursor, DocumentsContract.Document.COLUMN_ICON));
                row.add(DocumentsContract.Document.COLUMN_PATH, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_PATH));
            }
        }
        adLoader = null;
        result.cursor = matrixCursor;
        showData(result);
    }

    public abstract void showData(DirectoryResult result);
}
