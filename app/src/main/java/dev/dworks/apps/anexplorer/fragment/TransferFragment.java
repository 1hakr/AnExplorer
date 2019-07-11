package dev.dworks.apps.anexplorer.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.adapter.TransferAdapter;
import dev.dworks.apps.anexplorer.common.RecyclerFragment;
import dev.dworks.apps.anexplorer.directory.DividerItemDecoration;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ShareDeviceActivity;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.transfer.model.TransferStatus;
import dev.dworks.apps.anexplorer.ui.CircleImage;

import static android.widget.LinearLayout.VERTICAL;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_BROADCAST;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_REMOVE_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_STATUS;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.TRANSFER_UPDATED;

public class TransferFragment extends RecyclerFragment
        implements TransferAdapter.OnItemClickListener {

    private static final String TAG = "TransferFragment";

    private TransferAdapter mAdapter;
    private String emptyText;
    private TransferHelper mTransferHelper;

    public static void show(FragmentManager fm) {
        final TransferFragment fragment = new TransferFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static TransferFragment get(FragmentManager fm) {
        return (TransferFragment) fm.findFragmentByTag(TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        emptyText = getString(R.string.activity_transfer_empty_text);
        mTransferHelper = new TransferHelper(getActivity(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_transfer,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final Resources res = getActivity().getResources();

        // Indent our list divider to align with text
        final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
        final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
        DividerItemDecoration decoration = new DividerItemDecoration(getActivity(), VERTICAL);
        if (insetLeft) {
            decoration.setInset(insetSize, 0);
        } else {
            decoration.setInset(0, insetSize);
        }
        if(!isWatch()) {
            getListView().addItemDecoration(decoration);
        }

        if(!isSpecialDevice()) {
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(createItemTouchHelper());
            itemTouchHelper.attachToRecyclerView(getListView());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();

        mAdapter = new TransferAdapter(context);
        mAdapter.setOnItemClickListener(this);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if(isWatch()) {
                    Utils.setItemsCentered(getListView(), mAdapter.getItemCount() > 1);
                }
                super.onChanged();
            }
        });
        setListAdapter(mAdapter);
        showRecyclerView();
        if(isWatch()) {
            Utils.setItemsCentered(getListView(), mAdapter.getItemCount() > 1);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TRANSFER_UPDATED);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

        // Intent broadcastIntent = new Intent(getActivity(), TransferService.class);
        // broadcastIntent.setAction(ACTION_BROADCAST);
        // getActivity().startService(broadcastIntent);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_transfer, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_show_devices:
                Intent shareIntent = new Intent(getContext(), ShareDeviceActivity.class);
                getActivity().startActivity(shareIntent);
                break;
            case R.id.action_transfer_help:
                showHelp();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showRecyclerView(){
        setListShown(true);
        if (mAdapter.getItemCount() == 1) {
            setEmptyText(emptyText);
        } else {
            setEmptyText("");
        }
    }

    @Override
    public void onItemClick(RecyclerView.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemLongClick(RecyclerView.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemViewClick(RecyclerView.ViewHolder item, View view, int position) {
        if(!TransferHelper.isServerRunning(getActivity())){
            ((TransferAdapter.HeaderViewHolder)item).setStatus(true);
            mTransferHelper.startTransferServer();
        }
        else{
            ((TransferAdapter.HeaderViewHolder)item).setStatus(false);
            mTransferHelper.stopTransferServer();
        }
    }

    private ItemTouchHelper.Callback createItemTouchHelper() {
        ItemTouchHelper.Callback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START | ItemTouchHelper.END) {

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (viewHolder instanceof TransferAdapter.HeaderViewHolder) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if(position == 0){
                    return;
                }
                TransferStatus transferStatus = mAdapter.getItem(position);
                mAdapter.remove(position - 1);
                showRecyclerView();

                // Remove the item from the service
                Intent removeIntent = new Intent(getActivity(), TransferService.class);
                removeIntent.setAction(ACTION_REMOVE_TRANSFER);
                removeIntent.putExtra(EXTRA_TRANSFER, transferStatus.getId());
                getActivity().startService(removeIntent);
            }
        };
        return simpleCallback;
    }

    private BroadcastReceiver mBroadcastReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TransferStatus transferStatus = intent.getParcelableExtra(EXTRA_STATUS);
            mAdapter.update(transferStatus);
            showRecyclerView();
        }
    };

    public void showHelp(){
        new AlertDialog.Builder(getActivity(),
                R.style.AlertDialogStyle)
                .setTitle("How to use WiFi Share")
                .setMessage(R.string.transfer_help_description)
                .setPositiveButton("Got it!", null)
                .show();
    }
}