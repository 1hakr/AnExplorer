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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.BaseActivity.State;
import dev.dworks.apps.anexplorer.DialogFragment;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.RootCursorWrapper;
import dev.dworks.apps.anexplorer.loader.DirectoryLoader;
import dev.dworks.apps.anexplorer.loader.RecentLoader;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.IconColorUtils;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.misc.ImageUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.MimeTypes;
import dev.dworks.apps.anexplorer.misc.ProviderExecutor;
import dev.dworks.apps.anexplorer.misc.ProviderExecutor.Preemptable;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.SAFManager;
import dev.dworks.apps.anexplorer.misc.ThumbnailCache;
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
import dev.dworks.apps.anexplorer.ui.CompatTextView;
import dev.dworks.apps.anexplorer.ui.MaterialProgressBar;
import dev.dworks.apps.anexplorer.ui.MaterialProgressDialog;

import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_BROWSE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_CREATE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_MANAGE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_LIST;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_UNKNOWN;
import static dev.dworks.apps.anexplorer.BaseActivity.State.SORT_ORDER_UNKNOWN;
import static dev.dworks.apps.anexplorer.BaseActivity.TAG;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_COUNT;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_MOVE;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_TYPE;
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
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorLong;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorString;

/**
 * Display the documents inside a single directory.
 */
public class DirectoryFragment extends ListFragment {

	private CompatTextView mEmptyView;
	private ListView mListView;
	private GridView mGridView;

	private AbsListView mCurrentView;

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

	private boolean mSvelteRecents;
	private Point mThumbSize;

	private DocumentsAdapter mAdapter;
	private LoaderCallbacks<DirectoryResult> mCallbacks;
	private ArrayMap<Integer, Long> mSizes = new ArrayMap<Integer, Long>();
	private ArrayList<DocumentInfo> docsAppUninstall = new ArrayList<>();

	private final int mLoaderId = 42;
	private RootInfo root;
	private DocumentInfo doc;
	private boolean isApp;
    private int mDefaultColor;
    private MaterialProgressBar mProgressBar;
    private boolean isOperationSupported;
	private ContentProviderClient mExternalStorageClient;

	public static void showNormal(FragmentManager fm, RootInfo root, DocumentInfo doc, int anim) {
		show(fm, TYPE_NORMAL, root, doc, null, anim);
	}

	public static void showSearch(FragmentManager fm, RootInfo root, DocumentInfo doc, String query, int anim) {
		show(fm, TYPE_SEARCH, root, doc, query, anim);
	}

	public static void showRecentsOpen(FragmentManager fm, int anim) {
		show(fm, TYPE_RECENT_OPEN, null, null, null, anim);
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
        final Resources res = context.getResources();
		final View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mProgressBar = (MaterialProgressBar) view.findViewById(R.id.progressBar);

		mEmptyView = (CompatTextView)view.findViewById(android.R.id.empty);

		mListView = (ListView) view.findViewById(R.id.list);
		mListView.setOnItemClickListener(mItemListener);
		mListView.setMultiChoiceModeListener(mMultiListener);
		mListView.setRecyclerListener(mRecycleListener);

        // Indent our list divider to align with text
        final Drawable divider = mListView.getDivider();
        final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
        final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
        if (insetLeft) {
            mListView.setDivider(new InsetDrawable(divider, insetSize, 0, 0, 0));
        } else {
            mListView.setDivider(new InsetDrawable(divider, 0, 0, insetSize, 0));
        }

		mGridView = (GridView) view.findViewById(R.id.grid);
		mGridView.setOnItemClickListener(mItemListener);
		mGridView.setMultiChoiceModeListener(mMultiListener);
		mGridView.setRecyclerListener(mRecycleListener);

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		// Cancel any outstanding thumbnail requests
		final ViewGroup target = (mListView.getAdapter() != null) ? mListView : mGridView;
		final int count = target.getChildCount();
		for (int i = 0; i < count; i++) {
			final View view = target.getChildAt(i);
			mRecycleListener.onMovedToScrapHeap(view);
		}

		// Tear down any selection in progress
		mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
		mGridView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Context context = getActivity();
		final State state = getDisplayState(DirectoryFragment.this);

		root = getArguments().getParcelable(EXTRA_ROOT);
		doc = getArguments().getParcelable(EXTRA_DOC);

		if(null != root && root.isSecondaryStorage() && state.action == ACTION_BROWSE){
			if(!doc.isWriteSupported()){
				SAFManager.takeCardUriPermission(getActivity(), root, doc);
			}
		}
		isApp = root != null && root.isApp();
        isOperationSupported = root != null && (root.isRootedStorage() || root.isUsbStorage());

		mAdapter = new DocumentsAdapter();
		mType = getArguments().getInt(EXTRA_TYPE);
		mStateKey = buildStateKey(root, doc);

		if (mType == TYPE_RECENT_OPEN) {
			// Hide titles when showing recents for picking images/videos
			mHideGridTitles = MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, state.acceptMimes);
		} else {
			mHideGridTitles = (doc != null) && doc.isGridTitlesHidden();
		}

