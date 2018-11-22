package dev.dworks.apps.anexplorer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;

/**
 * Created by HaKr on 16/05/17.
 */

public abstract class AppFlavour extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void initializeBilling() {
    }

    public static boolean isPurchased() {
        return true;
    }

    public void loadOwnedPurchasesFromGoogle() {
    }

    public void onPurchaseHistoryRestored() {

    }

    public void reloadSubscription() {
    }

    private void getPurchSkuDetails() {

    }

    public static String getPurchaseId(){
        return "";
    }

    public static String getPurchasedProductId(){
        return "";
    }

    private static boolean isProVersion() {
        return false;
    }

    public boolean isBillingSupported() {
        return false;
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    public void releaseBillingProcessor() {
    }

    public void purchase(Activity activity, String productId){

    }

    public static void openPurchaseActivity(Context context){

    }
}