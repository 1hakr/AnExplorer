package dev.dworks.apps.anexplorer.cast;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.Arrays;
import java.util.List;


public class CastOptionsProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {
        CastOptions customCastOptions = Casty.customCastOptions;
        if(customCastOptions == null) {
            List<String> buttonActions = createButtonActions();
            int[] compatButtonAction = { 1, 3 };

            NotificationOptions notificationOptions = new NotificationOptions.Builder()
                    .setActions(buttonActions, compatButtonAction)
                    .setTargetActivityClassName(ExpandedControlsActivity.class.getName())
                    .build();

            CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                    .setNotificationOptions(notificationOptions)
                    .setExpandedControllerActivityClassName(ExpandedControlsActivity.class.getName())
                    .build();

            return new CastOptions.Builder()
                    .setReceiverApplicationId(Casty.receiverId)
                    .setCastMediaOptions(mediaOptions)
                    .build();
        } else {
            return customCastOptions;
        }
    }

    private List<String> createButtonActions() {
        return Arrays.asList(MediaIntentReceiver.ACTION_REWIND,
                MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                MediaIntentReceiver.ACTION_FORWARD,
                MediaIntentReceiver.ACTION_STOP_CASTING);
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
