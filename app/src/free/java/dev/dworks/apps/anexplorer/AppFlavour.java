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
import needle.Needle;
import needle.UiRelatedTask;

/**
 * Created by HaKr on 16/05/17.
 */

public abstract class AppFlavour extends Application implements BillingProcessor.IBillingHandler {
	private static final String PURCHASE_PRODUCT_ID = "purchase_product_id";
    public static final String PURCH_ID = BuildConfig.APPLICATION_ID + ".purch";
    private static final String PURCHASED = "purchased";
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
		return PreferenceUtils.getBooleanPrefs(PURCHASED);
	}

	@Override
	public void onBillingInitialized() {
		loadOwnedPurchasesFromGoogle();
		reloadSubscription();
	}
	public void loadOwnedPurchasesFromGoogle() {
		if(!isBillingSupported() || null == bp
				|| (bp != null && !bp.isInitialized())){
			return;
		}
		bp.loadOwnedPurchasesFromGoogle();
	}

	public void reloadSubscription() {
		if(!isBillingSupported() || null == bp
				|| (bp != null && !bp.isInitialized())){
			return;
		}

		Needle.onBackgroundThread().execute(new UiRelatedTask<Void>() {
			@Override
			protected Void doWork() {
				getPurchSkuDetails();
				boolean isPurchased = getBillingProcessor().isPurchased(getPurchasedProductId());
				PreferenceUtils.set(PURCHASED, isPurchased);
				return null;
			}

			@Override
			protected void thenDoUiRelatedWork(Void result) {
				//LocalBurst.getInstance().emit(BILLING_ACTION);
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
		reloadSubscription();
	}

	@Override
	public void onPurchaseHistoryRestored() {
		if (AppFlavour.isProVersion()) {
			Toast.makeText(this, R.string.restored_previous_purchase_please_restart, Toast.LENGTH_LONG).show();
			reloadSubscription();
		} else {
			Toast.makeText(this, R.string.no_purchase_found, Toast.LENGTH_SHORT).show();
		}
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
					reloadSubscription();
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
					getString(R.string.license_key), getString(R.string.merchant_id), this);
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

	public boolean isBillingSupported() {
		return BillingProcessor.isIabServiceAvailable(getApplicationContext());
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