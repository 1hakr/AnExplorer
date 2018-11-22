/*
 * Copyright (C) 2016 The Android Open Source Project
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

package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;

public class MessageHolder extends BaseHolder {

    final ImageView icon;
    final TextView title;
    final View background;

    public MessageHolder(DocumentsAdapter.Environment environment, Context context,
                         ViewGroup parent, int layoutId) {
        super(context, parent, layoutId);

        icon = (ImageView) itemView.findViewById(android.R.id.icon);
        title = (TextView) itemView.findViewById(android.R.id.title);
        background = itemView.findViewById(R.id.background);
    }

    public MessageHolder(DocumentsAdapter.Environment environment, Context context, ViewGroup parent) {
        super(context, parent, getLayoutId(environment));

        icon = (ImageView) itemView.findViewById(android.R.id.icon);
        title = (TextView) itemView.findViewById(android.R.id.title);
        background = itemView.findViewById(R.id.background);
    }

    public static int getLayoutId(DocumentsAdapter.Environment environment){
        int layoutId = R.layout.item_message_list;
        if(environment.getDisplayState().derivedMode == MODE_GRID){
            layoutId = R.layout.item_message_grid;
        }
        return layoutId;
    }

    @Override
    public void setData(String message, int resId) {
        super.setData(message, resId);
        if(null != icon) {
            icon.setImageResource(resId);
        }
        title.setText(message);
        if(null != background) {
            background.setBackgroundColor(SettingsActivity.getPrimaryColor());
        }
    }
}