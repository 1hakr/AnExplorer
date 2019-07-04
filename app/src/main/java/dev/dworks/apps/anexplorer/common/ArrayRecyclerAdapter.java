package dev.dworks.apps.anexplorer.common;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class ArrayRecyclerAdapter<E, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements List<E> {

    private final List<E> list;

    public ArrayRecyclerAdapter() {
        list = new ArrayList<E>();
    }

    public ArrayRecyclerAdapter(int capacity) {
        list = new ArrayList<E>(capacity);
    }

    public ArrayRecyclerAdapter(Collection<? extends E> collection) {
        list = new ArrayList<E>(collection);
    }

    @Override
    public int getItemCount() {
        return size();
    }

    @Override
    public void add(int location, E object) {
        list.set(location, object);
        notifyItemInserted(location);
    }

    @Override
    public boolean add(E object) {
        if (list.add(object)) {
            notifyItemInserted(list.size() - 1);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        if (list.addAll(location, collection)) {
            notifyItemRangeInserted(location, collection.size());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (list.addAll(collection)) {
            notifyItemRangeInserted(list.size() - 1, collection.size());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        list.clear();
        notifyDataSetChanged();
    }

    @Override
    public boolean contains(Object object) {
        return list.contains(object);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public E get(int location) {
        return list.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return list.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        return list.lastIndexOf(object);
    }

    @NonNull
    @Override
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<E> listIterator(int location) {
        return list.listIterator(location);
    }

    @Override
    public E remove(int location) {
        E item = list.remove(location);
        notifyItemRemoved(location);
        return item;
    }

    @Override
    public boolean remove(Object object) {
        int index = list.indexOf(object);
        if (list.remove(object)) {
            notifyItemRemoved(index);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        boolean modified = false;
        for (int i = 0; i < list.size(); i++) {
            if (collection.contains(list.get(i))) {
                list.remove(i);
                notifyItemRemoved(i);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        boolean modified = false;
        for (int i = 0; i < list.size(); i++) {
            if (!collection.contains(list.get(i))) {
                list.remove(i);
                notifyItemRemoved(i);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public E set(int location, E object) {
        E origin = list.set(location, object);
        notifyItemChanged(location);
        return origin;
    }

    @Override
    public int size() {
        return list.size();
    }

    @NonNull
    @Override
    public List<E> subList(int start, int end) {
        return list.subList(start, end);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] array) {
        return list.toArray(array);
    }
}