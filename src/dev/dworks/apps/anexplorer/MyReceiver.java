package dev.dworks.apps.anexplorer;

import java.io.IOException;
import java.util.Set;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

public class MyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if(ExplorerOperations.checkDevice()){
			SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
			Editor editor = preference.edit(); 
			int next = 0;
			int current = preference.getInt("currentWallpaper", -1);
			Set<String> set = preference.getStringSet("wallpaperSet", null);
	 		if(current != -1 && null != set && set.size() > 0){
	 			String[] wallpapaerPaths = set.toArray(new String[0]);
	 			next = current+1 < set.size() ? current+1 : 0;
	 		    WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
	 		    try {
	 				wallpaperManager.setBitmap(BitmapFactory.decodeFile(wallpapaerPaths[next]));
	 			} catch (IOException e) { }
	 		    current = next;
	 		    editor.putInt("currentWallpaper", current);
	 		    editor.commit();
			}
		}
	}
}
