package dev.dworks.apps.anexplorer.cast;

import android.content.Context;

import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteButton;

public class CastActionProvider extends MediaRouteActionProvider {

    public CastActionProvider(Context context) {
        super(context);
    }

    @Override
    public MediaRouteButton onCreateMediaRouteButton() {
        return new CastButton(getContext());
    }
}
