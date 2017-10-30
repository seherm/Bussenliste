package com.hermann.bussenliste;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class PlayersAdapter extends BaseAdapter {

    private List<Player> players;
    private final Context context;

    public PlayersAdapter(Context context, List<Player> players) {
        this.context = context;
        this.players = players;
    }

    @Override
    public int getCount() {
        return players.size();
    }

    @Override
    public Object getItem(int i) {
        return players.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Player player = players.get(position);

        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.player_item_layout, parent, false);
        }

        //final ImageView imageView = (ImageView) convertView.findViewById(R.id.player_image);
        final TextView nameTextView = (TextView) convertView.findViewById(R.id.player_name);
        final TextView fineTextView = (TextView) convertView.findViewById(R.id.fine_sum);

        //imageView.setImageResource(player.getImageResource());
        nameTextView.setText(player.getName());
        fineTextView.setText(context.getString(R.string.fineAmount, player.getTotalSumOfFines()));

        return convertView;

    }

    public void refresh(List<Player> players){
        this.players = players;
        notifyDataSetChanged();
    }
}
