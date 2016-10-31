package dev.dworks.apps.anexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.ConnectionUtils;
import dev.dworks.apps.anexplorer.service.ConnectionsService;

import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_FAILEDTOSTART;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_STARTED;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_STOPPED;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_START_FTPSERVER;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_STOP_FTPSERVER;

public class ConnectionsFragment extends Fragment implements View.OnClickListener {

    private TextView statusText;
    private TextView ftpAddrText;
    private ImageButton ftpBtn;

    public static void show(FragmentManager fm) {
        final ConnectionsFragment fragment = new ConnectionsFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_connections,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        statusText =(TextView) view.findViewById(R.id.statusText);
        ftpAddrText = (TextView) view.findViewById(R.id.ftpAddressText);
        ftpBtn = (ImageButton) view.findViewById(R.id.startStopButton);
        ftpBtn.setOnClickListener(this);

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageResource(R.drawable.ic_root_connections);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume(){
        super.onResume();
        updateStatus();
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(mWifiReceiver,wifiFilter);
        IntentFilter ftpFilter = new IntentFilter();
        ftpFilter.addAction(ACTION_FTPSERVER_STARTED);
        ftpFilter.addAction(ACTION_FTPSERVER_STOPPED);
        ftpFilter.addAction(ACTION_FTPSERVER_FAILEDTOSTART);
        getActivity().registerReceiver(ftpReceiver,ftpFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(mWifiReceiver);
        getActivity().unregisterReceiver(ftpReceiver);
    }

    @Override
    public  void onDestroy(){
        super.onDestroy();
    }

    private void startServer() {
        getActivity().sendBroadcast(new Intent(ACTION_START_FTPSERVER));
    }

    private void stopServer() {
        getActivity().sendBroadcast(new Intent(ACTION_STOP_FTPSERVER));
    }

    private void updateStatus(){
        setStatus(ConnectionsService.isRunning());
    }

    private void setStatus(boolean running){
        if(running){
            ftpAddrText.setText(ConnectionUtils.getFTPAddress(getActivity()));
            ftpAddrText.setTextColor(ContextCompat.getColor(getActivity(), R.color.material_blue_grey_800));
            statusText.setText(getString(R.string.ftp_status_running));
            ftpBtn.setImageResource(R.drawable.ic_stop);
        } else {
            ftpAddrText.setText("");
            statusText.setText(getString(R.string.ftp_status_not_running));
            ftpBtn.setImageResource(R.drawable.ic_start);
        }
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startStopButton:
                if(!ConnectionsService.isRunning()){
                    if(ConnectionUtils.isConnectedToWifi(getActivity()))
                        startServer();
                    else
                        ftpAddrText.setText(getString(R.string.ftp_no_wifi));
                        ftpAddrText.setTextColor(ContextCompat.getColor(getActivity(), R.color.material_red));
                }
                else{
                    stopServer();
                }
                break;
        }
    }

    private BroadcastReceiver mWifiReceiver = new  BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI){
                ftpAddrText.setText("");
            }
            else{
                stopServer();
                setStatus(false);
                ftpAddrText.setText(getString(R.string.ftp_no_wifi));
                ftpAddrText.setTextColor(ContextCompat.getColor(getActivity(), R.color.material_red));
            }
        }
    };

    private BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action == ACTION_FTPSERVER_STARTED) {
            setStatus(true);
        }
        else if(action == ACTION_FTPSERVER_FAILEDTOSTART){
            setStatus(false);
            ftpAddrText.setText("Oops! Something went wrong");
            ftpAddrText.setTextColor(ContextCompat.getColor(getActivity(), R.color.material_red));
        }
        else if(action == ACTION_FTPSERVER_STOPPED){
            setStatus(false);
        }
        }
    };
}