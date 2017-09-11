package com.hermann.bussenliste;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Player> players;
    private DataSourcePlayer dataSourcePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourcePlayer.open();
        players = dataSourcePlayer.getAllPlayers();

        if(players.isEmpty()) {
            for (String name : getResources().getStringArray(R.array.players)) {
                dataSourcePlayer.createPlayer(name);
            }
        }

        GridView gridView = (GridView)findViewById(R.id.players);
        final PlayersAdapter playersAdapter = new PlayersAdapter(this, players);
        gridView.setAdapter(playersAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Player selectedPlayer = (Player) playersAdapter.getItem(i);
                goToPlayerDetailsPage(selectedPlayer);
            }
        });

        dataSourcePlayer.close();
    }

    public void goToPlayerDetailsPage(Player selectedPlayer){
        Intent intent = new Intent(this, PlayerDetailsActivity.class);
        intent.putExtra ("SelectedPlayer", selectedPlayer);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
