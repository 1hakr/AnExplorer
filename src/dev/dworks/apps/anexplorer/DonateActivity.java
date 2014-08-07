package dev.dworks.apps.anexplorer;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;

import dev.dworks.apps.anexplorer.R;

public class DonateActivity extends Activity implements BillingProcessor.IBillingHandler{

    private static final String PRODUCT_ID = "dev.dworks.apps.anexplorer.donate";
    private BillingProcessor mBP;
    private boolean readyToPurchase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mBP = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr6Angrd3B/aHdtT4K1/4o/5BE0cS7wmRhxHau0l1I7dJnLqYGy2JeLkAt/woN3S/7aM+5vDRWTtiQbtjajEfQbsIzyukEknuCiu132tY03m1l7W5PbywtmZv682SbM00yteIAtOdPC1Ydclym36z4Yceu/BW/6QRR1Sj9mJo4i7KGFD6YHHXu64IfVxOCrQ4r80aSabF1M4fKMfKgRtdSu1wCgJitLqTLOaCa3rE199Fwz6usWfwio6Au2R1vKJnugVrZQkdbhZkJRByZdEnvdDGoimQYtIQ220SI9V9VLTpvaQ3eXaxQlYpYI7Bwq8xQO0a3kjxnOxML5IzXCEytwIDAQAB", this);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DonateFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.donate, menu);
        return true;
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
    public void onProductPurchased(String productId) {
        mBP.consumePurchase(productId);
        Toast.makeText(this, "Thank You. We Love You!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        for(String sku : mBP.listOwnedProducts())
            Log.d("Donate", "Owned Managed Product: " + sku);
    }

    @Override
    public void onBillingError(int errorCode, Throwable throwable) {

    }

    @Override
    public void onBillingInitialized() {
        readyToPurchase = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mBP.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void donate(int index) {
        if(null == mBP || !readyToPurchase){
            return;
        }
        mBP.purchase(PRODUCT_ID + index);
    }

    public boolean isDonated(int index){
        if(null == mBP || !readyToPurchase){
            return false;
        }
        return mBP.isPurchased(PRODUCT_ID + index);
    }

    @Override
    public void onDestroy() {
        if (mBP != null) {
            mBP.release();
        }
        super.onDestroy();
    }


    public static class DonateFragment extends Fragment {

        public DonateFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_donate, container, false);
            return rootView;
        }
    }
}