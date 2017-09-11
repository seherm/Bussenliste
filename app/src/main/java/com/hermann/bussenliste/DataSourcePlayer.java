package com.hermann.bussenliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
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
            DbHelper.COLUMN_FINES
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

        String name = cursor.getString(idName);
        long id = cursor.getLong(idIndex);
        String fines = cursor.getString(idFines);

        Player player = new Player(id, name);

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
}
