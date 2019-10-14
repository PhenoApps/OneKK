package org.wheatgenetics.onekk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;

import org.wheatgenetics.database.Data;

/**
 * This class is the activity class for View Data feature and is
 * initialized from the Navigation Drawer setup in the main menu of MainActivity
 */
public class ViewDataActivity extends AppCompatActivity {

    private Data data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TableLayout OneKKTable;
        Toolbar toolbar;

        setContentView(R.layout.view_data_activity);

        toolbar = findViewById(R.id.view_data_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        OneKKTable = findViewById(R.id.view_table);
        data = new Data(ViewDataActivity.this, OneKKTable);
        data.getAllData("sample");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_data_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_export:
                data.exportSeedSamplesDialog();
                return true;
            case R.id.action_clear:
                data.clearDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}