package dev.dworks.apps.anexplorer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import dev.dworks.apps.anexplorer.misc.SystemBarTintManager;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.DocumentsActivity.getStatusBarHeight;

public class PurchaseActivity extends ActionBarActivity {

    public static final String TAG = PurchaseActivity.class.getSimpleName();
    private AsyncTask restorePurchaseAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitleTextAppearance(this, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
        if(Utils.hasKitKat() && !Utils.hasLollipop()) {
            mToolbar.setPadding(0, getStatusBarHeight(this), 0, 0);
        }
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.support_app));
        setUpDefaultStatusBar();

        initControls();
        DocumentsApplication.getInstance().initializeBilling();
    }

    private void initControls() {


        Button restoreButton = (Button) findViewById(R.id.restore_button);
        Button purchaseButton = (Button) findViewById(R.id.purchase_button);
        restoreButton.setEnabled(true);
        purchaseButton.setEnabled(true);

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (restorePurchaseAsyncTask == null || restorePurchaseAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
                    restorePurchase();
                }
            }
        });

        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DocumentsApplication.getInstance().purchase(PurchaseActivity.this, DocumentsApplication.getPurchaseId());
            }
        });
    }

    @Override
    public String getTag() {
        return TAG;
    }

    private void restorePurchase() {
        if (restorePurchaseAsyncTask != null) {
            restorePurchaseAsyncTask.cancel(false);
        }
        restorePurchaseAsyncTask = new RestorePurchaseAsyncTask(this).execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!DocumentsApplication.getInstance().handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        DocumentsApplication.getInstance().releaseBillingProcessor();
        super.onDestroy();
    }

    private static class RestorePurchaseAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<PurchaseActivity> buyActivityWeakReference;

        public RestorePurchaseAsyncTask(PurchaseActivity purchaseActivity) {
            this.buyActivityWeakReference = new WeakReference<>(purchaseActivity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PurchaseActivity purchaseActivity = buyActivityWeakReference.get();
            if (purchaseActivity != null) {
                Toast.makeText(purchaseActivity, R.string.restoring_purchase, Toast.LENGTH_SHORT).show();
            } else {
                cancel(false);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            PurchaseActivity purchaseActivity = buyActivityWeakReference.get();
            if (purchaseActivity != null) {
                DocumentsApplication.getInstance().loadOwnedPurchasesFromGoogle();
            }
            cancel(false);
            return null;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            PurchaseActivity purchaseActivity = buyActivityWeakReference.get();
            if (purchaseActivity == null || b == null) return;

            if (b) {
                DocumentsApplication.getInstance().onPurchaseHistoryRestored();
            } else {
                Toast.makeText(purchaseActivity, R.string.could_not_restore_purchase, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setUpDefaultStatusBar() {
        int color = ContextCompat.getColor(this, R.color.md_blue_500);
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(Utils.getStatusBarColor(color));
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }
}