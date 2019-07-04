package dev.dworks.apps.anexplorer.common;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.ui.RecyclerViewPlus;


public class RecyclerFragment extends BaseFragment {
	private Adapter<RecyclerView.ViewHolder> mAdapter;
	private LayoutManager mLayoutManager;
    private CharSequence mEmptyText;
    final private Handler mHandler = new Handler();
    private RecyclerView mList;
    private View mListContainer;
    private boolean mListShown;
    private String mLoadingText;

    public interface onDataChangeListener {
        void onDataChanged();
        void onCancelled();
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder item, View view, int position);
        void onItemLongClick(RecyclerView.ViewHolder item, View view, int position);
        void onItemViewClick(RecyclerView.ViewHolder item, View view, int position);
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener{

    	private RecyclerViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        	onListItemClick(view, getLayoutPosition(), getItemId());
        }

        @Override
        public boolean onLongClick(View view) {
            onListItemLongClick(view, getLayoutPosition(), getItemId());
            return false;
        }
    }

    final private RecyclerItemClickListener.OnItemClickListener mOnItemClickListener = new RecyclerItemClickListener.OnItemClickListener(){
        @Override
        public void onItemClick(View view, int position) {
            onListItemClick(view, position, view.getId());
        }

        @Override
        public void onItemLongClick(View view, int position) {
            onListItemLongClick(view, position, view.getId());
        }

        @Override
        public void onItemViewClick(View view, int position) {
            onListItemViewClick(view, position, view.getId());
        }
    };

    private View mProgressContainer;
    final private Runnable mRequestFocus = new Runnable() {
        @Override
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };
    private TextView mStandardEmptyView;
    private TextView mLoadingView;
	private ItemAnimator mItemAnimator;

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            return;
        }
        if (root instanceof RecyclerView) {
            mList = (RecyclerViewPlus) root;
        } else {
            mStandardEmptyView = (TextView) root
                    .findViewById(android.R.id.empty);
            mProgressContainer = root.findViewById(R.id.progressContainer);
            mLoadingView = (TextView) root.findViewById(R.id.loading);
            mListContainer = root.findViewById(R.id.listContainer);
            View rawListView = root.findViewById(R.id.recyclerview);
            if (rawListView == null) {
                throw new RuntimeException(
                        "Your content must have a ListView whose id attribute is "
                                + "'R.id.recyclerview'");
            }
            else{
            	try {
                	@SuppressWarnings("unused")
                    RecyclerView list = (RecyclerView) rawListView;
				} catch (Exception e) {
		               throw new RuntimeException(
		                        "Content has view with id attribute 'android.R.id.list' "
		                                + "that is not a ListView class");
				}
            }
            mList = (RecyclerViewPlus) rawListView;

            mStandardEmptyView.setText(mEmptyText);
        }
        mListShown = true;
        if(null != mLayoutManager) {
            mList.setLayoutManager(mLayoutManager);
        }
        mList.setItemAnimator(mItemAnimator);
        mList.setHasFixedSize(true);
        mList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mOnItemClickListener));
        if (mAdapter != null) {
            Adapter<RecyclerView.ViewHolder> adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }

    protected TextView getEmptyView() {
        return mStandardEmptyView;
    }

    public Adapter<RecyclerView.ViewHolder> getListAdapter() {
        return mAdapter;
    }

    public RecyclerView getListView() {
        ensureList();
        return mList;
    }

