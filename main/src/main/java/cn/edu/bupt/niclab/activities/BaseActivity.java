package cn.edu.bupt.niclab.activities;

import android.app.Activity;

import com.umeng.analytics.MobclickAgent;


public class BaseActivity extends Activity {

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
