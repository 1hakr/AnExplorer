package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.dworks.apps.anexplorer.fragment.RootsFragment.BookmarkItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.GroupItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.Item;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.RootItem;
import dev.dworks.apps.anexplorer.model.GroupInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;

public class RootsExpandableAdapter extends BaseExpandableListAdapter {

    final List<GroupInfo> group = new ArrayList<>();

    public RootsExpandableAdapter(Context context, Collection<RootInfo> roots, Intent includeAppss) {
        processRoots(roots);
    }

    private void processRoots(Collection<RootInfo> roots) {
        List<GroupInfo> groupRoots = new ArrayList<>();
        final List<Item> phone = new ArrayList<>();
        final List<Item> recent = new ArrayList<>();
        final List<Item> connection = new ArrayList<>();
        final List<Item> rooted = new ArrayList<>();
        final List<Item> appbackup = new ArrayList<>();
        final List<Item> usb = new ArrayList<>();

        final List<Item> storage = new ArrayList<>();
        final List<Item> network = new ArrayList<>();
        final List<Item> apps = new ArrayList<>();
        final List<Item> library = new ArrayList<>();
        final List<Item> folders = new ArrayList<>();
        final List<Item> bookmarks = new ArrayList<>();

        for (RootInfo root : roots) {
            if (root.isRecents()) {
                recent.add(new RootItem(root));
            } else if (root.isConnections()) {
                connection.add(new RootItem(root));
            } else if (root.isRootedStorage()) {
                rooted.add(new RootItem(root));
            } else if (root.isPhoneStorage()) {
                phone.add(new RootItem(root));
            } else if (root.isAppBackupFolder()) {
                appbackup.add(new RootItem(root));
            } else if (root.isUsbStorage()) {
                usb.add(new RootItem(root));
            } else if (RootInfo.isLibrary(root)) {
                library.add(new RootItem(root));
            } else if (RootInfo.isFolder(root)) {
                folders.add(new RootItem(root));
            } else if (RootInfo.isBookmark(root)) {
                bookmarks.add(new BookmarkItem(root));
            } else if (RootInfo.isStorage(root)) {
                storage.add(new RootItem(root));
            } else if (RootInfo.isApps(root)) {
                apps.add(new RootItem(root));
            } else if (RootInfo.isNetwork(root)) {
                network.add(new RootItem(root));
            }
        }

        if(!storage.isEmpty()){
            storage.addAll(usb);
            storage.addAll(phone);
            storage.addAll(rooted);
            groupRoots.add(new GroupInfo("Storage", storage));
        } else if(!phone.isEmpty()){
            storage.addAll(usb);
            storage.addAll(phone);
            storage.addAll(rooted);
            groupRoots.add(new GroupInfo("Storage", phone));
        } else if(!rooted.isEmpty()){
            storage.addAll(usb);
            storage.addAll(rooted);
            groupRoots.add(new GroupInfo("Storage", rooted));
        }

        if(!network.isEmpty()){
            network.addAll(connection);
            groupRoots.add(new GroupInfo("Network", network));
        } else {
            groupRoots.add(new GroupInfo("Network", connection));
        }

        if(!apps.isEmpty()){
            if(!appbackup.isEmpty()) {
                apps.addAll(appbackup);
            }
            groupRoots.add(new GroupInfo("Apps", apps));
        }

        if(!library.isEmpty()){
            recent.addAll(library);
            groupRoots.add(new GroupInfo("Library", recent));
        } else {
            groupRoots.add(new GroupInfo("Library", recent));
        }

        if(!folders.isEmpty()){
            if(!bookmarks.isEmpty()){
                folders.addAll(bookmarks);
            }
            groupRoots.add(new GroupInfo("Folders", folders));
        }

        group.clear();
        group.addAll(groupRoots);
    }

    @Override
    public int getGroupCount() {
        return group.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return group.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        final GroupItem item = new GroupItem((GroupInfo) getGroup(groupPosition));
        return item.getView(convertView, parent);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return group.get(groupPosition).itemList.size();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return group.get(groupPosition).itemList.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final Item item = (Item) getChild(groupPosition, childPosition);
        return item.getView(convertView, parent);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    public void setData(Collection<RootInfo> roots){
        processRoots(roots);
        notifyDataSetChanged();
    }
}