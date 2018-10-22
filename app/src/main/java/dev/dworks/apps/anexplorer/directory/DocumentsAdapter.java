package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import dev.dworks.apps.anexplorer.BaseActivity.State;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import dev.dworks.apps.anexplorer.misc.IconHelper;
import dev.dworks.apps.anexplorer.model.DirectoryResult;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.RootInfo;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_LIST;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;

public class DocumentsAdapter extends RecyclerView.Adapter<BaseHolder> {
    public static final int ITEM_TYPE_LIST = 1;
    public static final int ITEM_TYPE_GRID = 2;

    public static final int ITEM_TYPE_LOADING = Integer.MAX_VALUE;
    public static final int ITEM_TYPE_INFO = Integer.MAX_VALUE - 1;
    public static final int ITEM_TYPE_ERROR = Integer.MAX_VALUE - 2;
    public static final int ITEM_TYPE_HEADER = Integer.MAX_VALUE - 3;
    private final boolean mShowHeader;
    private final int offsetPosition;

    private Cursor mCursor;
    private int mCursorCount;

    private ArrayList<Footer> mFooters = new ArrayList<>();
    private Footer mHeader;
    private final Environment mEnv;
    private final OnItemClickListener mOnItemClickListener;

    public DocumentsAdapter(OnItemClickListener onItemClickListener, Environment environment) {
        mEnv = environment;
        mOnItemClickListener = onItemClickListener;
        mShowHeader = isWatch();
        offsetPosition = mShowHeader ? 1 : 0;
    }

    public void swapResult(DirectoryResult result) {
        mCursor = result != null ? result.cursor : null;
        mCursorCount = mCursor != null ? mCursor.getCount() : 0;

        DocumentsApplication.getFolderSizes().clear();
        mFooters.clear();

        final Bundle extras = mCursor != null ? mCursor.getExtras() : null;
        if (extras != null) {
            final String info = extras.getString(DocumentsContract.EXTRA_INFO);
            if (info != null) {
                mFooters.add(new MessageFooter(mEnv, ITEM_TYPE_INFO, R.drawable.ic_dialog_info, info));
            }
            final String error = extras.getString(DocumentsContract.EXTRA_ERROR);
            if (error != null) {
                mFooters.add(new MessageFooter(mEnv, ITEM_TYPE_ERROR, R.drawable.ic_dialog_alert, error));
            }
            if (extras.getBoolean(DocumentsContract.EXTRA_LOADING, false)) {
                mFooters.add(new LoadingFooter(mEnv, ITEM_TYPE_LOADING));
            }
        }

        if (result != null && result.exception != null) {
            mFooters.add(new MessageFooter(mEnv, ITEM_TYPE_ERROR, R.drawable.ic_dialog_alert, getString(R.string.query_error)));
        }

        mHeader = new MessageFooter(mEnv, ITEM_TYPE_HEADER, R.drawable.ic_doc_folder,
                getTitle());
        mEnv.setEmptyState();

        notifyDataSetChanged();
    }

    private String getTitle(){
        if(mEnv.getDisplayState().stack.size() == 1 || null == mEnv.getDocumentInfo()){
            RootInfo rootInfo = mEnv.getRoot();
            return  null != rootInfo ? rootInfo.title : "";
        }
        return mEnv.getDocumentInfo().displayName;
    }

    private String getString(int resId){
        return mEnv.getContext().getString(resId);
    }

    @Override
    public int getItemCount() {
        return mCursorCount + (mShowHeader ? 1 : 0) + mFooters.size();
    }

    public Cursor getItem(int position) {
        if (position - offsetPosition< mCursorCount) {
            mCursor.moveToPosition(position - offsetPosition);
            return mCursor;
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public BaseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_TYPE_LIST: {
                return new ListDocumentHolder(mEnv.getContext(), parent, mOnItemClickListener,
                        mEnv);
            }
            case ITEM_TYPE_GRID: {
                return new GridDocumentHolder(mEnv.getContext(), parent, mOnItemClickListener,
                        mEnv);
            }
            case ITEM_TYPE_LOADING: {
                return new LoadingHolder(mEnv, mEnv.getContext(), parent);
            }
            case ITEM_TYPE_INFO:
            case ITEM_TYPE_ERROR: {
                return new MessageHolder(mEnv, mEnv.getContext(), parent);
            }
            case ITEM_TYPE_HEADER: {
                return new MessageHolder(mEnv, mEnv.getContext(), parent, R.layout.item_message_header);
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseHolder holder, int position) {
        if(position == 0 && mShowHeader){
            holder.setData(mHeader.mMessage, mHeader.mIcon);
            holder.itemView.setEnabled(false);
        }
        else if (position - offsetPosition < mCursorCount) {
            Cursor cursor = getItem(position );
            holder.setData(cursor, position);
        } else {
            position -= mCursorCount + offsetPosition;
            Footer footer = mFooters.get(position);
            holder.setData(footer.mMessage, footer.mIcon);
            // Only the view itself is disabled; contents inside shouldn't
            // be dimmed.
            holder.itemView.setEnabled(false);
        }

    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0 && mShowHeader){
            return mHeader.getItemViewType();
        }
        else if (position - offsetPosition < mCursorCount) {
            final State state = mEnv.getDisplayState();
            switch (state.derivedMode) {
                case MODE_GRID:
                    return ITEM_TYPE_GRID;
                case MODE_LIST:
                    return ITEM_TYPE_LIST;
                default:
                    return ITEM_TYPE_LIST;
            }
        } else {
            position -= mCursorCount + offsetPosition;
            return mFooters.get(position).getItemViewType();
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void setSelected(int position, boolean selected){
        getMultiChoiceHelper().setItemChecked(position - offsetPosition, selected, true);
    }

    public boolean isItemChecked(int position) {
        return getMultiChoiceHelper().isItemChecked(position - offsetPosition);
    }

    public int getCheckedItemCount() {
        return getMultiChoiceHelper().getCheckedItemCount();
    }

    public SparseBooleanArray getCheckedItemPositions() {
        return getMultiChoiceHelper().getCheckedItemPositions();
    }

    private MultiChoiceHelper getMultiChoiceHelper(){
        return mEnv.getMultiChoiceHelper();
    }

    /**
     * Environmental access for View adapter implementations.
     */
    public interface Environment {
        Context getContext();

        int getType();

        State getDisplayState();

        boolean isApp();

        RootInfo getRoot();

        DocumentInfo getDocumentInfo();

        boolean isDocumentEnabled(String mimeType, int flags);

        boolean hideGridTiles();

        void setEmptyState();

        MultiChoiceHelper getMultiChoiceHelper();

        IconHelper getIconHelper();
    }
}