		mSvelteRecents = Utils.isLowRamDevice(context) && (mType == TYPE_RECENT_OPEN);

		mCallbacks = new LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
				final String query = getArguments().getString(EXTRA_QUERY);

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

				mAdapter.swapResult(result);

				// Push latest state up to UI
				// TODO: if mode change was racing with us, don't overwrite it
				if (result.mode != MODE_UNKNOWN) {
					state.derivedMode = result.mode;
				}
                if (result.sortOrder != SORT_ORDER_UNKNOWN) {
                    state.derivedSortOrder = result.sortOrder;
                }
				((BaseActivity) context).onStateChanged();

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
				// Restore any previous instance state
				final SparseArray<Parcelable> container = state.dirState.remove(mStateKey);
				if (container != null && !getArguments().getBoolean(EXTRA_IGNORE_STATE, false)) {
					getView().restoreHierarchyState(container);
				} else if (mLastSortOrder != state.derivedSortOrder) {
					mListView.smoothScrollToPosition(0);
					mGridView.smoothScrollToPosition(0);
				}

				mLastSortOrder = state.derivedSortOrder;
				if(isTelevision()){
					mCurrentView.requestFocus();
				}
			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				mAdapter.swapResult(null);
			}
		};
		setListAdapter(mAdapter);
		setListShown(false);
		// Kick off loader at least once
		getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);

		updateDisplayState();
	}

	@Override
	public void onStop() {
		super.onStop();

		// Remember last scroll location
		final SparseArray<Parcelable> container = new SparseArray<Parcelable>();
		getView().saveHierarchyState(container);
		final State state = getDisplayState(this);
		state.dirState.put(mStateKey, container);
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
		getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
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
        mProgressBar.setColor(mLastShowColor);
		mListView.setVisibility(state.derivedMode == MODE_LIST ? View.VISIBLE : View.GONE);
		mGridView.setVisibility(state.derivedMode == MODE_GRID ? View.VISIBLE : View.GONE);

		final int choiceMode;
		if (state.allowMultiple) {
			choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL;
		} else {
			choiceMode = ListView.CHOICE_MODE_NONE;
		}

		final int thumbSize;
		if (state.derivedMode == MODE_GRID) {
			thumbSize = getResources().getDimensionPixelSize(R.dimen.grid_width);
			mListView.setAdapter(null);
			mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
			mGridView.setAdapter(mAdapter);
			mGridView.setColumnWidth(thumbSize);
			mGridView.setNumColumns(GridView.AUTO_FIT);
			mGridView.setChoiceMode(choiceMode);
			mCurrentView = mGridView;
		} else if (state.derivedMode == MODE_LIST) {
			thumbSize = getResources().getDimensionPixelSize(R.dimen.icon_size);
			mGridView.setAdapter(null);
			mGridView.setChoiceMode(ListView.CHOICE_MODE_NONE);
			mListView.setAdapter(mAdapter);
			mListView.setChoiceMode(choiceMode);
			mCurrentView = mListView;
		} else {
			throw new IllegalStateException("Unknown state " + state.derivedMode);
		}

        ((BaseActivity) getActivity()).upadateActionItems(mCurrentView);
		mThumbSize = new Point(thumbSize, thumbSize);

        if(refreshData) {
            onUserSortOrderChanged();
        }
	}

    private OnItemClickListener mItemListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
	};

	private MultiChoiceModeListener mMultiListener = new MultiChoiceModeListener() {

		boolean selectAll = true;
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
			int count = mCurrentView.getCheckedItemCount();
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
			final int count = mCurrentView.getCheckedItemCount();
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
				bookmark.setVisible(isTelevision() && count == 1);
			}
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			final SparseBooleanArray checked = mCurrentView.getCheckedItemPositions();
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
			final int id = item.getItemId();
			switch (id) {
			case R.id.menu_open:
				BaseActivity.get(DirectoryFragment.this).onDocumentsPicked(docs);
				mode.finish();
				return true;

			case R.id.menu_share:
				onShareDocuments(docs);
				mode.finish();
				return true;

			case R.id.menu_copy:
				moveDocument(docs, false);
				mode.finish();
				return true;

			case R.id.menu_cut:
				moveDocument(docs, true);
				mode.finish();
				return true;

			case R.id.menu_delete:
				deleteDocument(docs, id);
				mode.finish();
				return true;

			case R.id.menu_stop:
				stopDocument(docs, id);
				mode.finish();
				return true;
			case R.id.menu_save:
            case R.id.menu_compress:
				new OperationTask(docs, id).execute();
				mode.finish();
				return true;

			case R.id.menu_select_all:
				int count = mAdapter.getCount();
				for (int i = 0; i < count; i++) {
					mCurrentView.setItemChecked(i, selectAll);
				}
				selectAll = !selectAll;
				Bundle params = new Bundle();
				params.putInt(FILE_COUNT, count);
				AnalyticsManager.logEvent("select", params);
				return true;

			case R.id.menu_info:
				infoDocument(docs.get(0));
				mode.finish();
				return true;

			case R.id.menu_rename:
				renameDocument(docs.get(0));
				mode.finish();
				return true;

			default:
				return false;
			}
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
					mCurrentView.setItemChecked(position, false);
				}
			}

			int count = mCurrentView.getCheckedItemCount();
			mode.setTitle(getResources().getString(R.string.mode_selected_count, count));
			if (count == 1 || count == 2) {
				mode.invalidate();
			}
		}
	};

	private RecyclerListener mRecycleListener = new RecyclerListener() {
		@Override
		public void onMovedToScrapHeap(View view) {
			final ImageView iconThumb = (ImageView) view.findViewById(R.id.icon_thumb);
			if (iconThumb != null) {
				final ThumbnailAsyncTask oldTask = (ThumbnailAsyncTask) iconThumb.getTag();
				if (oldTask != null) {
					oldTask.preempt();
					iconThumb.setTag(null);
				}
			}
			final TextView size = (TextView) view.findViewById(R.id.size);
			if (size != null) {
				final FolderSizeAsyncTask oldTask = (FolderSizeAsyncTask) size.getTag();
				if (oldTask != null) {
					oldTask.preempt();
					size.setTag(null);
				}
			}
		}
	};

	private void onShareDocuments(ArrayList<DocumentInfo> docs) {
		Intent intent;
		if (docs.size() == 1) {
			final DocumentInfo doc = docs.get(0);

			intent = new Intent(Intent.ACTION_SEND);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			// intent.addCategory(Intent.CATEGORY_DEFAULT);
            if(!MimePredicate.mimeMatches(MimePredicate.SHARE_SKIP_MIMES, doc.mimeType)) {
                intent.setType(doc.mimeType);
            }
            else{
                intent.setType(MimeTypes.ALL_MIME_TYPES);
            }
			intent.putExtra(Intent.EXTRA_STREAM, doc.derivedUri);

			Bundle params = new Bundle();
			String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
			params.putString(FILE_TYPE, type);
			params.putInt(FILE_COUNT, docs.size());
			AnalyticsManager.logEvent("share"+"_"+type, params);

		} else if (docs.size() > 1) {
			intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			// intent.addCategory(Intent.CATEGORY_DEFAULT);

			final ArrayList<String> mimeTypes = new ArrayList<>();
			final ArrayList<Uri> uris = new ArrayList<>();
			for (DocumentInfo doc : docs) {
				mimeTypes.add(doc.mimeType);
				uris.add(doc.derivedUri);
			}

            String mimeType = findCommonMimeType(mimeTypes);
            if(!MimePredicate.mimeMatches(MimePredicate.SHARE_SKIP_MIMES, mimeType)) {
                intent.setType(mimeType);
            }
            else{
                intent.setType(MimeTypes.ALL_MIME_TYPES);
            }
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

			Bundle params = new Bundle();
			params.putInt(FILE_COUNT, docs.size());
			AnalyticsManager.logEvent("share", params);

		} else {
			return;
		}

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

		private MaterialProgressDialog progressDialog;
		private ArrayList<DocumentInfo> docs;
		private int id;

		public OperationTask(ArrayList<DocumentInfo> docs, int id) {
			this.docs = docs;
			this.id = id;
			progressDialog = new MaterialProgressDialog(getActivity());
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setIndeterminate(true);
            progressDialog.setColor(mDefaultColor);
			progressDialog.setCancelable(false);

			switch (id) {
			case R.id.menu_delete:
			case R.id.menu_stop:
				if (null != root && root.isApp()) {
					progressDialog.setMessage("Stopping processes...");
				} else {
					progressDialog.setMessage("Deleting files...");
				}
				break;

			case R.id.menu_save:
				progressDialog.setMessage("Saving apps...");
				break;

            case R.id.menu_uncompress:
                progressDialog.setMessage("Uncompressing files...");
                break;

            case R.id.menu_compress:
                progressDialog.setMessage("Compressing files...");
                break;

			default:
				break;
			}
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
                        ((BaseActivity) getActivity()).showError(R.string.toast_failed_delete);
                    }
					break;

				case R.id.menu_save:
                    if(!((BaseActivity) getActivity()).isSAFIssue(docs.get(0).documentId)) {
                        ((BaseActivity) getActivity()).showError(R.string.save_error);
                    }
					break;

                case R.id.menu_compress:
                    if(!((BaseActivity) getActivity()).isSAFIssue(docs.get(0).documentId)) {
                        ((BaseActivity) getActivity()).showError(R.string.compress_error);
                    }

                    break;
                case R.id.menu_uncompress:
                    if(!((BaseActivity) getActivity()).isSAFIssue(doc.documentId)) {
                        ((BaseActivity) getActivity()).showError(R.string.uncompress_error);
                    }
                    break;
				}
			} else{
				if(id == R.id.menu_save) {
					((BaseActivity) getActivity()).
							showSnackBar("App(s) Backed up to 'AppBackup' folder",
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
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(title).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int did) {
				dialog.dismiss();
				new OperationTask(docs, id).execute();
			}
		}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int did) {
				dialog.dismiss();
			}
		});
		DialogFragment.showThemedDialog(builder);
	}

	private static State getDisplayState(Fragment fragment) {
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

	private static abstract class Footer {
		private final int mItemViewType;

		public Footer(int itemViewType) {
			mItemViewType = itemViewType;
		}

		public abstract View getView(View convertView, ViewGroup parent);

		public int getItemViewType() {
			return mItemViewType;
		}
	}

	private class LoadingFooter extends Footer {
		public LoadingFooter() {
			super(1);
		}

		@Override
		public View getView(View convertView, ViewGroup parent) {
			final Context context = parent.getContext();
			final State state = getDisplayState(DirectoryFragment.this);

			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(context);
				if (state.derivedMode == MODE_LIST) {
					convertView = inflater.inflate(R.layout.item_loading_list, parent, false);
				} else if (state.derivedMode == MODE_GRID) {
					convertView = inflater.inflate(R.layout.item_loading_grid, parent, false);
				} else {
					throw new IllegalStateException();
				}
			}

			return convertView;
		}
	}

	private class MessageFooter extends Footer {
		private final int mIcon;
		private final String mMessage;

		public MessageFooter(int itemViewType, int icon, String message) {
			super(itemViewType);
			mIcon = icon;
			mMessage = message;
		}

		@Override
		public View getView(View convertView, ViewGroup parent) {
			final Context context = parent.getContext();
			final State state = getDisplayState(DirectoryFragment.this);

			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(context);
				if (state.derivedMode == MODE_LIST) {
					convertView = inflater.inflate(R.layout.item_message_list, parent, false);
				} else if (state.derivedMode == MODE_GRID) {
					convertView = inflater.inflate(R.layout.item_message_grid, parent, false);
				} else {
					throw new IllegalStateException();
				}
			}

			final ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
			final TextView title = (TextView) convertView.findViewById(android.R.id.title);
			icon.setImageResource(mIcon);
			title.setText(mMessage);
			return convertView;
		}
	}

	private class DocumentsAdapter extends BaseAdapter implements OnClickListener {
		private Cursor mCursor;
		private int mCursorCount;

		private ArrayList<Footer> mFooters = new ArrayList<>();

		public void swapResult(DirectoryResult result) {
			mCursor = result != null ? result.cursor : null;
			mCursorCount = mCursor != null ? mCursor.getCount() : 0;

			mSizes.clear();
			mFooters.clear();

			final Bundle extras = mCursor != null ? mCursor.getExtras() : null;
			if (extras != null) {
				final String info = extras.getString(DocumentsContract.EXTRA_INFO);
				if (info != null) {
					mFooters.add(new MessageFooter(2, R.drawable.ic_dialog_info, info));
				}
				final String error = extras.getString(DocumentsContract.EXTRA_ERROR);
				if (error != null) {
					mFooters.add(new MessageFooter(3, R.drawable.ic_dialog_alert, error));
				}
				if (extras.getBoolean(DocumentsContract.EXTRA_LOADING, false)) {
					mFooters.add(new LoadingFooter());
				}
			}

			if (result != null && result.exception != null) {
				mFooters.add(new MessageFooter(3, R.drawable.ic_dialog_alert, getString(R.string.query_error)));
			}

			setEmptyState();

			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position < mCursorCount) {
				return getDocumentView(position, convertView, parent);
			} else {
				position -= mCursorCount;
				convertView = mFooters.get(position).getView(convertView, parent);
				// Only the view itself is disabled; contents inside shouldn't
				// be dimmed.
				convertView.setEnabled(false);
				return convertView;
			}
		}

		private View getDocumentView(int position, View convertView, ViewGroup parent) {
			final Context context = parent.getContext();
			final State state = getDisplayState(DirectoryFragment.this);

			// final DocumentInfo doc = getArguments().getParcelable(EXTRA_DOC);

			final RootsCache roots = DocumentsApplication.getRootsCache(context);
			final ThumbnailCache thumbs = DocumentsApplication.getThumbnailsCache(context, mThumbSize);

            if (convertView == null) {
                final LayoutInflater inflater = LayoutInflater.from(context);
                if (state.derivedMode == MODE_LIST) {
					int layoutId = R.layout.item_doc_list;
					if(isApp){
						layoutId = root.isAppProcess() ? R.layout.item_doc_process_list : R.layout.item_doc_app_list;
					}
                    convertView = inflater.inflate(layoutId, parent, false);
                } else if (state.derivedMode == MODE_GRID) {
                    convertView = inflater.inflate(R.layout.item_doc_grid, parent, false);
                } else {
                    throw new IllegalStateException();
                }
            }

			final Cursor cursor = getItem(position);

			final String docAuthority = getCursorString(cursor, RootCursorWrapper.COLUMN_AUTHORITY);
			final String docRootId = getCursorString(cursor, RootCursorWrapper.COLUMN_ROOT_ID);
			final String docId = getCursorString(cursor, Document.COLUMN_DOCUMENT_ID);
			final String docMimeType = getCursorString(cursor, Document.COLUMN_MIME_TYPE);
			final String docPath = getCursorString(cursor, Document.COLUMN_PATH);
			final String docDisplayName = getCursorString(cursor, Document.COLUMN_DISPLAY_NAME);
			final long docLastModified = getCursorLong(cursor, Document.COLUMN_LAST_MODIFIED);
			final int docIcon = getCursorInt(cursor, Document.COLUMN_ICON);
			final int docFlags = getCursorInt(cursor, Document.COLUMN_FLAGS);
			final String docSummary = getCursorString(cursor, Document.COLUMN_SUMMARY);
			final long docSize = getCursorLong(cursor, Document.COLUMN_SIZE);

			final View line1 = convertView.findViewById(R.id.line1);
			final View line2 = convertView.findViewById(R.id.line2);

			final ImageView iconMime = (ImageView) convertView.findViewById(R.id.icon_mime);
			final ImageView iconThumb = (ImageView) convertView.findViewById(R.id.icon_thumb);
            final View iconMimeBackground = convertView.findViewById(R.id.icon_mime_background);
			final TextView title = (TextView) convertView.findViewById(android.R.id.title);
			final ImageView icon1 = (ImageView) convertView.findViewById(android.R.id.icon1);
			final ImageView icon2 = (ImageView) convertView.findViewById(android.R.id.icon2);
			final TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
			final TextView date = (TextView) convertView.findViewById(R.id.date);
			final TextView size = (TextView) convertView.findViewById(R.id.size);
			final View popupButton = convertView.findViewById(R.id.button_popup);

			popupButton.setOnClickListener(this);
            popupButton.setVisibility(isTelevision() ? View.INVISIBLE : View.VISIBLE);
            if(state.action == ACTION_BROWSE){
                final View iconView = convertView.findViewById(android.R.id.icon);
                if (null != iconView) {
                    iconView.setOnClickListener(this);
                }
            }

			final ThumbnailAsyncTask oldTask = (ThumbnailAsyncTask) iconThumb.getTag();
			if (oldTask != null) {
				oldTask.preempt();
				iconThumb.setTag(null);
			}

			iconMime.animate().cancel();
			iconThumb.animate().cancel();

			final boolean supportsThumbnail = (docFlags & Document.FLAG_SUPPORTS_THUMBNAIL) != 0;
			final boolean allowThumbnail = (state.derivedMode == MODE_GRID) || MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, docMimeType);
			final boolean showThumbnail = supportsThumbnail && allowThumbnail && !mSvelteRecents && state.showThumbnail;

            final boolean enabled = isDocumentEnabled(docMimeType, docFlags);
            final float iconAlpha = (state.derivedMode == MODE_LIST && !enabled) ? 0.5f : 1f;

            iconMimeBackground.setVisibility(View.VISIBLE);
            iconMimeBackground.setBackgroundColor(IconColorUtils.loadMimeColor(context, docMimeType, docAuthority, docId, mDefaultColor));
            boolean cacheHit = false;
			if (showThumbnail) {
				final Uri uri = DocumentsContract.buildDocumentUri(docAuthority, docId);
				final Bitmap cachedResult = thumbs.get(uri);
				if (cachedResult != null) {
					iconThumb.setScaleType(Utils.isAPK(docMimeType) && !TextUtils.isEmpty(docPath) ? ImageView.ScaleType.CENTER_INSIDE
									: ImageView.ScaleType.CENTER_CROP);
					iconThumb.setImageBitmap(cachedResult);
                    iconMimeBackground.setVisibility(View.INVISIBLE);
					cacheHit = true;
				} else {
					iconThumb.setImageDrawable(null);
					final ThumbnailAsyncTask task = new ThumbnailAsyncTask(uri, iconMime, iconThumb, iconMimeBackground, mThumbSize,
                            docPath, docMimeType, iconAlpha);
					iconThumb.setTag(task);
					ProviderExecutor.forAuthority(docAuthority).execute(task);
				}
			}

			// Always throw MIME icon into place, even when a thumbnail is being
			// loaded in background.
			if (cacheHit) {
				iconMime.setAlpha(0f);
				iconMime.setImageDrawable(null);
				iconThumb.setAlpha(1f);
			} else {
				iconMime.setAlpha(1f);
				iconThumb.setAlpha(0f);
				iconThumb.setImageDrawable(null);
				if (docIcon != 0) {
					iconMime.setImageDrawable(IconUtils.loadPackageIcon(context, docAuthority, docIcon));
				} else {
					iconMime.setImageDrawable(IconUtils.loadMimeIcon(context, docMimeType, docAuthority, docId, state.derivedMode));
				}
			}

			boolean hasLine1 = false;
			boolean hasLine2 = false;

			final boolean hideTitle = (state.derivedMode == MODE_GRID) && mHideGridTitles;
			if (!hideTitle) {
				title.setText(docDisplayName);
				hasLine1 = true;
			}

			Drawable iconDrawable = null;
			if (mType == TYPE_RECENT_OPEN) {
				// We've already had to enumerate roots before any results can
				// be shown, so this will never block.
				final RootInfo root = roots.getRootBlocking(docAuthority, docRootId);
                if (state.derivedMode == MODE_GRID) {
                    iconDrawable = root.loadGridIcon(context);
                } else {
                    iconDrawable = root.loadIcon(context);
                }

				if (summary != null) {
					final boolean alwaysShowSummary = getResources().getBoolean(R.bool.always_show_summary);
					if (alwaysShowSummary) {
						summary.setText(root.getDirectoryString());
						summary.setVisibility(View.VISIBLE);
						hasLine2 = true;
					} else {
						if (iconDrawable != null && roots.isIconUniqueBlocking(root)) {
							// No summary needed if icon speaks for itself
							summary.setVisibility(View.INVISIBLE);
						} else {
							summary.setText(root.getDirectoryString());
							summary.setVisibility(View.VISIBLE);
							// summary.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_END);
							hasLine2 = true;
						}
					}
				}
			} else {
				// Directories showing thumbnails in grid mode get a little icon
				// hint to remind user they're a directory.
				if (Utils.isDir(docMimeType) && state.derivedMode == MODE_GRID && showThumbnail) {
                    iconDrawable = IconUtils.applyTintAttr(context, R.drawable.ic_root_folder,
                            android.R.attr.textColorPrimaryInverse);
				}

				if (summary != null) {
					if (docSummary != null) {
						summary.setText(docSummary);
						summary.setVisibility(View.VISIBLE);
						hasLine2 = true;
					} else {
						summary.setVisibility(View.INVISIBLE);
					}
				}
			}

			if (icon1 != null)
				icon1.setVisibility(View.GONE);
			if (icon2 != null)
				icon2.setVisibility(View.GONE);

			if (iconDrawable != null) {
				if (hasLine1) {
					icon1.setVisibility(View.GONE);
					//icon1.setImageDrawable(iconDrawable);
				} else {
					icon2.setVisibility(View.VISIBLE);
					icon2.setImageDrawable(iconDrawable);
				}
			}

			if (docLastModified == -1) {
				date.setText(null);
			} else {
				date.setText(Utils.formatTime(context, docLastModified));
				hasLine2 = true;
			}

			final FolderSizeAsyncTask oldSizeTask = (FolderSizeAsyncTask) size.getTag();
			if (oldSizeTask != null) {
				oldSizeTask.preempt();
				size.setTag(null);
			}
			if (state.showSize) {
				size.setVisibility(View.VISIBLE);
				if (Utils.isDir(docMimeType) || docSize == -1) {
					size.setText(null);
					if (state.showFolderSize) {
						long sizeInBytes = mSizes.containsKey(position) ? mSizes.get(position) : -1;
						if (sizeInBytes != -1) {
							size.setText(Formatter.formatFileSize(context, sizeInBytes));
						} else {
							final FolderSizeAsyncTask task = new FolderSizeAsyncTask(size, docPath, position);
							size.setTag(task);
							ProviderExecutor.forAuthority(docAuthority).execute(task);
						}
					}
				} else {
					size.setText(Formatter.formatFileSize(context, docSize));
					hasLine2 = true;
				}
			} else {
				size.setVisibility(View.GONE);
			}

			if (line1 != null) {
				line1.setVisibility(hasLine1 ? View.VISIBLE : View.GONE);
			}
			if (line2 != null) {
				line2.setVisibility(hasLine2 ? View.VISIBLE : View.GONE);
			}

            setEnabledRecursive(convertView, enabled);

            iconMime.setAlpha(iconAlpha);
            iconThumb.setAlpha(iconAlpha);
            if (icon1 != null) icon1.setAlpha(iconAlpha);
            if (icon2 != null) icon2.setAlpha(iconAlpha);

			return convertView;
		}

		@Override
		public int getCount() {
			return mCursorCount + mFooters.size();
		}

		@Override
		public Cursor getItem(int position) {
			if (position < mCursorCount) {
				mCursor.moveToPosition(position);
				return mCursor;
			} else {
				return null;
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return 4;
		}

		@Override
		public int getItemViewType(int position) {
			if (position < mCursorCount) {
				return 0;
			} else {
				position -= mCursorCount;
				return mFooters.get(position).getItemViewType();
			}
		}

		@Override
		public void onClick(final View v) {

			final int position = mCurrentView.getPositionForView(v);
			if (position != ListView.INVALID_POSITION) {
				int count = mCurrentView.getCheckedItemCount();
				switch (v.getId()) {
				case android.R.id.icon:
					if (count == 0) {
						ActionMode mChoiceActionMode = null;
						if (mChoiceActionMode == null && (mChoiceActionMode = mCurrentView.startActionMode(mMultiListener)) != null) {
							mCurrentView.setItemChecked(position, true);
							mCurrentView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
						}
					} else {
						mCurrentView.setItemChecked(position, !mCurrentView.isItemChecked(position));
						mCurrentView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
					}
					break;

				case R.id.button_popup:
					v.post(new Runnable() {
						@Override
						public void run() {
							showPopupMenu(v, position);
						}
					});
					break;
				}
			}

		}
	}

	private void setEmptyState() {
		if (mAdapter.isEmpty()) {
			mEmptyView.setVisibility(View.VISIBLE);
			if(null == root){
				return;
			}
			if(root.isRootedStorage() && !isRooted()){
				mEmptyView.setText("Your phone is not rooted!");
			} else if(root.isNetworkStorage()){
				mEmptyView.setText("Couldnt connect to the server!");
			}
		} else {
			mEmptyView.setVisibility(View.GONE);
		}
	}

	private static class ThumbnailAsyncTask extends AsyncTask<Uri, Void, Bitmap> implements Preemptable {
		private final Uri mUri;
		private final ImageView mIconMime;
		private final ImageView mIconThumb;
        private final View mIconMimeBackground;
		private final Point mThumbSize;
        private final float mTargetAlpha;
		private final CancellationSignal mSignal;
		private final String mPath;
		private final String mMimeType;

        public ThumbnailAsyncTask(Uri uri, ImageView iconMime, ImageView iconThumb, View iconMimeBackground, Point thumbSize,
                String path, String mimeType, float targetAlpha) {
			mUri = uri;
			mIconMime = iconMime;
			mIconThumb = iconThumb;
            mIconMimeBackground = iconMimeBackground;
			mThumbSize = thumbSize;
            mTargetAlpha = targetAlpha;
			mSignal = new CancellationSignal();
			mPath = path;
			mMimeType = mimeType;
		}

		@Override
		public void preempt() {
			cancel(false);
			mSignal.cancel();
		}

		@Override
		protected Bitmap doInBackground(Uri... params) {
			if (isCancelled())
				return null;

			final Context context = mIconThumb.getContext();
			final ContentResolver resolver = context.getContentResolver();

			ContentProviderClient client = null;
			Bitmap result = null;
			try {
				if (Utils.isAPK(mMimeType)) {
					result = ((BitmapDrawable) IconUtils.loadPackagePathIcon(context, mPath, Document.MIME_TYPE_APK)).getBitmap();
				} else {
					client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, mUri.getAuthority());
					result = DocumentsContract.getDocumentThumbnail(resolver, mUri, mThumbSize, mSignal);
				}
				if (null == result){
					result = ImageUtils.getThumbnail(mPath, mMimeType, mThumbSize.x, mThumbSize.y);
				}
				if (result != null) {
					final ThumbnailCache thumbs = DocumentsApplication.getThumbnailsCache(context, mThumbSize);
					thumbs.put(mUri, result);
				}
			} catch (Exception e) {
				if (!(e instanceof OperationCanceledException)) {
					Log.w(TAG, "Failed to load thumbnail for " + mUri + ": " + e);
				}
				CrashReportingManager.logException(e);
			} finally {
				ContentProviderClientCompat.releaseQuietly(client);
			}
			return result;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
            if (isCancelled()) {
                result = null;
            }
            if (mIconThumb.getTag() == this && result != null) {
				mIconThumb.setScaleType(Utils.isAPK(mMimeType) ? ImageView.ScaleType.CENTER_INSIDE : ImageView.ScaleType.CENTER_CROP);
				mIconThumb.setTag(null);
				mIconThumb.setImageBitmap(result);
                mIconMimeBackground.setVisibility(View.INVISIBLE);

				final float targetAlpha = mIconMime.isEnabled() ? 1f : 0.5f;
				mIconMime.setAlpha(targetAlpha);
				mIconMime.animate().alpha(0f).start();
				mIconThumb.setAlpha(0f);
				mIconThumb.animate().alpha(targetAlpha).start();
			}
		}
	}

	private class FolderSizeAsyncTask extends AsyncTask<Uri, Void, Long> implements Preemptable {
		private final TextView mSizeView;
		private final CancellationSignal mSignal;
		private final String mPath;
		private final int mPosition;

		public FolderSizeAsyncTask(TextView sizeView, String path, int position) {
			mSizeView = sizeView;
			mSignal = new CancellationSignal();
			mPath = path;
			mPosition = position;
		}

		@Override
		public void preempt() {
			cancel(false);
			mSignal.cancel();
		}

		@Override
		protected Long doInBackground(Uri... params) {
			if (isCancelled())
				return null;

			Long result = null;
			try {
				if (!TextUtils.isEmpty(mPath)) {
					File dir = new File(mPath);
					result = Utils.getDirectorySize(dir);
				}
			} catch (Exception e) {
				if (!(e instanceof OperationCanceledException)) {
					Log.w(TAG, "Failed to calculate size for " + mPath + ": " + e);
				}
				CrashReportingManager.logException(e);
			}
			return result;
		}

		@Override
		protected void onPostExecute(Long result) {
            if (isCancelled()) {
                result = null;
            }
			if (mSizeView.getTag() == this && result != null) {
				mSizeView.setTag(null);
				String size = Formatter.formatFileSize(mSizeView.getContext(), result);
				mSizeView.setText(size);
				mSizes.put(mPosition, result);
			}
		}
	}

	private String findCommonMimeType(ArrayList<String> mimeTypes) {
		String[] commonType = mimeTypes.get(0).split("/");
		if (commonType.length != 2) {
			return "*/*";
		}

		for (int i = 1; i < mimeTypes.size(); i++) {
			String[] type = mimeTypes.get(i).split("/");
			if (type.length != 2)
				continue;

			if (!commonType[1].equals(type[1])) {
				commonType[1] = "*";
			}

			if (!commonType[0].equals(type[0])) {
				commonType[0] = "*";
				commonType[1] = "*";
				break;
			}
		}

		return commonType[0] + "/" + commonType[1];
	}

	private void setEnabledRecursive(View v, boolean enabled) {
		if (v == null)
			return;
		if (v.isEnabled() == enabled)
			return;
		v.setEnabled(enabled);

		if (v instanceof ViewGroup) {
			final ViewGroup vg = (ViewGroup) v;
			for (int i = vg.getChildCount() - 1; i >= 0; i--) {
				setEnabledRecursive(vg.getChildAt(i), enabled);
			}
		}
	}

	private boolean isDocumentEnabled(String docMimeType, int docFlags) {
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

	public boolean onPopupMenuItemClick(MenuItem item, int position) {
		final ArrayList<DocumentInfo> docs = new ArrayList<>();
		final Cursor cursor = mAdapter.getItem(position);
		final DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
		docs.add(doc);

		final int id = item.getItemId();
		switch (id) {
		case R.id.menu_share:
			onShareDocuments(docs);
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
            return true;
        case R.id.menu_details:
            Intent intent2 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"
                    + AppsProvider.getPackageForDocId(docs.get(0).documentId)));
            if(Utils.isIntentAvailable(getActivity(), intent2)) {
                getActivity().startActivity(intent2);
            }
            Bundle params = new Bundle();
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
			((BaseActivity) getActivity()).showInfo("Bookmark added");
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
			((BaseActivity) getActivity()).showError(R.string.unable_to_open_app);
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
		if (activity.isShowAsDialog()) {
			DetailFragment.showAsDialog(activity.getSupportFragmentManager(), doc);
		} else {
			DetailFragment.show(activity.getSupportFragmentManager(), doc);
		}
		Bundle params = new Bundle();
		String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
		params.putString(FILE_TYPE, type);
		AnalyticsManager.logEvent("details", params);
	}

	private synchronized ContentProviderClient getExternalStorageClient() {
		if (mExternalStorageClient == null) {
			mExternalStorageClient = getActivity().
					getContentResolver().acquireContentProviderClient(ExternalStorageProvider.AUTHORITY);
		}
		return mExternalStorageClient;
	}
}