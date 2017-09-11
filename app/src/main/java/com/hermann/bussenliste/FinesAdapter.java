package com.hermann.bussenliste;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by sebas on 05.09.2017.
 */

public class FinesAdapter extends BaseAdapter {


    private final ArrayList<Fine> fines;
    private final Context context;
    private SparseBooleanArray mSelectedItemsIds;

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
    public View getView(int i, View view, ViewGroup viewGroup) {

        final Fine fine = fines.get(i);

        if (view == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(R.layout.fine_list_item_layout, null);
        }

        final TextView fineAmountTextView = (TextView) view.findViewById(R.id.fineAmount);
        final TextView fineTypeTextView = (TextView) view.findViewById(R.id.fineType);
        fineAmountTextView.setText(Integer.toString(fine.getAmount()) + " CHF");
        fineTypeTextView.setText(fine.getType().name());

        return view;
    }

    public void remove(Fine fine) {
        fines.remove(fine);
    }

    private void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, value);
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
