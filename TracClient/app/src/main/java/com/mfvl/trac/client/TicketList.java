/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mfvl.trac.client;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

class TicketList implements List<Ticket> {
    private final ArrayList<Ticket> arrayList;

    TicketList() {
        arrayList = new ArrayList<>();
    }

    @Override
    public int size() {
        return arrayList.size();
    }

    @Override
    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return arrayList.contains(o);
    }

    @NonNull
    @Override
    public Iterator<Ticket> iterator() {
        return arrayList.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return arrayList.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        return arrayList.toArray(a);
    }

    @Override
    public boolean add(Ticket ticket) {
        return arrayList.add(ticket);
    }

    @Override
    public boolean remove(Object o) {
        return arrayList.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return arrayList.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends Ticket> c) {
        return arrayList.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends Ticket> c) {
        return arrayList.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return arrayList.removeAll(c);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return arrayList.retainAll(c);
    }

    @Override
    public void clear() {
        arrayList.clear();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return arrayList.equals(o);
    }

    @Override
    public int hashCode() {
        return arrayList.hashCode();
    }

    @Override
    public Ticket get(int index) {
        return arrayList.get(index);
    }

    @Override
    public Ticket set(int index, Ticket element) {
        return arrayList.set(index, element);
    }

    @Override
    public void add(int index, Ticket element) {
        arrayList.add(index, element);
    }

    @Override
    public Ticket remove(int index) {
        return arrayList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return arrayList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return arrayList.lastIndexOf(o);
    }

    @Override
    public ListIterator<Ticket> listIterator() {
        return arrayList.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<Ticket> listIterator(int index) {
        return arrayList.listIterator(index);
    }

    @NonNull
    @Override
    public List<Ticket> subList(int fromIndex, int toIndex) {
        return arrayList.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return arrayList.toString();
    }
}