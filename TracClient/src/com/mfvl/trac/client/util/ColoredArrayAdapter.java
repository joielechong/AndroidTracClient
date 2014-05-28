package com.mfvl.trac.client.util;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.mfvl.trac.client.TracStart;

public class ColoredArrayAdapter<T> extends ArrayAdapter<T> {

	private final List<T> items;
	private final int[] colors = new int[] { 0x00000000, 0x30111111 };

	public ColoredArrayAdapter(TracStart context, int resource, List<T> list) {
		super(context, resource, list);
		items=list;
	}
	
	@Override
	public void clear() {
		items.clear();
		super.clear();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final int colorPos = position % colors.length;
		view.setBackgroundColor(colors[colorPos]);
		return view;
	}
}