package dev.dworks.apps.anexplorer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.Constants;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.util.concurrent.ConcurrentHashMap;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.PreferenceUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import io.fabric.sdk.android.services.common.BackgroundPriorityRunnable;
import needle.Needle;

/**
 * Created by HaKr on 16/05/17.
 */

public abstract class AppPaymentFlavour extends Application implements BillingProcessor.IBillingHandler {
	private static final String PURCHASE_PRODUCT_ID = "purchase_product_id";
    public static final String PURCH_ID = BuildConfig.APPLICATION_ID + ".purch";
    public static final String PURCHASED = "purchased";
    private static final int IAP_ID_CODE = 1;

	private BillingProcessor bp;
	private ConcurrentHashMap<String, SkuDetails> skuDetails = new ConcurrentHashMap<>();
	private String currentProductId = "";

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public void initializeBilling() {
		getBillingProcessor();
		AnalyticsManager.setProperty("IsPurchased", String.valueOf(isPurchased()));
	}

	public static boolean isPurchased() {
		return BuildConfig.DEBUG || Utils.isProVersion() || PreferenceUtils.getBooleanPrefs(PURCHASED);
	}

	@Override
	public void onBillingInitialized() {
		loadOwnedPurchasesFromGoogle();
		reloadPurchase();
	}
	public void loadOwnedPurchasesFromGoogle() {
		if(!isBillingSupported() || null == bp
				|| (bp != null && !bp.isInitialized())){
			return;
		}
		bp.loadOwnedPurchasesFromGoogle();
	}

	public void reloadPurchase() {
		if(!isBillingSupported() || null == bp
				|| (bp != null && !bp.isInitialized())){
			return;
		}

		Needle.onBackgroundThread().execute(new BackgroundPriorityRunnable() {
			@Override
			protected void onRun() {
				getPurchSkuDetails();
				boolean isPurchased = getBillingProcessor().isPurchased(getPurchasedProductId());
				PreferenceUtils.set(PURCHASED, isPurchased);
			}
		});
	}

	private void getPurchSkuDetails() {
		SkuDetails details = getBillingProcessor().getPurchaseListingDetails(getPurchaseId());
		if(null == details){
			return;
		}
		skuDetails.put(details.productId, details);
	}

	public static String getPurchaseId(){
		return PURCH_ID+ ".pro" + IAP_ID_CODE;
	}

	public static String getPurchasedProductId(){
		String productId = PreferenceUtils.getStringPrefs(PURCHASE_PRODUCT_ID);
		return !TextUtils.isEmpty(productId) ? productId : getPurchaseId();
	}


	@Override
	public void onProductPurchased(String productId, TransactionDetails details) {
		Toast.makeText(getApplicationContext(), R.string.thank_you, Toast.LENGTH_SHORT).show();
		PreferenceUtils.set(PURCHASE_PRODUCT_ID, productId);
		PreferenceUtils.set(PURCHASED, true);
		reloadPurchase();
	}

	@Override
	public void onPurchaseHistoryRestored() {
		reloadPurchase();
	}

	private static boolean isProVersion() {
		return isPurchased();
	}

	@Override
	public void onBillingError(int errorCode, Throwable throwable) {
		switch (errorCode){
			case Constants.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
				if(!TextUtils.isEmpty(currentProductId)) {
					PreferenceUtils.set(PURCHASE_PRODUCT_ID, currentProductId);
					reloadPurchase();
				}
				break;
			case Constants.BILLING_RESPONSE_RESULT_USER_CANCELED:
			case Constants.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE:
				break;
			default:
				try {
					Toast.makeText(getApplicationContext(), "Something went wrong! error code="+ errorCode
							+ " , Contact Developer", Toast.LENGTH_LONG).show();

				} catch (Exception e){ }
				CrashReportingManager.logException(new Exception(throwable));
				break;
		}
	}

	public BillingProcessor getBillingProcessor() {
		if(!isBillingSupported()){
			return null;
		}
		if(null == bp) {
			bp = BillingProcessor.newBillingProcessor(this,
					BuildConfig.PLAYSTORE_LICENSE_KEY, BuildConfig.MERCHANT_ID, this);
		}
		if(!bp.isInitialized()) {
			bp.initialize();
		}
		return bp;
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		return null != bp && bp.handleActivityResult(requestCode, resultCode, data);
	}

	public void releaseBillingProcessor() {
		if(null != bp){
			bp.release();
		}
	}

	public static boolean isBillingSupported() {
		return BillingProcessor.isIabServiceAvailable(DocumentsApplication.getInstance().getApplicationContext());
	}

	public void purchase(Activity activity, String productId){
		if(isBillingSupported()) {
			currentProductId = productId;
			getBillingProcessor().subscribe(activity, productId);
		} else {
			Toast.makeText(activity, "Billing not supported", Toast.LENGTH_SHORT).show();
		}
	}

	public static void openPurchaseActivity(Context context){
		context.startActivity(new Intent(context, PurchaseActivity.class));
	}
}