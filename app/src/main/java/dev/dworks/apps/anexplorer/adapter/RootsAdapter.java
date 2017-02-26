package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dev.dworks.apps.anexplorer.fragment.RootsFragment.AppItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.BookmarkItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.Item;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.RootComparator;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.RootItem;
import dev.dworks.apps.anexplorer.fragment.RootsFragment.SpacerItem;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

public class RootsAdapter extends ArrayAdapter<Item> {

    public RootsAdapter(Context context, Collection<RootInfo> roots, Intent includeAppss) {
        super(context, 0);

        int defaultColor = SettingsActivity.getPrimaryColor(context);
        RootItem recents = null;
        RootItem images = null;
        RootItem videos = null;
        RootItem audio = null;
        RootItem downloads = null;
        RootItem root_root = null;
        RootItem phone = null;

        final List<RootInfo> clouds = new ArrayList<>();
        final List<RootInfo> locals = new ArrayList<>();
        final List<RootInfo> extras = new ArrayList<>();
        final List<RootInfo> bookmarks = new ArrayList<>();

        for (RootInfo root : roots) {
            if (root.isRecents()) {
                recents = new RootItem(root, defaultColor);
            } else if (root.isBluetoothFolder() || root.isDownloadsFolder() || root.isAppBackupFolder()) {
                extras.add(root);
            } else if (root.isBookmarkFolder()) {
                bookmarks.add(root);
            } else if (root.isPhoneStorage()) {
                phone = new RootItem(root, defaultColor);
            } else if (root.isStorage()) {
                locals.add(root);
            } else if (root.isRootedStorage()) {
                root_root = new RootItem(root, defaultColor);
            } else if (root.isDownloads()) {
                downloads = new RootItem(root, defaultColor);
            } else if (root.isImages()) {
                images = new RootItem(root, defaultColor);
            } else if (root.isVideos()) {
                videos = new RootItem(root, defaultColor);
            } else if (root.isAudio()) {
                audio = new RootItem(root, defaultColor);
            } else {
                clouds.add(root);
            }
        }

        final RootComparator comp = new RootComparator();
        Collections.sort(clouds, comp);
        //Collections.sort(locals, comp);
        //Collections.reverse(locals);

        for (RootInfo local : locals) {
            add(new RootItem(local, defaultColor));
        }
        if (phone != null) add(phone);

        for (RootInfo extra : extras) {
            add(new RootItem(extra, defaultColor));
        }

        if (root_root != null) {
            add(new SpacerItem());
            add(root_root);
        }

        if (bookmarks.size() > 0) {
            add(new SpacerItem());
            for (RootInfo bookmark : bookmarks) {
                add(new BookmarkItem(bookmark));
            }
        }

        add(new SpacerItem());
        if (recents != null) add(recents);
        if (images != null) add(images);
        if (videos != null) add(videos);
        if (audio != null) add(audio);
        if (downloads != null) add(downloads);

        //if (includeApps == null) {
        add(new SpacerItem());
        for (RootInfo cloud : clouds) {
            add(new RootItem(cloud, defaultColor));
        }
/*                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> infos = pm.queryIntentActivities(
                        includeApps, PackageManager.MATCH_DEFAULT_ONLY);

                final List<AppItem> apps = Lists.newArrayList();

                // Omit ourselves from the list
                for (ResolveInfo info : infos) {
                    if (!context.getPackageName().equals(info.activityInfo.packageName)) {
                        apps.add(new AppItem(info));
                    }
                }

                if (apps.size() > 0) {
                    add(new SpacerItem());
                    for (Item item : apps) {
                        add(item);
                    }
                }*/
        //}
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Item item = getItem(position);
        return item.getView(convertView, parent);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != 1;
    }

    @Override
    public int getItemViewType(int position) {
        final Item item = getItem(position);
        if (item instanceof RootItem || item instanceof AppItem) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }
}