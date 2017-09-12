package com.hermann.bussenliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sebas on 07.09.2017.
 */

public class DataSourcePlayer {

    private SQLiteDatabase database;
    private DbHelper dbHelper;


    private String[] columns = {
            DbHelper.COLUMN_ID,
            DbHelper.COLUMN_NAME,
            DbHelper.COLUMN_FINES,
            DbHelper.COLUMN_UPDATE_STATUS
    };

    public DataSourcePlayer(Context context) {
        dbHelper = new DbHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Player createPlayer(String name) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_NAME, name);
        values.put(DbHelper.COLUMN_UPDATE_STATUS, "no");

        long insertId = database.insert(DbHelper.TABLE_PLAYERS, null, values);

        Cursor cursor = database.query(DbHelper.TABLE_PLAYERS,
                columns, DbHelper.COLUMN_ID + "=" + insertId,
                null, null, null, null);

        cursor.moveToFirst();
        Player player = cursorToPlayer(cursor);
        cursor.close();

        return player;
    }

    public Player updatePlayer(long id, List<Fine> newFairs) throws JSONException {

        Gson gson = new Gson();
        String inputString= gson.toJson(newFairs);

        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_FINES, inputString);

        database.update(DbHelper.TABLE_PLAYERS,
                values,
                DbHelper.COLUMN_ID + "=" + id,
                null);

        Cursor cursor = database.query(DbHelper.TABLE_PLAYERS,
                columns, DbHelper.COLUMN_ID + "=" + id,
                null, null, null, null);

        cursor.moveToFirst();
        Player player = cursorToPlayer(cursor);
        cursor.close();

        return player;
    }


    private Player cursorToPlayer(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(DbHelper.COLUMN_ID);
        int idName = cursor.getColumnIndex(DbHelper.COLUMN_NAME);
        int idFines = cursor.getColumnIndex(DbHelper.COLUMN_FINES);
        int idUpdateStatus = cursor.getColumnIndex(DbHelper.COLUMN_UPDATE_STATUS);

        long id = cursor.getLong(idIndex);
        String name = cursor.getString(idName);
        String fines = cursor.getString(idFines);
        String updateStatus = cursor.getString(idUpdateStatus);

        Player player = new Player(id, name, updateStatus);

        Type type = new TypeToken<ArrayList<Fine>>() {}.getType();
        Gson gson = new Gson();
        ArrayList<Fine>  finesList = gson.fromJson(fines, type);
        if(finesList != null){
            player.setFines(finesList);
        }

        return player;
    }

    public List<Player> getAllPlayers() {
        List<Player> playersList = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.TABLE_PLAYERS,
                columns, null, null, null, null, null);

        cursor.moveToFirst();
        Player player;

        while(!cursor.isAfterLast()) {
            player = cursorToPlayer(cursor);
            playersList.add(player);
            cursor.moveToNext();
        }

        cursor.close();

        return playersList;
    }


    /**
     * Compose JSON out of SQLite records
     * @return
     */
    public String composeJSONfromSQLite(){
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM players where udpateStatus = '"+"no"+"'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("playerId", cursor.getString(0));
                map.put("playerName", cursor.getString(1));
                wordList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        Gson gson = new GsonBuilder().create();
        //Use GSON to serialize Array List to JSON
        return gson.toJson(wordList);
    }

    /**
     * Get Sync status of SQLite
     * @return
     */
    public String getSyncStatus(){
        String msg = null;
        if(this.dbSyncCount() == 0){
            msg = "SQLite and Remote MySQL DBs are in Sync!";
        }else{
            msg = "DB Sync neededn";
        }
        return msg;
    }

    /**
     * Get SQLite records that are yet to be Synced
     * @return
     */
    public int dbSyncCount(){
        int count = 0;
        String selectQuery = "SELECT  * FROM players where udpateStatus = '"+"no"+"'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Update Sync status against each User ID
     * @param id
     * @param status
     */
    public void updateSyncStatus(String id, String status){
        String updateQuery = "Update players set udpateStatus = '"+ status +"' where userId="+"'"+ id +"'";
        Log.d("query",updateQuery);
        database.execSQL(updateQuery);
        database.close();
    }
}
