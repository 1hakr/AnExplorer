package dev.dworks.apps.anexplorer.misc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.R.id.action_close;
import static dev.dworks.apps.anexplorer.R.id.action_feedback;
import static dev.dworks.apps.anexplorer.R.id.action_rate;
import static dev.dworks.apps.anexplorer.R.id.action_support;
import static dev.dworks.apps.anexplorer.misc.Utils.openFeedback;
import static dev.dworks.apps.anexplorer.misc.Utils.openPlaystore;

/**
 * Created by nicolas on 06/03/14.
 */
public class AppRate implements View.OnClickListener{

    private static final String PREFS_NAME = "app_rate_prefs";
    private final String KEY_COUNT = "count";
    private final String KEY_CLICKED = "clicked";
    private Activity activity;
    private ViewGroup viewGroup;
    private String text;
    private int initialLaunchCount = 10;
    private RetryPolicy policy = RetryPolicy.EXPONENTIAL;
    private OnShowListener onShowListener;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private int delay = 0;
    private ViewGroup mainView;

    private AppRate(Activity activity) {
        this.activity = activity;
    }

    private AppRate(Activity activity, ViewGroup viewGroup) {
        this.activity = activity;
        this.viewGroup = viewGroup;
    }

    public static AppRate with(Activity activity) {
        AppRate instance = new AppRate(activity);
        instance.text = "Enjoying the app? Spread the word!";//activity.getString(R.string.dra_rate_app);
        instance.settings = activity.getSharedPreferences(PREFS_NAME, 0);
        instance.editor = instance.settings.edit();
        return instance;
    }

    public static AppRate with(Activity activity, ViewGroup viewGroup) {
        AppRate instance = new AppRate(activity, viewGroup);
        instance.text = "Enjoying the app? Spread the word!";//activity.getString(R.string.dra_rate_app);
        instance.settings = activity.getSharedPreferences(PREFS_NAME, 0);
        instance.editor = instance.settings.edit();
        return instance;
    }

    /**
     * Text to be displayed in the viewGroup
     *
     * @param text text to be displayed
     * @return the {@link AppRate} instance
     */
    public AppRate text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Text to be displayed in the viewGroup
     *
     * @param textRes text ressource to be displayed
     * @return the {@link AppRate} instance
     */
    public AppRate text(int textRes) {
        this.text = activity.getString(textRes);
        return this;
    }

    /**
     * Initial times {@link AppRate} has to be called before the viewGroup is shown
     *
     * @param initialLaunchCount times count
     * @return the {@link AppRate} instance
     */
    public AppRate initialLaunchCount(int initialLaunchCount) {
        this.initialLaunchCount = initialLaunchCount;
        return this;
    }

    /**
     * Policy to use to show the {@link AppRate} again
     *
     * @param policy the {@link RetryPolicy} to be used
     * @return the {@link AppRate} instance
     */
    public AppRate retryPolicy(RetryPolicy policy) {
        this.policy = policy;
        return this;
    }

    /**
     * Listener used to get {@link AppRate} lifecycle
     *
     * @param onShowListener the listener
     * @return the {@link AppRate} instance
     */
    public AppRate listener(OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
        return this;
    }

    /**
     * Delay the {@link AppRate showing time}
     * @param delay the delay in ms
     * @return the {@link AppRate} instance
     */
    public AppRate delay(int delay) {
        this.delay = delay;
        return this;
    }


    /**
     * Check and show if showing the viewGroup is needed
     */
    public void checkAndShow() {
        incrementViews();

        boolean clicked = settings.getBoolean(KEY_CLICKED, false);
        if (clicked) return;
        int count = settings.getInt(KEY_COUNT, 0);
        if (count == initialLaunchCount) {
            showAppRate();
        } else if (policy == RetryPolicy.INCREMENTAL && count % initialLaunchCount == 0) {
            showAppRate();
        }else if (policy == RetryPolicy.EXPONENTIAL && count % initialLaunchCount == 0 && isPowerOfTwo(count / initialLaunchCount)) {
            showAppRate();
        }
    }

    /**
     * Reset the count to start over
     */
    public void reset() {
        editor.putInt(KEY_COUNT, 0);
        editor.apply();
    }

