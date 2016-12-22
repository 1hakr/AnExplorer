package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import dev.dworks.apps.anexplorer.fragment.RootsFragment.BookmarkItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.GroupItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.Item;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.RootItem;
import dev.dworks.apps.anexplorer.model.GroupInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;

public class RootsExpandableAdapter extends BaseExpandableListAdapter {

    final List<GroupInfo> group = Lists.newArrayList();

    public RootsExpandableAdapter(Context context, Collection<RootInfo> roots, Intent includeAppss) {
        processRoots(roots);
    }

    private void processRoots(Collection<RootInfo> roots) {
        List<GroupInfo> groupRoots = Lists.newArrayList();
        final List<Item> phone = Lists.newArrayList();
        final List<Item> recent = Lists.newArrayList();
        final List<Item> storage = Lists.newArrayList();
        final List<Item> library = Lists.newArrayList();
        final List<Item> folders = Lists.newArrayList();
        final List<Item> tools = Lists.newArrayList();
        final List<Item> bookmarks = Lists.newArrayList();

        for (RootInfo root : roots) {
            if (root.isRecents()) {
                recent.add(new RootItem(root));
            } else if (root.isPhoneStorage()) {
                phone.add(new RootItem(root));
            } else if (RootInfo.isLibrary(root)) {
                library.add(new RootItem(root));
            } else if (RootInfo.isFolder(root)) {
                folders.add(new RootItem(root));
            } else if (RootInfo.isBookmark(root)) {
                bookmarks.add(new BookmarkItem(root));
            } else if (RootInfo.isStorage(root)) {
                storage.add(new RootItem(root));
            } else if (RootInfo.isTools(root)) {
                tools.add(new RootItem(root));
            }
        }
        if(!storage.isEmpty()){
            storage.addAll(phone);
            groupRoots.add(new GroupInfo("Storage", storage));
        } else if(!phone.isEmpty()){
            groupRoots.add(new GroupInfo("Storage", phone));
        }
        if(!library.isEmpty()){
            recent.addAll(library);
            groupRoots.add(new GroupInfo("Library", recent));
        }
        if(!folders.isEmpty()){
            groupRoots.add(new GroupInfo("Folders", folders));
        }
        if(!bookmarks.isEmpty()){
            groupRoots.add(new GroupInfo("Bookmarks", bookmarks));
        }
        if(!tools.isEmpty()){
            groupRoots.add(new GroupInfo("Tools", tools));
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