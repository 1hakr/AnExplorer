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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.FragmentManager;
import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.common.DialogFragment;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.ProviderExecutor;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.network.NetworkConnection;
import dev.dworks.apps.anexplorer.provider.ExplorerProvider;
import dev.dworks.apps.anexplorer.provider.NetworkStorageProvider;

import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_CONNECTION_ID;
import static dev.dworks.apps.anexplorer.network.NetworkConnection.CLIENT;
import static dev.dworks.apps.anexplorer.network.NetworkConnection.SERVER;

/**
 * Dialog to create a new connection.
 */
public class CreateConnectionFragment extends DialogFragment {
    private static final String TAG = "create_connection";
    private AppCompatEditText name;
    private AppCompatEditText host;
    private AppCompatEditText port;
    private AppCompatEditText username;
    private AppCompatEditText password;
    private AppCompatSpinner scheme;
    private AppCompatCheckBox anonymous;
    private View passwordContainer;
    private View usernameContainer;
    private int connection_id;
    private AppCompatEditText path;
    private View hostContainer;
    private View pathContainer;

    public static void show(FragmentManager fm) {
        final CreateConnectionFragment dialog = new CreateConnectionFragment();
        dialog.show(fm, TAG);
    }

    public static void show(FragmentManager fm, int connection_id) {
        final CreateConnectionFragment dialog = new CreateConnectionFragment();
        final Bundle args = new Bundle();
        args.putInt(EXTRA_CONNECTION_ID, connection_id);
        dialog.setArguments(args);
        dialog.show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(null != args){
            connection_id = args.getInt(EXTRA_CONNECTION_ID);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final DialogBuilder builder = new DialogBuilder(context);
        final LayoutInflater dialogInflater = getActivity().getLayoutInflater();

        final View view = dialogInflater.inflate(R.layout.dialog_create_connection, null, false);
        name = (AppCompatEditText) view.findViewById(R.id.name);
        host = (AppCompatEditText) view.findViewById(R.id.host);
        port = (AppCompatEditText) view.findViewById(R.id.port);
        path = (AppCompatEditText) view.findViewById(R.id.path);
        hostContainer = view.findViewById(R.id.hostContainer);
        pathContainer = view.findViewById(R.id.pathContainer);
        username = (AppCompatEditText) view.findViewById(R.id.username);
        usernameContainer = view.findViewById(R.id.usernameContainer);
        password = (AppCompatEditText) view.findViewById(R.id.password);
        passwordContainer = view.findViewById(R.id.passwordContainer);
        scheme = (AppCompatSpinner) view.findViewById(R.id.scheme);
        anonymous = (AppCompatCheckBox) view.findViewById(R.id.anonymous);
        anonymous.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                usernameContainer.setVisibility(b ? View.GONE : View.VISIBLE);
                passwordContainer.setVisibility(b ? View.GONE : View.VISIBLE);
            }
        });

        if(connection_id != 0){
            NetworkConnection connection =
                    NetworkConnection.fromConnectionId(getActivity(), connection_id);

            ArrayAdapter myAdap = (ArrayAdapter) scheme.getAdapter();
            int spinnerPosition = myAdap.getPosition(connection.getScheme().toUpperCase());
            scheme.setSelection(spinnerPosition);

            name.setText(connection.getName());
            host.setText(connection.getHost());
            port.setText(Integer.toString(connection.getPort()));
            path.setText(connection.getPath());
            username.setText(connection.getUserName());
            password.setText(connection.getPassword());
            anonymous.setChecked(connection.isAnonymousLogin());
            if(SERVER.equals(connection.getType())){
                scheme.setVisibility(View.GONE);
                hostContainer.setVisibility(View.GONE);
                pathContainer.setVisibility(View.VISIBLE);
            }
        }
        builder.setTitle( (connection_id == 0 ? "New" : "Edit") + " Connection");
        builder.setView(view);

        builder.setPositiveButton(connection_id == 0 ? "ADD" : "SAVE", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final BaseActivity activity = (BaseActivity) getActivity();

                NetworkConnection networkConnection = getNetworkConnection();
                if(validate(networkConnection)){
                    new CreateConnectionTask(activity, networkConnection).executeOnExecutor(
                            ProviderExecutor.forAuthority(ExplorerProvider.AUTHORITY));
                }

            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    private NetworkConnection getNetworkConnection(){
        NetworkConnection networkConnection =
                NetworkConnection.fromConnectionId(getActivity(), connection_id);
        networkConnection.name = name.getText().toString();
        networkConnection.host = host.getText().toString();
        String portNumber = port.getText().toString();
        if(!TextUtils.isEmpty(portNumber)) {
            networkConnection.port = Integer.parseInt(portNumber);
        }
        networkConnection.username = username.getText().toString();
        networkConnection.password = password.getText().toString();
        networkConnection.path = path.getText().toString();
        networkConnection.scheme = scheme.getSelectedItem().toString().toLowerCase();

        networkConnection.setAnonymous(anonymous.isChecked());
        if(connection_id == 0) {
            networkConnection.type = CLIENT;
            networkConnection.build();
        }
        return networkConnection;
    }

    private boolean validate(NetworkConnection networkConnection){
        boolean isValid = true;
        if(TextUtils.isEmpty(networkConnection.name)){
            return false;
        }
        if(TextUtils.isEmpty(networkConnection.host) && !SERVER.equals(networkConnection.getType())){
            return false;
        }
        if(networkConnection.port == 0){
            return false;
        }
        if(!networkConnection.isAnonymousLogin) {
            if (TextUtils.isEmpty(networkConnection.username)) {
                return false;
            }
            if (TextUtils.isEmpty(networkConnection.password)) {
                return false;
            }
        }
        return isValid;
    }

    private class CreateConnectionTask extends AsyncTask<Void, Void, Boolean> {
        private final BaseActivity mActivity;
        private final NetworkConnection mNetworkConnection;

        public CreateConnectionTask(
                BaseActivity activity, NetworkConnection networkConnection) {
            mActivity = activity;
            mNetworkConnection = networkConnection;
        }

        @Override
        protected void onPreExecute() {
            mActivity.setPending(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return NetworkStorageProvider.addUpdateConnection(mActivity, mNetworkConnection, connection_id);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                RootsCache.updateRoots(mActivity, NetworkStorageProvider.AUTHORITY);
                ConnectionsFragment connectionsFragment = ConnectionsFragment.get(mActivity.getSupportFragmentManager());
                ServerFragment serverFragment = ServerFragment.get(mActivity.getSupportFragmentManager());
                if(null != connectionsFragment){
                    connectionsFragment.reload();
                    if(connection_id == 0) {
                        connectionsFragment.openConnectionRoot(mNetworkConnection);
                    }
                } else if(null != serverFragment){
                    serverFragment.reload();
                }
            }
        }
    }
}
