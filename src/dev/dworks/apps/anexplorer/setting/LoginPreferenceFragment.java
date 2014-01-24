package dev.dworks.apps.anexplorer.setting;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.PinViewHelper;

public class LoginPreferenceFragment extends PreferenceFragment {
    private Preference pin_set_preference;
    
    public LoginPreferenceFragment() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_login);
        
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
                	pin_set_preference.setSummary(SettingsActivity.isPinProtected(getActivity()) ? R.string.pin_set : R.string.pin_disabled);
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
        if (SettingsActivity.isPinProtected(getActivity())) {
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