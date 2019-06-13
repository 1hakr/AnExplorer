package dev.dworks.apps.anexplorer.common;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.Utils;

/**
 * Created by HaKr on 18-Oct-14.
 */
public abstract class ActionBarActivity extends BaseCommonActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Utils.changeThemeStyle();
        super.onCreate(savedInstanceState);
    }

    @Override
    public ActionBar getSupportActionBar() {
        return super.getSupportActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsManager.setCurrentScreen(this, getTag());
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

    public abstract String getTag();
}
