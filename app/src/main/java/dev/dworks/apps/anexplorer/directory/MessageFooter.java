package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.directory.DocumentsAdapter.Environment;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_LIST;

public class MessageFooter extends Footer {

    public MessageFooter(Environment environment, int itemViewType, int icon, String message) {
        super(itemViewType);
        mIcon = icon;
        mMessage = message;
        mEnv = environment;
    }
}