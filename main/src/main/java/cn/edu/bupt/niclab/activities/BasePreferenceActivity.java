package cn.edu.bupt.niclab.activities;

import android.preference.PreferenceActivity;

import com.umeng.analytics.MobclickAgent;

public class BasePreferenceActivity extends PreferenceActivity {
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
