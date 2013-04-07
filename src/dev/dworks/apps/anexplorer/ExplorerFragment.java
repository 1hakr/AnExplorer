package dev.dworks.apps.anexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.CmdListItem;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.FileList;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.FileNavList;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.GalleryFilter;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.MODES;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.OnFragmentInteractionListener;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.SearchFilter;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.TYPES;

/**
 * @author HaKr
 * 
 */
public class ExplorerFragment extends SherlockListFragment implements
		OnQueryTextListener, OnScrollListener, OnTouchListener {

	private static final String TAG = "Explorer";
	// private static final int FLAG_UPDATED_SYS_APP = 0x80;
	private static final String CURRENT_PATH = null;

	private List<FileList> fileListEntries = null;

	private List<String> navigationListPaths = null;
	// icons cached
	public SparseIntArray iconCache = new SparseIntArray();
	public LruCache<Integer, Bitmap> thumbCache;
	public HashMap<Long, Integer> albumArtCache;
	public SparseArray<String> processTypeCache;
	public String[] newlist;

	// file path and empty view
	RelativeLayout titlePane;
	private TextView mypath, empty;
	private TextView selectCount, selectCount2;
	private String root = "/";
	private String incomingPath, currentPath, originalPath, searchOriginalPath;
	private String selectedFilePath, contextFilePath = "", copyPath = "";
	private boolean isRoot = true, isSource = true, multiSelectMode = false;
	private String queryString, resultCount = "";

	private Context context;
	private ExplorerOperations newFileExplorer = new ExplorerOperations();

	private File mainFile = new File("root");
	private File[] resultFiles = null;
	private List<String> copiedFilesList = null;
	private List<String> appNameList = new ArrayList<String>();

	private ListAdapter fileListAdapter;
	private List<Parcelable> fileListState, NavListState;
	private boolean filesCopied = false;
	private ProgressBar progress;
	private SearchTask searchTask;
	private GalleryTask galleryTask;
	private LoadListTask loadListTask;
	private boolean isGoBack = false;
	private boolean isResetList = false;
	private boolean searchPathLock = false;
	private boolean reSearchGallery = false;
	private boolean explicitlyRunSU = false;
	private boolean isSelectFromNavigation = false;
	private boolean copyORCut = false;
	private Long parentFolderSize;

	// preferences
	private SharedPreferences preference = null;
	private boolean showImageThumbnails, showVideoThumbnails, showApkThumbnails, showAlbumArts;
	private boolean showFolderSizes;
	private boolean showHiddenFolders;
	private boolean showThumbScroll;
	private boolean showStorage;

	private static boolean stopTasks;
	private boolean hasRootAccess;
	private boolean hasMountWrite;

	private String sortType;
	private String sortOrder;
	private boolean sortingOder;
	private boolean showDateModified = false;
	private boolean showSmallDateModified;
	private boolean showNavigationPane;
	private Comparator<File> sortingType;
	private Comparator<CmdListItem> sortingTypeCmd = ExplorerOperations.typeAscendingSU;
	private String[] imageProjection = { 
			MediaStore.Images.Media._ID,
			MediaStore.Images.Media.DATA, 
			MediaStore.Images.Media.DISPLAY_NAME };
	private String[] videoProjection = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA };
	private String[] audioProjection = { MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA };
	private String[] audioAlbumProjection = { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART };
	private CursorLoader imageCursorLoader;
	private CursorLoader videoCursorLoader;
	private CursorLoader audioCursorLoader;
	private CursorLoader audioAlbumCursorLoader;

	private Bitmap apkBitmap;// , imageBitmap, audioBitmap;
	private PackageManager packageManager;
	private ActivityManager activityManager;
	private PackageInfo packageInfo;
	private GridView gridView;
	private ListView listView_explorer;
	private boolean checkDevice;
	private boolean runSU = false, appBackupMultiSelect = false;
	private ExplorerOperations explorerOperationsSU;
	private int curNavPosition = -1;
	private File[] fileList;

	private AdView adView;

	private MODES mode;
	private TYPES type;
	private ActionMode actionMode;
	private AnActionMode actionModeCallback;
	private View multiSelectActionBarView;
	private boolean show = true, wasPaused;
	private boolean loadList = false;
	private int scrollStateAll;
	private int itemId;
	private File[] resultFilesList;

	private ArrayList<FileNavList> fileListNavEntries;
	private Bundle args;

	private Cursor cursor, cursorAudio;
	private Long origId;
	private OnFragmentInteractionListener mListener;
	private View view;
	private boolean isAttached;
	private boolean isCurrentList;
	private String currentpath_onpause;
	
	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 * 
	 * @param param1
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment HomeFragment.
	 */
	public static ExplorerFragment newInstance(Bundle bundle) {
		ExplorerFragment fragment = new ExplorerFragment();
		Bundle args = bundle;
		fragment.setArguments(args);
		fragment.setMode(MODES.None);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (getArguments() != null) {
			args = getArguments();
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_explorer, container, false);
		context = this.getSherlockActivity();
		// get preferences
		preference = PreferenceManager.getDefaultSharedPreferences(context);
		getSharedPreference();
		fillBitmapCache();
		initMode();

		type = ExplorerOperations.isPhone(context) ? TYPES.Phone : TYPES.Tablet;
		wasPaused = false;
		scrollStateAll = OnScrollListener.SCROLL_STATE_IDLE;
		checkDevice = ExplorerOperations.checkDevice();
		newFileExplorer.setContext(context);

		setupActionBar();
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initControls();
		if(null != savedInstanceState){
			String path = savedInstanceState.getString(CURRENT_PATH, "");
			currentPath = !ExplorerOperations.isEmpty(path) ? path : currentPath;
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		loadList = true;
		if (!wasPaused) {
			if(currentpath_onpause != ""){
				currentPath = currentpath_onpause;
				currentpath_onpause = "";
			}
			show();
		}
		this.onConfigurationChanged(getResources().getConfiguration());

		copiedFilesList = new ArrayList<String>();
		if (actionMode != null)
			actionMode.finish();
		super.onResume();
	}

	@Override
	public void onPause() {
		wasPaused = true;
		loadList = false;
		currentpath_onpause = currentPath;
		super.onPause();
	}

	/*
	 * @Override protected void onRestart() { super.onRestart(); if(!runSU){
	 * //createThumbnails = true; resetList(currentPath); } if(actionMode !=
	 * null) actionMode.finish(); }
	 */	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			isAttached = true;
			mListener = (OnFragmentInteractionListener) activity;
			currentpath_onpause = "";
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		isAttached = false;
		super.onDetach();
		mListener = null;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(CURRENT_PATH, currentPath);
		super.onSaveInstanceState(outState);
	}

	/*
	 * @Override protected void onNewIntent(Intent intent) { Bundle newbundle =
	 * intent.getExtras(); currentPath = incomingPath =
	 * newbundle.getString(ExplorerOperations.CONSTANT_PATH);
	 * this.onConfigurationChanged(getResources().getConfiguration());
	 * updateMenu(); showList(currentPath); super.onNewIntent(intent); }
	 */

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		boolean showNavigation = false, isPhone = type == TYPES.Phone;
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			showDateModified = !isPhone;
			showSmallDateModified = showNavigationPane;
			showNavigation = isPhone ? false : true;
		} else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			showDateModified = !ExplorerOperations.isSmall(context);
			showSmallDateModified = isPhone ? showNavigationPane : false;
			showNavigation = true;
		}
		showNavigation = show
				&& showNavigation
				&& showNavigationPane
				&& (mode == MODES.ExplorerMode || mode == MODES.RootMode || searchPathLock);
	}
	
	public int getlayoutId(){
		itemId = isCurrentList ? R.layout.row_item : R.layout.grid_item;		
		if(currentPath.compareTo(ExplorerOperations.DIR_APP_BACKUP) == 0 ||
				currentPath.compareTo(ExplorerOperations.DIR_APP_PROCESS) == 0 || 
				currentPath.compareTo(ExplorerOperations.DIR_GALLERY_HIDDEN) == 0){
			itemId = isCurrentList ? R.layout.row_item_special : R.layout.grid_item;
		}
		else if(currentPath.compareTo(ExplorerOperations.DIR_WALLPAPER) == 0){
			itemId = R.layout.grid_wallpaper_item;
		}
		return itemId;
	}
	
	public void setMode(MODES mode){
		this.mode = mode;
	}

	public void initMode(){
		if (mode == MODES.FileMode || mode == MODES.SearchMode) {
			//currentPath = originalPath = incomingPath = ExplorerOperations.DIR_SDCARD;
			currentPath = originalPath = incomingPath = args.getString("path");
			queryString = args.getString("query");
		} else {
			//bundle = getIntent().getBundleExtra(SearchManager.APP_DATA);
			currentPath = originalPath = incomingPath = args.getString(ExplorerOperations.CONSTANT_PATH);
			if(currentPath.compareTo(ExplorerOperations.DIR_APP_BACKUP) == 0){
				mode = MODES.AppMode;
				File newAppBackupFile = new File(ExplorerOperations.DIR_APP_BACKUP);
				if(!newAppBackupFile.exists()){
					newAppBackupFile.mkdir();
				}
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_APP_PROCESS) == 0){
				mode = MODES.ProcessMode;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_GALLERY_HIDDEN) == 0){
				mode = MODES.HideFromGalleryMode;
			}
			else if(currentPath.compareTo(ExplorerOperations.DIR_WALLPAPER) == 0){
				mode = MODES.WallpaperMode;
				isCurrentList = false;
			}			
			
			runSU = hasRootAccess && hasMountWrite && originalPath.equals(root);
			
			if(mode == MODES.None){
				mode = originalPath.equals(root) ? MODES.RootMode : MODES.ExplorerMode;
				if(runSU)
					explorerOperationsSU = new ExplorerOperations(runSU);
			}
		}
	}

	private final class AnActionMode implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.context_options, menu);
			mode.setCustomView(multiSelectActionBarView);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if (originalPath.equals(root) && !ExplorerOperations.isExtStorage(currentPath)) {
				menu.findItem(R.id.menu_compress).setVisible(false);
				menu.findItem(R.id.menu_delete).setVisible(canUseSU(currentPath));
				menu.findItem(R.id.menu_cut).setVisible(canUseSU(currentPath));
			}

			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			boolean finishMode = false;
			switch (item.getItemId()) {

			case R.id.menu_copy:
				copyORCut = true;
				finishMode = copySelectedFiles(copyORCut);
				break;

			case R.id.menu_cut:
				copyORCut = false;
				finishMode = copySelectedFiles(copyORCut);
				break;

			case R.id.menu_delete:
				contextFilePath = "";
				explicitlyRunSU = canUseSU(currentPath);
				finishMode = showSelectedDialog(ExplorerOperations.DIALOG_DELETE, true);
				explicitlyRunSU = false;
				break;

			case R.id.menu_share:
				finishMode = showSelectedDialog(ExplorerOperations.DIALOG_SHARE, true);
				break;

			case R.id.menu_compress:
				finishMode = showSelectedDialog(ExplorerOperations.DIALOG_COMPRESS, true);
				break;

			default:
				return false;
			}

			if (finishMode) {
				mode.finish();
				return true;
			}
			return false;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
			unSelectAllFiles(true);
		}
	}

	private void setupActionBar() {
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();

		ViewGroup v = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.actionbar, null);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(v, new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));

		selectCount2 = (TextView) v.findViewById(R.id.count_selected);
		if (multiSelectActionBarView == null) {
			multiSelectActionBarView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.actionbar, null);
			selectCount = (TextView) multiSelectActionBarView.findViewById(R.id.count_selected);
		}

		actionModeCallback = new AnActionMode();
	}

	/**
	 * Resets the list for a current path. this is useful after the operations
	 * like Delete, rename etc
	 * 
	 * @param path
	 *            The path to which the list needs to be reset
	 */
	protected void resetList(String path) {
		isResetList = true;
		fileListState.add(isCurrentList ? getListView().onSaveInstanceState() : gridView.onSaveInstanceState());

		switch (mode) {
		case SearchMode:
			if (!searchPathLock) {
				searchTask = new SearchTask();
				searchTask.execute(incomingPath, queryString);
			}
			break;

		case HideFromGalleryMode:
			if (reSearchGallery) {
				unSelectAllFiles(false);
				clearListAdapter();
				galleryTask = new GalleryTask();
				galleryTask.execute("");
				reSearchGallery = false;
			}
			break;

		case WallpaperMode:
			break;

		default:
			showList(path);
			unSelectAllFiles(false);
			break;
		}
	}

	protected void unSelectApps() {
		for (int pos = 0; pos < fileListEntries.size(); pos++) {
			fileListEntries.get(pos).setSelection(0);
		}
		fileListAdapter.notifyDataSetChanged();
	}

	private void setThumbnail(int position, Bitmap bitmap) {
		synchronized (thumbCache) {
			if (getThumbnail(position) == null && bitmap != null) {
				thumbCache.put(position, bitmap);
			}
		}
	}

	private Bitmap getThumbnail(int position) {
		return thumbCache.get(position);
	}

	/**
	 * Initialize the controls
	 */
	public void initControls() {
		initData();
		// ad
		adView = (AdView) view.findViewById(R.id.adView);

		mypath = (TextView) view.findViewById(R.id.pathTitle);
		titlePane = (RelativeLayout) view.findViewById(R.id.title_pane);

		empty = (TextView) view.findViewById(R.id.listempty);
		gridView = (GridView) view.findViewById(R.id.grid_explorer);
		listView_explorer = (ListView) view.findViewById(android.R.id.list);
		progress = (ProgressBar) view.findViewById(android.R.id.progress);

		fileListState = new ArrayList<Parcelable>();
		NavListState = new ArrayList<Parcelable>();
		packageManager = context.getPackageManager();
		activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		gridView.setColumnWidth(convertDp2Px(type == TYPES.Tablet && checkDevice ? 80 : 70));
		gridView.setHorizontalSpacing(convertDp2Px(type == TYPES.Tablet && checkDevice ? 10 : 5));
		gridView.setVerticalSpacing(convertDp2Px(type == TYPES.Tablet && checkDevice ? 10 : 5));
		if (type == TYPES.Tablet && checkDevice) {
			gridView.setPadding(10, 10, 10, 10);
		} else {
			gridView.setPadding(10, 0, 10, 0);
		}
		// int padding = type == TYPES.Tablet && checkDevice ? 10 : 0;

		if (mode == MODES.WallpaperMode) {
			gridView.setColumnWidth(convertDp2Px(100));
			gridView.setHorizontalSpacing(0);
			gridView.setVerticalSpacing(0);
			gridView.setPadding(0, 0, 0, 0);
		}
		fileListAdapter = new ListAdapter(context, itemId);

		initDataPost();
	}

	private void initDataPost() {

		AbsListView absListView = isCurrentList ? getListView() : gridView;
		if (isCurrentList) {
			gridView.setVisibility(View.GONE);
			getListView().setAdapter(fileListAdapter);
			gridView.setAdapter(null);
		} else {
			listView_explorer.setVisibility(View.GONE);
			gridView.setEmptyView(empty);
			gridView.setAdapter(fileListAdapter);
			getListView().setAdapter(null);
		}
		absListView.setTextFilterEnabled(true);
		absListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				onListClick(v, position, id);
			}
		});
		registerForContextMenu(absListView);
		absListView.setFastScrollEnabled(showThumbScroll);
		absListView.setOnScrollListener(ExplorerFragment.this);
		// change list dynamically
		fileListAdapter.setNotifyOnChange(true);
	}

	private void initData() {
		final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		final int cacheSize = 1024 * 1024 * memClass / 4;
		thumbCache = new LruCache<Integer, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(Integer key, Bitmap value) {
				return super.sizeOf(key, value);
			}
		};

		imageCursorLoader = new CursorLoader(context);
		imageCursorLoader.setUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		imageCursorLoader.setProjection(imageProjection);

		videoCursorLoader = new CursorLoader(context);
		videoCursorLoader.setUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
		videoCursorLoader.setProjection(videoProjection);
		videoCursorLoader.setSelection(MediaStore.Video.Media.DATA + "=?");

		audioCursorLoader = new CursorLoader(context);
		audioCursorLoader.setUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		audioCursorLoader.setProjection(audioProjection);
		audioCursorLoader.setSelection(MediaStore.Audio.Media.DATA + " LIKE ?");

		audioAlbumCursorLoader = new CursorLoader(context);
		audioAlbumCursorLoader.setUri(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI);
		audioAlbumCursorLoader.setProjection(audioAlbumProjection);
		audioAlbumCursorLoader.setSelection(MediaStore.Audio.Albums._ID + "=?");
	}

	public void show() {
		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSherlockActivity().getSupportActionBar().setTitle(getTitle(mode));

		switch (mode) {
		case SearchMode:
			search();
			break;
		case HideFromGalleryMode:
			titlePane.setVisibility(View.GONE);
			galleryTask = new GalleryTask();
			galleryTask.execute("");
			break;
		case WallpaperMode:
			multiSelectMode = true;
			titlePane.setVisibility(View.GONE);
			showList(incomingPath);
			break;
		case AppMode:
			multiSelectMode = true;
			titlePane.setVisibility(View.GONE);
			showList(incomingPath);			
			break;
		case ProcessMode:
			multiSelectMode = true;
			titlePane.setVisibility(View.GONE);
			fillProcessType();
			showList(incomingPath);		
			break;			
		default:
			showList(currentPath);
			break;
		}
	}

	/**
	 * This is used to generate the result File's list to be show after search
	 */
	public void search() {
		fileListState = new ArrayList<Parcelable>();
		mypath.setText(format2String(R.string.msg_search_results));
		incomingPath = ExplorerOperations.isEmpty(incomingPath) ? ExplorerOperations.DIR_SDCARD : incomingPath; 
		
		searchTask = new SearchTask();
		searchTask.execute(incomingPath, queryString);
	}

	/**
	 * This method is used to show the List of files of a particular path
	 * 
	 * @param dirPath
	 *            The path for which the list of files needs to be shown
	 */
	private void showList(String dirPath) {
		loadList = true;
		currentPath = dirPath;
		mypath.setText(dirPath);
		mainFile = new File(dirPath);
		curNavPosition = -1;

		if (originalPath.equals(root)) {
			isRoot = dirPath.equals(root) ? true : false;
			isSource = dirPath.equals(originalPath) ? true : false;
		} else {
			if (!ExplorerOperations.isSpecialMode(mode) && (!mainFile.exists() || !mainFile.canRead())) {
				Toast.makeText(
						context,
						incomingPath == ExplorerOperations.DIR_SDCARD ? format2String(R.string.msg_sdcard_unmounted) : format2String(R.string.msg_folder_not_present),
						Toast.LENGTH_SHORT).show();
				return;
			}

			isRoot = dirPath.equals(root) ? true : false;
			isSource = dirPath.equals(originalPath) ? true : false;
		}

		showList(mainFile.listFiles());
		isSelectFromNavigation = false;
	}

	private void fillNavData() {
		if (mode == MODES.ExplorerMode || mode == MODES.RootMode) {
			if (!isSelectFromNavigation
					&& ((type == TYPES.Tablet && showNavigationPane) || (type == TYPES.Phone && showNavigationPane))) {
				if (searchPathLock) {
					showNavigationList(resultFiles);
				} else if (runSU) {
					if (incomingPath.equals(root) && !ExplorerOperations.isExtStorage(currentPath)) {
						show = false;
						clearNavList();
					} else if (ExplorerOperations.isExtStorage(currentPath)) {
						show = true;
						showNavigationList(isRoot ? mainFile.listFiles() : mainFile.getParentFile().listFiles());
					} else {
						show = false;
						clearNavList();
					}
					this.onConfigurationChanged(getResources().getConfiguration());
				} else {
					showNavigationList(isRoot ? mainFile.listFiles() : mainFile.getParentFile().listFiles());
				}
			}
		}
	}

	private void clearNavList() {
		Bundle bundle = new Bundle();
		bundle.putInt("operation", 3);
		bundle.putInt("action", 2);
		mListener.onFragmentInteraction(bundle);
	}

	private void showNavigationList(File[] listResultFiles) {
		fileListNavEntries = new ArrayList<FileNavList>();
		navigationListPaths = new ArrayList<String>();
		FileNavList fileNavList;
		ExplorerOperations navFileExplorer = new ExplorerOperations();
		File[] fileList = listResultFiles;

		Arrays.sort(fileList, sortingType);
		int i = 0;
		for (File eachFile : fileList) {
			String name = "", path = "";
			int icon, special_icon;
			navFileExplorer.setFile(eachFile);
			navFileExplorer.setContext(context);
			if (navFileExplorer.isDirectory && navFileExplorer.canRead
					&& (showHiddenFolders || navFileExplorer.canShow())) {
				name = eachFile.getName();
				path = eachFile.getAbsolutePath();
				navigationListPaths.add(path);
				icon = navFileExplorer.getFileBasicType();
				icon = ExplorerOperations.isExtStoragePath(path) ? 0 : iconCache.get(98);
				special_icon = icon == 0 ? 0 : iconCache.get(icon);
				fileNavList = new FileNavList(path, name, icon, i);
				fileNavList.setSpecialIcon(special_icon);
				fileListNavEntries.add(fileNavList);
				i++;
			}
		}
	}

	/**
	 * This method is used to show the List of files
	 * 
	 * @param listResultFiles
	 *            This is the File's list to be show in the Activity
	 */
	private void showList(File[] listResultFiles) {
		loadList = true;
		resultFilesList = listResultFiles;
		loadListTask = new LoadListTask();
		clearListAdapter();
		if (loadListTask.getStatus() == AsyncTask.Status.PENDING) {
			loadListTask.execute("");
		}
	}

	public List<FileList> fillData(File[] listResultFiles) {
		fileListEntries = new ArrayList<FileList>();
		albumArtCache = new HashMap<Long, Integer>();
		thumbCache.evictAll();

		FileList fileListItem;
		String fileCount = "", fileAccess = "", fileDateModified = "", fileSmallDateModified = "";
		String path = "", name = "", size = "", packageName = "";
		Integer icon;
		ApplicationInfo appInfo = null;
		if (stopTasks) {
			return null;
		}
		if (mode == MODES.ProcessMode) {
			List<RunningAppProcessInfo> runningProcessesList = activityManager.getRunningAppProcesses();
			int i = 0;
			for (RunningAppProcessInfo processInfo : runningProcessesList) {
				if (processInfo.importance != RunningAppProcessInfo.IMPORTANCE_EMPTY
						&& processInfo.importance != RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
					if (stopTasks) {
						break;
					}
					String process = (String) (processInfo.processName);
					process = process.substring(process.lastIndexOf(".") + 1, process.length());
					try {
						appInfo = packageManager.getPackageInfo(processInfo.processName, PackageManager.GET_ACTIVITIES).applicationInfo;
						name = (String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : process);
					} catch (NameNotFoundException e) {
					}

					if (ExplorerOperations.isEmpty(name)) {
						continue;
					} else {
						name = process;
					}

					path = processInfo.processName;
					icon = 99;
					fileCount = processTypeCache.get(processInfo.importance);
					size = Formatter.formatShortFileSize(context, getProcessSize(processInfo.pid));
					packageName = processInfo.processName;
					fileListItem = new FileList(path, name, size, icon, i, 0);
					fileListItem.setInfo(fileCount, fileAccess, fileDateModified, fileSmallDateModified);
					fileListItem.setpackageName(packageName);
					fileListEntries.add(fileListItem);
					i++;
				}
			}
		} else if (mode == MODES.AppMode) {
			int i = 0;
			List<PackageInfo> allAppList = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
			for (PackageInfo pkgInfo : allAppList) {
				appInfo = pkgInfo.applicationInfo;
				if (stopTasks) {
					break;
				}
				if (appInfo.flags != 0) {
					String ver = "";
					try {
						name = (String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : appInfo.packageName);
						ver = pkgInfo.versionName == null ? "" : pkgInfo.versionName;
					} catch (Exception e) { }
					
					path = appInfo.sourceDir;
					icon = 5;
					fileCount = ver;
					size = Formatter.formatShortFileSize(context, new File(appInfo.sourceDir).length());
					packageName = pkgInfo.packageName;
					fileListItem = new FileList(path, name, size, icon, i, 0);
					fileListItem.setInfo(fileCount, fileAccess, fileDateModified, fileSmallDateModified);
					fileListItem.setpackageName(packageName);
					fileListEntries.add(fileListItem);
					i++;
				}
			}
		} else if (mode == MODES.HideFromGalleryMode) {
			fileList = listResultFiles;
			Arrays.sort(fileList, sortingType);
			int i = 0;
			for (File eachFile : fileList) {
				if (stopTasks) {
					break;
				}
				eachFile = eachFile.getParentFile();
				newFileExplorer.setFile(eachFile);
				newFileExplorer.setContext(context);
				if (newFileExplorer.canRead && newFileExplorer.canShow()) {
					fileCount = (newFileExplorer.isDirectory ? eachFile.list().length + " files" : "");
					try {
						path = eachFile.getCanonicalPath();
					} catch (IOException e) {
					}
					name = eachFile.getName();
					icon = -9;
					fileListItem = new FileList(path, name, size, icon, i, 0);
					fileListItem.setInfo(fileCount, fileAccess, fileDateModified, fileSmallDateModified);
					fileListEntries.add(fileListItem);
					i++;
				}
			}
		} else if (mode == MODES.WallpaperMode) {
			imageCursorLoader.setSelection(MediaStore.Images.Media.DATA+ " NOT LIKE ?");
			imageCursorLoader.setSelectionArgs(new String[] { "%DCIM%" });
			cursor = imageCursorLoader.loadInBackground();

			if (cursor != null && cursor.moveToFirst()) {
				int i = 0;
				do {
					if (stopTasks) {
						break;
					}
					origId = Long.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
					path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
					name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
					icon = 14;
					fileCount = origId.toString();
					fileListItem = new FileList(path, name, size, icon, i, 0);
					fileListItem.setInfo(fileCount, fileAccess, fileDateModified, fileSmallDateModified);
					fileListEntries.add(fileListItem);
					i++;
				} while (cursor.moveToNext());
			}
			cursor.close();
		} else {
			if (canUseSU(currentPath)) {
				String command = "ls -l " + currentPath;
				// Log.i(TAG, command);
				// ExplorerOperations.runCommand("mount -o remount,rw "+currentPath);
				// | awk '{print $1,\";\",$4,\";\",$5$6$7,\";\",$8}'
				ArrayList<CmdListItem> result = explorerOperationsSU.runCommand(command);
				// explorerOperationsSU.getRootDirListing();
				if (result != null) {
					fileListEntries = new ArrayList<FileList>();
					File eachFile = null;
					CmdListItem[] array = (CmdListItem[]) result.toArray(new CmdListItem[0]);
					Arrays.sort(array, sortingTypeCmd);
					int i = 0;
					for (CmdListItem cmdListItem : array) {
						if (stopTasks) {
							break;
						}
						name = cmdListItem.getName();
						path = cmdListItem.getType() == 0 ? currentPath + name + "/" : currentPath + name;
						eachFile = new File(currentPath + name);
						newFileExplorer.setFile(eachFile);
						newFileExplorer.setContext(context);
						fileCount = (newFileExplorer.isDirectory ? (eachFile.list() == null ? "~" : eachFile.list().length)+ " files" : "");
						fileDateModified = String.format("%tr, %tF", eachFile.lastModified(), eachFile.lastModified());
						fileSmallDateModified = String.format("%tF",eachFile.lastModified());

						if (!ExplorerOperations.isEmpty(name)) {
							icon = newFileExplorer.getFileBasicType();
							fileAccess = cmdListItem.getPermission().substring(0, 3);
							size = cmdListItem.getType() == 0 ? "" : Formatter.formatShortFileSize(context, Long.valueOf(String.valueOf(eachFile.length())));
							fileListItem = new FileList(path, name, size, icon, i, 0);
							fileListItem.setInfo(fileCount, fileAccess, fileDateModified, fileSmallDateModified);
							fileListEntries.add(fileListItem);
							i++;
						}
					}
				}
			} else {
				fileList = listResultFiles;
				if (fileList == null && fileList.length == 0) {
					return null;
				}
				Arrays.sort(fileList, sortingType);
				int i = 0;
				for (File eachFile : fileList) {
					if (stopTasks) {
						break;
					}
					newFileExplorer.setFile(eachFile);
					newFileExplorer.setContext(context);
					if (newFileExplorer.canRead && (showHiddenFolders || newFileExplorer.canShow())) {
						fileCount = (newFileExplorer.isDirectory ? eachFile.list().length + " files" : "");
						fileDateModified = String.format("%tr, %tF", eachFile.lastModified(), eachFile.lastModified());
						fileSmallDateModified = String.format("%tF", eachFile.lastModified());
						fileAccess = ExplorerOperations.getFilePermissions(eachFile);

						try {
							path = eachFile.getCanonicalPath();
						} catch (IOException e) {
						}
						name = eachFile.getName();
						icon = newFileExplorer.getFileBasicType();
						size = showStorage ? "" : newFileExplorer.fileLength();
						fileListItem = new FileList(path, name, size, icon, i, 0);
						fileListItem.setInfo(fileCount, fileAccess, fileDateModified, fileSmallDateModified);
						fileListEntries.add(fileListItem);
						i++;
					}
				}
			}
		}

		return fileListEntries;
	}

	public boolean canUseSU(String path) {
		return runSU && !(ExplorerOperations.isExtStorage(path));
	}

	/**
	 * Clears the data in the file adapter
	 */
	public void clearListAdapter() {

		if (fileListAdapter != null) {
			fileListAdapter.clear();
			fileListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		int menu_id;

		switch (mode) {
		case AppMode:
		case ProcessMode:
		case HideFromGalleryMode:
		case WallpaperMode:
			menu_id = R.menu.options_others;
			break;

		default:
			menu_id = R.menu.options;
			break;
		}
		getSherlockActivity().getSupportMenuInflater().inflate(menu_id, menu);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean showSearchMenu = false, showMenuAppBackup = false;
		boolean nonExplorerMode = ExplorerOperations.isSpecialMode(mode);
		boolean showOthers;
		SearchView searchView;

		switch (mode) {
		case AppMode:
			showMenuAppBackup = true;
			showOthers = true;
			searchView = new SearchView(getSherlockActivity().getSupportActionBar().getThemedContext());
			searchView.setOnQueryTextListener(this);
			searchView.setSubmitButtonEnabled(false);
			searchView.setQueryHint("Filter Apps");
			menu.findItem(R.id.menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			menu.findItem(R.id.menu_search).setActionView(searchView);
			break;

		case ProcessMode:
			menu.findItem(R.id.menu_uninstall).setIcon(R.drawable.ic_menu_stop);
			menu.findItem(R.id.menu_uninstall).setTitle("Stop process");
			showOthers = true;
			searchView = new SearchView(getSherlockActivity().getSupportActionBar().getThemedContext());
			searchView.setOnQueryTextListener(this);
			searchView.setSubmitButtonEnabled(false);
			searchView.setQueryHint("Filter Processes");
			menu.findItem(R.id.menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			menu.findItem(R.id.menu_search).setActionView(searchView);
			break;

		case HideFromGalleryMode:
			menu.findItem(R.id.menu_uninstall).setIcon(R.drawable.ic_menu_unhide);
			menu.findItem(R.id.menu_uninstall).setTitle("Unhide from Gallery");
			showOthers = true;
			searchView = new SearchView(getSherlockActivity().getSupportActionBar().getThemedContext());
			searchView.setOnQueryTextListener(this);
			searchView.setSubmitButtonEnabled(false);
			searchView.setQueryHint("Filter Hidden Folders");
			menu.findItem(R.id.menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			menu.findItem(R.id.menu_search).setActionView(searchView);
			break;

		case WallpaperMode:
			menu.findItem(R.id.menu_uninstall).setIcon(R.drawable.ic_menu_set);
			menu.findItem(R.id.menu_uninstall).setTitle("Set Wallpaper");
			menu.findItem(R.id.menu_search).setVisible(false);
			showOthers = true;
			break;

		case SearchMode:
			showSearchMenu = false;
			showOthers = false;
			break;

		default:
			showSearchMenu = true;
			showMenuAppBackup = false;
			showOthers = false;
			searchView = new SearchView(getSherlockActivity().getSupportActionBar().getThemedContext());
			searchView.setOnQueryTextListener(this);
			searchView.setSubmitButtonEnabled(true);
			searchView.setQueryHint("Search Storage");
			//searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
			menu.findItem(R.id.menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			menu.findItem(R.id.menu_search).setActionView(searchView);
			menu.findItem(R.id.menu_view).setIcon(isCurrentList ? R.drawable.ic_menu_grid_view : R.drawable.ic_menu_list_view);
			break;
		}

		if (showOthers) {
			menu.findItem(R.id.menu_select_unselect).setVisible(nonExplorerMode);
			menu.findItem(R.id.menu_uninstall).setVisible(nonExplorerMode);

			// app backup menu
			menu.findItem(R.id.menu_save).setVisible(showMenuAppBackup);
			// menu.findItem(R.id.menu_clear_cache).setVisible(showMenuAppBackup);
		} else {
			// options menu
			menu.findItem(R.id.menu_paste).setVisible(filesCopied);
			menu.findItem(R.id.menu_create).setVisible(showSearchMenu ? !originalPath.equals(root) : searchPathLock);

			menu.findItem(R.id.menu_search).setVisible(showSearchMenu);

		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			finishActiviy();
			break;
			
		case R.id.menu_view:
			isCurrentList = !isCurrentList;
			initDataPost();
			updateMenu();
			break;

		case R.id.menu_create:
			contextFilePath = currentPath;
			explicitlyRunSU = canUseSU(currentPath);
			showSelectedDialog(ExplorerOperations.DIALOG_CREATE, false);
			explicitlyRunSU = false;
			break;

		case R.id.menu_share:
			showSelectedDialog(ExplorerOperations.DIALOG_SHARE, true);
			break;

		case R.id.menu_compress:
			showSelectedDialog(ExplorerOperations.DIALOG_COMPRESS, true);
			break;

		case R.id.menu_select_mode:
			multiSelect();
			openCAB();
			break;

		case R.id.menu_select_all:
			multiSelectOnOff(true);
			selectAllFiles();
			openCAB();
			break;

		case R.id.menu_select_unselect:
			appBackupMultiSelect = !appBackupMultiSelect;
			selectAllOnOff(appBackupMultiSelect);
			break;

		case R.id.menu_paste:
			setInfo("");
			explicitlyRunSU = canUseSU(currentPath);
			multiSelectOnOff(false);
			showSelectedDialog(copyORCut ? ExplorerOperations.DIALOG_PASTE : ExplorerOperations.DIALOG_CUT, false);
			filesCopied = false;
			updateMenu();
			explicitlyRunSU = false;
			break;

		case R.id.menu_setting:
			showSettings();
			break;

		case R.id.menu_about:
			showSelectedDialog(ExplorerOperations.DIALOG_ABOUT, false);
			break;

		case R.id.menu_save:
			contextFilePath = "";
			if (mode == MODES.AppMode) {
				currentPath = ExplorerOperations.DIR_APP_BACKUP;
				showSelectedDialog(ExplorerOperations.DIALOG_SAVE, true);
			}
			break;

		case R.id.menu_uninstall:
			contextFilePath = "";
			if (mode == MODES.AppMode) {
				currentPath = ExplorerOperations.DIR_APP_BACKUP;
				showSelectedDialog(ExplorerOperations.DIALOG_UNINSTALL, true);
			} else if (mode == MODES.ProcessMode) {
				currentPath = ExplorerOperations.DIR_APP_PROCESS;
				showSelectedDialog(ExplorerOperations.DIALOG_STOP_PROCESS, true);
			} else if (mode == MODES.HideFromGalleryMode) {
				currentPath = ExplorerOperations.DIR_GALLERY_HIDDEN;
				reSearchGallery = true;
				showSelectedDialog(ExplorerOperations.DIALOG_GALLERY_UNHIDE, true);
			} else if (mode == MODES.WallpaperMode) {
				currentPath = ExplorerOperations.DIR_WALLPAPER;
				showSelectedDialog(ExplorerOperations.DIALOG_SET_WALLPAPER, true);
			}
			break;

		case R.id.menu_clear_cache:
			contextFilePath = "";
			if (mode == MODES.AppMode) {
				currentPath = ExplorerOperations.DIR_APP_BACKUP;
				showSelectedDialog(ExplorerOperations.DIALOG_CLEAR_CACHE, true);
			}

			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		final int position = ((AdapterContextMenuInfo) menuInfo).position; 
		if (mode == MODES.AppMode || mode == MODES.ProcessMode) {
			selectedFilePath = fileListAdapter.getItem(position).getpackageName();
			menu.setHeaderTitle(R.string.app_option);
			menu.setHeaderIcon(mode == MODES.AppMode ? iconCache.get(5) : iconCache.get(99));
			menu.add(0, ExplorerOperations.CONTEXT_MENU_APP_OPEN, 0, R.string.constant_open_app);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_APP_DETAILS, 0, R.string.constant_app_details);
			if (mode == MODES.AppMode) {
				menu.add(0, ExplorerOperations.CONTEXT_MENU_APP_MARKET, 0, R.string.constant_market);
			} else {
				menu.add(0, ExplorerOperations.CONTEXT_MENU_STOP_PROCESS, 0, R.string.constant_stop_process);
			}
		} else if (mode == MODES.HideFromGalleryMode) {
			selectedFilePath = fileListAdapter.getItem(position).getPath();
			menu.setHeaderTitle(R.string.hide_option);
			menu.setHeaderIcon(iconCache.get(96));
			menu.add(0, ExplorerOperations.CONTEXT_MENU_UNHIDE_FOLDER, 0, "Unhide from Gallery");
			menu.add(0, ExplorerOperations.CONTEXT_MENU_PROPERTIES, 0, R.string.constant_details).setEnabled(!multiSelectMode);
		} else if (mode == MODES.WallpaperMode) {
			selectedFilePath = fileListAdapter.getItem(position).getPath();
			menu.setHeaderTitle("Wallpaper");
			menu.setHeaderIcon(iconCache.get(4));
			menu.add(0, ExplorerOperations.CONTEXT_MENU_PROPERTIES, 0, R.string.constant_details);
		} else {
			if (multiSelectMode) {
				return;
			}
			boolean showHideGallery = true;
			// current selected file
			selectedFilePath = fileListAdapter.getItem(position).getPath();
			showHideGallery = !isRoot && new File(selectedFilePath).isDirectory() && !new File(selectedFilePath + "/.nomedia").exists();
			boolean showSU = originalPath.equals(root) && !ExplorerOperations.isExtStorage(currentPath) ? canUseSU(currentPath) : true;

			menu.setHeaderTitle(R.string.folder_option);
			menu.setHeaderIcon(R.drawable.ic_menu_edit);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_CUT, 0, R.string.constant_cut).setEnabled(!filesCopied && showSU);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_COPY, 0, R.string.constant_copy).setEnabled(!filesCopied);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_PASTE, 0, R.string.constant_paste).setEnabled(filesCopied && showSU);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_RENAME, 0, R.string.constant_rename).setEnabled(showSU && !multiSelectMode);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_DELETE, 0, R.string.constant_delete).setEnabled(showSU);

			if (showHideGallery) {
				menu.add(0, ExplorerOperations.CONTEXT_MENU_HIDE_FOLDER, 0, R.string.hide_folder);
			} else {
				menu.add(0, ExplorerOperations.CONTEXT_MENU_UNHIDE_FOLDER, 0, R.string.unhide_folder);
			}

			boolean isZip = ExplorerOperations.getMIMEType(new File(selectedFilePath)).equalsIgnoreCase("application/zip");
			if (isZip)
				menu.add(0, ExplorerOperations.CONTEXT_MENU_EXTRACT, 0, R.string.constant_extract);

			menu.add(0, ExplorerOperations.CONTEXT_MENU_OPEN_FILE_FOLDER, 0, R.string.constant_open_file)
				.setEnabled(mode == MODES.SearchMode && !multiSelectMode);
			menu.add(0, ExplorerOperations.CONTEXT_MENU_PROPERTIES, 0, R.string.constant_details)
				.setEnabled(!multiSelectMode);
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		switch (item.getItemId()) {
		case ExplorerOperations.CONTEXT_MENU_RENAME:
			explicitlyRunSU = canUseSU(currentPath);
			contextFilePath = selectedFilePath;
			showSelectedDialog(ExplorerOperations.DIALOG_RENAME, true);
			explicitlyRunSU = false;
			break;

		case ExplorerOperations.CONTEXT_MENU_CUT:
			copyORCut = false;
			copySelectedFiles(copyORCut);
			break;

		case ExplorerOperations.CONTEXT_MENU_COPY:
			copyORCut = true;
			copySelectedFiles(copyORCut);
			break;

		case ExplorerOperations.CONTEXT_MENU_DELETE:
			contextFilePath = selectedFilePath;
			explicitlyRunSU = canUseSU(currentPath);
			showSelectedDialog(ExplorerOperations.DIALOG_DELETE, true);
			explicitlyRunSU = false;
			break;

		case ExplorerOperations.CONTEXT_MENU_PASTE:
			explicitlyRunSU = canUseSU(currentPath);
			if (copyPath == currentPath) {
				Toast.makeText(context, format2String(R.string.msg_cant_copy), Toast.LENGTH_SHORT).show();
			} else {
				setInfo("");
				showSelectedDialog(copyORCut ? ExplorerOperations.DIALOG_PASTE : ExplorerOperations.DIALOG_CUT, false);
				filesCopied = false;
			}
			explicitlyRunSU = false;
			break;

		case ExplorerOperations.CONTEXT_MENU_EXTRACT:
			contextFilePath = selectedFilePath;
			showSelectedDialog(ExplorerOperations.DIALOG_UNCOMPRESS, false);
			break;

		case ExplorerOperations.CONTEXT_MENU_SELECTALL:
			selectAllFiles();
			break;

		case ExplorerOperations.CONTEXT_MENU_UNSELECTALL:
			unSelectAllFiles(false);
			multiSelectOnOff(false);
			break;

		case ExplorerOperations.CONTEXT_MENU_OPEN_FILE_FOLDER:
			File newFile = new File(selectedFilePath);
			openFolder(newFile.isDirectory() ? newFile.getPath() : newFile.getParent());
			break;

		case ExplorerOperations.CONTEXT_MENU_PROPERTIES:
			contextFilePath = selectedFilePath;
			showSelectedDialog(ExplorerOperations.DIALOG_PROPERTIES, false);
			break;

		case ExplorerOperations.CONTEXT_MENU_APP_DETAILS:
			openAppDetails(selectedFilePath);
			break;

		case ExplorerOperations.CONTEXT_MENU_APP_OPEN:
			try {
				startActivity(packageManager.getLaunchIntentForPackage(selectedFilePath));
			} catch (Exception e) {
				Toast.makeText(context, "Cant Open", Toast.LENGTH_SHORT).show();
			}

			break;
		case ExplorerOperations.CONTEXT_MENU_APP_MARKET:
			startActivity(new Intent().setData(Uri.parse("market://details?id="+ selectedFilePath)));
			break;
		case ExplorerOperations.CONTEXT_MENU_STOP_PROCESS:
			activityManager.killBackgroundProcesses(selectedFilePath);
			break;
		case ExplorerOperations.CONTEXT_MENU_HIDE_FOLDER:
			try {
				new File(selectedFilePath + "/.nomedia").createNewFile();
			} catch (IOException e) {
			}
			context.sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.fromFile(new File(selectedFilePath))));

			MediaScannerConnection.scanFile(context, new String[] { new File(
					selectedFilePath).getAbsolutePath() }, null,
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) {
							// Log.i("ExternalStorage", "Scanned " + path +
							// ":");
							// Log.i("ExternalStorage", "-> uri=" + uri);
						}
					});
			break;
		case ExplorerOperations.CONTEXT_MENU_UNHIDE_FOLDER:
			new File(selectedFilePath + "/.nomedia").delete();
			context.sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.fromFile(new File(selectedFilePath))));

			MediaScannerConnection.scanFile(context, new String[] { new File(selectedFilePath).toString() }, null,
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) {
							// Log.i("ExternalStorage", "-> uri=" + uri);
						}
					});
			break;
		}

		return super.onContextItemSelected(item);
	}

	/**
	 * @param isCopy
	 */
	private boolean copySelectedFiles(boolean isCopy) {
		copiedFilesList.clear();
		if (multiSelectMode) {
			copiedFilesList = getSelectedFileList();
		} else {
			copiedFilesList.add(selectedFilePath);
		}

		if (copiedFilesList.size() == 0) {
			filesCopied = false;
			Toast.makeText(context, format2String(R.string.msg_file_not_selected), Toast.LENGTH_SHORT).show();
			return false;
		} else {
			multiSelectOnOff(true);
			filesCopied = true;
			unSelectAllFiles(false);
			copyPath = currentPath;
			setInfo2(copiedFilesList.size() + "*");
			Toast.makeText(context, copiedFilesList.size()+ " "+ (isCopy ? format2String(R.string.msg_files_copied) : format2String(R.string.msg_files_cut)),
					Toast.LENGTH_SHORT).show();
			updateMenu();
			return true;
		}
	}

	private long getProcessSize(int pid) {
		android.os.Debug.MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(new int[] { pid });
		return memInfos[0].getTotalPss() * 1024;
	}

	/**
	 * @param path
	 * @return
	 */
	private Bitmap getAudioThumbnail(int position) {
		if (stopTasks) {
			return null;
		}
		String pathLocal = fileListEntries.get(position).getPath();
		audioCursorLoader.setSelectionArgs(new String[] { pathLocal.replaceAll("'", "''") });
		cursorAudio = audioCursorLoader.loadInBackground();
		if (cursorAudio != null && cursorAudio.moveToFirst()) {
			Long albumId = Long.valueOf(cursorAudio.getString(cursorAudio.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
			cursorAudio.close();
			audioAlbumCursorLoader.setSelectionArgs(new String[] { String.valueOf(albumId) });
			cursorAudio = audioAlbumCursorLoader.loadInBackground();
			if (albumArtCache.get(albumId) == null) {
				if (cursorAudio != null && cursorAudio.moveToFirst()) {
					String imagePath = cursorAudio.getString(cursorAudio.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
					cursorAudio.close();
					if (imagePath != null) {
						final Bitmap bitmap = getImageThumbnail(imagePath,MediaStore.Images.Thumbnails.MICRO_KIND);
						albumArtCache.put(albumId, position);
						return bitmap;
					}
				}
			} else {
				final Bitmap bitmap = getThumbnail(albumArtCache.get(albumId));
				return bitmap;
			}
		}
		return null;
	}

	/**
	 * @param path
	 * @return
	 */
	private Bitmap getVideoThumbnail(String path) {
		if (stopTasks) {
			return null;
		}
		videoCursorLoader.setSelectionArgs(new String[] { path.replaceAll("'", "''") });
		cursor = videoCursorLoader.loadInBackground();
		if (cursor != null && cursor.moveToFirst()) {
			origId = Long.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID)));
			cursor.close();
			return MediaStore.Video.Thumbnails.getThumbnail(
					context.getContentResolver(), origId,
					MediaStore.Video.Thumbnails.MICRO_KIND, null);
		} else {
			return ThumbnailUtils.createVideoThumbnail(path,
					MediaStore.Images.Thumbnails.MICRO_KIND);
		}
	}

	/**
	 * @param path
	 * @return
	 */
	private Bitmap getImageThumbnail(String path, int kind) {
		if (stopTasks) {
			return null;
		}
		imageCursorLoader.setSelection(MediaStore.Images.Media.DATA + "=?");
		imageCursorLoader.setSelectionArgs(new String[] { path.replaceAll("'", "''") });
		cursor = imageCursorLoader.loadInBackground();

		if (cursor != null && cursor.moveToFirst()) {
			origId = Long.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
			cursor.close();
			try {
				final Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), origId, kind, null);
				if (kind == MediaStore.Images.Thumbnails.MINI_KIND) {
					return ThumbnailUtils.extractThumbnail(bitmap, 200, 200, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
				}
				return bitmap;
			} catch (Exception e) {
				return null;
			}
		} else {
			return ExplorerOperations.getThumbnailBitmap2(path);
		}

	}

	/**
	 * @param path
	 * @return
	 */
	private Bitmap getApkThumbnail(String path) {
		if (stopTasks) {
			return null;
		}
		if (mode == MODES.ProcessMode) {
			try {
				packageInfo = packageManager.getPackageInfo(path, PackageManager.GET_ACTIVITIES);
				if (packageInfo != null) {
					packageInfo.applicationInfo.sourceDir = packageInfo.applicationInfo.publicSourceDir = path;
					// know issue with nine patch image instead of drawable
					return ((BitmapDrawable) packageManager.getApplicationIcon(path)).getBitmap();
				}
			} catch (NameNotFoundException e) {
			}
		} else {
			packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
			if (packageInfo != null) {
				packageInfo.applicationInfo.sourceDir = packageInfo.applicationInfo.publicSourceDir = path;
				// know issue with nine patch image instead of drawable
				return ((BitmapDrawable) packageManager.getApplicationIcon(packageInfo.applicationInfo)).getBitmap();
			}
		}
		return apkBitmap;
	}

	/**
	 * This is used in , when the Application is called in Select mode
	 * 
	 * @param data
	 *            The URI of the file that needs to be sent
	 */
	private void sendBackResult(File data) {
		cancelTasks();
		Bundle bundle = new Bundle();
		bundle.putInt("operation", 0);
		bundle.putInt("action", 3);
		bundle.putString("file", data.getAbsolutePath().toString());
		mListener.onFragmentInteraction(bundle);		
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		onListClick(v, position, id);
	}

	private void setInfo(String info) {
		selectCount.setText(info);
	}

	private void setInfo2(String info) {
		selectCount2.setText(info);
	}

	protected void onNavListClick(int position) {
		searchOriginalPath = fileListNavEntries.get(position).getPath();
		String path = canUseSU(currentPath) ? searchOriginalPath + "/": searchOriginalPath;
		openFolder(path);
	}

	private void onListClick(View v, int position, long id) {
		View select = (View) v.findViewById(R.id.select);
		FileList selectionFile = fileListAdapter.getItem(position);
		switch (mode) {
		case AppMode:
		case ProcessMode:
		case HideFromGalleryMode:
		case WallpaperMode:
			if (selectionFile.getSelection() == 0) {
				select.setVisibility(View.VISIBLE);
				selectionFile.setSelection(1);
			} else {
				select.setVisibility(View.INVISIBLE);
				selectionFile.setSelection(0);
			}
			setInfo2(getSelectedFilesCount() + "");
			break;

		default:
			File file = new File(fileListEntries.get(position).getPath());
			newFileExplorer.setFile(file);
			newFileExplorer.setContext(context);
			if (multiSelectMode) {
				if (selectionFile.getSelection() == 0) {
					select.setVisibility(View.VISIBLE);
					selectionFile.setSelection(1);
				} else {
					select.setVisibility(View.INVISIBLE);
					selectionFile.setSelection(0);
				}
				setInfo(getSelectedFilesCount() + "");
			} else {
				if (newFileExplorer.isDirectory
						&& (runSU || newFileExplorer.canRead)) {
					try {
						openFolder(position);
					} catch (Exception e) {
					}
					/*
					 * if(infoPermission.get(position).charAt(1) == 'r'){
					 * openFolder(position); } else{ Toast.makeText(context,
					 * format2String(R.string.msg_folder_not_present),
					 * Toast.LENGTH_SHORT).show(); }
					 */
				} else {
					if (mode == MODES.FileMode) {
						sendBackResult(file);
					} else {
						ExplorerOperations.openFile(file, packageManager);
					}
				}
			}
			break;
		}
	}

	private void openFolder(int position) {
		openFolder(fileListEntries.get(position).getPath());
	}

	private void openFolder(String folderPath) {
		cancelTasks();
		// save state of the list views
		fileListState.add(isCurrentList ? getListView().onSaveInstanceState() : gridView.onSaveInstanceState());
/*		if (showNavigationPane && !isSelectFromNavigation) {
			// NavListState.add(listView_navigation.onSaveInstanceState());
		}*/

		if (!(mode == MODES.SearchMode) && !multiSelectMode) {
			clearListAdapter();
			showList(folderPath);
		} else {
			// when the a file is opened from search results , make it
			// search original path and lock the search path we are opening the
			// file path in the new explorer fragment for search
			if (!searchPathLock) {
				searchOriginalPath = folderPath;
				searchPathLock = true;
			}
			showList(folderPath);
		}
	}

	static class ViewHolder {
		TextView fileName;
		TextView fileSize;
		TextView fileCount;
		TextView filePermission;
		TextView fileDateModified;
		ImageView fileIcon;
		ImageView fileIconSpecial;
		View select;
		ProgressBar sizeBar;
		RelativeLayout bar;
		TextView totalSpace;
		RelativeLayout details;
		int position;
	}

	/**
	 * @author HaKr
	 * 
	 */
	private class ListAdapter extends ArrayAdapter<FileList> {

		Activity context;
		LayoutInflater inflater;
		FileList fileListItem;

		public ListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			this.context = (Activity) context;
			inflater = this.context.getLayoutInflater();
		}

		public void setData(List<FileList> data) {
			clear();
			parentFolderSize = 0L;
			if (data != null) {
				if (Build.VERSION.SDK_INT >= 11) {
					addAll(data);
				} else {
					for (FileList file : data) {
						add(file);
					}
				}
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(getlayoutId(), null);
				holder = new ViewHolder();
				holder.fileName = (TextView) convertView.findViewById(R.id.name);
				if (isCurrentList) {
					holder.fileSize = (TextView) convertView.findViewById(R.id.size);
					holder.fileCount = (TextView) convertView.findViewById(R.id.file_count);
					holder.filePermission = (TextView) convertView.findViewById(R.id.permission);
					holder.fileDateModified = (TextView) convertView.findViewById(R.id.date_modified);
				}
				holder.details = (RelativeLayout) convertView.findViewById(R.id.details);
				holder.fileIcon = (ImageView) convertView.findViewById(R.id.icon);
				holder.fileIconSpecial = (ImageView) convertView.findViewById(R.id.icon_special);
				holder.select = (View) convertView.findViewById(R.id.select);
				holder.sizeBar = (ProgressBar) convertView.findViewById(R.id.sizeBar);
				holder.bar = (RelativeLayout) convertView.findViewById(R.id.bar);
				holder.totalSpace = (TextView) convertView.findViewById(R.id.totalSize);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.position = position;
			fileListItem = getItem(position);
			holder.fileName.setText(fileListItem.getName());
			holder.select.setVisibility(fileListItem.getSelection() == 0 ? View.INVISIBLE : View.VISIBLE);

			try {
				holder.fileIcon.setImageResource(iconCache.get(fileListItem.getIcon()));
				holder.fileIconSpecial.setImageResource(0);
				loadImage(fileListItem.getPosition(), holder);
				if (showStorage && mode != MODES.SearchMode
						&& !ExplorerOperations.isSpecialMode(mode)) {
					holder.bar.setVisibility(View.VISIBLE);
					holder.totalSpace.setVisibility(View.VISIBLE);
					if (isCurrentList) {
						holder.details.setVisibility(View.GONE);
					}
					holder.sizeBar.setMax(0);
					holder.sizeBar.setProgress(0);
					holder.totalSpace.setText("");
					if (canShowSizeOrStorage()) {
						loadStorage(position, holder);
					}
				} else if (isCurrentList) {
					holder.fileDateModified.setVisibility(ExplorerOperations.showView(showDateModified));
					holder.fileCount.setText(fileListItem.getInfoCount());
					holder.filePermission.setText(fileListItem.getInfoPermission());
					holder.fileDateModified.setText(showSmallDateModified ? fileListItem .getInfoSmallDate() : fileListItem.getInfoDate());
					holder.fileSize.setText(fileListItem.getSize());
					if (canShowSizeOrStorage()) {
						loadSize(position, holder);
					}
				}
			} catch (Exception e) {
			}

			return convertView;
		}
	}

	private boolean canShowSizeOrStorage() {
		return !ExplorerOperations.isSpecialMode(mode)
				&& !canUseSU(currentPath) && (showFolderSizes || showStorage)
				&& !isRoot && (isCurrentList || showStorage);
	}

	private void loadStorage(int position, ViewHolder holder) {
		if (fileListEntries.size() > position) {
			final String size = fileListEntries.get(position).getSize();
			if ("" != size) {
				if (parentFolderSize == 0) {
					return;
				}
				final int maxSize = (int) (parentFolderSize / (1024 * 1024));
				final int currentSize = (int) (Long.valueOf(size) / (1024 * 1024));
				float percentage = (float) (Float.valueOf((Long.valueOf(size))) * 100 / Float.valueOf(parentFolderSize));
				percentage = (float) new BigDecimal(percentage).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
				holder.sizeBar.setMax(maxSize);
				holder.sizeBar.setProgress(currentSize);
				holder.totalSpace.setText(isCurrentList ? "" + percentage + " %" : "");
			} else if (loadList) {
				FolderSizeTask task = new FolderSizeTask(holder, position);
				task.execute();
			}
		}
	}

	private void loadSize(int position, ViewHolder holder) {
		if (fileListEntries.size() > position) {
			final String size = fileListEntries.get(position).getSize();
			if ("" != size) {
				holder.fileSize.setText(size);
			} else if (loadList) {
				FolderSizeTask task = new FolderSizeTask(holder, position);
				task.execute();
			}
		}
	}

	private void loadImage(int position, ViewHolder holder) {
		if (fileListEntries.size() > position) {
			if (stopTasks) {
				return;
			}
			Integer icon = fileListEntries.get(position).getIcon();
			switch (icon) {
			case 0:
			case 1:
				if (iconCache.size() > icon) {
					holder.fileIcon.setImageResource(iconCache.get(icon));
				}
				break;

			case 2:
			case 3:
			case 4:
			case 5:
			case 14:
			case 99:
				final Bitmap bitmap = getThumbnail(position);
				if (null != bitmap) {
					holder.fileIcon.setImageBitmap(bitmap);
				} else if (loadList) {
					ThumbnailTask task = new ThumbnailTask(holder, position);
					task.execute();
				}
				break;

			default:
				if (icon < -1 && iconCache.size() > icon) { // special folders
					holder.fileIcon.setImageResource(iconCache.get(98));
					holder.fileIconSpecial.setImageResource(iconCache.get(icon));
				}
				break;
			}
		}
	}

	protected void startTasks() {
		stopTasks = false;
	}

	protected void cancelTasks() {
		stopTasks = true;
	}

	/**
	 * This will open the parent folder files list of the current folder
	 */
	protected boolean goBack() {
		boolean finish = false;
		isGoBack = true;
		// remove multiselection mode
		if (multiSelectMode && (!ExplorerOperations.isSpecialMode(mode) || !(mode == MODES.SearchMode))) {
			multiSelectMode = false;
		}

		cancelTasks();

		if (mode == MODES.SearchMode) {
			// In search mode
			if (isRoot || isSource) {
				// in search search results
				finish = true;
			} else if (currentPath.compareTo(searchOriginalPath) == 0) {
				// check if current path is search original path and if yes show
				// search results and unlock search path
				mypath.setText(format2String(R.string.msg_search_results) + ": " + resultCount + " files");
				searchPathLock = false;
				this.onConfigurationChanged(getResources().getConfiguration());
				updateMenu();
				showList(resultFiles);
				isSource = true;
			} else {
				// go back to parent path
				showList(mainFile.getParent());
			}
		} else if (ExplorerOperations.isSpecialMode(mode)) {
			finish = true;
		} else {
			// if (isRoot || isSource) {
			if (isRoot) {
				// if is root or came to the source path from where the activity
				// started then finish
				finish = true;
			} else {
				// go back to parent path
				showList(runSU ? mainFile.getParent()+ (mainFile.getParent().equals(root) ? "" : "/") : mainFile.getParent());
			}
		}

		if (showNavigationPane && !NavListState.isEmpty()
				&& (isGoBack || isResetList)) {
			// listView_navigation.onRestoreInstanceState(NavListState.get(NavListState.size()
			// - 1));
			NavListState.remove(NavListState.size() - 1);
		}
		showAds();
		return finish;
	}

	private int getSelectedFilesCount() {
		int count = 0;
		for (int pos = 0; pos < fileListEntries.size(); pos++) {
			if (fileListEntries.get(pos).getSelection() == 1) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return List of selected files path
	 */
	private List<String> getSelectedFileList() {
		List<String> localSelectedFilesList = new ArrayList<String>();

		if (null != fileListEntries) {
			for (int pos = 0; pos < fileListEntries.size(); pos++) {
				if (fileListEntries.get(pos).getSelection() == 1) {
					localSelectedFilesList.add(fileListEntries.get(pos).getPath());
					if (mode == MODES.AppMode) {
						appNameList.add(fileListEntries.get(pos).getName());
					}
				}
			}
		}
		return localSelectedFilesList;
	}

	/**
	 * This selects all the files in the current list
	 */
	private void selectAllFiles() {
		if (fileListEntries != null) {
			for (int pos = 0; pos < fileListEntries.size(); pos++) {
				fileListEntries.get(pos).setSelection(1);
			}

			if (fileListAdapter != null) {
				fileListAdapter.notifyDataSetChanged();
			}
			multiSelectMode = true;
			setInfo(getSelectedFilesCount() + "");
			if (!filesCopied) {
				setInfo2(getSelectedFilesCount() + "");
			}
		}
	}

	/**
	 * This un selects all the files in the current list
	 */
	private void unSelectAllFiles(boolean unSelectMode) {

		if (fileListEntries != null) {
			for (int pos = 0; pos < fileListEntries.size(); pos++) {
				fileListEntries.get(pos).setSelection(0);
			}

			if (fileListAdapter != null) {
				fileListAdapter.notifyDataSetChanged();
			}

			if (unSelectMode) {
				multiSelectMode = false;
			}

			if (!filesCopied) {
				setInfo("");
				setInfo2("");
			}
		}
	}

	private void updateMenu() {
		getSherlockActivity().invalidateOptionsMenu();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	protected void openAppDetails(String packageName) {
		if (Build.VERSION.SDK_INT >= 9) {
			startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)));
		} else {
			String APP_PKG_NAME = Build.VERSION.SDK_INT == 8 ? "pkg" : "com.android.settings.ApplicationPkgName";
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.InstalledAppDetails"));
			intent.putExtra(APP_PKG_NAME, packageName);
			startActivity(intent);
		}
	}

	public int convertDp2Px(int value) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
	}

	private void showAds() {
		adView.loadAd(new AdRequest());
	}

	/*
	 * @Override public void onBackPressed() { goBack(); }
	 */

	public void closeApp() {
		// setResult(Activity.RESULT_OK);
		// finish();
	}

	/**
	 * This method shows the Dialog for the corresponding id's present in
	 * <b>ExplorerOperations</b> This in turn call the method
	 * <b>ExplorerOperations.onCreateSelectedDialog()</b>
	 * 
	 * @param id
	 *            The id for the dialog to show
	 */
	protected boolean showSelectedDialog(int id, boolean check) {
		ExplorerOperations newExplorerOperations = new ExplorerOperations();
		newExplorerOperations.setContext(context);
		Bundle fileInfo = new Bundle();

		if (check) {
			if (getSelectedFileList().size() == 0
					&& ExplorerOperations.isEmpty(contextFilePath)) {
				Toast.makeText(context, format2String(R.string.msg_file_not_selected), Toast.LENGTH_SHORT).show();
				return false;
			}
		}

		fileInfo.putString(ExplorerOperations.CONSTANT_PATH, ExplorerOperations.isEmpty(contextFilePath) ? currentPath : contextFilePath);
		fileInfo.putString(ExplorerOperations.CONSTANT_TO_PATH, currentPath);
		fileInfo.putStringArray(ExplorerOperations.CONSTANT_PATH_LIST,
				(id == ExplorerOperations.DIALOG_PASTE
						|| id == ExplorerOperations.DIALOG_CUT ? copiedFilesList.toArray(new String[0]) : getSelectedFileList().toArray(new String[0])));
		fileInfo.putBoolean(ExplorerOperations.CONSTANT_MULTI_SELECTION, multiSelectMode);
		fileInfo.putBoolean(ExplorerOperations.CONSTANT_RUN_SU, explicitlyRunSU);

		switch (mode) {
		case AppMode:
			fileInfo.putStringArray(ExplorerOperations.CONSTANT_APPS_NAME, appNameList.toArray(new String[0]));
			break;
		case WallpaperMode:
			fileInfo.putBoolean(ExplorerOperations.CONSTANT_MULTI_SELECTION, 
					ExplorerOperations.isEmpty(contextFilePath) ? multiSelectMode: false);
			break;
		default:
			break;
		}

		// clearing context filepath
		contextFilePath = "";
		newExplorerOperations.onCreateSelectedDialog(id, context, fileInfo);
		return true;

	}

	protected void finishActiviy() {
		cancelTasks();
		Bundle bundle = new Bundle();
		bundle.putInt("operation", 0);
		if(mode == MODES.SearchMode){
			bundle.putInt("action", 2);
		}
		if(mode == MODES.FileMode){
			bundle.putInt("action", 3);
		}		
		else{
			bundle.putInt("action", 1);
		}
		mListener.onFragmentInteraction(bundle);
	}

	protected void selectAllOnOff(boolean onOff) {
		if (onOff)
			selectAllFiles();
		else
			unSelectAllFiles(false);
	}

	protected void multiSelectOnOff(boolean onOff) {
		multiSelectMode = onOff;
		if (!multiSelectMode) {
			unSelectAllFiles(false);
		}
		setInfo(onOff ? "0" : "");
	}

	/**
	 * toggles the {@link #multiSelectMode} parameter along with the button
	 * image
	 */
	protected void multiSelect() {
		multiSelectMode = multiSelectMode ? false : true;
		multiSelectOnOff(multiSelectMode);

		unSelectAllFiles(false);
		if (fileListAdapter != null) {
			fileListAdapter.notifyDataSetChanged();
		}
	}

	public void openCAB() {
		if (actionMode != null) {
			return;
		}
		actionMode = getSherlockActivity().startActionMode(actionModeCallback);
	}

	/**
	 * Calls the Setting Activity
	 */
	protected void showSettings() {
		Intent settingIntent = new Intent(context, Setting.class);
		startActivityForResult(settingIntent, 0);
	}

	private String format2String(int id) {
		return null != mListener ? getResources().getString(id) : "";
	}

	/**
	 * This gets the shared preferences for the Application
	 */
	private void getSharedPreference() {
		// display
		isCurrentList = Integer.valueOf(preference.getString("ViewPref", "0")) == 0;
		showStorage = preference.getBoolean("StorageModePref", false);
		showFolderSizes = preference.getBoolean("FolderPreviewPref", false);
		showHiddenFolders = preference.getBoolean("HiddenFolderPref", false);
		showThumbScroll = preference.getBoolean("ScrollThumbPref", false);
		showNavigationPane = preference.getBoolean("NavigationPanePref", true);

		// preview
		showImageThumbnails = preference.getBoolean("ImagePreviewPref", true);
		showAlbumArts = preference.getBoolean("AudioPreviewPref", true);
		showVideoThumbnails = preference.getBoolean("VideoPreviewPref", true);
		showApkThumbnails = preference.getBoolean("ApkPreviewPref", true);

		// sorting
		sortOrder = preference.getString("SortingOrderPref", "0");
		sortingOder = Integer.valueOf(sortOrder) == 0;
		sortType = preference.getString("SortingTypePref", "0");

		// advanced
		hasRootAccess = preference.getBoolean("RootAccessPref", false);
		hasMountWrite = preference.getBoolean("MounWritePref", false);

		switch (Integer.valueOf(sortType)) {
		case 1:
			sortingType = sortingOder ? ExplorerOperations.alphaAscending : ExplorerOperations.alphaDescending;
			break;
		case 2:
			sortingType = sortingOder ? ExplorerOperations.typeAscending : ExplorerOperations.typeDescending;
			break;
		case 3:
			sortingType = sortingOder ? ExplorerOperations.sizesAscending : ExplorerOperations.sizesDescending;
			break;
		case 4:
			sortingType = sortingOder ? ExplorerOperations.datesAscending : ExplorerOperations.datesDescending;
			break;
		default:
			sortingType = ExplorerOperations.typeAlpha;
			break;
		}
	}

	private String getTitle(MODES mode) {
		String title = "";
		switch (mode) {
		case SearchMode:
			title = format2String(R.string.constant_search);
			break;
		case AppMode:
			title = format2String(R.string.msg_installed);
			break;
		case ProcessMode:
			title = format2String(R.string.msg_running);
			break;
		case HideFromGalleryMode:
			title = format2String(R.string.name_unscannable);
			break;
		case WallpaperMode:
			title = format2String(R.string.name_wallpaper);
			break;
		default:
			if (currentPath.compareTo(ExplorerOperations.DIR_ROOT) == 0) {
				title = type == TYPES.Tablet ? format2String(R.string.name_tablet) : format2String(R.string.name_phone);
			} else if (currentPath.compareTo(ExplorerOperations.DIR_SDCARD) == 0) {
				title = format2String(R.string.name_sdcard) + ExplorerOperations.getExtStorageString();
			} else if (currentPath.compareTo(ExplorerOperations.DIR_EMMC) == 0) {
				title = format2String(R.string.name_emmc);
			} else if (currentPath.contains("sd")) {
				title = format2String(R.string.name_sdcard) + ExplorerOperations.getExtStorageString();
			} else if (currentPath.contains("usb")) {
				title = "USB Storage";
			} else {
				title = format2String(R.string.name_explorer);
			}
			break;
		}
		return title;
	}

	/**
	 * Adding drawable to list iconCache for re-usability
	 */
	private void fillBitmapCache() {
		// imageBitmap = BitmapFactory.decodeResource(getResources(),
		// R.drawable.image);
		// audioBitmap = BitmapFactory.decodeResource(getResources(),
		// R.drawable.audio);
		//videoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.video);
		apkBitmap = BitmapFactory.decodeResource(getResources(), android.R.drawable.sym_def_app_icon);
		TypedArray a = context.getTheme().obtainStyledAttributes(R.styleable.AppTheme);

		iconCache.put(0, a.getResourceId(R.styleable.AppTheme_mainFolder, 0));
		iconCache.put(98, a.getResourceId(R.styleable.AppTheme_specialFolder, 0));
		iconCache.put(-1, R.drawable.sdcard);
		iconCache.put(-2, R.drawable.system);
		iconCache.put(-3, R.drawable.download);
		iconCache.put(-4, R.drawable.ringtone);
		iconCache.put(-5, R.drawable.music);
		iconCache.put(-6, R.drawable.gallery);
		iconCache.put(-7, R.drawable.bluetooth);
		iconCache.put(-8, R.drawable.movies);
		iconCache.put(-9, R.drawable.locked);
		iconCache.put(93, R.drawable.hdd);
		iconCache.put(94, R.drawable.usb);
		iconCache.put(95, R.drawable.locked);
		iconCache.put(96, R.drawable.lock);
		iconCache.put(97, R.drawable.emmc);
		iconCache.put(99, R.drawable.process);

		iconCache.put(1, R.drawable.file);
		iconCache.put(2, R.drawable.audio);
		iconCache.put(22, R.drawable.mp3);
		iconCache.put(3, R.drawable.video);
		iconCache.put(4, R.drawable.image);
		iconCache.put(5, android.R.drawable.sym_def_app_icon);
		iconCache.put(6, R.drawable.zip);
		iconCache.put(7, R.drawable.swf);
		iconCache.put(8, R.drawable.pdf);
		iconCache.put(9, R.drawable.doc);
		iconCache.put(10, R.drawable.ppt);
		iconCache.put(11, R.drawable.xls);
		iconCache.put(12, R.drawable.html);
		iconCache.put(14, R.drawable.wallpaper);
	}

	private void fillProcessType() {
		processTypeCache = new SparseArray<String>();
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_SERVICE, "Service");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_BACKGROUND, "Background Process");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_FOREGROUND, "Foreground Process");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_VISIBLE, "Visible");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_EMPTY, "Empty");
	}

	/**
	 * @author HaKr
	 * 
	 */
	private class LoadListTask extends AsyncTask<String, Void, List<FileList>> {

		@Override
		protected List<FileList> doInBackground(String... args) {
			startTasks();
			fillNavData();
			return fillData(resultFilesList);
		}

		@Override
		protected void onPreExecute() {
			progress.setVisibility(View.VISIBLE);
			empty.setText(format2String(R.string.msg_loading));
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(List<FileList> result) {
			progress.setVisibility(View.GONE);
			empty.setText(mode == MODES.AppMode ? format2String(R.string.msg_file_not_found) : format2String(R.string.msg_folder_empty));
			if(isAttached)
			loadListFinally(result, true);
		}
	}

	/**
	 * @author HaKr
	 * 
	 */
	private class SearchTask extends AsyncTask<String, Void, File[]> {
		@Override
		protected File[] doInBackground(String... args) {
			startTasks();
			incomingPath = args[0];
			queryString = args[1];
			resultFiles = searchDirectory(incomingPath, queryString);
			return resultFiles;
		}

		@Override
		protected void onPreExecute() {
			progress.setVisibility(View.VISIBLE);
			empty.setText(format2String(R.string.msg_searching));
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(File[] result) {
			resultCount = String.valueOf(result.length);
			mypath.setText(format2String(R.string.msg_search_results) + ": " + resultCount + " files");
			progress.setVisibility(View.GONE);
			empty.setText(format2String(R.string.msg_file_not_found));

			// show list
			showList(result);
		}
	}

	private class GalleryTask extends AsyncTask<String, Void, File[]> {
		@Override
		protected File[] doInBackground(String... args) {
			startTasks();
			resultFiles = searchGallery();
			return resultFiles;
		}

		@Override
		protected void onPreExecute() {
			progress.setVisibility(View.VISIBLE);
			empty.setText(format2String(R.string.msg_searching));
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(File[] result) {
			resultCount = String.valueOf(result.length);
			mypath.setText(format2String(R.string.msg_search_results) + ": " + resultCount + " files");
			progress.setVisibility(View.GONE);
			empty.setText(format2String(R.string.msg_file_not_found));

			// show list
			showList(result);
		}
	}

	private class ThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
		private final WeakReference<ViewHolder> viewReference;
		private final int position;

		public ThumbnailTask(ViewHolder viewHolder, int pos) {
			viewReference = new WeakReference<ViewHolder>(viewHolder);
			position = pos;
		}

		@Override
		protected Bitmap doInBackground(Void... arg0) {
			try {
				if (!loadList || stopTasks) {
					return null;
				}
				final Integer icon = fileListEntries.get(position).getIcon();
				final String path = fileListEntries.get(position).getPath();
				switch (icon) {
				case 2:
					if (showAlbumArts) {
						final Bitmap bitmap = getAudioThumbnail(position);
						setThumbnail(position, bitmap);
						return bitmap;
					}
					break;
				case 3:
					if (showVideoThumbnails) {
						final Bitmap bitmap = getVideoThumbnail(path);
						setThumbnail(position, bitmap);
						return bitmap;
					}
					break;
				case 4:
				case 14:
					if (mode == MODES.WallpaperMode) {
						final Bitmap bitmap = getImageThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
						setThumbnail(position, bitmap);
						return bitmap;
					} else if (showImageThumbnails) {
						final Bitmap bitmap = getImageThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
						setThumbnail(position, bitmap);
						return bitmap;
					}
					break;
				case 5:
				case 99:
					if (showApkThumbnails) {
						final Bitmap bitmap = getApkThumbnail(path);
						setThumbnail(position, bitmap);
						return bitmap;
					}
					break;
				}
				return null;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (null != viewReference && null != result) {
				final ViewHolder holder = viewReference.get();
				if (null != holder && null != holder.fileIcon
						&& holder.position == position) {
					holder.fileIcon.setImageBitmap(result);
				}
			}
			super.onPostExecute(result);
		}
	}

	/**
	 * @author HaKr
	 * 
	 */
	private class FolderSizeTask extends AsyncTask<String, Void, String> {
		private final WeakReference<ViewHolder> viewReference;
		private final int position;

		public FolderSizeTask(ViewHolder viewHolder, int pos) {
			viewReference = new WeakReference<ViewHolder>(viewHolder);
			position = pos;
		}

		@Override
		protected String doInBackground(String... args) {
			String localSize = "";
			Long lsize = 0L;
			if (!stopTasks) {
				if (showStorage && parentFolderSize == 0L) {
					parentFolderSize = getDirectorySize(new File(currentPath));
				}
				if (isCancelled()) {
					return null;
				}
				File eachFile = new File(fileListEntries.get(position).getPath());
				lsize = getDirectorySize(eachFile);
				localSize = showStorage ? String.valueOf(lsize) : Formatter.formatFileSize(context, lsize);
				if (fileListEntries != null && fileListEntries.size() > position) {
					fileListEntries.get(position).setSize(localSize);
				}
			}
			return localSize;
		}

		@Override
		protected void onPostExecute(String result) {
			if (null != viewReference && null != result && result != "") {
				final ViewHolder holder = viewReference.get();
				if (showStorage) {
					if (parentFolderSize != 0L && null != holder
							&& null != holder.sizeBar
							&& null != holder.totalSpace
							&& holder.position == position) {
						final int maxSize = (int) (parentFolderSize / (1024 * 1024));
						final int currentSize = (int) (Long.valueOf(result) / (1024 * 1024));
						float percentage = (float) (Float.valueOf((Long.valueOf(result))) * 100 / Float.valueOf(parentFolderSize));
						percentage = (float) new BigDecimal(percentage).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
						holder.sizeBar.setMax(maxSize);
						holder.sizeBar.setProgress(currentSize);
						holder.totalSpace.setText(isCurrentList ? "" + percentage+ " %" : "");
					}
				} else {
					if (null != holder && null != holder.fileSize && holder.position == position) {
						holder.fileSize.setText(result);
					}
				}
			}
		}
	}

	private static List<File> searchFiles(File dir, FilenameFilter filter) {
		List<File> result = new ArrayList<File>();
		File[] filesFiltered = dir.listFiles(filter), filesAll = dir.listFiles();

		if (filesFiltered != null) {
			result.addAll(Arrays.asList(filesFiltered));
		}

		if (filesAll != null) {
			for (File file : filesAll) {
				if (stopTasks) {
					break;
				}
				if (file.isDirectory()) {
					List<File> deeperList = searchFiles(file, filter);
					result.addAll(deeperList);
				}
			}
		}
		return result;
	}

	private File[] searchDirectory(String searchPath, String searchQuery) {
		ArrayList<File> totalList = new ArrayList<File>();
		File searchDirectory = new File(searchPath);

		totalList.addAll(searchFiles(searchDirectory, new SearchFilter(searchQuery)));
		return (File[]) totalList.toArray(new File[0]);
	}

	private File[] searchGallery() {
		ArrayList<File> totalList = new ArrayList<File>();
		File searchDirectory = new File(ExplorerOperations.DIR_SDCARD);
		totalList.addAll(searchFiles(searchDirectory, new GalleryFilter()));
		return (File[]) totalList.toArray(new File[0]);
	}

	private static long getDirectorySize(File dir) {
		long result = 0;
		if (dir.listFiles() != null && dir.listFiles().length > 0) {
			for (File eachFile : dir.listFiles()) {
				if (stopTasks) {
					break;
				}
				result += eachFile.isDirectory() && eachFile.canRead() ? getDirectorySize(eachFile) : eachFile.length();
			}
		} else if (!dir.isDirectory()) {
			result = dir.length();
		}
		return result;
	}
	
	private void loadListFinally(List<FileList> result, boolean updateNavigation) {
		parentFolderSize = 0L;
		if(null == mListener){
			return;
		}
		AbsListView absListView = isCurrentList ? getListView() : gridView;
		fileListAdapter.setData(result);
		
		// restore list state
		if (!fileListState.isEmpty() && (isGoBack || isResetList)) {
			absListView.onRestoreInstanceState(fileListState.get(fileListState.size() - 1));
			fileListState.remove(fileListState.size() - 1);
		}

		isGoBack = isResetList = false;
		if (updateNavigation && fileListNavEntries != null) {
			curNavPosition = navigationListPaths.indexOf(currentPath);
		}

		Bundle bundle = new Bundle();
		bundle.putInt("operation", 3);
		bundle.putInt("action", 1);
		bundle.putInt("position", curNavPosition);
		bundle.putString("base", "explorer");
		bundle.putParcelableArrayList("navlist", fileListNavEntries);		
		mListener.onFragmentInteraction(bundle);		
		showAds();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

		boolean stoppedFling = scrollState != SCROLL_STATE_FLING;
		if (scrollState == SCROLL_STATE_IDLE) {
			loadList = true;
			fileListAdapter.notifyDataSetChanged();
		} else if (stoppedFling) {
			loadList = true;
		} else if (scrollState == SCROLL_STATE_FLING) {
			loadList = false;
		}
		scrollStateAll = scrollState;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getAction();
		boolean fingerUp = (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL);

		if (fingerUp && scrollStateAll != OnScrollListener.SCROLL_STATE_FLING) {
			loadList = true;
			fileListAdapter.notifyDataSetChanged();
		}
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		if (mode == MODES.SearchMode || ExplorerOperations.isSpecialMode(mode)) {
			return true;
		}
		else if(mode == MODES.RootMode){
			
		}
		else if(mode == MODES.ExplorerMode){
			Bundle bundle = new Bundle();
			bundle.putInt("operation", 2);
			bundle.putString("query", query);
			bundle.putString("path", currentPath);
			mListener.onFragmentInteraction(bundle);
			return true;
		}
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		if (!ExplorerOperations.isSpecialMode(mode)) {
			return false;
		}
		if (null != fileListAdapter) {
			fileListAdapter.getFilter().filter(!TextUtils.isEmpty(newText) ? newText : null);
		}
		return true;
	}
}