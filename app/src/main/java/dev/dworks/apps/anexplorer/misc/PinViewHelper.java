/*
 * Copyright (C) 2013 Koushik Dutta (@koush)
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

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

public class PinViewHelper {
    private TextView passwordInstruction;
	public PinViewHelper(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View ret = inflater.inflate(R.layout.view_pin, container, false);

        final TextView password = (TextView)ret.findViewById(R.id.password);
        passwordInstruction = (TextView)ret.findViewById(R.id.passwordInstruction);
        passwordInstruction.setText(R.string.enter_pin);
        int color = SettingsActivity.getPrimaryColor(inflater.getContext());
        password.setTextColor(color);
        passwordInstruction.setTextColor(color);
        final ShapeDrawable pinEntered = new ShapeDrawable(new OvalShape());
        pinEntered.getPaint().setColor(color);
        //passwordInstruction.setText(confirm ? R.string.confirm_pin : R.string.enter_pin);
        int[] ids = new int[] { R.id.p0, R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5, R.id.p6, R.id.p7, R.id.p8, R.id.p9, };
        final int[] pinBoxIds = new int[]{R.id.pinBox0, R.id.pinBox1, R.id.pinBox2, R.id.pinBox3};
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            Button b = (Button)ret.findViewById(id);
            final String text = String.valueOf(i);
            b.setText(text);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    password.setText(password.getText().toString() + text);
                    ret.findViewById(pinBoxIds[password.getText().length()-1]).setBackgroundDrawable(pinEntered);
                }
            });
        }
	
        ret.findViewById(R.id.pd).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String curPass = password.getText().toString();
                if (curPass.length() > 0) {
                    curPass = curPass.substring(0, curPass.length() - 1);
                    password.setText(curPass);
                    ret.findViewById(pinBoxIds[password.getText().length()]).setBackgroundResource(R.drawable.pin_empty);
                }
            }
        });
        
        ret.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });
        
        ret.findViewById(R.id.ok).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnter(password.getText().toString());
                for (int i = 0; i < password.getText().length(); i++) {
                	ret.findViewById(pinBoxIds[i]).setBackgroundResource(R.drawable.pin_empty);
                }
                password.setText("");
            }
        });
        
        mView = ret;
    }
    
    View mView;
    public View getView() {
        return mView;
    }
    
    public void setInstruction(int stringId){
    	passwordInstruction.setText(stringId);
    }
    
    public void onEnter(String password) {
    }
    
    public void onCancel() {
    }
    
    public static class PINDialogFragment extends DialogFragment {
        private Dialog mDialog;
        public PINDialogFragment() {
            super();
            mDialog = null;
        }
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	setRetainInstance(true);
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}