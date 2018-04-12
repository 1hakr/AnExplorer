package dev.dworks.apps.anexplorer.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import dev.dworks.apps.anexplorer.DocumentsApplication;

public class PreferenceUtils {

    public static Boolean getBooleanPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(key, false);
	}

	public static Boolean getBooleanPrefs(Context ctx, String key, boolean defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(key, defaultValue);
	}

	public static Boolean getBooleanPrefs(String key) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getBoolean(key, false);
	}

	public static Boolean getBooleanPrefs(String key, boolean defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getBoolean(key, defaultValue);
	}

	public static String getStringPrefs(String key) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getString(key, "");
	}

	public static String getStringPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, "");
	}

	public static String getStringPrefs(Context ctx, String key, String defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, defaultValue);
	}

	public static int getIntegerPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(key, 0);
	}

	public static int getIntegerPrefs(String key) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getInt(key, 0);
	}

	public static int getIntegerPrefs(Context ctx, String key, int defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(key, defaultValue);
	}

	public static long getLongPrefs(Context ctx, String key) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(key, 0);
	}

	public static long getLongPrefs(Context ctx, String key, int defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(key, defaultValue);
	}

	public static Long getLongPrefs(String key) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getLong(key, 0);
	}

	public static long getLongPrefs(String key, int defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getLong(key, defaultValue);
	}

	public static Set<String> getStrinSetPrefs(String key) {
		return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()).getStringSet(key, new HashSet<String>());
	}

	public static void set(final String key, final Object value) {
		set(PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext()), key, value);
	}

	public static void set(final Context context, final String key, final Object value) {
		set(PreferenceManager.getDefaultSharedPreferences(context), key, value);
	}

	public static void set(final SharedPreferences preferences, final String key, final Object value) {
		SharedPreferences.Editor sharedPreferenceEditor = preferences.edit();
		if (value instanceof String) {
			sharedPreferenceEditor.putString(key, (String) value);
		} else if (value instanceof Long) {
			sharedPreferenceEditor.putLong(key, (Long) value);
		} else if (value instanceof Integer) {
			sharedPreferenceEditor.putInt(key, (Integer) value);
		} else if (value instanceof Boolean) {
			sharedPreferenceEditor.putBoolean(key, (Boolean) value);
		} else  if (value instanceof Set){
			sharedPreferenceEditor.putStringSet(key, (Set<String>) value);
		}
		sharedPreferenceEditor.apply();
	}

	public static void clear(final Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
	}
}