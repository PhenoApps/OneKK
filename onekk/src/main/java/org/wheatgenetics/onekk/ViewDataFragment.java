package org.wheatgenetics.onekk;

/**
 * Created by sid on 1/25/18.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import org.wheatgenetics.database.MySQLiteHelper;

import java.io.InputStream;

/**
 * A placeholder fragment containing a simple view.
 */
public class ViewDataFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ViewTableContent viewTableContent;
    private View rootView;
    public ViewDataFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ViewDataFragment newInstance(int sectionNumber) {
        ViewDataFragment fragment = new ViewDataFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        switch (getArguments().getInt(ARG_SECTION_NUMBER))
        {
            case 1: {
                TableLayout SamplesTable;
                rootView = inflater.inflate(R.layout.view_data_fragment, container, false);
                SamplesTable = (TableLayout) rootView.findViewById(R.id.tlInventory);
                viewTableContent = new ViewTableContent(getContext(),SamplesTable);
                viewTableContent.getAllData("sample");

                break;
            }
            case 2: {
                TableLayout CoinsTable;
                rootView = inflater.inflate(R.layout.coin_table, container, false);
                CoinsTable = (TableLayout) rootView.findViewById(R.id.tlInventory);
                viewTableContent = new ViewTableContent(getContext(),CoinsTable);
                viewTableContent.getAllData("coins");
                setHasOptionsMenu(true);
                break;
            }
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        menu.clear();
        inflater.inflate(R.menu.menu_coin_table,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_add:
                //do something
                return true;

            case R.id.action_export:
                //do something
                return true;

            case R.id.action_reset:
                MySQLiteHelper mySQLiteHelper = new MySQLiteHelper(getContext());
                InputStream inputStream = null;
                try {
                  inputStream   = getContext().getAssets().open("coin_database.csv");
                }
                catch(Exception ex) {
                    Log.e("Coin DB file error : ", ex.getMessage());
                }
                mySQLiteHelper.importCoinData(inputStream);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return ViewDataFragment.newInstance(position + 1);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 2;
    }
}
