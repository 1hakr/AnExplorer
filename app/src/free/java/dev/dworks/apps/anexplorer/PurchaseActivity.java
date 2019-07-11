package dev.dworks.apps.anexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.github.lykmapipo.localburst.LocalBurst;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.widget.Toolbar;
import dev.dworks.apps.anexplorer.common.ActionBarActivity;
import dev.dworks.apps.anexplorer.misc.SystemBarTintManager;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import needle.Needle;
import needle.UiRelatedTask;

import static dev.dworks.apps.anexplorer.AppPaymentFlavour.BILLING_ACTION;
import static dev.dworks.apps.anexplorer.DocumentsActivity.getStatusBarHeight;

public class PurchaseActivity extends ActionBarActivity implements LocalBurst.OnBroadcastListener {

    public static final String TAG = PurchaseActivity.class.getSimpleName();
    protected LocalBurst broadcast;
    private Button purchaseButton;
    private String purchaseText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mToolbar.setTitleTextAppearance(this, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
        if(Utils.hasKitKat() && !Utils.hasLollipop()) {
            mToolbar.setPadding(0, getStatusBarHeight(this), 0, 0);
        }
        int color = SettingsActivity.getPrimaryColor();
        mToolbar.setBackgroundColor(color);
        appBarLayout.setBackgroundColor(color);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.support_app));
        setUpDefaultStatusBar();

        purchaseText = getString(R.string.purchase);
        broadcast = LocalBurst.getInstance();

        initControls();
        DocumentsApplication.getInstance().initializeBilling();
    }

    private void initControls() {

        Button restoreButton = (Button) findViewById(R.id.restore_button);
        purchaseButton = (Button) findViewById(R.id.purchase_button);
        restoreButton.setEnabled(true);
        purchaseButton.setEnabled(true);

        if(!AppPaymentFlavour.isBillingSupported()){
            restoreButton.setVisibility(View.GONE);
        }

        purchaseButton.setTextColor(SettingsActivity.getAccentColor());
        updatePrice();
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restorePurchase();
            }
        });

        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!AppPaymentFlavour.isBillingSupported()){
                    Intent intentMarketAll = new Intent("android.intent.action.VIEW");
                    intentMarketAll.setData(Utils.getAppProStoreUri());
                    if(Utils.isIntentAvailable(PurchaseActivity.this, intentMarketAll)) {
                        startActivity(intentMarketAll);
                    }
                } else {
                    if(DocumentsApplication.isPurchased()){
                        Utils.showSnackBar(PurchaseActivity.this, getString(R.string.thank_you));
                        finish();
                    } else {
                        DocumentsApplication.getInstance().purchase(PurchaseActivity.this, DocumentsApplication.getPurchaseId());
                    }
                }
            }
        });
    }

    private void updatePrice() {
        if(!AppPaymentFlavour.isBillingSupported()){
            return;
        }
        String purchaseId = DocumentsApplication.getPurchaseId();
        String price = DocumentsApplication.getInstance().getPurchasePrice(purchaseId);
        if (!TextUtils.isEmpty(price)) {
            purchaseButton.setText(purchaseText + " with " + price);
        }

    }

    @Override
    public String getTag() {
        return TAG;
    }

    private void restorePurchase() {
        Utils.showSnackBar(this, getString(R.string.restoring_purchase));
        Needle.onBackgroundThread().execute(new UiRelatedTask<Boolean>(){
            @Override
            protected Boolean doWork() {
                DocumentsApplication.getInstance().loadOwnedPurchasesFromGoogle();
                DocumentsApplication.getInstance().onPurchaseHistoryRestored();
                return true;
            }

            @Override
            protected void thenDoUiRelatedWork(Boolean aBoolean) {
                onPurchaseRestored();
            }
        });
    }

    public void onPurchaseRestored(){
        if (DocumentsApplication.isPurchased()) {
            Utils.showSnackBar(this, getString(R.string.restored_previous_purchase_please_restart));
            finish();
        } else {
            Utils.showSnackBar(this, getString(R.string.could_not_restore_purchase));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!DocumentsApplication.getInstance().handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        broadcast.removeListeners(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcast.on(BILLING_ACTION, this);
        broadcast.on(LocalBurst.DEFAULT_ACTION, this);
    }

    @Override
    public void onDestroy() {
        DocumentsApplication.getInstance().releaseBillingProcessor();
        broadcast.removeListeners(this);
        super.onDestroy();
    }

    public void setUpDefaultStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor());
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(Utils.getStatusBarColor(color));
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

    @Override
    public void onBroadcast(String action, Bundle extras) {
        updatePrice();
    }
}