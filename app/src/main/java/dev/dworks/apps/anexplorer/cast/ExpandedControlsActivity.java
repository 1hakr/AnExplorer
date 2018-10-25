package dev.dworks.apps.anexplorer.cast;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.TintUtils;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;


/**
 * Fullscreen media controls
 */
public class ExpandedControlsActivity extends ExpandedControllerActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setFitsSystemWindows(true);
        ImageView imageView = findViewById(R.id.background_place_holder_image_view);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_root_image);
        TintUtils.tintDrawable(drawable, Color.WHITE);
        imageView.setImageDrawable(drawable);

        int color = SettingsActivity.getAccentColor();
        int accentColor = SettingsActivity.getAccentColor();

        ProgressBar progressBar = findViewById(R.id.loading_indicator);
        TintUtils.tintDrawable(progressBar.getIndeterminateDrawable(), accentColor);
        SeekBar seekBar = findViewById(R.id.seek_bar);
        TintUtils.tintWidget(seekBar, color);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_cast, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.casty_media_route_menu_item);
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean b) {
//        super.onWindowFocusChanged(b);
    }
}
