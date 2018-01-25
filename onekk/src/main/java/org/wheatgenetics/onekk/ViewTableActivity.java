package org.wheatgenetics.onekk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;

/**
 * This class is the activity class for View Data feature and is
 * initialized from the Navigation Drawer setup in the main menu of MainActivity
 * */
public class ViewTableActivity extends AppCompatActivity {

    private ViewTableContent viewTableContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TableLayout OneKKTable;
        Toolbar toolbar;

        setContentView(R.layout.activity_view_data);

        toolbar = (Toolbar) findViewById(R.id.view_data_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        OneKKTable = (TableLayout) findViewById(R.id.tlInventory);
        viewTableContent = new ViewTableContent(ViewTableActivity.this,OneKKTable);
        viewTableContent.getAllData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_data_table, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_export:
                viewTableContent.exportDialog();
                return true;
            case R.id.action_clear:
                viewTableContent.clearDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
