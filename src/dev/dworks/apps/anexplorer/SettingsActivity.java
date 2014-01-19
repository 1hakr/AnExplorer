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

package dev.dworks.apps.anexplorer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import dev.dworks.apps.anexplorer.misc.PinViewHelper;

public class SettingsActivity extends Activity {
    private static final String KEY_ADVANCED_DEVICES = "advancedDevices";
    private static final String KEY_FILE_SIZE = "fileSize";
    private static final String KEY_FOLDER_SIZE = "folderSize";
    private static final String KEY_FILE_THUMBNAIL = "fileThumbnail";
    public static final String KEY_PIN = "pin";
	public static final String PIN_ENABLED = "pin_enable";
    
    public static boolean getDisplayAdvancedDevices(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ADVANCED_DEVICES, true);
    }

    public static boolean getDisplayFileSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILE_SIZE, true);
    }

    public static boolean getDisplayFolderSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FOLDER_SIZE, false);
    }
    
    public static boolean getDisplayFileThumbnail(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILE_THUMBNAIL, true);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        final ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {
        private Preference pin_set_preference;

		@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            
    		pin_set_preference = findPreference("pin_set");
    		pin_set_preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
    			
    			@Override
    			public boolean onPreferenceClick(Preference preference) {
    				checkPin();
    				return false;
    			}
    		});
    		pin_set_preference.setSummary(SettingsActivity.isPinProtected(getActivity()) ? R.string.pin_set : R.string.pin_disabled);
        }
		
	    private void confirmPin(final String pin) {
	    	final Dialog d = new Dialog(getActivity(), R.style.Theme_DailogPIN);	
	    	d.getWindow().setWindowAnimations(R.style.DialogEnterNoAnimation);
	    	PinViewHelper pinViewHelper = new PinViewHelper((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null) {
	            public void onEnter(String password) {
	                super.onEnter(password);
	                if (pin.equals(password)) {
	                	SettingsActivity.setPin(getActivity(), password);
	                	pin_set_preference.setSummary(isPinProtected(getActivity()) ? R.string.pin_set : R.string.pin_disabled);
	                    if (password != null && password.length() > 0){
	                        Toast.makeText(getActivity(), getString(R.string.pin_set), Toast.LENGTH_SHORT).show();
	                        setInstruction(R.string.pin_set);
	                    }
	                    d.dismiss();
	                    return;
	                }
	                Toast.makeText(getActivity(), getString(R.string.pin_mismatch), Toast.LENGTH_SHORT).show();
	                setInstruction(R.string.pin_mismatch);
	            };
	            
	            public void onCancel() {
	                super.onCancel();
	                d.dismiss();
	            };
	        };
	        View view = pinViewHelper.getView();
	        view.findViewById(R.id.logo).setVisibility(View.GONE);
	        pinViewHelper.setInstruction(R.string.confirm_pin);
			d.setContentView(view);
	        d.show();
	    }
	    
	    private void setPin() {
	    	final Dialog d = new Dialog(getActivity(), R.style.Theme_DailogPIN);
	    	d.getWindow().setWindowAnimations(R.style.DialogExitNoAnimation);
	        View view = new PinViewHelper((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null) {
	            public void onEnter(String password) {
	                super.onEnter(password);
	                confirmPin(password);
	                d.dismiss();
	            };
	            
	            public void onCancel() {
	                super.onCancel();
	                d.dismiss();
	            };
	        }.getView();
	        view.findViewById(R.id.logo).setVisibility(View.GONE);
			d.setContentView(view);
	        d.show();
	    }

	    private void checkPin() {
	        if (isPinProtected(getActivity())) {
	            final Dialog d = new Dialog(getActivity(), R.style.Theme_DailogPIN);
	            View view = new PinViewHelper((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null) {
	                public void onEnter(String password) {
	                    super.onEnter(password);
	                    if (SettingsActivity.checkPin(getActivity(), password)) {
	                        super.onEnter(password);
	                        SettingsActivity.setPin(getActivity(), "");
	                        pin_set_preference.setSummary(R.string.pin_disabled);
	                        Toast.makeText(getActivity(), getString(R.string.pin_disabled), Toast.LENGTH_SHORT).show();
	                        setInstruction(R.string.pin_disabled);
	                        d.dismiss();
	                        return;
	                    }
	                    Toast.makeText(getActivity(), getString(R.string.incorrect_pin), Toast.LENGTH_SHORT).show();
	                    setInstruction(R.string.incorrect_pin);
	                };
	                
	                public void onCancel() {
	                    super.onCancel();
	                    d.dismiss();
	                };
	            }.getView();
	            view.findViewById(R.id.logo).setVisibility(View.GONE);
				d.setContentView(view);
				d.show();
	        }
	        else {
	            setPin();
	        }
	    }
    }
    
	public static final boolean isPinEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PIN_ENABLED, false)
        		&& isPinProtected(context);
    }
	
    public static final boolean isPinProtected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PIN, "") != "";
    }
    
    public static void setPin(Context context, String pin) {
    	PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PIN, hashKeyForPIN(pin)).commit();
    }
    
    public static boolean checkPin(Context context, String pin) {
        pin = hashKeyForPIN(pin);
        String hashed = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PIN, "");
        if (TextUtils.isEmpty(pin))
            return TextUtils.isEmpty(hashed);
        return pin.equals(hashed);
    }
    
    private static String hashKeyForPIN(String value) {
        if (TextUtils.isEmpty(value))
            return null;
        try {
            //MessageDigest digester = MessageDigest.getInstance("MD5");
            //return Base64.encodeToString(value.getBytes(), Base64.DEFAULT);
        }
        catch (Exception e) {
        }
        return value;
    }
    
/*    public static String hashKeyForPIN(String key) {
        String cacheKey = key;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        cacheKey = Base64.encodeToString(key.getBytes(), Base64.DEFAULT);
        return cacheKey;
    }*/
}
