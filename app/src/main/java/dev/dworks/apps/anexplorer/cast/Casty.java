package dev.dworks.apps.anexplorer.cast;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.MediaQueue;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.mediarouter.app.MediaRouteButton;
import dev.dworks.apps.anexplorer.R;

/**
 * Core class of Casty. It manages buttons/widgets and gives access to the media player.
 */
public class Casty implements CastyPlayer.OnMediaLoadedListener {
    private final static String TAG = "Casty";
    static String receiverId = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
    static CastOptions customCastOptions;

    private SessionManagerListener<CastSession> sessionManagerListener;
    private OnConnectChangeListener onConnectChangeListener;
    private OnCastSessionUpdatedListener onCastSessionUpdatedListener;

    private CastSession castSession;
    private CastyPlayer castyPlayer;
    private Activity activity;
    private IntroductoryOverlay introductionOverlay;

    /**
     * Sets the custom receiver ID. Should be used in the {@link Application} class.
     *
     * @param receiverId the custom receiver ID, e.g. Styled Media Receiver - with custom logo and background
     */
    public static void configure(@NonNull String receiverId) {
        Casty.receiverId = receiverId;
    }

    /**
     * Sets the custom CastOptions, should be used in the {@link Application} class.
     *
     * @param castOptions the custom CastOptions object, must include a receiver ID
     */
    public static void configure(@NonNull CastOptions castOptions) {
        Casty.customCastOptions = castOptions;
    }

    /**
     * Creates the Casty object.
     *
     * @param activity {@link Activity} in which Casty object is created
     * @return the Casty object
     */
    public static Casty create(@NonNull Activity activity) {
        int playServicesState = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
        if (playServicesState == ConnectionResult.SUCCESS) {
            try {
                return new Casty(activity);
            } catch (Exception e){}
        }
        Log.w(Casty.TAG, "Google Play services not found on a device, Casty won't work.");
        return new CastyNoOp();
    }

    public static boolean isAvailable(@NonNull Activity activity){
        return ConnectionResult.SUCCESS == GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
    }

    //Needed for NoOp instance
    Casty() {
        //no-op
    }

    private Casty(@NonNull Activity activity) {
        this.activity = activity;
        sessionManagerListener = createSessionManagerListener();
        castyPlayer = new CastyPlayer(this);
        CastContext.getSharedInstance(activity).addCastStateListener(createCastStateListener());
        activity.getApplication().registerActivityLifecycleCallbacks(createActivityCallbacks());
    }

    /**
     * Gives access to {@link CastyPlayer}, which allows to control the media files.
     *
     * @return the instance of {@link CastyPlayer}
     */
    public CastyPlayer getPlayer() {
        return castyPlayer;
    }

    /**
     * Checks if a Google Cast device is connected.
     *
     * @return true if a Google Cast is connected, false otherwise
     */
    public boolean isConnected() {
        return castSession != null;
    }

    /**
     * Adds the discovery menu item on a toolbar and creates Introduction Overlay
     * Should be used in {@link Activity#onCreateOptionsMenu(Menu)}.
     *
     * @param menu Menu in which MenuItem should be added
     */
    @UiThread
    public void addMediaRouteMenuItem(@NonNull Menu menu) {
        activity.getMenuInflater().inflate(R.menu.menu_cast, menu);
        setUpMediaRouteMenuItem(menu);
        MenuItem menuItem = menu.findItem(R.id.casty_media_route_menu_item);
        introductionOverlay = createIntroductionOverlay(menuItem);
    }

    /**
     * Makes {@link MediaRouteButton} react to discovery events.
     * Must be run on UiThread.
     *
     * @param mediaRouteButton Button to be set up
     */
    @UiThread
    public void setUpMediaRouteButton(@NonNull MediaRouteButton mediaRouteButton) {
        CastButtonFactory.setUpMediaRouteButton(activity, mediaRouteButton);
    }

    /**
     * Adds the Mini Controller at the bottom of Activity's layout.
     * Must be run on UiThread.
     *
     * @return the Casty instance
     */
    @UiThread
    public Casty withMiniController() {
        addMiniController();
        return this;
    }

    /**
     * Adds the Mini Controller at the bottom of Activity's layout
     * Must be run on UiThread.
     */
    @UiThread
    public void addMiniController() {
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        View rootView = contentView.getChildAt(0);
        LinearLayout linearLayout = new LinearLayout(activity);
        LinearLayout.LayoutParams linearLayoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(linearLayoutParams);

        contentView.removeView(rootView);

        ViewGroup.LayoutParams oldRootParams = rootView.getLayoutParams();
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(oldRootParams.width, 0, 1f);
        rootView.setLayoutParams(rootParams);

        linearLayout.addView(rootView);
        activity.getLayoutInflater().inflate(R.layout.fragment_mini_controller, linearLayout, true);
        activity.setContentView(linearLayout);
    }

    /**
     * Sets {@link OnConnectChangeListener}
     *
     * @param onConnectChangeListener Connect change callback
     */
    public void setOnConnectChangeListener(@Nullable OnConnectChangeListener onConnectChangeListener) {
        this.onConnectChangeListener = onConnectChangeListener;
    }

