package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.view.ViewGroup;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import dev.dworks.apps.anexplorer.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
