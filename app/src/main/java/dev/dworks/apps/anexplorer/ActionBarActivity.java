package dev.dworks.apps.anexplorer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import dev.dworks.apps.anexplorer.misc.Utils;

/**
 * Created by HaKr on 18-Oct-14.
 */
public class ActionBarActivity extends AppCompatActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Utils.changeThemeStyle(getDelegate());
        super.onCreate(savedInstanceState);
    }

    @Override
    public ActionBar getSupportActionBar() {
        return super.getSupportActionBar();
    }

    @Override
    public void recreate() {
        Utils.changeThemeStyle(getDelegate());
        super.recreate();
    }
}
