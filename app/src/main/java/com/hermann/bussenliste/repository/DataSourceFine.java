package com.hermann.bussenliste.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hermann.bussenliste.domain.Fine;
import com.hermann.bussenliste.repository.DatabaseHelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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

    public Fine getFine(long fine_id) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + DatabaseHelper.TABLE_FINES + " WHERE "
                + DatabaseHelper.COLUMN_ID + " = " + fine_id;

        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

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

    public int updateFine(Fine fine){
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        long id = fine.getId();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, fine.getDescription());
        values.put(DatabaseHelper.COLUMN_AMOUNT, fine.getAmount());
        values.put(DatabaseHelper.COLUMN_UPDATE_STATUS, "no");

        return database.update(DatabaseHelper.TABLE_FINES,
                values,
                DatabaseHelper.COLUMN_ID + "=" + id,
                null);
    }

    public long deleteFine(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_FINES, DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{Long.toString(id)});
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


    public boolean hasFine(String description) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String Query = "SELECT * FROM fines WHERE description =" + "'" + description + "'";
        Cursor cursor = database.rawQuery(Query, null);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
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
                map.put("fineDate", cursor.getString(3));
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

    public void updateSyncStatus(String description, String status) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        String updateQuery = "Update fines set updateStatus = '" + status + "' WHERE description=" + "'" + description + "'";
        database.execSQL(updateQuery);
    }
}


