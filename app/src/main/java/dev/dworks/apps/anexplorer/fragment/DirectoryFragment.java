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

import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.BaseActivity.State;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.common.RecyclerFragment;
import dev.dworks.apps.anexplorer.directory.DividerItemDecoration;
import dev.dworks.apps.anexplorer.directory.DocumentsAdapter;
import dev.dworks.apps.anexplorer.directory.FolderSizeAsyncTask;
import dev.dworks.apps.anexplorer.directory.MarginDecoration;
import dev.dworks.apps.anexplorer.directory.MultiChoiceHelper;
import dev.dworks.apps.anexplorer.directory.MultiChoiceHelper.MultiChoiceModeListener;
import dev.dworks.apps.anexplorer.loader.DirectoryLoader;
import dev.dworks.apps.anexplorer.loader.RecentLoader;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.IconHelper;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.MimeTypes;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.SAFManager;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DirectoryResult;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.AppsProvider;
import dev.dworks.apps.anexplorer.provider.ExplorerProvider;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;
import dev.dworks.apps.anexplorer.provider.RecentsProvider;
import dev.dworks.apps.anexplorer.provider.RecentsProvider.StateColumns;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.ui.CompatTextView;
import dev.dworks.apps.anexplorer.ui.MaterialProgressBar;
import dev.dworks.apps.anexplorer.ui.RecyclerViewPlus;

import static android.widget.LinearLayout.VERTICAL;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_BROWSE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_CREATE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_MANAGE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_UNKNOWN;
import static dev.dworks.apps.anexplorer.BaseActivity.State.SORT_ORDER_UNKNOWN;
import static dev.dworks.apps.anexplorer.BaseActivity.TAG;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_COUNT;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_MOVE;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_TYPE;
import static dev.dworks.apps.anexplorer.misc.MimeTypes.ALL_MIME_TYPES;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.ACTION_FORCE_STOP_REQUEST;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.EXTRA_PACKAGE_NAMES;
import static dev.dworks.apps.anexplorer.misc.Utils.DIRECTORY_APPBACKUP;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_DOC;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_IGNORE_STATE;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_QUERY;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_ROOT;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_TYPE;
import static dev.dworks.apps.anexplorer.misc.Utils.isRooted;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorInt;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorString;
import static dev.dworks.apps.anexplorer.ui.RecyclerViewPlus.TYPE_GRID;
import static dev.dworks.apps.anexplorer.ui.RecyclerViewPlus.TYPE_LIST;

/**
 * Display the documents inside a single directory.
 */
public class DirectoryFragment extends RecyclerFragment implements MenuItem.OnMenuItemClickListener {

	private static final String KEY_ADAPTER = "key_adapter";
	private CompatTextView mEmptyView;

	public static final int TYPE_NORMAL = 1;
	public static final int TYPE_SEARCH = 2;
	public static final int TYPE_RECENT_OPEN = 3;

	public static final int ANIM_NONE = 1;
	public static final int ANIM_SIDE = 2;
	public static final int ANIM_DOWN = 3;
	public static final int ANIM_UP = 4;

	private int mType = TYPE_NORMAL;
	private String mStateKey;

	private int mLastMode = MODE_UNKNOWN;
	private int mLastSortOrder = SORT_ORDER_UNKNOWN;
	private boolean mLastShowSize = false;
	private boolean mLastShowFolderSize = false;
	private boolean mLastShowThumbnail = false;
    private int mLastShowColor = 0;
    private int mLastShowAccentColor = 0;
    private boolean mLastShowHiddenFiles = false;

	private boolean mHideGridTitles = false;

	private DocumentsAdapter mAdapter;
	private LoaderCallbacks<DirectoryResult> mCallbacks;
	private ArrayList<DocumentInfo> docsAppUninstall = new ArrayList<>();

	private final int mLoaderId = 42;
	private RootInfo root;
	private DocumentInfo doc;
	private boolean isApp;
    private int mDefaultColor;
    private MaterialProgressBar mProgressBar;
    private boolean isOperationSupported;
	private ContentProviderClient mExternalStorageClient;
	private BaseActivity mActivity;
	private final DocumentsAdapter.Environment mAdapterEnv = new AdapterEnvironment();
	private IconHelper mIconHelper;
	private MultiChoiceHelper mMultiChoiceHelper;
	boolean selectAll = true;

	public static void showNormal(FragmentManager fm, RootInfo root, DocumentInfo doc, int anim) {
		show(fm, TYPE_NORMAL, root, doc, null, anim);
	}

	public static void showSearch(FragmentManager fm, RootInfo root, DocumentInfo doc, String query, int anim) {
		show(fm, TYPE_SEARCH, root, doc, query, anim);
	}

	public static void showRecentsOpen(FragmentManager fm, int anim, RootInfo root) {
		show(fm, TYPE_RECENT_OPEN, root, null, null, anim);
	}

	private static void show(FragmentManager fm, int type, RootInfo root, DocumentInfo doc, String query, int anim) {
		final Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE, type);
		args.putParcelable(EXTRA_ROOT, root);
		args.putParcelable(EXTRA_DOC, doc);
		args.putString(EXTRA_QUERY, query);

		final FragmentTransaction ft = fm.beginTransaction();
		switch (anim) {
		case ANIM_SIDE:
			args.putBoolean(EXTRA_IGNORE_STATE, true);
			break;
		case ANIM_DOWN:
			ft.setCustomAnimations(R.animator.dir_down, R.animator.dir_frozen);
			break;
		case ANIM_UP:
			ft.setCustomAnimations(R.animator.dir_frozen, R.animator.dir_up);
			break;
		}

		final DirectoryFragment fragment = new DirectoryFragment();
		fragment.setArguments(args);

