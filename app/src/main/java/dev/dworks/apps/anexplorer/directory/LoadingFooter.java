package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.directory.DocumentsAdapter.Environment;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_LIST;

public class LoadingFooter extends Footer {

    public LoadingFooter(Environment environment, int type) {
        super(type);
        mEnv = environment;
        mIcon = 0;
        mMessage = "";
    }
}