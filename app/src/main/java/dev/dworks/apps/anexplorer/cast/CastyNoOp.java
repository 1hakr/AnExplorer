package dev.dworks.apps.anexplorer.cast;

import android.view.Menu;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.MediaQueue;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.app.MediaRouteButton;

class CastyNoOp extends Casty {
    private CastyPlayer castyPlayer;

    CastyNoOp() {
        castyPlayer = new CastyPlayerNoOp();
    }

    @Override
    public CastyPlayer getPlayer() {
        return castyPlayer;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void addMediaRouteMenuItem(@NonNull Menu menu) {
        //no-op
    }

    @Override
    public void setUpMediaRouteButton(@NonNull MediaRouteButton mediaRouteButton) {
        //no-op
    }

    @Override
    public Casty withMiniController() {
        return this;
    }

    @Override
    public void addMiniController() {
        //no-op
    }

    @Override
    public void setOnConnectChangeListener(@Nullable Casty.OnConnectChangeListener onConnectChangeListener) {
        //no-op
    }

    @Override
    public void setOnCastSessionUpdatedListener(@Nullable Casty.OnCastSessionUpdatedListener onCastSessionUpdatedListener) {
        //no-op
    }

    @Override
    public CastSession getCastSession() {
        return null;
    }

    @Override
    public RemoteMediaClient getRemoteMediaClient() {
        return null;
    }

    @Override
    public MediaQueue getMediaQueue() {
        return null;
    }
}
