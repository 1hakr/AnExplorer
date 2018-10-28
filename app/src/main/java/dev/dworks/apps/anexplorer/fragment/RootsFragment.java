/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
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

package dev.dworks.apps.anexplorer.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.BaseActivity.State;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.adapter.RootsExpandableAdapter;
import dev.dworks.apps.anexplorer.common.BaseFragment;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.libcore.util.Objects;
import dev.dworks.apps.anexplorer.loader.RootsLoader;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.GroupInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.ExplorerProvider;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.NumberProgressBar;

import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_BROWSE;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.R.layout.item_root_spacer;

/**
 * Display list of known storage backend roots.
 */
public class RootsFragment extends BaseFragment {

    private ExpandableListView mList;
    private RootsExpandableAdapter mAdapter;

    private LoaderCallbacks<Collection<RootInfo>> mCallbacks;

    public static final String EXTRA_INCLUDE_APPS = "includeApps";
    private static final String GROUP_SIZE = "group_size";
    private static final String GROUP_IDS = "group_ids";
    private int group_size = 0;
    private ArrayList<Long> expandedIds = new ArrayList<>();
    private View proWrapper;
    private View title;

    public static void show(FragmentManager fm, Intent includeApps) {
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_INCLUDE_APPS, includeApps);

        final RootsFragment fragment = new RootsFragment();
        fragment.setArguments(args);

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_roots, fragment);
        ft.commitAllowingStateLoss();
    }

    public static RootsFragment get(FragmentManager fm) {
        return (RootsFragment) fm.findFragmentById(R.id.container_roots);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_roots, container, false);
        proWrapper = view.findViewById(R.id.proWrapper);
        title = view.findViewById(android.R.id.title);
        View headerLayout = view.findViewById(R.id.headerLayout);
        if(isTelevision()){
            title.setVisibility(View.VISIBLE);
        } else {
            headerLayout.setVisibility(View.VISIBLE);
            headerLayout.setBackgroundColor(SettingsActivity.getPrimaryColor());
        }
        mList = (ExpandableListView) view.findViewById(android.R.id.list);
        mList.setOnChildClickListener(mItemListener);
        mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = getResources().getDimensionPixelSize(R.dimen.side_navigation_width);

        boolean rtl = Utils.isRTL();
        int leftPadding = rtl ? 10 : 60;
        int rightPadding = rtl ? 50 : 10;
        int leftWidth = width - Utils.dpToPx(leftPadding);
        int rightWidth = width - Utils.dpToPx(rightPadding);

        if(Utils.hasJellyBeanMR2()){
            mList.setIndicatorBoundsRelative(leftWidth, rightWidth);

        } else {
            mList.setIndicatorBounds(leftWidth, rightWidth);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(null != savedInstanceState) {
            group_size = savedInstanceState.getInt(GROUP_SIZE, 0);
            expandedIds = (ArrayList<Long>) savedInstanceState.getSerializable(GROUP_IDS);
        }

        final Context context = getActivity();
        final RootsCache roots = DocumentsApplication.getRootsCache(context);
        final State state = ((BaseActivity) context).getDisplayState();

        mCallbacks = new LoaderCallbacks<Collection<RootInfo>>() {
            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int id, Bundle args) {
                return new RootsLoader(context, roots, state);
            }

            @Override
            public void onLoadFinished(
                    Loader<Collection<RootInfo>> loader, Collection<RootInfo> result) {
                if (!isAdded()) return;

                final Intent includeApps = getArguments().getParcelable(EXTRA_INCLUDE_APPS);

                if (mAdapter == null) {
                    mAdapter = new RootsExpandableAdapter(context, result, includeApps);
                    Parcelable state = mList.onSaveInstanceState();
                    mList.setAdapter(mAdapter);
                    mList.onRestoreInstanceState(state);
                } else {
                    mAdapter.setData(result);
                }

                int groupCount = mAdapter.getGroupCount();
                if(group_size != 0 && group_size == groupCount){
                    if (expandedIds != null) {
                        restoreExpandedState(expandedIds);
                    }
                } else {
                    group_size = groupCount;
                    for (int i = 0; i < group_size; i++) {
                        mList.expandGroup(i);
                    }
                    expandedIds = getExpandedIds();
                    mList.setOnGroupExpandListener(mOnGroupExpandListener);
                    mList.setOnGroupCollapseListener(mOnGroupCollapseListener);
                }
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                mAdapter = null;
                mList.setAdapter((RootsExpandableAdapter)null);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        changeThemeColor();
        final Context context = getActivity();
        final State state = ((BaseActivity) context).getDisplayState();
        state.showAdvanced = state.forceAdvanced
                | SettingsActivity.getDisplayAdvancedDevices(context);
        state.rootMode = SettingsActivity.getRootMode(getActivity());
        
        if (state.action == ACTION_BROWSE) {
            mList.setOnItemLongClickListener(mItemLongClickListener);
        } else {
            mList.setOnItemLongClickListener(null);
            mList.setLongClickable(false);
        }
        if(null != proWrapper) {
            proWrapper.setVisibility(DocumentsApplication.isPurchased() ? View.GONE : View.VISIBLE);
        }
        LoaderManager.getInstance(getActivity()).restartLoader(2, null, mCallbacks);
    }

    private void changeThemeColor() {

        if(isTelevision()){
            int color = SettingsActivity.getPrimaryColor(getActivity());
            Drawable colorDrawable = new ColorDrawable(color);
            getView().setBackground(colorDrawable);
        }
    }

    public void onCurrentRootChanged() {
        if (mAdapter == null || mList == null) return;

        final RootInfo root = ((BaseActivity) getActivity()).getCurrentRoot();
        for (int i = 0; i < mAdapter.getGroupCount(); i++) {
            for (int j = 0; j < mAdapter.getChildrenCount(i); j++) {
                final Object item = mAdapter.getChild(i,j);
                if (item instanceof RootItem) {
                    final RootInfo testRoot = ((RootItem) item).root;
                    if (Objects.equal(testRoot, root)) {
                        try {
                            long id = ExpandableListView.getPackedPositionForChild(i, j);
                            int index = mList.getFlatListPosition(id);
                            //mList.setSelection(index);
                            mList.setItemChecked(index, true);
                        } catch (Exception e){
                            CrashReportingManager.logException(e);
                        }

                        return;
                    }
                }
            }
        }
    }

    private void showAppDetails(ResolveInfo ri) {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", ri.activityInfo.packageName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        if(Utils.isIntentAvailable(getActivity(), intent)) {
            startActivity(intent);
        }
    }

    private ExpandableListView.OnChildClickListener mItemListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                    int childPosition, long id) {
            final BaseActivity activity = BaseActivity.get(RootsFragment.this);
            final Item item = (Item) mAdapter.getChild(groupPosition, childPosition);
            if (item instanceof RootItem) {
                    int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                parent.setItemChecked(index, true);
                RootInfo rootInfo = ((RootItem) item).root;
                if(RootInfo.isProFeature(rootInfo) && !DocumentsApplication.isPurchased()){
                    DocumentsApplication.openPurchaseActivity(activity);
                    return false;
                }
                activity.onRootPicked(rootInfo, true);
                Bundle params = new Bundle();
                params.putString("type", rootInfo.title);
                AnalyticsManager.logEvent("navigate", rootInfo, params);
            } else if (item instanceof AppItem) {
                activity.onAppPicked(((AppItem) item).info);
            } else {
                throw new IllegalStateException("Unknown root: " + item);
            }
            return false;
        }
    };

    private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            int itemType = ExpandableListView.getPackedPositionType(id);
            int childPosition;
            int groupPosition;

            if ( itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                childPosition = ExpandableListView.getPackedPositionChild(id);
                groupPosition = ExpandableListView.getPackedPositionGroup(id);
                final Item item = (Item) mAdapter.getChild(groupPosition, childPosition);
                if (item instanceof AppItem) {
                    showAppDetails(((AppItem) item).info);
                    return true;
                } else if (item instanceof BookmarkItem) {
                    removeBookark((BookmarkItem)item);
                    return true;
                }  else {
                    return false;
                }

            } else if(itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                groupPosition = ExpandableListView.getPackedPositionGroup(id);
                return false;

            } else {
                return false;
            }
        }
    };

    private ExpandableListView.OnGroupExpandListener mOnGroupExpandListener = new ExpandableListView.OnGroupExpandListener() {
        @Override
        public void onGroupExpand(int i) {
            expandedIds.add(mAdapter.getGroupId(i));
        }
    };

    private ExpandableListView.OnGroupCollapseListener mOnGroupCollapseListener = new ExpandableListView.OnGroupCollapseListener() {
        @Override
        public void onGroupCollapse(int i) {
            expandedIds.remove(mAdapter.getGroupId(i));
        }
    };

    private void removeBookark(final BookmarkItem item) {
        DialogBuilder builder = new DialogBuilder(getActivity());
        builder.setMessage("Remove bookmark?")
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int did) {
                int rows = getActivity().getContentResolver().delete(ExplorerProvider.buildBookmark(),
                        ExplorerProvider.BookmarkColumns.PATH + " = ? AND " +
                                ExplorerProvider.BookmarkColumns.TITLE + " = ? ",
                        new String[]{item.root.path, item.root.title}
                );
                if (rows > 0) {
                    Utils.showSnackBar(getActivity(), "Bookmark removed");
                    RootsCache.updateRoots(getActivity(), ExternalStorageProvider.AUTHORITY);
                }
            }
        }).setNegativeButton(android.R.string.cancel, null);
        builder.showDialog();
    }

    public static class GroupItem {
        public final String mLabel;
        private final int mLayoutId;

        public GroupItem(GroupInfo groupInfo) {
            mLabel = groupInfo.label;
            mLayoutId = R.layout.item_root_header;
        }

        public View getView(View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(mLayoutId, parent, false);
            }
            bindView(convertView);
            return convertView;
        }

        public void bindView(View convertView) {
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            title.setText(mLabel);
        }

    }

    public static abstract class Item {
        private final int mLayoutId;

        public Item(int layoutId) {
            mLayoutId = layoutId;
        }

        public View getView(View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(mLayoutId, parent, false);
            }
            bindView(convertView);
            return convertView;
        }

        public abstract void bindView(View convertView);
    }

    public static class RootItem extends Item {
        public final RootInfo root;
        private final int color;

        public RootItem(RootInfo root) {
            super(R.layout.item_root);
            this.root = root;
            this.color = SettingsActivity.getPrimaryColor();
        }

        public RootItem(RootInfo root, int color) {
            super(R.layout.item_root);
            this.root = root;
            this.color = color;
        }

        @Override
        public void bindView(View convertView) {
            final ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            final TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
            final NumberProgressBar progress = (NumberProgressBar) convertView.findViewById(android.R.id.progress);

            final Context context = convertView.getContext();
            icon.setImageDrawable(root.loadDrawerIcon(context));
            title.setText(root.title);

            // Show available space if no summary
            if(root.isNetworkStorage() || root.isCloudStorage() || root.isApp()) {
                String summaryText = root.summary;
                summary.setText(summaryText);
                summary.setVisibility(TextUtils.isEmpty(summaryText) ? View.GONE : View.VISIBLE);
            } else {
                summary.setVisibility(View.GONE);
            }
        }
    }

    public static class SpacerItem extends Item {
        public SpacerItem() {
            super(item_root_spacer);
        }

        @Override
        public void bindView(View convertView) {
            // Nothing to bind
        }
    }

    public static class AppItem extends Item {
        public final ResolveInfo info;

        public AppItem(ResolveInfo info) {
            super(R.layout.item_root);
            this.info = info;
        }

        @Override
        public void bindView(View convertView) {
            final ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            final TextView summary = (TextView) convertView.findViewById(android.R.id.summary);

            final PackageManager pm = convertView.getContext().getPackageManager();
            icon.setImageDrawable(info.loadIcon(pm));
            title.setText(info.loadLabel(pm));

            // TODO: match existing summary behavior from disambig dialog
            summary.setVisibility(View.GONE);
        }
    }

    public static class BookmarkItem extends RootItem {
        public BookmarkItem(RootInfo root) {
            super(root, 0);
        }
    }

    public static class RootComparator implements Comparator<RootInfo> {
        @Override
        public int compare(RootInfo lhs, RootInfo rhs) {
            final int score = DocumentInfo.compareToIgnoreCaseNullable(lhs.title, rhs.title);
            if (score != 0) {
                return score;
            } else {
                return DocumentInfo.compareToIgnoreCaseNullable(lhs.summary, rhs.summary);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.expandedIds != null) {
            restoreExpandedState(expandedIds);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        expandedIds = getExpandedIds();
    }

    private ArrayList<Long> getExpandedIds() {
        ExpandableListView list = mList;
        ExpandableListAdapter adapter = mAdapter;
        if (adapter != null) {
            int length = adapter.getGroupCount();
            ArrayList<Long> expandedIds = new ArrayList<Long>();
            for(int i=0; i < length; i++) {
                if(list.isGroupExpanded(i)) {
                    expandedIds.add(adapter.getGroupId(i));
                }
            }
            return expandedIds;
        } else {
            return null;
        }
    }

    private void restoreExpandedState(ArrayList<Long> expandedIds) {
        this.expandedIds = expandedIds;
        if (expandedIds != null) {
            ExpandableListView list = mList;
            ExpandableListAdapter adapter = mAdapter;
            if (adapter != null) {
                for (int i=0; i<adapter.getGroupCount(); i++) {
                    long id = adapter.getGroupId(i);
                    if (expandedIds.contains(id)) list.expandGroup(i);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(GROUP_SIZE, group_size);
        this.expandedIds = getExpandedIds();
        outState.putSerializable(GROUP_IDS, this.expandedIds);
        super.onSaveInstanceState(outState);
    }
}