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
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.ArcProgress;

/**
 * Display home.
 */
public class HomeFragment extends Fragment {

    private ArcProgress storageStats;
    private ArcProgress memoryStats;
    private View statsBackground;
    private TextView storageSummary;
    private Timer storageTimer;
    private Timer processTimer;

    public static void show(FragmentManager fm) {
        final HomeFragment fragment = new HomeFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storageTimer = new Timer();
        processTimer = new Timer();
        statsBackground = view.findViewById(R.id.stats_background);
        storageStats = (ArcProgress) view.findViewById(R.id.storage_stats);
        memoryStats = (ArcProgress) view.findViewById(R.id.memory_stats);
        storageSummary = (TextView) view.findViewById(R.id.storage_summary);
        showData();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateColor();
    }

    public void showData(){
        Context context = getActivity();
        final RootsCache roots = DocumentsApplication.getRootsCache(context);
        RootInfo root = roots.getPrimaryRoot();
        String summaryText = "";
        if (root.availableBytes >= 0) {
            summaryText = context.getString(R.string.storage_stats,
                    Formatter.formatFileSize(context, root.availableBytes),
                    Formatter.formatFileSize(context, root.totalBytes));
            storageSummary.setText(summaryText);
            try {
                final double percentStore = (((root.totalBytes - root.availableBytes) / (double) root.totalBytes) * 100);
                storageStats.setProgress(0);
                storageTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (storageStats.getProgress() >= (int) percentStore) {
                                    storageTimer.cancel();
                                } else {
                                    storageStats.setProgress(storageStats.getProgress() + 1);
                                }

                            }
                        });
                    }
                }, 50, 20);
            }
            catch (Exception e){
                storageStats.setVisibility(View.GONE);
            }
        }

        root = roots.getProcessRoot();
        if (root.availableBytes >= 0) {
            try {
                final double percentStore = (((root.totalBytes - root.availableBytes) / (double) root.totalBytes) * 100);
                memoryStats.setProgress(0);
                processTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (memoryStats.getProgress() >= (int) percentStore) {
                                    processTimer.cancel();
                                } else {
                                    memoryStats.setProgress(memoryStats.getProgress() + 1);
                                }
                            }
                        });
                    }
                }, 50, 20);
            }
            catch (Exception e){
                memoryStats.setVisibility(View.GONE);
            }
        }
        updateColor();
    }

    public void updateColor(){
        int accentColor = SettingsActivity.getActionBarColor();
        int complimentartyColor = Utils.getComplementaryColor(accentColor);

        statsBackground.setBackgroundColor(accentColor);
        storageStats.setFinishedStrokeColor(complimentartyColor);
        memoryStats.setFinishedStrokeColor(complimentartyColor);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        storageTimer.cancel();
        processTimer.cancel();
    }
}