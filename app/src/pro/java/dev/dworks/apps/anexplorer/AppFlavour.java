package dev.dworks.apps.anexplorer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.PreferenceUtils;
import needle.Needle;
import needle.UiRelatedTask;

/**
 * Created by HaKr on 16/05/17.
 */

public abstract class AppFlavour extends Application{

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public boolean isSubscribedMonthly() {
		return false;
	}

	private String getFormattedPrice(String code, Double value){
		Currency currency = Currency.getInstance(code);
		return currency.getSymbol() + " "+ String.format("%.2f", value);
	}

	public void loadOwnedPurchasesFromGoogle() {
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

	public void purchase(Activity activity, String productId){

	}

	public static void openPurchaseActivity(Context context){

	}
}