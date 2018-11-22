/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dworks.apps.anexplorer.misc;

import android.os.Bundle;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;

import java.util.HashSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
/**
 * Utilities for handling multiple selection in list views. Contains functionality similar to {@link
 * AbsListView#CHOICE_MODE_MULTIPLE_MODAL} which works with {@link AppCompatActivity} and
 * backward-compatible action bars.
 */
public class MultiSelectionUtil {
    /**
     * Attach a Controller to the given <code>listView</code>, <code>activity</code>
     * and <code>listener</code>.
     *
     * @param listView ListView which displays {@link android.widget.Checkable} items.
     * @param activity Activity which contains the ListView.
     * @param listener Listener that will manage the selection mode.
     * @return the attached Controller instance.
     */
    public static Controller attachMultiSelectionController(final AbsListView listView,
            final AppCompatActivity activity, final MultiChoiceModeListener listener) {
        return new Controller(listView, activity, listener);
    }
    /**
     * Class which provides functionality similar to {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL}
     * for the {@link AbsListView} provided to it. A
     * {@link android.widget.AdapterView.OnItemLongClickListener} is set on the ListView so that
     * when an item is long-clicked an ActionBarCompat Action Mode is started. Once started, a
     * {@link android.widget.AdapterView.OnItemClickListener} is set so that an item click toggles
     * that item's checked state.
     */
    public static class Controller {
        private final AbsListView mListView;
        private final AppCompatActivity mActivity;
        private final MultiChoiceModeListener mListener;
        private final Callbacks mCallbacks;
        // Current Action Mode (if there is one)
        private ActionMode mActionMode;
        // Keeps record of any items that should be checked on the next action mode creation
        private HashSet<Pair<Integer, Long>> mItemsToCheck;
        // Reference to the replace OnItemClickListener (so it can be restored later)
        private AdapterView.OnItemClickListener mOldItemClickListener;
        private final Runnable mSetChoiceModeNoneRunnable = new Runnable() {
            @Override
            public void run() {
                mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            }
        };
        private Controller(AbsListView listView, AppCompatActivity activity,
                MultiChoiceModeListener listener) {
            mListView = listView;
            mActivity = activity;
            mListener = listener;
            mCallbacks = new Callbacks();
            // We set ourselves as the OnItemLongClickListener so we know when to start
            // an Action Mode
            listView.setOnItemLongClickListener(mCallbacks);
        }
        /**
         * Finish the current Action Mode (if there is one).
         */
        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
        /**
         * This method should be called from your {@link AppCompatActivity} or
         * {@link Fragment Fragment} to allow the controller to restore any
         * instance state.
         *
         * @param savedInstanceState - The state passed to your Activity or Fragment.
         */
        public void restoreInstanceState(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                long[] checkedIds = savedInstanceState.getLongArray(getStateKey());
                if (checkedIds != null && checkedIds.length > 0) {
                    HashSet<Long> idsToCheckOnRestore = new HashSet<Long>();
                    for (long id : checkedIds) {
                        idsToCheckOnRestore.add(id);
                    }
                    tryRestoreInstanceState(idsToCheckOnRestore);
                }
            }
        }
        /**
         * This method should be called from
         * {@link AppCompatActivity#onSaveInstanceState(android.os.Bundle)} or
         * {@link Fragment#onSaveInstanceState(android.os.Bundle)
         * Fragment.onSaveInstanceState(Bundle)} to allow the controller to save its instance
         * state.
         *
         * @param outState - The state passed to your Activity or Fragment.
         */
        public void saveInstanceState(Bundle outState) {
            if (mActionMode != null && mListView.getAdapter().hasStableIds()) {
                outState.putLongArray(getStateKey(), mListView.getCheckedItemIds());
            }
        }
        // Internal utility methods
        private String getStateKey() {
            return MultiSelectionUtil.class.getSimpleName() + "_" + mListView.getId();
        }
        private void tryRestoreInstanceState(HashSet<Long> idsToCheckOnRestore) {
            if (idsToCheckOnRestore == null || mListView.getAdapter() == null) {
                return;
            }
            boolean idsFound = false;
            Adapter adapter = mListView.getAdapter();
            for (int pos = adapter.getCount() - 1; pos >= 0; pos--) {
                if (idsToCheckOnRestore.contains(adapter.getItemId(pos))) {
                    idsFound = true;
                    if (mItemsToCheck == null) {
                        mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                    }
                    mItemsToCheck.add(new Pair<Integer, Long>(pos, adapter.getItemId(pos)));
                }
            }
            if (idsFound) {
                // We found some IDs that were checked. Let's now restore the multi-selection
                // state.
                mActionMode = mActivity.startSupportActionMode(mCallbacks);
            }
        }
        /**
         * This class encapsulates all of the callbacks necessary for the controller class.
         */
        final class Callbacks implements ActionMode.Callback, AdapterView.OnItemClickListener,
                AdapterView.OnItemLongClickListener {
            @Override
            public final boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                if (mListener.onCreateActionMode(actionMode, menu)) {
                    mActionMode = actionMode;
                    // Keep a reference to the existing OnItemClickListener so we can restore it
                    mOldItemClickListener = mListView.getOnItemClickListener();
                    // Set-up the ListView to emulate CHOICE_MODE_MULTIPLE_MODAL
                    mListView.setOnItemClickListener(this);
                    mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                    mListView.removeCallbacks(mSetChoiceModeNoneRunnable);
                    // If there are some items to check, do it now
                    if (mItemsToCheck != null) {
                        for (Pair<Integer, Long> posAndId : mItemsToCheck) {
                            mListView.setItemChecked(posAndId.first, true);
                            // Notify the listener that the item has been checked
                            mListener.onItemCheckedStateChanged(mActionMode, posAndId.first,
                                    posAndId.second, true);
                        }
                    }
                    return true;
                }
                return false;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                // Proxy listener
                return mListener.onPrepareActionMode(actionMode, menu);
            }
            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                // Proxy listener
                return mListener.onActionItemClicked(actionMode, menuItem);
            }
            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                mListener.onDestroyActionMode(actionMode);
                // Clear all the checked items
                SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
                if (checkedPositions != null) {
                    for (int i = 0; i < checkedPositions.size(); i++) {
                        mListView.setItemChecked(checkedPositions.keyAt(i), false);
                    }
                }
                // Restore the original onItemClickListener
                mListView.setOnItemClickListener(mOldItemClickListener);
                // Clear the Action Mode
                mActionMode = null;
                // Reset the ListView's Choice Mode
                mListView.post(mSetChoiceModeNoneRunnable);
            }
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Check to see what the new checked state is, and then notify the listener
                final boolean checked = mListView.isItemChecked(position);
                mListener.onItemCheckedStateChanged(mActionMode, position, id, checked);
                boolean hasCheckedItem = checked;
                // Check to see if we have any checked items
                if (!hasCheckedItem) {
                    SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
                    if (checkedItemPositions != null) {
                        // Iterate through the SparseBooleanArray to see if there is a checked item
                        int i = 0;
                        while (!hasCheckedItem && i < checkedItemPositions.size()) {
                            hasCheckedItem = checkedItemPositions.valueAt(i++);
                        }
                    }
                }
                // If we don't have any checked items, finish the action mode
                if (!hasCheckedItem) {
                    mActionMode.finish();
                }
            }
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position,
                    long id) {
                // If we already have an action mode started return false
                // (onItemClick will be called anyway)
                if (mActionMode != null) {
                    return false;
                }
                mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                mItemsToCheck.add(new Pair<Integer, Long>(position, id));
                mActionMode = mActivity.startSupportActionMode(this);
                return true;
            }
        }
    }
    /**
     * @see android.widget.AbsListView.MultiChoiceModeListener
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * @see android.widget.AbsListView.MultiChoiceModeListener#onItemCheckedStateChanged(
         *android.view.ActionMode, int, long, boolean)
         */
        void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                       boolean checked);
    }
}