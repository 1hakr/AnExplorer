package dev.dworks.apps.anexplorer;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import dev.dworks.apps.anexplorer.common.ActionBarActivity;

import static dev.dworks.apps.anexplorer.misc.Utils.REWARDED_APP_UNIT_ID;

public abstract class AboutVariantFlavour extends ActionBarActivity {

    private RewardedAd rewardedAd;

    protected void initAd() {
        rewardedAd = createAndLoadRewardedAd();
    }

    public RewardedAd createAndLoadRewardedAd() {
        RewardedAd rewardedAd = new RewardedAd(this, REWARDED_APP_UNIT_ID);
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                // Ad successfully loaded.
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                // Ad failed to load.
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
        return rewardedAd;
    }

    public void showAd() {
        if (rewardedAd != null && rewardedAd.isLoaded()) {
            rewardedAd.show(this, adCallback);
        } else {
            Toast.makeText(this, "No sponsor available", Toast.LENGTH_SHORT).show();
            initAd();
        }
    }

    RewardedAdCallback adCallback = new RewardedAdCallback() {
        @Override
        public void onRewardedAdOpened() {
            // Ad opened.
        }

        @Override
        public void onRewardedAdClosed() {
            initAd();
        }

        @Override
        public void onUserEarnedReward(@NonNull RewardItem reward) {
            // User earned reward.
        }

        @Override
        public void onRewardedAdFailedToShow(int errorCode) {
            // Ad failed to display
        }
    };
}
