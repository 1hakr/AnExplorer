package dev.dworks.apps.anexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.FileNavList;
import dev.dworks.apps.anexplorer.util.ExplorerOperations.OnFragmentInteractionListener;

public class NavigationFragment extends SherlockListFragment {
	private Context context;
	private OnFragmentInteractionListener mListener;
	private int curNavPosition;
	private NavingationAdapter navListAdapter;
	private Bundle myBundle;
	private String base;

	public static NavigationFragment newInstance(String param1, String param2) {
		NavigationFragment fragment = new NavigationFragment();
		return fragment;
	}

	public NavigationFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getSherlockActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_navigation, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		navListAdapter = new NavingationAdapter(context, R.layout.row_navigation);
		getListView().setAdapter(navListAdapter);
		navListAdapter.setNotifyOnChange(true);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
					onListClick(v, position, id);
			}
		});
	}
	
	private String format2String(int id){
		return getResources().getString(id);
	}	
	
	private boolean validateFolder(String path){
		File mainFile = new File(path);
		if(!ExplorerOperations.isSpecialPath(path) && !(mainFile.exists() && mainFile.canRead())){
			Toast.makeText(context, path == ExplorerOperations.DIR_SDCARD ? format2String(R.string.msg_sdcard_unmounted) : format2String(R.string.msg_folder_not_present), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}	
	
	protected void onListClick(View v, int position, long id){
		if(base.compareToIgnoreCase("home") == 0){
			if(validateFolder(ExplorerOperations.DIRS_NAV[position])){
				myBundle = new Bundle();
				myBundle.putInt("operation", 1);
				myBundle.putString(ExplorerOperations.CONSTANT_PATH, ExplorerOperations.DIRS_NAV[position]);
				myBundle.putBoolean(ExplorerOperations.CONSTANT_SEARCH, false);
				mListener.onFragmentInteraction(myBundle);
			}
		}
		else if(base.compareToIgnoreCase("explorer") == 0){
			curNavPosition = position;
			myBundle = new Bundle();
			myBundle.putInt("operation", 3);
			myBundle.putInt("action", 3);
			myBundle.putInt("position", curNavPosition);
			mListener.onFragmentInteraction(myBundle);
		}
	}	

	protected void initData(Bundle data) {
		int action = data.getInt("action");
		base = data.getString("base");
		if(action == 1){
			curNavPosition = data.getInt("position");
			ArrayList<FileNavList> list = data.getParcelableArrayList("navlist");
			if(null == list){
				return;
			}
			navListAdapter.setData(list);
			navListAdapter.notifyDataSetChanged();
			if (list != null) {				
				getListView().smoothScrollToPosition(curNavPosition);
				getListView().setSelection(curNavPosition);
				getListView().setItemChecked(curNavPosition , true);
				navListAdapter.notifyDataSetChanged();
				if(Build.VERSION.SDK_INT >= 11){
					getListView().smoothScrollToPositionFromTop(curNavPosition, 150);
				}
				else{ 
					getListView().smoothScrollToPosition(curNavPosition); 
				}
			}			
		}
		else if(action == 2){
			navListAdapter.clear();
			navListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	class NavingationAdapter extends ArrayAdapter<FileNavList> {
		LayoutInflater inflater;

		public NavingationAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			inflater = ((Activity) context).getLayoutInflater();
		}

		public void setData(List<FileNavList> data) {
			clear();
			if (data != null) {
				if (Build.VERSION.SDK_INT >= 11) {
					addAll(data);
				} else {
					for (FileNavList file : data) {
						add(file);
					}
				}
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = inflater.inflate(R.layout.row_navigation, null);
			TextView name = (TextView) convertView.findViewById(R.id.name);
			ImageView fileIcon = (ImageView) convertView.findViewById(R.id.icon);
			ImageView fileIconSpecial = (ImageView) convertView.findViewById(R.id.icon_special);
			FileNavList fileList = getItem(position);
			name.setText(fileList.getName());
			fileIcon.setImageResource(fileList.getIcon());
			fileIconSpecial.setImageResource(fileList.getSpecialIcon());
			if (position == curNavPosition) {
				convertView.setBackgroundResource(R.drawable.item_activated_background);
			}
			return (convertView);
		}
	}	
}
