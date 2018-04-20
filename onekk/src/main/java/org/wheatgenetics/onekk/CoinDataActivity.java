package org.wheatgenetics.onekk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;

import org.wheatgenetics.database.Data;

import static org.wheatgenetics.database.Data.getAllData;

/**
 * This class is the activity class for Coin Data feature and is
 * initialized as an intent in the SettingsActivity preferences
 * */
public class CoinDataActivity extends AppCompatActivity {

    private TableLayout OneKKTable;
    private Data data;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar;

        setContentView(R.layout.coin_data_activity);

        toolbar = (Toolbar) findViewById(R.id.coin_data_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        OneKKTable = (TableLayout) findViewById(R.id.tlInventory);
        data = new Data(CoinDataActivity.this,OneKKTable);
        getAllData("coins");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_coin_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                data.coinDialog("",true);
                return true;

            case R.id.action_export:
                data.exportCoinsDialog();
                return true;

            case R.id.action_reset:
                data.resetDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}