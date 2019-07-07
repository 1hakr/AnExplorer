package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import dev.dworks.apps.anexplorer.fragment.RootsFragment;
import dev.dworks.apps.anexplorer.model.GroupInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;


public class RootsCommonAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

    private final List<RootInfo> group = new ArrayList<>();
    private Context context;

    public RootsCommonAdapter(Context context, Collection<RootInfo> roots, Intent includeAppss) {
        this.context = context;
        processRoots(roots);
    }

    public void setData(Collection<RootInfo> roots){
        processRoots(roots);
        notifyDataSetChanged();
    }

    private void processRoots(Collection<RootInfo> roots) {
        List<RootInfo> groupRoots = new ArrayList<>();
        final List<RootInfo> home = new ArrayList<>();
        final List<RootInfo> phone = new ArrayList<>();
        final List<RootInfo> recent = new ArrayList<>();
        final List<RootInfo> connection = new ArrayList<>();
        final List<RootInfo> transfer = new ArrayList<>();
        final List<RootInfo> receive = new ArrayList<>();
        final List<RootInfo> rooted = new ArrayList<>();
        final List<RootInfo> appbackup = new ArrayList<>();
        final List<RootInfo> usb = new ArrayList<>();

        final List<RootInfo> storage = new ArrayList<>();
        final List<RootInfo> secondaryStorage = new ArrayList<>();
        final List<RootInfo> network = new ArrayList<>();
        final List<RootInfo> cloud = new ArrayList<>();
        final List<RootInfo> apps = new ArrayList<>();
        final List<RootInfo> libraryMedia = new ArrayList<>();
        final List<RootInfo> libraryNonMedia = new ArrayList<>();
        final List<RootInfo> folders = new ArrayList<>();
        final List<RootInfo> bookmarks = new ArrayList<>();

        for (RootInfo root : roots) {
            if (root.isHome()) {
                home.add(root);
            } else if (root.isRecents()) {
                if(recent.size() == 0) {
                    recent.add(root);
                }
            } else if (root.isConnections()) {
                // connection.add(root);
            } else if (root.isTransfer()) {
                transfer.add(root);
            }  else if (root.isReceiveFolder()) {
                receive.add(root);
            } else if (root.isRootedStorage()) {
                rooted.add(root);
            } else if (root.isPhoneStorage()) {
                phone.add(root);
            } else if (root.isAppBackupFolder()) {
                appbackup.add(root);
            } else if (root.isUsbStorage()) {
                usb.add(root);
            } else if (RootInfo.isLibraryMedia(root)) {
                libraryMedia.add(root);
            } else if (RootInfo.isLibraryNonMedia(root)) {
                libraryNonMedia.add(root);
            } else if (RootInfo.isFolder(root)) {
                folders.add(root);
            } else if (RootInfo.isBookmark(root)) {
                bookmarks.add(root);
            } else if (RootInfo.isStorage(root)) {
                if(root.isSecondaryStorage()){
                    secondaryStorage.add(root);
                } else {
                    storage.add(root);
                }
            } else if (RootInfo.isApps(root)) {
                apps.add(root);
            } else if (RootInfo.isNetwork(root)) {
                network.add(root);
            } else if (RootInfo.isCloud(root)) {
                cloud.add(root);
            }
        }

        if(!home.isEmpty() || !storage.isEmpty() || !phone.isEmpty() || !rooted.isEmpty()){
            home.addAll(storage);
            home.addAll(secondaryStorage);
            home.addAll(usb);
            home.addAll(phone);
            home.addAll(rooted);
            groupRoots.addAll(home);
        }

        if(!transfer.isEmpty()){
            transfer.addAll(receive);
            groupRoots.addAll(transfer);
        }

        if(!network.isEmpty()){
            network.addAll(connection);
            network.addAll(transfer);
            network.addAll(cloud);
            groupRoots.addAll(network);
        } else if(!connection.isEmpty()){
            groupRoots.addAll(connection);
        }

        if(!apps.isEmpty()){
            if(!appbackup.isEmpty()) {
                apps.addAll(appbackup);
            }
            groupRoots.addAll(apps);
        }

        if(!libraryMedia.isEmpty() || !libraryNonMedia.isEmpty()){
            recent.addAll(libraryMedia);
            recent.addAll(libraryNonMedia);
            groupRoots.addAll(recent);
        } else if(!recent.isEmpty()){
            groupRoots.addAll(recent);
        }

        if(!folders.isEmpty()){
            folders.addAll(bookmarks);
            groupRoots.addAll(folders);
        }

        group.clear();
        group.addAll(groupRoots);
    }

    @Override
    public String getItemText(int i) {
        return group.get(i).title;
    }

    @Override
    public Drawable getItemDrawable(int i) {
        return group.get(i).loadNavDrawerIcon(context);
    }

    public RootInfo getItem(int position){
        return group.get(position);
    }

    @Override
    public int getCount() {
        return group.size();
    }
}