    /**
     * Will force the {@link AppRate} to show
     */
    public void forceShow() {
        showAppRate();
    }

    private void incrementViews() {

        editor.putInt(KEY_COUNT, settings.getInt(KEY_COUNT, 0) + 1);
        editor.apply();
    }

    private void showAppRate() {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(null != viewGroup){
            mainView = (ViewGroup) inflater.inflate(R.layout.layout_app_feedback, viewGroup, false);
        }
        else{
            mainView = (ViewGroup) inflater.inflate(R.layout.layout_app_feedback, null);
        }

        ImageView icon = (ImageView) mainView.findViewById(android.R.id.icon);
        ImageView action_close = (ImageView) mainView.findViewById(R.id.action_close);
        Button action_rate = (Button) mainView.findViewById(R.id.action_rate);
        Button action_feedback = (Button) mainView.findViewById(R.id.action_feedback);
        Button action_support = (Button) mainView.findViewById(R.id.action_support);

        boolean hideSupport = DocumentsApplication.isPurchased();
        action_support.setVisibility(hideSupport ? View.GONE : View.VISIBLE);

        int color = SettingsActivity.getAccentColor();
        icon.setImageDrawable(IconUtils.applyTint(activity, R.drawable.ic_support, color));
        action_rate.setTextColor(color);
        action_support.setTextColor(color);
        action_feedback.setTextColor(color);

        action_close.setOnClickListener(this);
        action_rate.setOnClickListener(this);
        action_support.setOnClickListener(this);
        action_feedback.setOnClickListener(this);

        if (delay > 0) {
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    displayViews(mainView);
                }
            }, delay);
        } else {
            displayViews(mainView);
        }

    }

    private void hideAllViews(final ViewGroup mainView) {
        Animation hideAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(null != viewGroup){
                    viewGroup.setVisibility(View.GONE);
                    viewGroup.removeAllViews();
                }
                else {
                    mainView.removeAllViews();
                }

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mainView.startAnimation(hideAnimation);
    }

    private void displayViews(ViewGroup mainView) {
        if(null != viewGroup){
            viewGroup.setVisibility(View.VISIBLE);
            viewGroup.addView(mainView);
        }
        else{
            LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            activity.addContentView(mainView, params);
        }

        Animation fadeInAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
        mainView.startAnimation(fadeInAnimation);

        if (onShowListener != null) onShowListener.onRateAppShowing();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case action_close:
                hideAllViews(mainView);
                if (onShowListener != null)onShowListener.onRateAppDismissed();
                break;
            case action_rate:
                openPlaystore(activity);
                if (onShowListener != null)onShowListener.onRateAppClicked();
                hideAllViews(mainView);
                editor.putBoolean(KEY_CLICKED, true);
                editor.apply();
                break;
            case action_feedback:
                openFeedback(activity);
                hideAllViews(mainView);
                editor.putBoolean(KEY_CLICKED, true);
                editor.apply();
                break;

            case action_support:
                DocumentsApplication.openPurchaseActivity(activity);
                hideAllViews(mainView);
                break;
        }
    }

    public interface OnShowListener {
        void onRateAppShowing();

        void onRateAppDismissed();

        void onRateAppClicked();
    }
    
    public enum  RetryPolicy {
        /**
         * Will retry each time initial count has been triggered
         * Ex: if initial is set to 3, it will be shown on the 3rd, 6th, 9th, ... times
         */
        INCREMENTAL,
        /**
         * Will retry exponentially to be less intrusive
         * Ex: if initial is set to 3, it will be shown on the 3rd, 6th, 12th, ... times
         */
        EXPONENTIAL,
        /**
         * Will never retry
         */
        NONE
    }
    
    /**
     * Convert a size in dp to a size in pixels
     * @param context the {@link android.content.Context} to be used
     * @param dpi size in dp
     * @return the size in pixels
     */
    public static int convertDPItoPixels(Context context, int dpi) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpi * scale + 0.5f);
    }

    public static boolean isPowerOfTwo(int x)    {
        return (x & (x - 1)) == 0;
    }
}