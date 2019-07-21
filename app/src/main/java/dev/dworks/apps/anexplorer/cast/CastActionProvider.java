package dev.dworks.apps.anexplorer.cast;

import android.content.Context;
import android.view.View;

import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteButton;

public class CastActionProvider extends MediaRouteActionProvider {

    public CastActionProvider(Context context) {
        super(context);
    }

    @Override
    public MediaRouteButton onCreateMediaRouteButton() {
        try {
            return new CastButton(getContext());
        } catch (Exception e) {
            try {
                return new MediaRouteButton(getContext());
            } catch (Exception e1){
                return null;
            }
        }
    }

    @Override
    public View onCreateActionView() {
        try {
            return super.onCreateActionView();
        } catch (Exception ignored){}
        return null;
    }

    @Override
    public boolean isVisible() {
        if (getMediaRouteButton() == null){
            return false;
        }
        return super.isVisible();
    }
}
