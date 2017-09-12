package com.hermann.bussenliste;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by sebas on 04.09.2017.
 */

public class PlayersAdapter extends BaseAdapter {

    private final List<Player> players;
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
    public View getView(int position, View view, ViewGroup viewGroup) {

        final Player player = players.get(position);

        if (view == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(R.layout.player_item_layout, null);
        }

        final ImageView imageView = (ImageView) view.findViewById(R.id.player_image);
        final TextView nameTextView = (TextView) view.findViewById(R.id.player_name);
        final TextView fineTextView = (TextView) view.findViewById(R.id.fine_sum);

        //imageView.setImageResource(player.getImageResource());
        nameTextView.setText(player.getName());
        fineTextView.setText(Integer.toString(player.getTotalSumOfFines()) + " CHF");

        return view;

    }
}
