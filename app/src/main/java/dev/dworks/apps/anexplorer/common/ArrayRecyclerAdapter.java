package dev.dworks.apps.anexplorer.common;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class ArrayRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    protected final ArrayList<T> mData = new ArrayList<T>();

    public void add(final T item) {
        if (item == null) return;
        mData.add(item);
        notifyDataSetChanged();
    }

    public void addAll(final Collection<? extends T> collection) {
        mData.addAll(collection);
        notifyDataSetChanged();
    }

    public T set(int location, T object) {
        T origin = mData.set(location, object);
        notifyItemChanged(location  + 1);
        return origin;
    }

    public int indexOf(Object object) {
        return mData.indexOf(object);
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
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