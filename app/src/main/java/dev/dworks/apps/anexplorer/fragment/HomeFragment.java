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

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;

import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.AppsProvider;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.HomeItem;
import dev.dworks.apps.anexplorer.ui.MaterialProgressDialog;

import static dev.dworks.apps.anexplorer.provider.AppsProvider.getRunningAppProcessInfo;

/**
 * Display home.
 */
public class HomeFragment extends Fragment {

    private HomeItem storageStats;
    private HomeItem memoryStats;
    private Timer storageTimer;
    private Timer processTimer;
    private RootsCache roots;

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
        storageStats = (HomeItem) view.findViewById(R.id.storage_stats);
        memoryStats = (HomeItem) view.findViewById(R.id.memory_stats);
        showData();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void showData(){
        Context context = getActivity();
        roots = DocumentsApplication.getRootsCache(context);
        showStorage();
        showMemory(0);
    }

    private void showStorage() {
        final RootInfo primaryRoot = roots.getPrimaryRoot();
        if (null != primaryRoot) {
            storageStats.setInfo(primaryRoot);
            storageStats.setAction(R.drawable.ic_analyze, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((DocumentsActivity)getActivity()).showInfo("Coming Soon!");
                }
            });
            storageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DocumentsActivity activity = ((DocumentsActivity)getActivity());
                    activity.onRootPicked(primaryRoot, true);
                }
            });
            try {
                final double percentStore = (((primaryRoot.totalBytes - primaryRoot.availableBytes) / (double) primaryRoot.totalBytes) * 100);
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
                CrashReportingManager.logException(e);
            }
        }

    }

    private void showMemory(long currentAvailableBytes) {

        final RootInfo processRoot = roots.getProcessRoot();
        if (null != processRoot) {
            memoryStats.setInfo(processRoot);
            memoryStats.setAction(R.drawable.ic_clean, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new OperationTask(processRoot).execute();
                }
            });
            memoryStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DocumentsActivity activity = ((DocumentsActivity)getActivity());
                    activity.onRootPicked(processRoot, true);
                }
            });
            if(currentAvailableBytes != 0) {
                long availableBytes = currentAvailableBytes - processRoot.availableBytes;
                String summaryText = availableBytes <= 0 ? "Already cleaned up!" : getActivity().getString(R.string.root_available_bytes,
                        Formatter.formatFileSize(getActivity(), availableBytes));
                ((DocumentsActivity) getActivity()).showInfo(summaryText);
            }

            try {
                final double percentStore = (((processRoot.totalBytes - processRoot.availableBytes) / (double) processRoot.totalBytes) * 100);
                memoryStats.setProgress(0);
                processTimer = new Timer();
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
                CrashReportingManager.logException(e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        storageTimer.cancel();
        processTimer.cancel();
    }


    private class OperationTask extends AsyncTask<Void, Void, Boolean> {

        private MaterialProgressDialog progressDialog;
        private RootInfo root;
        private long currentAvailableBytes;

        public OperationTask(RootInfo root) {
            progressDialog = new MaterialProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setColor(SettingsActivity.getActionBarColor());
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Cleaning up RAM...");
            this.root = root;
            currentAvailableBytes = root.availableBytes;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            cleanupMemory(getActivity());
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Utils.isActivityAlive(getActivity())) {
                return;
            }
            AppsProvider.notifyDocumentsChanged(getActivity(), root.rootId);
            AppsProvider.notifyRootsChanged(getActivity());
            roots = DocumentsApplication.getRootsCache(getActivity());
            showMemory(currentAvailableBytes);
            progressDialog.dismiss();
        }
    }

    public void cleanupMemory(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcessesList = getRunningAppProcessInfo(context);
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcessesList) {
            activityManager.killBackgroundProcesses(processInfo.processName);
        }
    }
}