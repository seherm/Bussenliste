package com.hermann.bussenliste;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Database Name
    private static final String DB_NAME = "bussenliste.db";

    //Database Version
    private static final int DB_VERSION = 1;

    //Table Names
    public static final String TABLE_PLAYERS = "players";
    public static final String TABLE_FINES = "fines";

    //Common column names
    public static final String COLUMN_ID = "_id";

    //PLAYERS Table - column names
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FINES = "fines";
    public static final String COLUMN_UPDATE_STATUS = "updateStatus";

    //FINES Table - column names
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_DATE = "date";

    //Table Creation Statements
    //PLAYERS table creation statement
    private static final String SQL_CREATE_TABLE_PLAYERS =
            "CREATE TABLE " + TABLE_PLAYERS +
                    "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " + COLUMN_FINES + " TEXT, " + COLUMN_UPDATE_STATUS + " TEXT);";

    //FINES table creation statement
    private static final String SQL_CREATE_TABLE_FINES =
            "CREATE TABLE " + TABLE_FINES +
                    "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL, " + COLUMN_AMOUNT + " INTEGER, " + COLUMN_DATE + " TEXT, " + COLUMN_UPDATE_STATUS + " TEXT);";


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            //creating required tables
            db.execSQL(SQL_CREATE_TABLE_PLAYERS);
            db.execSQL(SQL_CREATE_TABLE_FINES);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS players");
        db.execSQL("DROP TABLE IF EXISTS fines");

        //create new tables
        this.onCreate(db);
    }
}
