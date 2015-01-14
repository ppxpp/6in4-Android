package cn.edu.bupt.niclab.activities;

import android.app.Activity;
import android.util.Log;

import com.umeng.analytics.MobclickAgent;


public class BaseActivity extends Activity {

    private String tag = getClass().getName();
    protected void debug(String msg){Log.d(tag, msg);};
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
