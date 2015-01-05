package cn.edu.bupt.niclab.activities;

import android.app.ListActivity;

import com.umeng.analytics.MobclickAgent;

public class BaseListActivity extends ListActivity {

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);//统计时长
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