/*    public long getSelectedItemId() {
        ensureList();
        return mList.getSelectedItemId();
    }

    public int getSelectedItemPosition() {
        ensureList();
        return mList.getSelectedItemPosition();
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mItemAnimator = new DefaultItemAnimator();
        mLoadingText = getString(R.string.loading);
        return inflater.inflate(R.layout.fragment_recycler_content, container, false);
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mListShown = false;
        mProgressContainer = mListContainer = null;
        mStandardEmptyView = null;
        mLayoutManager = null;
        super.onDestroyView();
    }

    public void onListItemClick(View v, int position, long id) {
    }

    public void onListItemLongClick(View v, int position, long id) {
    }

    public void onListItemViewClick(View v, int position, long id) {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    	super.onViewCreated(view, savedInstanceState);
    	ensureList();
    }

    public void setEmptyText(CharSequence text) {
        ensureList();
        if (mStandardEmptyView == null) {
            return;
        }
        mStandardEmptyView.setText(text);
        mStandardEmptyView.setVisibility(Utils.getVisibility(!TextUtils.isEmpty(text)));
        mEmptyText = text;
    }

    private void setLoadingText(CharSequence text) {
        ensureList();
        if (mLoadingView == null) {
            return;
        }
        mLoadingView.setText(text);
    }

    public void setHasFixedSize(boolean fixedSize){
        if (mList != null) {
            mList.setHasFixedSize(fixedSize);
        }
    }

    public void setItemAnimator(ItemAnimator animator){
        mItemAnimator = animator;
        if (mList != null) {
            mList.setItemAnimator(mItemAnimator);
        }
    }

    public void setLayoutManager(LayoutManager layoutManager){
    	mLayoutManager = layoutManager;
        if (mList != null) {
            mList.setLayoutManager(mLayoutManager);
        }
    }

    public void addItemDecoration(RecyclerView.ItemDecoration itemDecoration){
        RecyclerView.ItemDecoration mItemDecoration = itemDecoration;
        if (mList != null) {
            mList.addItemDecoration(mItemDecoration);
        }
    }

    public void setListAdapter(Adapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if (!mListShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter. It is now time to show it.
                setListShown(true, getView().getWindowToken() != null);
            }
        }
    }

    public void setListShown(boolean shown, String loading) {
    	setLoadingText(loading);
        setListShown(shown, true);
    }

    public void setListShown(boolean shown) {
    	setLoadingText(mLoadingText);
        setListShown(shown, true);
    }

    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mProgressContainer == null) {
            return;
        }
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
        	if(null != mStandardEmptyView){
        		mStandardEmptyView.setText("");
        	}
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    public void setSelection(int position) {
        ensureList();
        //mList.setSelection(position);
        mList.getLayoutManager().scrollToPosition(position);
    }

    public static class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
        private final OnItemClickListener mListener;

        public interface OnItemClickListener {
            /**
             * Fires when recycler view receives a single tap event on any item
             *
             * @param view     tapped view
             * @param position item position in the list
             */
            void onItemClick(View view, int position);

            /**
             * Fires when recycler view receives a long tap event on item
             *
             * @param view     long tapped view
             * @param position item position in the list
             */
            void onItemLongClick(View view, int position);

            /**
             * Fires when recycler view receives a single tap event on any item
             *
             * @param view     tapped view
             * @param position item position in the list
             */
            void onItemViewClick(View view, int position);

        }

        final GestureDetector mGestureDetector;
        final ExtendedGestureListener mGestureListener;

        public RecyclerItemClickListener(Context context, OnItemClickListener listener) {
            mListener = listener;
            mGestureListener = new ExtendedGestureListener();
            mGestureDetector = new GestureDetector(context, mGestureListener);
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
            View childView = view.findChildViewUnder(e.getX(), e.getY());
            if (childView != null && mListener != null) {
                mGestureListener.setHelpers(childView, view.getChildAdapterPosition(childView));
                mGestureDetector.onTouchEvent(e);
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }

        /**
         * Extended Gesture listener react for both item clicks and item long clicks
         */
        private class ExtendedGestureListener extends GestureDetector.SimpleOnGestureListener {
            private View view;
            private int position;

            public void setHelpers(View view, int position) {
                this.view = view;
                this.position = position;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mListener.onItemClick(view, position);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                mListener.onItemLongClick(view, position);
            }
        }
    }
}
