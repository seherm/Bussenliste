package com.hermann.bussenliste;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class PlayersAdapter extends BaseAdapter {

    private List<Player> players;
    private final Context context;
    private final SparseBooleanArray mSelectedItemsIds;

    public PlayersAdapter(Context context, List<Player> players) {
        this.context = context;
        this.players = players;
        mSelectedItemsIds = new SparseBooleanArray();
    }

    @Override
    public int getCount() {
        return players.size();
    }

    @Override
    public Player getItem(int i) {
        return players.get(i);
    }

    @Override
    public long getItemId(int i) {
        return players.get(i).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Player player = players.get(position);

        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.player_item_layout, parent, false);
        }

        final ImageView imageView = convertView.findViewById(R.id.player_image);
        final TextView nameTextView = convertView.findViewById(R.id.player_name);
        final TextView fineTextView = convertView.findViewById(R.id.fine_sum);

        nameTextView.setText(player.getName());
        fineTextView.setText(context.getString(R.string.fineAmount, player.getTotalSumOfFines()));
        if(player.getPhoto() != null){
            imageView.setImageBitmap(player.getPhoto());
        }else{
            imageView.setImageResource(R.drawable.player);
        }

        return convertView;
    }

    public void refresh(List<Player> players){
        this.players = players;
        Collections.sort(players);
        notifyDataSetChanged();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    public void toggleSelection(int i) {
        selectView(i, !mSelectedItemsIds.get(i));
    }

    private void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, true);
        else
            mSelectedItemsIds.delete(position);
        notifyDataSetChanged();
    }
}
