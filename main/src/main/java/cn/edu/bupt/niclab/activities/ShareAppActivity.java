package cn.edu.bupt.niclab.activities;

import android.os.Bundle;
import android.view.MenuItem;

import cn.edu.bupt.niclab.fragments.ShareAppFragment;

/**
 * Created by ppxpp on 2014/11/1.
 */
public class ShareAppActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().add(android.R.id.content, ShareAppFragment.newInstance()).commit();
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
