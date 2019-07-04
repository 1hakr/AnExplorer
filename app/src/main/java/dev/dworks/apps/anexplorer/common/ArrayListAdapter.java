package dev.dworks.apps.anexplorer.common;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by hakr on 02/07/19.
 */

public abstract class ArrayListAdapter<T, VH extends ArrayListAdapter.ViewHolder> extends ArrayAdapter<T> {

    protected final List<T> mData = new ArrayList<T>();

    public static abstract class ViewHolder {
        final View itemView;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }

        public abstract void setData(int position);
    }

    public ArrayListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public ArrayListAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        VH viewHolder;
        if (convertView == null) {
            viewHolder = onCreateViewHolder(parent, getItemViewType(position));
            viewHolder.itemView.setTag(viewHolder);
        } else {
            viewHolder = (VH) convertView.getTag();
        }
        onBindViewHolder(viewHolder, position);
        return viewHolder.itemView;
    }

    public abstract int getItemViewType(int position);

    /**
     * Create a new view holder
     * @param parent
     * @return view holder
     */
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    /**
     * Bind data at position into viewHolder
     * @param viewHolder
     * @param position
     */
    public abstract void onBindViewHolder(VH viewHolder, int position);

    public void add(final T item) {
        if (item == null) return;
        mData.add(item);
        notifyDataSetChanged();
    }

    public void add(int position, final T item) {
        if (item == null) return;
        mData.add(position, item);
        notifyDataSetChanged();
    }

    public void addAll(final Collection<? extends T> collection) {
        mData.addAll(collection);
        notifyDataSetChanged();
    }

    public int indexOf(Object object) {
        return mData.indexOf(object);
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    public T getItem(final int position) {
        return mData.get(position);
    }

    public boolean remove(final int position) {
        final boolean ret = mData.remove(position) != null;
        notifyDataSetChanged();
        return ret;
    }

    public void removeAll(final List<T> collection) {
        mData.removeAll(collection);
        notifyDataSetChanged();
    }

    public void sort(final Comparator<? super T> comparator) {
        Collections.sort(mData, comparator);
        notifyDataSetChanged();
    }
}
