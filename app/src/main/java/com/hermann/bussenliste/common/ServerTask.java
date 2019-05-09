package com.hermann.bussenliste.common;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hermann.bussenliste.domain.Fine;
import com.hermann.bussenliste.domain.Player;
import com.hermann.bussenliste.repository.DataSourceFine;
import com.hermann.bussenliste.repository.DataSourcePlayer;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class ServerTask {

    private static final String BASE_URL = "https://bussenliste.000webhostapp.com/";

    private AsyncHttpClient asyncHttpClient;
    private RequestParams requestParams;
    private Context context;
    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;
    private OnServerTaskListener listener;

    public ServerTask(Context context, OnServerTaskListener listener) {
        this.asyncHttpClient = new AsyncHttpClient();
        this.requestParams = new RequestParams();
        this.context = context;
        this.listener = listener;
        this.dataSourceFine = new DataSourceFine(context);
        this.dataSourcePlayer = new DataSourcePlayer(context);
    }

    //Download table entries from online MySQL DB and load it into local SQLite DB
    public void getData() {
        // Make Http call to getdata.php
        asyncHttpClient.post(BASE_URL + "getdata.php", requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                updateSQLiteData(new String(responseBody));
            }

            //When error occurred
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                listener.downloadTaskFailed(statusCode);
            }
        });
    }


    //Upload table entries from SQLite DB and load it into online MySQL DB
    public void putData() {

        if (dataSourcePlayer.dbSyncCount() != 0 && !dataSourcePlayer.getAllPlayers().isEmpty()) {
            requestParams.put("playersJSON", dataSourcePlayer.composeJSONfromSQLite());
        }
        if (dataSourceFine.dbSyncCount() != 0 && !dataSourceFine.getAllFines().isEmpty()) {
            requestParams.put("finesJSON", dataSourceFine.composeJSONfromSQLite());
        }

        // Make Http call to insertdata.php
        if (requestParams.has("playersJSON") || requestParams.has("finesJSON")) {
            asyncHttpClient.post(BASE_URL + "insertdata.php", requestParams, new TextHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseBody) {
                    updateSyncStatus(responseBody);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable error) {
                    listener.uploadTaskFailed(statusCode);
                }
            });
        }
    }

    public void deletePlayer(final Player player) {
        Gson gson = new GsonBuilder().create();
        requestParams.put("playersJSON", gson.toJson(player.getName()));
        asyncHttpClient.post(BASE_URL + "deleteplayer.php", requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (new String(responseBody).equals("true")) {
                    listener.deletePlayerTaskCompleted(player);
                } else {
                    listener.deletePlayerTaskFailed(statusCode);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                listener.deletePlayerTaskFailed(statusCode);
            }
        });
    }


    public void deleteFine(final Fine fine) {
        Gson gson = new GsonBuilder().create();
        requestParams.put("finesJSON", gson.toJson(fine.getDescription()));
        asyncHttpClient.post(BASE_URL + "deletefine.php", requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (new String(responseBody).equals("true")) {
                    listener.deleteFineTaskCompleted(fine);
                } else {
                    listener.deleteFineTaskFailed(statusCode);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                listener.deleteFineTaskFailed(statusCode);
            }
        });
    }


    //Insert result from http call into SQLite DB
    private void updateSQLiteData(String response) {
        try {
            JSONArray array = new JSONArray(response);
            if (array.length() != 0) {
                // Loop through each array element
                for (int i = 0; i < array.length(); i++) {
                    // Get JSON object
                    JSONObject obj = (JSONObject) array.get(i);
                    String tableName = obj.get("table").toString();

                    switch (tableName) {
                        case "players":
                            // Insert player into local SQLite DB
                            String playerId = obj.get("playerId").toString();
                            String playerName = obj.get("playerName").toString();
                            String playerFines = obj.get("playerFines").toString().trim();
                            byte[] playerPhoto = Base64.decode(obj.get("playerPhoto").toString(), Base64.DEFAULT);

                            if (!dataSourcePlayer.hasPlayer(playerName)) {
                                Player player = new Player(playerName);
                                if (playerFines != null) {
                                    player.setFines(DataSourcePlayer.getFinesList(playerFines));
                                }

                                player.setPhoto(DataSourcePlayer.getImage(playerPhoto));

                                dataSourcePlayer.createPlayer(player);
                            } else {
                                Player currentPlayer = dataSourcePlayer.getPlayer(playerName);
                                if (playerFines != null) {
                                    currentPlayer.setFines(DataSourcePlayer.getFinesList(playerFines));
                                }
                                currentPlayer.setPhoto(DataSourcePlayer.getImage(playerPhoto));
                                dataSourcePlayer.updatePlayer(currentPlayer);
                            }
                            break;
                        case "fines":
                            // Insert fine into local SQLite DB
                            String fineId = obj.get("fineId").toString();
                            String fineDescription = obj.get("fineDescription").toString();
                            String fineAmount = obj.get("fineAmount").toString();

                            if (!dataSourceFine.hasFine(fineDescription)) {
                                dataSourceFine.createFine(fineDescription, Integer.parseInt(fineAmount));
                            }
                            break;
                    }
                }
            }
            listener.downloadTaskCompleted(response);
        } catch (JSONException e) {
            listener.updateSQLiteDataFailed(e);
        }
    }

    //Set update status of uploaded data to "no" in SQLiteDB
    private void updateSyncStatus(String responseBody) {
        try {
            JSONArray arr = new JSONArray(responseBody);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = (JSONObject) arr.get(i);
                if (obj.get("table").toString().equals("players")) {
                    dataSourcePlayer.updateSyncStatus(obj.get("name").toString(), obj.get("status").toString());
                } else if (obj.get("table").toString().equals("fines")) {
                    dataSourceFine.updateSyncStatus(obj.get("description").toString(), obj.get("status").toString());
                }
            }
            listener.uploadTaskCompleted(responseBody);
        } catch (JSONException e) {
            listener.updateSyncStatusFailed(e);
        }
    }
}
