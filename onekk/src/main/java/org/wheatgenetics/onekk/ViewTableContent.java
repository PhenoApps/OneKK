package org.wheatgenetics.onekk;

/**
 * Created by sid on 1/23/18.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.wheatgenetics.database.MySQLiteHelper;
import org.wheatgenetics.database.SampleRecord;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.wheatgenetics.onekkUtils.oneKKUtils.getDate;
import static org.wheatgenetics.onekkUtils.oneKKUtils.makeFileDiscoverable;
import static org.wheatgenetics.onekkUtils.oneKKUtils.stringDecimal;

public class ViewTableContent {

    private static Context context;
    private static MySQLiteHelper db;
    private static int currentItemNum = 1;
    private static TableLayout OneKKTable;

    public ViewTableContent(Context context, TableLayout tableLayout) {
        this.context = context;
        db = new MySQLiteHelper(context);
        OneKKTable = tableLayout;
    }

    public ViewTableContent(Context context) {
        this.context = context;
        db = new MySQLiteHelper(context);
    }

    public static void getLastData() {
        OneKKTable.removeAllViews();
        List<SampleRecord> list = db.getLastSample();
        parseListToTable(list);
    }

    public static void getAllData() {
        OneKKTable.removeAllViews();
        List<SampleRecord> list = db.getAllSamples();
        parseListToTable(list);
    }

    // FIXME: 1/23/18
    public static void parseListToTable(List<SampleRecord> list) {

        int itemCount = list.size();
        if (itemCount != 0 && itemCount > 1) {
            for (int i = 0; i < itemCount; i++) {
                String[] temp = list.get(i).toString().split(",");

                /*Log.d(TAG, temp[0] + " " + temp[1] + " " + temp[2] + " "
                        + temp[3] + " " + temp[4] + " " + temp[5] + " "
                        + temp[6] + " " + temp[7]);
*/
                createNewTableEntry(temp[0], temp[5], stringDecimal(temp[7]), stringDecimal(temp[8]), stringDecimal(temp[6]));
            }
        } else {
            String[] temp = list.get(0).toString().split(",");
            createNewTableEntry(temp[0], temp[5]);
        }

    }

    public static void createNewTableEntry(String sample, String seedCount) {
        //inputText.setText("");

		/* Create a new row to be added. */
        TableRow tr = new TableRow(context);
        tr.setLayoutParams(new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		/* Create the sample name field */
        TextView sampleName = new TextView(context);
        sampleName.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        sampleName.setTextColor(Color.BLACK);
        sampleName.setTextSize(20.0f);
        sampleName.setText("Last Sample : " + sample);
        sampleName.setTag(sample);
        sampleName.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

		/* Create the number of seeds field */
        TextView numSeeds = new TextView(context);
        numSeeds.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        numSeeds.setTextColor(Color.BLACK);
        numSeeds.setTextSize(20.0f);
        numSeeds.setText("Count : " + seedCount);
        numSeeds.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

        tr.addView(sampleName);
        tr.addView(numSeeds);

        OneKKTable.addView(tr, 0, new ViewGroup.LayoutParams( // Adds row to top of table
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT));
    }

    // FIXME: 1/23/18

    /************************************************************************************
     * Adds a new entry to the end of the View Data
     ************************************************************************************/
    public static void createNewTableEntry(String sample, String seedCount, String avgL, String avgW, String wt) {

		/* Create a new row to be added. */
        TableRow tr = new TableRow(context);
        tr.setLayoutParams(new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tr.setPadding(0,30,0,30);

        /* Create the sample name field */
        TextView sampleName = new TextView(context);
        sampleName.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        sampleName.setTextColor(Color.BLACK);
        sampleName.setTextSize(20.0f);
        sampleName.setText(sample);
        sampleName.setTag(sample);
        sampleName.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

		/* Create the number of seeds field */
        TextView numSeeds = new TextView(context);
        numSeeds.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        numSeeds.setTextColor(Color.BLACK);
        numSeeds.setTextSize(20.0f);
        numSeeds.setText(seedCount);
        numSeeds.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the length field */
        TextView avgLength = new TextView(context);
        avgLength.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        avgLength.setTextColor(Color.BLACK);
        avgLength.setTextSize(20.0f);
        avgLength.setText(avgL);
        avgLength.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the width field */
        TextView avgWidth = new TextView(context);
        avgWidth.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        avgWidth.setTextColor(Color.BLACK);
        avgWidth.setTextSize(20.0f);
        avgWidth.setText(avgW);
        avgWidth.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

		/* Create the area field */
        TextView sampleWeight = new TextView(context);
        sampleWeight.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        sampleWeight.setTextColor(Color.BLACK);
        sampleWeight.setTextSize(20.0f);
        sampleWeight.setText(wt);
        sampleWeight.setLayoutParams(new TableRow.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0.12f));

		/* Define the listener for the longclick event */
        sampleName.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                final String tag = (String) v.getTag();
                ViewTableContent.deleteDialog(tag);
                return false;
            }
        });

		/* Add UI elements to row and add row to table */
        tr.addView(sampleName);
        tr.addView(numSeeds);
        tr.addView(avgLength);
        tr.addView(avgWidth);
        tr.addView(sampleWeight);

        OneKKTable.addView(tr, 0, new ViewGroup.LayoutParams( // Adds row to top of table
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT));
    }

    /************************************************************************************
     * Adds a new record to the internal list of records
     ************************************************************************************/
    // FIXME: 1/23/18

    /**
     * private void addRecord() {
     * <p>
     * <p>
     * // Add all measured seeds to database
     * for (int j = 0; j < seeds.size(); j++) {
     * //TODO add other parameters (weight, color)
     * db.addSeedRecord(new SeedRecord(inputText.getText().toString(), seeds.get(j).getLength(), seeds.get(j).getWidth(), seeds.get(j).getCirc(), seeds.get(j).getArea(), "", ""));
     * }
     * <p>
     * // Calculate averages
     * double lengthAvg = db.averageSample(inputText.getText().toString(), "length");
     * double lengthVar = Math.pow(db.sdSample(inputText.getText().toString(), "length"), 2);
     * double lengthCV = (db.sdSample(inputText.getText().toString(), "length")) / (db.averageSample(inputText.getText().toString(), "length"));
     * <p>
     * double widthAvg = db.averageSample(inputText.getText().toString(), "width");
     * double widthVar = Math.pow(db.sdSample(inputText.getText().toString(), "width"), 2);
     * double widthCV = (db.sdSample(inputText.getText().toString(), "width")) / db.averageSample(inputText.getText().toString(), "width");
     * <p>
     * double areaAvg = db.averageSample(inputText.getText().toString(), "area");
     * double areaVar = Math.pow(db.sdSample(inputText.getText().toString(), "area"), 2);
     * double areaCV = (db.sdSample(inputText.getText().toString(), "area")) / (db.averageSample(inputText.getText().toString(), "area"));
     * <p>
     * String seedCountString = String.valueOf(seedCount);
     * <p>
     * // Add sample to database
     * db.addSampleRecord(new SampleRecord(inputText.getText().toString(), photoName,
     * ep.getString(SettingsFragment.FIRST_NAME, "").toLowerCase() + "_" + ep.getString(SettingsFragment.LAST_NAME, "").toLowerCase(),
     * date, seedCountString, weight, lengthAvg, lengthVar, lengthCV, widthAvg,
     * widthVar, widthCV, areaAvg, areaVar, areaCV));
     * <p>
     * // Round values for UI
     * String avgLengthStr = String.format("%.2f", lengthAvg);
     * String avgWidthStr = String.format("%.2f", widthAvg);
     * <p>
     * createNewTableEntry(inputText.getText().toString(), seedCountString, avgLengthStr, avgWidthStr, weight);
     * currentItemNum++;
     * }
     */

    // FIXME: change the parameters to a Seed Record and a Sample Record
    public static void addSimpleRecord(String inputText, int seedCount, String weight) {

        String seedCountString = String.valueOf(seedCount);
        String date = getDate();

        // Add sample to database
        db.addSampleRecord(new SampleRecord(inputText, "photoName",
                "first_name" + "last_name",
                date, seedCountString, "", 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0));

        // Round values for UI
        String avgLengthStr = String.format("%.2f", 0.0);
        String avgWidthStr = String.format("%.2f", 0.0);

        createNewTableEntry(inputText, seedCountString);
        currentItemNum++;
    }

    // FIXME: 1/23/18
    public static void clearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.delete_database))
                .setCancelable(false)
                .setTitle("Clear Data")
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //makeToast(context.getResources().getString(R.string.data_deleted));
                                dropTables();
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // FIXME: 1/23/18
    public static void deleteDialog(String tag) {
        final String sampleName = tag;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.delete_entry));
        builder.setMessage(context.getResources().getString(R.string.delete_msg_1) + " \"" + sampleName + "\". " + context.getResources().getString(R.string.delete_msg_2))
                .setCancelable(true)
                .setPositiveButton(context.getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                db.deleteSample(sampleName);
                                getAllData();
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    // FIXME: 1/23/18
    public static void dropTables() {
        db.deleteAll();
        OneKKTable.removeAllViews();
        currentItemNum = 1;
    }

    // FIXME: 1/23/18
    public static void exportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getString(R.string.export_choice))
                .setCancelable(false)

                .setPositiveButton(context.getResources().getString(R.string.export_raw),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportRawData();
                                try {
                                    exportDatabase(exportCursor, "RawData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNeutralButton(context.getResources().getString(R.string.export_all),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportSummaryData();
                                try {
                                    exportDatabase(exportCursor, "RawData");
                                    exportDatabase(exportCursor, "SummaryData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNegativeButton(context.getResources().getString(R.string.export_summaries),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Cursor exportCursor = db.exportSummaryData();
                                try {
                                    exportDatabase(exportCursor, "SummaryData");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setTitle(context.getResources().getString(R.string.export_data))
                .setNeutralButton(context.getResources().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // FIXME: 1/23/18
    public static void exportDatabase(Cursor cursorForExport, String type) throws Exception {
        File file = null;

        try {
            file = new File(Constants.EXPORT_PATH, "export_" + type + "_" + getDate() + ".csv");
        } catch (Exception e) {
            Log.e("ViewTableActivity", e.getMessage());
        }

        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            csvWrite.writeNext(cursorForExport.getColumnNames());

            while (cursorForExport.moveToNext()) {
                String arrStr[] = new String[cursorForExport.getColumnCount()];

                for (int k = 0; k < cursorForExport.getColumnCount(); k++) {
                    arrStr[k] = cursorForExport.getString(k);
                }

                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            cursorForExport.close();
        } catch (Exception sqlEx) {
            Log.e("ViewTableActivity", sqlEx.getMessage(), sqlEx);
        }

        makeFileDiscoverable(file, context);
        shareFile(file.toString());
    }

    // FIXME: 1/23/18
    public static void shareFile(String filePath) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
        //startActivity(Intent.createChooser(intent, "Sending File..."));
    }

}