		ft.replace(R.id.container_directory, fragment);
		ft.commitAllowingStateLoss();
	}

	private static String buildStateKey(RootInfo root, DocumentInfo doc) {
        return (root != null ? root.authority : "null") + ';' + (root != null ? root.rootId : "null") + ';' + (doc != null ? doc.documentId : "null");
	}

	public static Fragment get(FragmentManager fm) {
		// TODO: deal with multiple directories shown at once
		return fm.findFragmentById(R.id.container_directory);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Context context = inflater.getContext();
		mActivity = (BaseActivity) getActivity();
		final View view = inflater.inflate(R.layout.fragment_directory, container, false);

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mProgressBar = (MaterialProgressBar) view.findViewById(R.id.progressBar);
		mEmptyView = (CompatTextView)view.findViewById(android.R.id.empty);
		getListView().setRecyclerListener(mRecycleListener);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mMultiChoiceHelper.clearChoices();
		// Cancel any outstanding thumbnail requests
		final ViewGroup target = getListView();
		final int count = target.getChildCount();
		for (int i = 0; i < count; i++) {
			final View view = target.getChildAt(i);
			cancelThumbnailTask(view);
		}
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final DocumentsActivity context = (DocumentsActivity)getActivity();
		final State state = getDisplayState(DirectoryFragment.this);

		root = getArguments().getParcelable(EXTRA_ROOT);
		doc = getArguments().getParcelable(EXTRA_DOC);

		if(null != root && root.isSecondaryStorage() && state.action == ACTION_BROWSE){
			if((null != doc && !doc.isWriteSupported()) && !context.getSAFPermissionRequested()){
				context.setSAFPermissionRequested(true);
				SAFManager.takeCardUriPermission(getActivity(), root, doc);
			}
		}
		isApp = root != null && root.isApp();
        isOperationSupported = root != null && (root.isRootedStorage() || root.isUsbStorage());

		mIconHelper = new IconHelper(mActivity, MODE_GRID);
		mAdapter = new DocumentsAdapter(mItemListener, mAdapterEnv);

		mMultiChoiceHelper = new MultiChoiceHelper(mActivity, mAdapter);
		mMultiChoiceHelper.setMultiChoiceModeListener(mMultiListener);
		if (isWatch()){
			mMultiChoiceHelper.setMenuItemClickListener(this);
		}

		if(null != savedInstanceState) {
			mMultiChoiceHelper.onRestoreInstanceState(savedInstanceState.getParcelable(KEY_ADAPTER));
		}
		mType = getArguments().getInt(EXTRA_TYPE);
		mStateKey = buildStateKey(root, doc);

		if (mType == TYPE_RECENT_OPEN) {
			// Hide titles when showing recents for picking images/videos
			mHideGridTitles = MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, state.acceptMimes);
		} else {
			mHideGridTitles = (doc != null) && doc.isGridTitlesHidden();
		}

		mCallbacks = new LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
				final String query = getArguments().getString(EXTRA_QUERY);
				setListShown(false);
				Uri contentsUri;
				switch (mType) {
				case TYPE_NORMAL:
					contentsUri = DocumentsContract.buildChildDocumentsUri(doc.authority, doc.documentId);
					if (state.action == ACTION_MANAGE) {
						contentsUri = DocumentsContract.setManageMode(contentsUri);
					}
					return new DirectoryLoader(context, mType, root, doc, contentsUri, state.userSortOrder);
				case TYPE_SEARCH:
					contentsUri = DocumentsContract.buildSearchDocumentsUri(root.authority, root.rootId, query);
					if (state.action == ACTION_MANAGE) {
						contentsUri = DocumentsContract.setManageMode(contentsUri);
					}
					return new DirectoryLoader(context, mType, root, doc, contentsUri, state.userSortOrder);
				case TYPE_RECENT_OPEN:
					final RootsCache roots = DocumentsApplication.getRootsCache(context);
					return new RecentLoader(context, roots, state);
				default:
					throw new IllegalStateException("Unknown type " + mType);
				}
			}

			@Override
			public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
				if (!isAdded())
					return;

				if(null != savedInstanceState) {
					saveDisplayState();
				}
				mAdapter.swapResult(result);

				// Push latest state up to UI
				// TODO: if mode change was racing with us, don't overwrite it
				if (result.mode != MODE_UNKNOWN) {
					state.derivedMode = result.mode;
				}
                if (result.sortOrder != SORT_ORDER_UNKNOWN) {
                    state.derivedSortOrder = result.sortOrder;
                }
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						((BaseActivity) context).onStateChanged();
					}
				}, 500);

				updateDisplayState();

				// When launched into empty recents, show drawer
				if (mType == TYPE_RECENT_OPEN && mAdapter.isEmpty() && !state.stackTouched) {
					((BaseActivity) context).setRootsDrawerOpen(true);
				}
				if (isResumed()) {
					setListShown(true);
				} else {
					setListShownNoAnimation(true);
				}
				if(isWatch()) {
					Utils.setItemsCentered(getListView(), mAdapter.getItemCount() > 1);
				}
				mLastSortOrder = state.derivedSortOrder;

				restoreDisplaySate();

				if(isTelevision()){
					getListView().requestFocus();
				}
			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				mAdapter.swapResult(null);
				if(isWatch()) {
					Utils.setItemsCentered(getListView(), false);
				}
			}
		};
		setListAdapter(mAdapter);
		setListShown(false);
		// Kick off loader at least once
		LoaderManager.getInstance(getActivity()).restartLoader(mLoaderId, null, mCallbacks);

		updateDisplayState();
	}

	public void saveDisplayState(){
		// Remember last scroll location
		final SparseArray<Parcelable> container = new SparseArray<Parcelable>();
		getView().saveHierarchyState(container);
		final State state = getDisplayState(this);
		state.dirState.put(mStateKey, container);
	}

	// Restore any previous instance state
	public void restoreDisplaySate(){
		final State state = getDisplayState(this);
		final SparseArray<Parcelable> container = state.dirState.remove(mStateKey);
		if (container != null && !getArguments().getBoolean(EXTRA_IGNORE_STATE, false)) {
			getView().restoreHierarchyState(container);
		} else if (mLastSortOrder != state.derivedSortOrder) {
			getListView().smoothScrollToPosition(0);
		}
	}

    @Override
	public void onPause() {
		super.onPause();
		saveDisplayState();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateDisplayState();
		onUninstall();
	}

    public void onDisplayStateChanged() {
        updateDisplayState();
    }

	public void onUserSortOrderChanged() {
        updateUserState(StateColumns.SORT_ORDER);
		// Sort order change always triggers reload; we'll trigger state change
		// on the flip side.
		LoaderManager.getInstance(getActivity()).restartLoader(mLoaderId, null, mCallbacks);
		getListView().smoothScrollToPosition(0);
	}

    public void onUserModeChanged() {

        updateUserState(StateColumns.MODE);
		// Mode change is just visual change; no need to kick loader, and
		// deliver change event immediately.
		((BaseActivity) getActivity()).onStateChanged();

		updateDisplayState();
	}

    private void updateUserState(String column) {
        final ContentResolver resolver = getActivity().getContentResolver();
        final State state = getDisplayState(this);

        final RootInfo root = getArguments().getParcelable(EXTRA_ROOT);
        final DocumentInfo doc = getArguments().getParcelable(EXTRA_DOC);

        if (root != null && doc != null) {
            final Uri stateUri = RecentsProvider.buildState(root.authority, root.rootId, doc.documentId);
            final ContentValues values = new ContentValues();
            if(column.startsWith(StateColumns.MODE)) {
                values.put(StateColumns.MODE, state.userMode);
            } else {
                values.put(StateColumns.SORT_ORDER, state.userSortOrder);
            }

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    resolver.insert(stateUri, values);
                    return null;
                }
            }.execute();
        }
        if(column.startsWith(StateColumns.MODE)) {
            state.derivedMode = state.userMode;
        } else {
            state.derivedSortOrder = state.userSortOrder;
        }
    }

	private void updateDisplayState() {
		final State state = getDisplayState(this);

        mDefaultColor = SettingsActivity.getPrimaryColor(getActivity());
        int accentColor = SettingsActivity.getAccentColor();
        if (mLastMode == state.derivedMode &&  mLastSortOrder == state.derivedSortOrder
                && mLastShowSize == state.showSize
                && mLastShowFolderSize == state.showFolderSize
				&& mLastShowThumbnail == state.showThumbnail
				&& mLastShowHiddenFiles == state.showHiddenFiles
                && (mLastShowColor != 0 && mLastShowColor == mDefaultColor)
                && (mLastShowAccentColor != 0 && mLastShowAccentColor == accentColor))
			return;
        boolean refreshData = mLastShowHiddenFiles != state.showHiddenFiles;
		mLastMode = state.derivedMode;
        mLastSortOrder = state.derivedSortOrder;
		mLastShowSize = state.showSize;
		mLastShowFolderSize = state.showFolderSize;
		mLastShowThumbnail = state.showThumbnail;
		mLastShowHiddenFiles = state.showHiddenFiles;

        mLastShowColor = mDefaultColor;
        mProgressBar.setColor(SettingsActivity.getAccentColor());
		mIconHelper.setThumbnailsEnabled(state.showThumbnail);

		if (state.derivedMode == MODE_GRID) {
			((RecyclerViewPlus)getListView()).setType(TYPE_GRID);
		} else {
			((RecyclerViewPlus)getListView()).setType(TYPE_LIST);
		}
		mIconHelper.setViewMode(state.derivedMode);
		setItemDivider();

        ((BaseActivity) getActivity()).upadateActionItems(getListView());

        if(refreshData) {
            onUserSortOrderChanged();
        }
	}

	private void setItemDivider(){
		if(getListView().getItemDecorationCount() != 0){
			getListView().removeItemDecorationAt(0);
		}
		final Resources res = mActivity.getResources();
		RecyclerView.ItemDecoration itemDecoration = null;
		if (mLastMode == MODE_GRID) {
			itemDecoration = new MarginDecoration(mActivity);
		} else {
			// Indent our list divider to align with text
			final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
			final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
			DividerItemDecoration decoration = new DividerItemDecoration(mActivity, VERTICAL);
			if (insetLeft) {
				decoration.setInset(insetSize, 0);
			} else {
				decoration.setInset(0, insetSize);
			}
			itemDecoration = decoration;
		}
		if(!isWatch()) {
			getListView().addItemDecoration(itemDecoration);
		}
	}

	private RecyclerItemClickListener.OnItemClickListener mItemListener
			= new RecyclerItemClickListener.OnItemClickListener() {
		@Override
		public void onItemClick(View view, int position) {
			final Cursor cursor = mAdapter.getItem(position);
			if (cursor != null) {
				final String docId = getCursorString(cursor, Document.COLUMN_DOCUMENT_ID);
				final String docMimeType = getCursorString(cursor, Document.COLUMN_MIME_TYPE);
				final int docFlags = getCursorInt(cursor, Document.COLUMN_FLAGS);
				if (null != root && root.isApp()) {
/*					startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"
							+ AppsProvider.getPackageForDocId(docId))));*/
				} else if (isDocumentEnabled(docMimeType, docFlags)) {
					final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
					((BaseActivity) getActivity()).onDocumentPicked(doc);
					Bundle params = new Bundle();
					String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
					params.putString(FILE_TYPE, type);
					if(doc.isDirectory()) {
						AnalyticsManager.logEvent("browse", root, params);
					} else {
						AnalyticsManager.logEvent("open" + "_" + type, params);
					}
				}
			}
		}

		@Override
		public void onItemLongClick(View view, int position) {

		}

		@Override
		public void onItemViewClick(final View view, final int position) {
			//final int position = mCurrentView.getPositionForView(v);
			if (position != ListView.INVALID_POSITION) {
				int count = mAdapter.getCheckedItemCount();
				switch (view.getId()) {
					case android.R.id.icon:
						if (count == 0) {
							mMultiChoiceHelper.startSupportActionModeIfNeeded();
							mMultiChoiceHelper.setItemChecked(position,true, true);
						} else {
							mMultiChoiceHelper.setItemChecked(position,
									!mAdapter.isItemChecked(position), true);
						}
						break;

					case R.id.button_popup:
						view.post(new Runnable() {
							@Override
							public void run() {
								showPopupMenu(view, position);
							}
						});
						break;
				}
			}
		}
	};

	private MultiChoiceModeListener mMultiListener = new MultiChoiceModeListener() {

		private boolean editMode;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			editMode = root != null && root.isEditSupported();
			int menuId = R.menu.mode_simple_directory;
			if (null != root && root.isApp()) {
				menuId = R.menu.mode_apps;
			} else {
				menuId = R.menu.mode_directory;
			}

			mode.getMenuInflater().inflate(menuId, menu);
			int count = mAdapter.getCheckedItemCount();
			mode.setTitle(count+"");
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

			final Context context = getActivity();
			if(null != context){
				final BaseActivity activity = (BaseActivity) context;
				if(!activity.getActionMode()){
                    activity.setUpDefaultStatusBar();
					activity.setActionMode(true);
				}
			}
			final int count = mAdapter.getCheckedItemCount();
			final State state = getDisplayState(DirectoryFragment.this);

			final MenuItem open = menu.findItem(R.id.menu_open);
			final MenuItem share = menu.findItem(R.id.menu_share);
			final MenuItem delete = menu.findItem(R.id.menu_delete);
			final MenuItem rename = menu.findItem(R.id.menu_rename);

			final boolean manageMode = state.action == ACTION_BROWSE;
			final boolean canDelete = doc != null && doc.isDeleteSupported();
			final boolean canRename = doc != null && doc.isRenameSupported();
			open.setVisible(!manageMode);
			share.setVisible(manageMode);
			delete.setVisible(manageMode && canDelete);
			if(null != rename) {
				rename.setVisible(manageMode && canRename && count == 1);
			}

            if (mType == TYPE_RECENT_OPEN) {
                delete.setVisible(true);
            }
			if (isApp) {
				share.setVisible(false);
				final MenuItem save = menu.findItem(R.id.menu_save);
				save.setVisible(root.isAppPackage());
				delete.setVisible(root.isAppPackage());

			} else {
				final MenuItem edit = menu.findItem(R.id.menu_edit);
				if (edit != null) {
					edit.setVisible(manageMode);
				}
				final MenuItem info = menu.findItem(R.id.menu_info);
				final MenuItem bookmark = menu.findItem(R.id.menu_bookmark);

				final MenuItem copy = menu.findItem(R.id.menu_copy);
				final MenuItem cut = menu.findItem(R.id.menu_cut);
				final MenuItem compress = menu.findItem(R.id.menu_compress);
				copy.setVisible(editMode);
				cut.setVisible(editMode);
				compress.setVisible(editMode && !isOperationSupported);

				info.setVisible(count == 1);
				bookmark.setVisible(isSpecialDevice() && count == 1);
			}
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if(handleMenuAction(item)){
				mode.finish();
				return true;
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
            selectAll = true;
			final Context context = getActivity();
			if(null != context){
				final BaseActivity activity = (BaseActivity) context;
				activity.setActionMode(false);
                activity.setUpStatusBar();
			}
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			if (checked) {
				// Directories and footer items cannot be checked
				boolean valid = false;

				final Cursor cursor = mAdapter.getItem(position);
				if (cursor != null) {
					final String docMimeType = getCursorString(cursor, Document.COLUMN_MIME_TYPE);
					final int docFlags = getCursorInt(cursor, Document.COLUMN_FLAGS);
					// if (!Document.MIME_TYPE_DIR.equals(docMimeType)) {
					valid = isDocumentEnabled(docMimeType, docFlags);
					// }
				}

				if (!valid) {
					mAdapter.setSelected(position, false);
				}
			}

			int count = mAdapter.getCheckedItemCount();
			mode.setTitle(getResources().getString(R.string.mode_selected_count, count));
			if (count == 1 || count == 2) {
				mode.invalidate();
			}
		}
	};

	private RecyclerView.RecyclerListener mRecycleListener = new RecyclerView.RecyclerListener() {

		@Override
		public void onViewRecycled(RecyclerView.ViewHolder holder) {
			cancelThumbnailTask(holder.itemView);
			cancelFolderSizeTask(holder.itemView);
		}
	};

	private void cancelThumbnailTask(View view) {
		final ImageView iconThumb = (ImageView) view.findViewById(R.id.icon_thumb);
		if (iconThumb != null) {
			mIconHelper.stopLoading(iconThumb);
		}
	}

	private void cancelFolderSizeTask(View view) {
		final TextView size = (TextView) view.findViewById(R.id.size);
		if (size != null) {
			final FolderSizeAsyncTask oldTask = (FolderSizeAsyncTask) size.getTag();
			if (oldTask != null) {
				oldTask.preempt();
				size.setTag(null);
			}
		}
	}

	private void onShareDocuments(ArrayList<DocumentInfo> docs) {
		Intent intent;
		String mimeType = ALL_MIME_TYPES;
		if (docs.size() == 1) {
			final DocumentInfo doc = docs.get(0);
			mimeType = doc.mimeType;
			intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, doc.derivedUri);

			Bundle params = new Bundle();
			String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
			params.putString(FILE_TYPE, type);
			params.putInt(FILE_COUNT, docs.size());
			AnalyticsManager.logEvent("share"+"_"+type, params);

		} else if (docs.size() > 1) {
			intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

			final ArrayList<String> mimeTypes = new ArrayList<>();
			final ArrayList<Uri> uris = new ArrayList<>();
			for (DocumentInfo doc : docs) {
				if(!doc.isDirectory()) {
					mimeTypes.add(doc.mimeType);
					uris.add(doc.derivedUri);
				}
			}

			if(uris.isEmpty()){
				Utils.showSnackBar(getActivity(), "Nothing to share");
				return;
			}
            mimeType = MimeTypes.findCommonMimeType(mimeTypes);
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

			Bundle params = new Bundle();
			params.putInt(FILE_COUNT, docs.size());
			AnalyticsManager.logEvent("share", params);

		} else {
			return;
		}

		if(!MimePredicate.mimeMatches(MimePredicate.SHARE_SKIP_MIMES, mimeType)) {
			intent.setType(mimeType);
		} else{
			intent.setType(ALL_MIME_TYPES);
		}
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);

		intent = Intent.createChooser(intent, getActivity().getText(R.string.share_via));
        if(Utils.isIntentAvailable(getActivity(), intent)) {
            startActivity(intent);
        }
	}

	private boolean onDeleteDocuments(ArrayList<DocumentInfo> docs) {
		final Context context = getActivity();
		final ContentResolver resolver = context.getContentResolver();

		boolean hadTrouble = false;
		for (DocumentInfo doc : docs) {
			if (!doc.isDeleteSupported()) {
				Log.w(TAG, "Skipping " + doc);
				hadTrouble = true;
				continue;
			}

			try {
                hadTrouble = ! DocumentsContract.deleteDocument(resolver, doc.derivedUri);
			} catch (Exception e) {
				Log.w(TAG, "Failed to delete " + doc);
				CrashReportingManager.logException(e);
				hadTrouble = true;
			}
		}

		return hadTrouble;
	}

	private void onUninstall() {
		if (!docsAppUninstall.isEmpty()) {
			DocumentInfo doc = docsAppUninstall.get(docsAppUninstall.size() - 1);
			onUninstallApp(doc);
			docsAppUninstall.remove(docsAppUninstall.size() - 1);
		}
        else{
            if (null != root && root.isAppPackage()) {
                AppsProvider.notifyDocumentsChanged(getActivity(), root.rootId);
            }
        }
	}

	private void forceStopApps(ArrayList<DocumentInfo> docs) {
		ArrayList<String> packageList = new ArrayList<>();
		for (DocumentInfo documentInfo : docs){
			packageList.add(AppsProvider.getPackageForDocId(documentInfo.documentId));
		}

		Intent intent = new Intent(ACTION_FORCE_STOP_REQUEST);
		intent.putExtra(EXTRA_PACKAGE_NAMES, packageList);
		getActivity().sendBroadcast(intent);
	}

	private boolean onUninstallApp(DocumentInfo doc) {
		final Context context = getActivity();
		final ContentResolver resolver = context.getContentResolver();

		boolean hadTrouble = false;
		if (!doc.isDeleteSupported()) {
			Log.w(TAG, "Skipping " + doc);
			hadTrouble = true;
			return hadTrouble;
		}

		try {
			DocumentsContract.deleteDocument(resolver, doc.derivedUri);
		} catch (Exception e) {
			Log.w(TAG, "Failed to delete " + doc);
			CrashReportingManager.logException(e);
			hadTrouble = true;
		}

		return hadTrouble;
	}

	private class OperationTask extends AsyncTask<Void, Void, Boolean> {

		private Dialog progressDialog;
		private ArrayList<DocumentInfo> docs;
		private int id;

		public OperationTask(ArrayList<DocumentInfo> docs, int id) {
			this.docs = docs;
			this.id = id;
			DialogBuilder builder = new DialogBuilder(getActivity());
			builder.setCancelable(false);
			builder.setIndeterminate(true);
			saveDisplayState();
			switch (id) {
			case R.id.menu_delete:
			case R.id.menu_stop:
				if (null != root && root.isApp()) {
					builder.setMessage("Stopping processes...");
				} else {
					builder.setMessage("Deleting files...");
				}
				break;

			case R.id.menu_save:
				builder.setMessage("Saving apps...");
				break;

            case R.id.menu_uncompress:
				builder.setMessage("Uncompressing files...");
                break;

            case R.id.menu_compress:
				builder.setMessage("Compressing files...");
                break;

			default:
				break;
			}
			progressDialog = builder.create();
		}

		@Override
		protected void onPreExecute() {
			progressDialog.show();
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			Bundle params2 = new Bundle();
			boolean result = false;
			switch (id) {
			case R.id.menu_delete:
			case R.id.menu_stop:
				result = onDeleteDocuments(docs);
				break;

			case R.id.menu_save:
				result = onSaveDocuments(docs);
				params2 = new Bundle();
				params2.putInt(FILE_COUNT, docs.size());
				AnalyticsManager.logEvent("backup", params2);
				break;

            case R.id.menu_uncompress:
                result = onUncompressDocuments(docs);
				params2 = new Bundle();
				AnalyticsManager.logEvent("uncompress", params2);
                break;
            case R.id.menu_compress:
                result = onCompressDocuments(doc, docs);
				params2 = new Bundle();
				params2.putInt(FILE_COUNT, docs.size());
				AnalyticsManager.logEvent("compress", params2);
                break;
			}

			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
            if(!Utils.isActivityAlive(getActivity())) {
                return;
            }
			progressDialog.dismiss();
			if (result) {
				switch (id) {
				case R.id.menu_delete:
                    if(!((BaseActivity) getActivity()).isSAFIssue(docs.get(0).documentId)) {
						Utils.showError(getActivity(), R.string.toast_failed_delete);
                    }
					break;

				case R.id.menu_save:
                    if(!((BaseActivity) getActivity()).isSAFIssue(docs.get(0).documentId)) {
						Utils.showError(getActivity(), R.string.save_error);
                    }
					break;

                case R.id.menu_compress:
                    if(!((BaseActivity) getActivity()).isSAFIssue(docs.get(0).documentId)) {
						Utils.showError(getActivity(), R.string.compress_error);
                    }

                    break;
                case R.id.menu_uncompress:
                    if(!((BaseActivity) getActivity()).isSAFIssue(doc.documentId)) {
                        Utils.showError(getActivity(), R.string.uncompress_error);
                    }
                    break;
				}
			} else{
				if(id == R.id.menu_save) {
					Utils.showSnackBar(getActivity(), "App(s) Backed up to 'AppBackup' folder",
							Snackbar.LENGTH_LONG, "View", new OnClickListener() {
								@Override
								public void onClick(View view) {
									DocumentsActivity activity = ((DocumentsActivity)getActivity());
									activity.onRootPicked(activity.getAppsBackupRoot(), true);
								}
							});
				}
			}

			if (mType == TYPE_RECENT_OPEN) {
				onUserSortOrderChanged();
			}
		}
	}

	private void deleteFiles(final ArrayList<DocumentInfo> docs, final int id, String title) {
		DialogBuilder builder = new DialogBuilder(getActivity());
		builder.setMessage(title).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int did) {
				new OperationTask(docs, id).execute();
			}
		}).setNegativeButton(android.R.string.cancel, null);
		builder.showDialog();
	}

	public State getDisplayState(Fragment fragment) {
		return ((BaseActivity) fragment.getActivity()).getDisplayState();
	}

    private static boolean isCreateSupported(Fragment fragment) {
        return ((BaseActivity) fragment.getActivity()).isCreateSupported();
    }

	public boolean onSaveDocuments(ArrayList<DocumentInfo> docs) {
		final Context context = getActivity();
		final ContentResolver resolver = context.getContentResolver();

		boolean hadTrouble = false;
		for (DocumentInfo doc : docs) {
			if (!doc.isCopySupported()) {
				Log.w(TAG, "Skipping " + doc);
				hadTrouble = true;
				continue;
			}

			try {
				Uri appBackupUri = DocumentsContract.buildDocumentUri(
						ExternalStorageProvider.AUTHORITY, DIRECTORY_APPBACKUP);
                hadTrouble = DocumentsContract.copyDocument(resolver, doc.derivedUri, appBackupUri) == null;
			} catch (Exception e) {
				Log.w(TAG, "Failed to save " + doc);
				CrashReportingManager.logException(e);
				hadTrouble = true;
			}
		}

		return hadTrouble;
	}

    public boolean onCompressDocuments(DocumentInfo parent, ArrayList<DocumentInfo> docs) {
        final Context context = getActivity();
        final ContentResolver resolver = context.getContentResolver();

        boolean hadTrouble = false;
        if (!parent.isArchiveSupported()) {
            Log.w(TAG, "Skipping " + doc);
            hadTrouble = true;
        }

        try {
            ArrayList<String> documentIds = new ArrayList<>();
            for (DocumentInfo doc : docs){
                documentIds.add(DocumentsContract.getDocumentId(doc.derivedUri));
            }
            hadTrouble = ! DocumentsContract.compressDocument(resolver, doc.derivedUri, documentIds);
        } catch (Exception e) {
            Log.w(TAG, "Failed to Compress " + doc);
			CrashReportingManager.logException(e);
            hadTrouble = true;
        }

        return hadTrouble;
    }

    public boolean onUncompressDocuments(ArrayList<DocumentInfo> docs) {
        final Context context = getActivity();
        final ContentResolver resolver = context.getContentResolver();

        boolean hadTrouble = false;
        for (DocumentInfo doc : docs) {
            if (!doc.isArchiveSupported()) {
                Log.w(TAG, "Skipping " + doc);
                hadTrouble = true;
                continue;
            }

            try {
                hadTrouble = ! DocumentsContract.uncompressDocument(resolver, doc.derivedUri);
            } catch (Exception e) {
                Log.w(TAG, "Failed to Uncompress " + doc);
				CrashReportingManager.logException(e);
                hadTrouble = true;
            }
        }

        return hadTrouble;
    }

	private void setEmptyState() {
		boolean isEmpty = mAdapter.isEmpty();
		if(isWatch()){
			isEmpty = mAdapter.getItemCount() == 1;
		}
		if (isEmpty) {
			mEmptyView.setVisibility(View.VISIBLE);
			if(null == root){
				return;
			}
			if(root.isRootedStorage() && !isRooted()){
				mEmptyView.setText("Your phone is not rooted!");
			} else if(root.isNetworkStorage()){
				mEmptyView.setText("Couldnt connect to the server!");
			} else  {
				mEmptyView.setText(R.string.empty);
			}
		} else {
			mEmptyView.setVisibility(View.GONE);
		}
	}

	public boolean isDocumentEnabled(String docMimeType, int docFlags) {
		final State state = getDisplayState(DirectoryFragment.this);

		if (Document.MIME_TYPE_HIDDEN.equals(docMimeType)) {
			return false;
		}
		// Directories are always enabled
		if (Utils.isDir(docMimeType)) {
			return true;
		}

		// Read-only files are disabled when creating
		if (state.action == ACTION_CREATE && (docFlags & Document.FLAG_SUPPORTS_WRITE) == 0) {
			return false;
		}

		return MimePredicate.mimeMatches(state.acceptMimes, docMimeType);
	}

	private void showPopupMenu(View view, final int position) {
		PopupMenu popup = new PopupMenu(getActivity(), view);

		int menuId = R.menu.popup_simple_directory;
		if (isApp) {
			menuId = R.menu.popup_apps;
		} else {
			menuId = R.menu.popup_directory;
		}

		popup.getMenuInflater().inflate(menuId, popup.getMenu());
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				return onPopupMenuItemClick(menuItem, position);
			}
		});
		
		if (isApp) {
            final MenuItem open = popup.getMenu().findItem(R.id.menu_open);
			final MenuItem delete = popup.getMenu().findItem(R.id.menu_delete);
			final MenuItem save = popup.getMenu().findItem(R.id.menu_save);
            open.setVisible(root.isAppPackage());
			save.setVisible(root.isAppPackage());
			delete.setVisible(root.isUserApp());
		}
		else{
			final State state = getDisplayState(DirectoryFragment.this);
			final MenuItem share = popup.getMenu().findItem(R.id.menu_share);
			final MenuItem delete = popup.getMenu().findItem(R.id.menu_delete);
			final MenuItem rename = popup.getMenu().findItem(R.id.menu_rename);
			final MenuItem copy = popup.getMenu().findItem(R.id.menu_copy);
			final MenuItem cut = popup.getMenu().findItem(R.id.menu_cut);
            final MenuItem compress = popup.getMenu().findItem(R.id.menu_compress);
            final MenuItem uncompress = popup.getMenu().findItem(R.id.menu_uncompress);
            final MenuItem bookmark = popup.getMenu().findItem(R.id.menu_bookmark);

            final Cursor cursor = mAdapter.getItem(position);
            final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
			final boolean manageMode = state.action == ACTION_BROWSE;
			if(null != doc){
				final boolean isCompressed = doc != null && MimePredicate.mimeMatches(MimePredicate.COMPRESSED_MIMES, doc.mimeType);
				if(null != compress)
					compress.setVisible(manageMode && doc.isArchiveSupported() && !isCompressed && !isOperationSupported);
				if(null != uncompress)
					uncompress.setVisible(manageMode && doc.isArchiveSupported() && isCompressed && !isOperationSupported);
				if(null != bookmark) {
					bookmark.setVisible(manageMode && doc.isBookmarkSupported() && Utils.isDir(doc.mimeType) && !isOperationSupported);
				}
				share.setVisible(manageMode);
				delete.setVisible(manageMode && doc.isDeleteSupported());
				rename.setVisible(manageMode && doc.isRenameSupported());
				copy.setVisible(manageMode && doc.isCopySupported());
				cut.setVisible(manageMode && doc.isMoveSupported());
			}
		}

		popup.show();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if(handleMenuAction(item)){
			((DocumentsActivity)getActivity()).closeDrawer();
			mMultiChoiceHelper.clearChoices();
			return true;
		}
		return false;
	}

	public boolean onPopupMenuItemClick(MenuItem item, int position) {
		final ArrayList<DocumentInfo> docs = new ArrayList<>();
		final Cursor cursor = mAdapter.getItem(position);
		final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
		docs.add(doc);

		return handleMenuAction(item, docs);
	}

	public boolean handleMenuAction(MenuItem item){
		final SparseBooleanArray checked = mAdapter.getCheckedItemPositions();
		final ArrayList<DocumentInfo> docs = new ArrayList<>();
		final int size = checked.size();
		for (int i = 0; i < size; i++) {
			if (checked.valueAt(i)) {
				final Cursor cursor = mAdapter.getItem(checked.keyAt(i));
				if(null != cursor) {
					final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
					docs.add(doc);
				}
			}
		}
		if(docs.isEmpty()){
			return false;
		}
		return handleMenuAction(item, docs);
	}

	public boolean handleMenuAction(MenuItem item, ArrayList<DocumentInfo> docs) {
		final int id = item.getItemId();
		Bundle params = new Bundle();
		switch (id) {

			case R.id.menu_share:
				onShareDocuments(docs);
				return true;

			case R.id.menu_transfer:
				TransferHelper.sendDocs(getActivity(), docs);
				return true;

			case R.id.menu_copy:
				moveDocument(docs, false);
				return true;

			case R.id.menu_cut:
				moveDocument(docs, true);
				return true;

			case R.id.menu_delete:
				deleteDocument(docs, id);
				return true;

			case R.id.menu_stop:
				stopDocument(docs, id);
				return true;

			case R.id.menu_save:
			case R.id.menu_uncompress:
			case R.id.menu_compress:
				new OperationTask(docs, id).execute();
				return true;
			case R.id.menu_open:
				openDocument(docs.get(0));
				//BaseActivity.get(DirectoryFragment.this).onDocumentsPicked(docs);
				return true;
			case R.id.menu_details:
				Intent intent2 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"
						+ AppsProvider.getPackageForDocId(docs.get(0).documentId)));
				if(Utils.isIntentAvailable(getActivity(), intent2)) {
					getActivity().startActivity(intent2);
				}
				String type = IconUtils.getTypeNameFromMimeType(docs.get(0).mimeType);
				params.putString(FILE_TYPE, type);
				AnalyticsManager.logEvent("details", params);
				return true;
			case R.id.menu_info:
				infoDocument(docs.get(0));
				return true;

			case R.id.menu_rename:
				renameDocument(docs.get(0));
				return true;

			case R.id.menu_bookmark:
				bookmarkDocument(docs.get(0));
				return true;
			case R.id.menu_select_all:
				int count = mAdapter.getItemCount();
				for (int i = 0; i < count; i++) {
					mAdapter.setSelected(i, selectAll);
				}
				selectAll = !selectAll;
				params.putInt(FILE_COUNT, count);
				AnalyticsManager.logEvent("select", params);
				return false;

			default:
				return false;
		}
	}

	private void bookmarkDocument(DocumentInfo doc) {
		DocumentInfo document = doc;
		ContentValues contentValues = new ContentValues();
		contentValues.put(ExplorerProvider.BookmarkColumns.PATH, document.path);
		contentValues.put(ExplorerProvider.BookmarkColumns.TITLE, document.displayName);
		contentValues.put(ExplorerProvider.BookmarkColumns.ROOT_ID, document.displayName);
		Uri uri = getActivity().getContentResolver().insert(ExplorerProvider.buildBookmark(), contentValues);
		if(null != uri) {
			Utils.showSnackBar(getActivity(), "Bookmark added");
			RootsCache.updateRoots(getActivity(), ExternalStorageProvider.AUTHORITY);
		}
		Bundle params = new Bundle();
		AnalyticsManager.logEvent("bookmarked", root, params);
	}

	private void stopDocument(ArrayList<DocumentInfo> docs, int type) {
		Bundle params = new Bundle();
		params.putInt(FILE_COUNT, docs.size());
		if (isApp && root.isAppPackage()) {
			forceStopApps(docs);
			AnalyticsManager.logEvent("stop", params);
		} else {
			deleteFiles(docs, type, isApp && root.isAppProcess() ? "Stop processes ?" : "Delete files ?");
			AnalyticsManager.logEvent("delete", params);
		}
	}

	private void deleteDocument(ArrayList<DocumentInfo> docs, int type) {
		Bundle params = new Bundle();
		params.putInt(FILE_COUNT, docs.size());
		if (isApp && root.isAppPackage()) {
			docsAppUninstall = docs;
			onUninstall();
			AnalyticsManager.logEvent("uninstall", params);
		} else {
			deleteFiles(docs, type, "Delete files ?");
			AnalyticsManager.logEvent("delete", params);
		}
	}

	private void openDocument(DocumentInfo doc) {
		Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(AppsProvider.getPackageForDocId(doc.documentId));
		if (intent!= null) {
			if(Utils.isIntentAvailable(getActivity(), intent)) {
				getActivity().startActivity(intent);
			}
		}
		else{
			Utils.showError(getActivity(), R.string.unable_to_open_app);
		}
		Bundle params = new Bundle();
		String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
		params.putString(FILE_TYPE, type);
		AnalyticsManager.logEvent("open"+"_"+type, params);
	}

	private void moveDocument(ArrayList<DocumentInfo> docs, boolean move) {
		MoveFragment.show(getFragmentManager(), docs, move);
		Bundle params = new Bundle();
		params.putBoolean(FILE_MOVE, move);
		params.putInt(FILE_COUNT, docs.size());
		AnalyticsManager.logEvent("move_"+move, params);
	}

	private void renameDocument(DocumentInfo doc){
		RenameFragment.show(((BaseActivity) getActivity()).getSupportFragmentManager(), doc);
		Bundle params = new Bundle();
		params.putString(FILE_TYPE, IconUtils.getTypeNameFromMimeType(doc.mimeType));
		AnalyticsManager.logEvent("rename", params);
	}

	private void infoDocument(DocumentInfo doc) {
		final BaseActivity activity = (BaseActivity) getActivity();
		activity.setInfoDrawerOpen(true);
		if (activity.isShowAsDialog() || isWatch()) {
			DetailFragment.showAsDialog(activity.getSupportFragmentManager(), doc);
		} else {
			DetailFragment.show(activity.getSupportFragmentManager(), doc);
		}
		Bundle params = new Bundle();
		String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
		params.putString(FILE_TYPE, type);
		AnalyticsManager.logEvent("details", params);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_ADAPTER, mMultiChoiceHelper.onSaveInstanceState());
	}

	private synchronized ContentProviderClient getExternalStorageClient() {
		if (mExternalStorageClient == null) {
			mExternalStorageClient = getActivity().
					getContentResolver().acquireContentProviderClient(ExternalStorageProvider.AUTHORITY);
		}
		return mExternalStorageClient;
	}

	private final class AdapterEnvironment implements DocumentsAdapter.Environment {

		@Override
		public Context getContext() {
			return mActivity;
		}

		@Override
		public State getDisplayState() {
			return DirectoryFragment.this.getDisplayState(DirectoryFragment.this);
		}

		@Override
		public boolean isApp() {
			return isApp;
		}

		@Override
		public RootInfo getRoot() {
			return root;
		}

		@Override
		public DocumentInfo getDocumentInfo() {
			return doc;
		}

		@Override
		public int getType() {
			return mType;
		}

		@Override
		public boolean isDocumentEnabled(String mimeType, int flags) {
			return DirectoryFragment.this.isDocumentEnabled(mimeType, flags);
		}

		@Override
		public boolean hideGridTiles() {
			return mHideGridTitles;
		}

		@Override
		public void setEmptyState() {
			DirectoryFragment.this.setEmptyState();
		}

		@Override
		public MultiChoiceHelper getMultiChoiceHelper() {
			return mMultiChoiceHelper;
		}

		@Override
		public IconHelper getIconHelper() {
			return mIconHelper;
		}
	}

}