    /**
     * Sets {@link OnCastSessionUpdatedListener}
     *
     * @param onCastSessionUpdatedListener Cast session updated callback
     */
    public void setOnCastSessionUpdatedListener(@Nullable OnCastSessionUpdatedListener onCastSessionUpdatedListener) {
        this.onCastSessionUpdatedListener = onCastSessionUpdatedListener;
    }

    private void setUpMediaRouteMenuItem(Menu menu) {
        CastButtonFactory.setUpMediaRouteButton(activity, menu, R.id.casty_media_route_menu_item);
    }

    @NonNull
    private CastStateListener createCastStateListener() {
        return new CastStateListener() {
            @Override
            public void onCastStateChanged(int state) {
                if (state != CastState.NO_DEVICES_AVAILABLE && introductionOverlay != null) {
                    showIntroductionOverlay();
                }
            }
        };
    }

    private void showIntroductionOverlay() {
        introductionOverlay.show();
    }

    private SessionManagerListener<CastSession> createSessionManagerListener() {
        return new SessionManagerListener<CastSession>() {
            @Override
            public void onSessionStarted(CastSession castSession, String s) {
                activity.invalidateOptionsMenu();
                onConnected(castSession);
            }

            @Override
            public void onSessionEnded(CastSession castSession, int i) {
                activity.invalidateOptionsMenu();
                onDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession castSession, boolean b) {
                activity.invalidateOptionsMenu();
                onConnected(castSession);
            }

            @Override
            public void onSessionStarting(CastSession castSession) {
                //no-op
            }

            @Override
            public void onSessionStartFailed(CastSession castSession, int i) {
                //no-op
            }

            @Override
            public void onSessionEnding(CastSession castSession) {
                //no-op
            }

            @Override
            public void onSessionResuming(CastSession castSession, String s) {
                //no-op
            }

            @Override
            public void onSessionResumeFailed(CastSession castSession, int i) {
                //no-op
            }

            @Override
            public void onSessionSuspended(CastSession castSession, int i) {
                //no-op
            }
        };
    }

    private void onConnected(CastSession castSession) {
        this.castSession = castSession;
        castyPlayer.setRemoteMediaClient(castSession.getRemoteMediaClient());
        if (onConnectChangeListener != null) onConnectChangeListener.onConnected();
        if (onCastSessionUpdatedListener != null) onCastSessionUpdatedListener.onCastSessionUpdated(castSession);
    }

    private void onDisconnected() {
        this.castSession = null;
        if (onConnectChangeListener != null) onConnectChangeListener.onDisconnected();
        if (onCastSessionUpdatedListener != null) onCastSessionUpdatedListener.onCastSessionUpdated(null);
    }

    private Application.ActivityLifecycleCallbacks createActivityCallbacks() {
        return new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                //no-op
            }

            @Override
            public void onActivityStarted(Activity activity) {
                //no-op
            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (Casty.this.activity == activity) {
                    handleCurrentCastSession();
                    registerSessionManagerListener();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (Casty.this.activity == activity) unregisterSessionManagerListener();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                //no-op
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                //no-op
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (Casty.this.activity == activity) {
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }
        };
    }

    private IntroductoryOverlay createIntroductionOverlay(MenuItem menuItem) {
        return new IntroductoryOverlay.Builder(activity, menuItem)
                .setTitleText(R.string.casty_introduction_text)
                .setSingleTime()
                .build();
    }

    private void registerSessionManagerListener() {
        CastContext.getSharedInstance(activity).getSessionManager()
                .addSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    private void unregisterSessionManagerListener() {
        CastContext.getSharedInstance(activity).getSessionManager()
                .removeSessionManagerListener(sessionManagerListener, CastSession.class);
    }

    private void handleCurrentCastSession() {
        CastSession newCastSession = CastContext.getSharedInstance(activity).getSessionManager().getCurrentCastSession();
        if (castSession == null) {
            if (newCastSession != null) {
                onConnected(newCastSession);
            }
        } else {
            if (newCastSession == null) {
                onDisconnected();
            } else if (newCastSession != castSession) {
                onConnected(newCastSession);
            }
        }
    }

    @Override
    public void onMediaLoaded() {
        //startExpandedControlsActivity();
    }

    private void startExpandedControlsActivity() {
        Intent intent = new Intent(activity, ExpandedControlsActivity.class);
        activity.startActivity(intent);
    }

    public MediaQueue getMediaQueue() {
        RemoteMediaClient remoteClient = castyPlayer.getRemoteMediaClient();
        if(null != remoteClient){
            return remoteClient.getMediaQueue();
        }
        return null;
    }

    public RemoteMediaClient getRemoteMediaClient() {
        return castyPlayer.getRemoteMediaClient();
    }

    public CastSession getCastSession() {
        return castSession;
    }

    public interface OnConnectChangeListener {
        void onConnected();

        void onDisconnected();
    }

    public interface OnCastSessionUpdatedListener {
        void onCastSessionUpdated(CastSession castSession);
    }
}
