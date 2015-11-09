package org.wheatgenetics.onekk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "onekkdb";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sample (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT, photo TEXT, person TEXT, date TEXT, seed_count TEXT, weight TEXT, avg_length TEXT, avg_width TEXT, avg_area TEXT)");
        db.execSQL("CREATE TABLE seed (id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id TEXT, length TEXT, width TEXT, circularity TEXT, area TEXT, color TEXT, weight TEXT )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS sample");
        db.execSQL("DROP TABLE IF EXISTS seed");

        // create fresh tables
        this.onCreate(db);
    }

    // Table names
    private static final String TABLE_SAMPLE = "sample";
    private static final String TABLE_SEED = "seed";

    // Sample table columns names
    private static final String SAMPLE_ID = "id";
    private static final String SAMPLE_SID = "sample_id";
    private static final String SAMPLE_POSITION = "position";
    private static final String SAMPLE_PHOTO = "photo";
    private static final String SAMPLE_PERSON = "person";
    private static final String SAMPLE_TIME = "date";
    private static final String SAMPLE_NUMSEEDS = "seed_count";
    private static final String SAMPLE_WT = "weight";
    private static final String SAMPLE_AVGLENGTH = "avg_length";
    private static final String SAMPLE_AVGWIDTH = "avg_width";
    private static final String SAMPLE_AVGAREA = "avg_area";

    // Sample table columns names
    private static final String SEED_ID = "id";
    private static final String SEED_SID = "sample_id";
    private static final String SEED_LEN = "length";
    private static final String SEED_WID = "width";
    private static final String SEED_CIRC = "circularity";
    private static final String SEED_AREA = "area";
    private static final String SEED_COL = "color";
    private static final String SEED_WT = "weight";

    public void addSampleRecord(SampleRecord sample) {
        Log.d("Add Sample: ", sample.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(SAMPLE_SID, sample.getSampleId());
        values.put(SAMPLE_PHOTO, sample.getPhoto());
        values.put(SAMPLE_PERSON, sample.getPersonId());
        values.put(SAMPLE_TIME, sample.getDate());
        values.put(SAMPLE_NUMSEEDS, sample.getSeedCount());
        values.put(SAMPLE_WT, sample.getWeight());
        values.put(SAMPLE_AVGAREA, sample.getAvgArea());
        values.put(SAMPLE_AVGLENGTH, sample.getAvgLength());
        values.put(SAMPLE_AVGWIDTH, sample.getAvgWidth());

        // 3. insert
        db.insert(TABLE_SAMPLE, null, values);

        // 4. close
        db.close();
    }

    public void addSeedRecord(SeedRecord seed) {
        // for logging
        Log.d("Add Seed: ", seed.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(SEED_SID, seed.getSampleId());
        values.put(SEED_LEN, seed.getLength());
        values.put(SEED_WID, seed.getWidth());
        values.put(SEED_CIRC, seed.getCircularity());
        values.put(SEED_AREA, seed.getArea());
        values.put(SEED_COL, seed.getColor());
        values.put(SEED_WT, seed.getWeight());

        // 3. insert
        db.insert(TABLE_SEED, null, values);

        // 4. close
        db.close();
    }

    // Average seeds in a sample
    public float averageSample(String sampleName, String trait) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(" + trait
                + ") FROM (SELECT seed." + trait
                + ", seed.sample_id FROM seed WHERE seed.sample_id = \""
                + sampleName + "\")", null);

        float traitValue = 0;

        if (cursor != null) {
            cursor.moveToFirst();
            traitValue = cursor.getFloat(0);

        }
        return traitValue;
    }

    // Export summary statistics
    public Cursor exportSummaryData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db
                .rawQuery(
                        "SELECT sample_id, photo, person, date, seed_count, weight, avg_length, avg_width, avg_area FROM sample",
                        null);
        return cursor;
    }

    // Export raw data
    public Cursor exportRawData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db
                .rawQuery(
                        "SELECT seed.sample_id, photo, person, date, length, width, circularity, seed.weight, area, color, sample.weight, avg_length, avg_width, avg_area, seed_count FROM seed, sample WHERE seed.sample_id = sample.sample_id",
                        null);
        return cursor;
    }

    public List<SampleRecord> getAllSamples() {
        List<SampleRecord> samples = new LinkedList<SampleRecord>();

        // 1. build the query
        String query = "SELECT * FROM " + TABLE_SAMPLE;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build sample and add it to list
        SampleRecord sample = null;
        if (cursor.moveToFirst()) {
            do {
                sample = new SampleRecord();

                sample.setSampleId(cursor.getString(1));
                sample.setPhoto(cursor.getString(2));
                sample.setPersonId(cursor.getString(3));
                sample.setDate(cursor.getString(4));
                sample.setWeight(cursor.getString(5));
                sample.setSeedCount(cursor.getString(6));
                sample.setAvgArea(Double.parseDouble(cursor.getString(7)));
                sample.setAvgLength(Double.parseDouble(cursor.getString(8)));
                sample.setAvgWidth(Double.parseDouble(cursor.getString(9)));
                samples.add(sample);
            } while (cursor.moveToNext());
        }
        Log.d("getAllSamples()", samples.toString());
        // return samples
        return samples;
    }

    public void deleteSample(String sample) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("Delete sample: ", sample);

        try {
            db.execSQL("DELETE FROM sample WHERE sample_id = \"" + sample + "\"");
            db.execSQL("DELETE FROM seed WHERE sample_id = \"" + sample + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SAMPLE, null, null);
        db.delete(TABLE_SEED, null, null);
        db.close();
    }
}
