package dev.dworks.apps.anexplorer.common;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;

public abstract class SettingsCommonActivity extends AppCompatPreferenceActivity {

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        getSupportActionBar().hide();
    }
}
