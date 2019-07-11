package dev.dworks.apps.anexplorer;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.dworks.apps.anexplorer.adapter.ShareDeviceAdapter;
import dev.dworks.apps.anexplorer.common.ActionBarActivity;
import dev.dworks.apps.anexplorer.misc.PermissionUtil;
import dev.dworks.apps.anexplorer.misc.SystemBarTintManager;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.transfer.model.Device;
import needle.Needle;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_START_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_DEVICE;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_URIS;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.SERVICE_TYPE;

public class ShareDeviceActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "ShareDeviceActivity";
    private ShareDeviceAdapter mShareDeviceAdapter;
    private final List<NsdServiceInfo> mQueue = new ArrayList<>();
    private final Map<String, Device> mDevices = new HashMap<>();
    private NsdManager mNsdManager;
    private String mThisDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_device);

        if(PermissionUtil.hasStoragePermission(this)) {
            init();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.activity_share_permissions)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        }
        int color = SettingsActivity.getPrimaryColor();

        ActionBar bar = getSupportActionBar();
        if(null != bar) {
            bar.setBackgroundDrawable(new ColorDrawable(color));
            bar.setDisplayHomeAsUpEnabled(true);
            if(isWatch()) {
                bar.setHomeAsUpIndicator(R.drawable.ic_dummy_icon);
                bar.hide();
            }
            bar.setTitle("Nearby devices");
        }
        setUpDefaultStatusBar();
        findViewById(R.id.progressContainer).setVisibility(View.VISIBLE);
        timer.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpDefaultStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor());
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(color);
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

    private void init() {
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mThisDeviceName = TransferHelper.deviceName();
        mShareDeviceAdapter = new ShareDeviceAdapter(this);
        mShareDeviceAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                showData();
            }
        });
        startDiscovery();

        final ListView listView = findViewById(R.id.selectList);
        listView.setAdapter(mShareDeviceAdapter);
        listView.setOnItemClickListener(this);
    }

    public void showData(){
        if(!Utils.isActivityAlive(ShareDeviceActivity.this)){
            return;
        }
        findViewById(R.id.progressContainer).setVisibility(View.GONE);
        int count = mShareDeviceAdapter.getCount();
        if (count == 0){
            findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        stopDisovery();
        timer.cancel();
        super.onDestroy();
    }

    private boolean isValidIntent() {
        String action = getIntent().getAction();
        if (TextUtils.isEmpty(action)){
            return false;
        }
        return (action.equals(Intent.ACTION_SEND_MULTIPLE) || action.equals(Intent.ACTION_SEND)) &&
                getIntent().hasExtra(Intent.EXTRA_STREAM);
    }

    private ArrayList<Uri> buildUriList() {
        String action = getIntent().getAction();
        if (TextUtils.isEmpty(action)){
            return null;
        }
        if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            return getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        } else {
            ArrayList<Uri> uriList = new ArrayList<>();
            uriList.add((Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
            return uriList;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!isValidIntent()){
            return;
        }
        Device device = mShareDeviceAdapter.getItem(position);

        Intent startTransfer = new Intent(ShareDeviceActivity.this, TransferService.class);
        startTransfer.setAction(ACTION_START_TRANSFER);
        startTransfer.putExtra(EXTRA_DEVICE, device);
        startTransfer.putParcelableArrayListExtra(EXTRA_URIS, buildUriList());
        startService(startTransfer);

        // Close the activity
        ShareDeviceActivity.this.setResult(RESULT_OK);
        ShareDeviceActivity.this.finish();
    }

    @Override
    public String getTag() {
        return TAG;
    }

    public void startDiscovery() {
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDisovery() {
        if (null == mNsdManager){
            return;
        }
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    /**
     * Prepare to resolve the next service
     * <p>
     * For some inexplicable reason, Android chokes miserably when
     * resolving more than one service at a time. The queue performs each
     * resolution sequentially.
     */
    private void prepareNextService() {
        synchronized (mQueue) {
            mQueue.remove(0);
            if (mQueue.size() == 0) {
                return;
            }
        }
        resolveNextService();
    }

    /**
     * Resolve the next service in the queue
     */
    private void resolveNextService() {
        NsdServiceInfo serviceInfo;
        synchronized (mQueue) {
            serviceInfo = mQueue.get(0);
        }
        Log.d(TAG, String.format("resolving \"%s\"", serviceInfo.getServiceName()));
        mNsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo) {
                Log.d(TAG, String.format("resolved \"%s\"", serviceInfo.getServiceName()));
                final Device device = new Device(
                        serviceInfo.getServiceName(),
                        "",
                        serviceInfo.getHost(),
                        serviceInfo.getPort()
                );
                Needle.onMainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        mDevices.put(serviceInfo.getServiceName(), device);
                        mShareDeviceAdapter.update(device);
                        mShareDeviceAdapter.notifyDataSetChanged();
                    }
                });
                prepareNextService();
            }

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, String.format("unable to resolve \"%s\": %d",
                        serviceInfo.getServiceName(), errorCode));
                prepareNextService();
            }
        });
    }

    private NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            if (serviceInfo.getServiceName().equals(mThisDeviceName)) {
                return;
            }
            Log.d(TAG, String.format("found \"%s\"; queued for resolving", serviceInfo.getServiceName()));
            boolean resolve;
            synchronized (mQueue) {
                resolve = mQueue.size() == 0;
                mQueue.add(serviceInfo);
            }
            if (resolve) {
                resolveNextService();
            }
        }

        @Override
        public void onServiceLost(final NsdServiceInfo serviceInfo) {
            Log.d(TAG, String.format("lost \"%s\"", serviceInfo.getServiceName()));
            Needle.onMainThread().execute(new Runnable() {
                @Override
                public void run() {
                    Device device = mDevices.get(serviceInfo.getServiceName());
                    mShareDeviceAdapter.remove(device);
                    mDevices.remove(serviceInfo.getServiceName());
                    mShareDeviceAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "service discovery started");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "service discovery stopped");
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "unable to start service discovery");
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "unable to stop service discovery");
        }
    };

    private CountDownTimer timer = new CountDownTimer(120000, 1000) {

        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            showData();
        }
    };
}
