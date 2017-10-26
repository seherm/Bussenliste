package com.hermann.bussenliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sebas on 26.10.2017.
 */


public class DataSourceFine {

    private final DatabaseHelper dbHelper;

    private final String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_DESCRIPTION,
            DatabaseHelper.COLUMN_AMOUNT,
            DatabaseHelper.COLUMN_DATE,
            DatabaseHelper.COLUMN_UPDATE_STATUS
    };

    public DataSourceFine(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long createFine(String description, int amount) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, description);
        values.put(DatabaseHelper.COLUMN_AMOUNT, amount);
        values.put(DatabaseHelper.COLUMN_DATE, DateFormat.getDateInstance().format(new Date()));
        values.put(DatabaseHelper.COLUMN_UPDATE_STATUS, "no");

        long insertId = database.insert(DatabaseHelper.TABLE_FINES, null, values);

        return insertId;
    }

    private Fine getFine(long fine_id) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + DatabaseHelper.TABLE_FINES + " WHERE "
                + DatabaseHelper.COLUMN_ID + " = " + fine_id;

        Cursor cursor = database.rawQuery(selectQuery, null);

        int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
        int idDescription = cursor.getColumnIndex(DatabaseHelper.COLUMN_DESCRIPTION);
        int idAmount = cursor.getColumnIndex(DatabaseHelper.COLUMN_AMOUNT);
        int idDate = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);

        long id = cursor.getLong(idIndex);
        String description = cursor.getString(idDescription);
        int amount = cursor.getInt(idAmount);
        String date = cursor.getString(idDate);

        return new Fine(id, description, amount, date);
    }

    public List<Fine> getAllFines() {
        List<Fine> finesList = new ArrayList<>();

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = database.query(DatabaseHelper.TABLE_FINES,
                columns, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                int idDescription = cursor.getColumnIndex(DatabaseHelper.COLUMN_DESCRIPTION);
                int idAmount = cursor.getColumnIndex(DatabaseHelper.COLUMN_AMOUNT);
                int idDate = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);

                long id = cursor.getLong(idIndex);
                String description = cursor.getString(idDescription);
                int amount = cursor.getInt(idAmount);
                String date = cursor.getString(idDate);

                finesList.add(new Fine(id, description, amount, date));
            } while (cursor.moveToNext());
        }
        return finesList;
    }


    public String composeJSONfromSQLite() {
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<>();
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT  * FROM fines WHERE updateStatus = '" + "no" + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("fineId", cursor.getString(0));
                map.put("fineDescription", cursor.getString(1));
                map.put("fineAmount", cursor.getString(2));
                map.put("fineDate", cursor.getString(2));
                wordList.add(map);

            } while (cursor.moveToNext());
        }
        Gson gson = new GsonBuilder().create();
        return gson.toJson(wordList);
    }

    public int dbSyncCount() {
        int count;
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT  * FROM fines where updateStatus = '" + "no" + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        return count;
    }

    public void updateSyncStatus(String id, String status) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        String updateQuery = "Update fines set updateStatus = '" + status + "' where _id=" + "'" + id + "'";
        database.execSQL(updateQuery);
    }
}


