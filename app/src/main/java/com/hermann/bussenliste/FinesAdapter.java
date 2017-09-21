package com.hermann.bussenliste;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FinesAdapter extends BaseAdapter {

    private final ArrayList<Fine> fines;
    private final Context context;
    private final SparseBooleanArray mSelectedItemsIds;

    public FinesAdapter(Context context, ArrayList<Fine> fines) {
        this.fines = fines;
        this.context = context;
        mSelectedItemsIds = new SparseBooleanArray();
    }

    @Override
    public int getCount() {
        return fines.size();
    }

    @Override
    public Fine getItem(int i) {
        return fines.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {

        final Fine fine = fines.get(i);

        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.fine_list_item_layout, parent, false);
        }

        final TextView fineAmountTextView = (TextView) convertView.findViewById(R.id.fineAmount);
        final TextView fineTypeTextView = (TextView) convertView.findViewById(R.id.fineType);
        final TextView fineDateTextView = (TextView) convertView.findViewById(R.id.date);
        String fineAmount = context.getString(R.string.fineAmount, fine.getAmount());
        fineAmountTextView.setText(fineAmount);
        fineTypeTextView.setText(fine.getDescription());
        fineDateTextView.setText(fine.getDate());
        return convertView;
    }

    public void remove(Fine fine) {
        fines.remove(fine);
    }

    private void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, true);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }


    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    public void toggleSelection(int i) {
        selectView(i, !mSelectedItemsIds.get(i));
    }
}
