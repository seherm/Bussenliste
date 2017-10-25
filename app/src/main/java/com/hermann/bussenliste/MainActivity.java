package com.hermann.bussenliste;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static final String PRODUCTION_SERVER_ADDRESS = "https://bussenliste.000webhostapp.com/";
    //private static final String TEST_SERVER_ADDRESS = "http://192.168.0.101:80/sqlitemysqlsync/";
    private DataSource dataSource;
    List<Player> players;
    List<Fine> fines;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Transferring Data from Remote MySQL DB and Syncing SQLite. Please wait...");
        progressDialog.setCancelable(false);


        dataSource = new DataSource(this);
        dataSource.open();
        players = dataSource.getAllPlayers();
        fines = dataSource.getAllFines();
        dataSource.close();

        if (noPlayers() || noFines()) {
            showImportDataDialog();
        }

        //initialize players view
        GridView gridView = (GridView) findViewById(R.id.players);
        final PlayersAdapter playersAdapter = new PlayersAdapter(this, players);
        gridView.setAdapter(playersAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Player selectedPlayer = (Player) playersAdapter.getItem(i);
                goToPlayerDetailsPage(selectedPlayer);
            }
        });
    }

    private boolean noPlayers() {
        return players.isEmpty();
    }

    private boolean noFines() {
        return fines.isEmpty();
    }

    private void showImportDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.no_players_available);
        builder.setMessage(R.string.import_players_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                syncMySQLSQLiteDB();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    private void goToPlayerDetailsPage(Player selectedPlayer) {
        Intent intent = new Intent(this, PlayerDetailsActivity.class);
        intent.putExtra("SelectedPlayer", selectedPlayer);
        startActivity(intent);
    }

    private void goToImportDataPage() {
        Intent intent = new Intent(this, ImportDataActivity.class);
        startActivity(intent);
    }

    private void goToSettingsPage() {
        Intent intent = new Intent(this, SettingsActivity.class);
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

        switch (id) {
            case R.id.action_settings:
                goToSettingsPage();
                break;
            case R.id.action_sync:
                syncSQLiteMySQLDB();
                break;
            default:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void syncSQLiteMySQLDB() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        dataSource.open();
        List<Player> playerList = dataSource.getAllPlayers();
        progressDialog.setMessage("Synching SQLite Data with Remote MySQL DB. Please wait...");
        progressDialog.setCancelable(false);

        if (playerList.size() != 0) {
            if (dataSource.dbSyncCount(DatabaseHelper.TABLE_PLAYERS) != 0) {
                progressDialog.show();
                params.put("playersJSON", dataSource.composeJSONfromSQLite(DatabaseHelper.TABLE_PLAYERS));
                client.post(PRODUCTION_SERVER_ADDRESS + "insertplayer.php", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            progressDialog.hide();
                            JSONArray arr = new JSONArray(new String(responseBody));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = (JSONObject) arr.get(i);
                                dataSource.updateSyncStatus(DatabaseHelper.TABLE_PLAYERS, obj.get("id").toString(), obj.get("status").toString());
                            }
                            Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error occurred [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Unexpected Error occurred! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else if (dataSource.dbSyncCount(DatabaseHelper.TABLE_FINES) != 0) {
                params.put("finesJSON", dataSource.composeJSONfromSQLite(DatabaseHelper.TABLE_FINES));
                client.post(PRODUCTION_SERVER_ADDRESS + "insertfine.php", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            JSONArray arr = new JSONArray(new String(responseBody));
                            System.out.println(arr.length());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = (JSONObject) arr.get(i);
                                System.out.println(obj.get("id"));
                                System.out.println(obj.get("status"));
                                dataSource.updateSyncStatus(DatabaseHelper.TABLE_FINES, obj.get("id").toString(), obj.get("status").toString());
                            }
                            Toast.makeText(getApplicationContext(), "DB Sync completed!", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error occurred [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if (statusCode == 404) {
                            Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 500) {
                            Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Unexpected Error occurred! [Most common Error: Device might not be connected to Internet]", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "No data in SQLite DB", Toast.LENGTH_LONG).show();
        }
    }


    // Method to Sync online MySQL DB with local SQLite DB
    public void syncMySQLSQLiteDB() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        progressDialog.show();
        // Make Http call to getplayers.php
        client.post(PRODUCTION_SERVER_ADDRESS + "getplayers.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // Update SQLite DB with response sent by getplayers.php
                progressDialog.hide();
                updatePlayersSQLite(new String(responseBody));
            }

            // When error occurred
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        client.post(PRODUCTION_SERVER_ADDRESS + "getfines.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // Update SQLite DB with response sent by getfines.php
                updateFinesSQLite(new String(responseBody));
            }

            // When error occurred
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void updatePlayersSQLite(String response) {
        ArrayList<HashMap<String, String>> playersSyncList = new ArrayList<>();
        Gson gson = new GsonBuilder().create();
        try {
            JSONArray array = new JSONArray(response);

            if (array.length() != 0) {
                // Loop through each array element
                for (int i = 0; i < array.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) array.get(i);
                    // Insert Player into local SQLite DB
                    String playerId = obj.get("playerId").toString();
                    String playerName = obj.get("playerName").toString();
                    String playerFines = obj.get("playerFines").toString();
                    dataSource.open();
                    dataSource.createPlayer(playerName);
                    if (playerFines != null) {
                        Type type = new TypeToken<ArrayList<Fine>>() {
                        }.getType();
                        ArrayList<Fine> finesList = gson.fromJson(playerFines, type);
                        dataSource.updatePlayer(Integer.parseInt(playerId), finesList);
                    }
                    dataSource.close();
                    HashMap<String, String> map = new HashMap<>();
                    // Add status for each User in Hashmap
                    map.put("Id", obj.get("playerId").toString());
                    map.put("status", "1");
                    playersSyncList.add(map);
                }
                // Inform Remote MySQL DB about the completion of Sync activity by passing Sync status of Users
                updateMySQLSyncSts(gson.toJson(playersSyncList));
                // Reload the Main Activity
                this.recreate();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateFinesSQLite(String response) {
        ArrayList<HashMap<String, String>> finesSyncList = new ArrayList<>();
        Gson gson = new GsonBuilder().create();
        try {
            // Extract JSON array from the response
            JSONArray arr = new JSONArray(response);
            System.out.println(arr.length());
            // If no of array elements is not zero
            if (arr.length() != 0) {
                // Loop through each array element, get JSON object which has userid and username
                for (int i = 0; i < arr.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) arr.get(i);
                    // Insert User into SQLite DB
                    dataSource.open();
                    dataSource.createFine(obj.get("fineDescription").toString(), Integer.parseInt(obj.get("fineAmount").toString()));
                    dataSource.close();
                    HashMap<String, String> map = new HashMap<>();
                    // Add status for each User in Hashmap
                    map.put("Id", obj.get("fineId").toString());
                    map.put("status", "1");
                    finesSyncList.add(map);
                }
                // Inform Remote MySQL DB about the completion of Sync activity by passing Sync status of Users
                updateMySQLSyncSts(gson.toJson(finesSyncList));
                // Reload the Main Activity
                this.recreate();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // Method to inform remote MySQL DB about completion of Sync activity
    public void updateMySQLSyncSts(String json) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("syncsts", json);
        // Make Http call to updatesyncsts.php with JSON parameter which has Sync statuses of Users
        client.post(PRODUCTION_SERVER_ADDRESS + "updatesyncsts.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(getApplicationContext(), "MySQL DB has been informed about Sync activity", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getApplicationContext(), "Error Occured", Toast.LENGTH_LONG).show();
            }
        });
    }
}
