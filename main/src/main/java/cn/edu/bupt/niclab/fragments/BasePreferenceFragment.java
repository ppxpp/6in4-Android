package cn.edu.bupt.niclab.fragments;


import android.app.Fragment;
import android.preference.PreferenceFragment;

import com.umeng.analytics.MobclickAgent;

/**
 * A simple {@link Fragment} subclass.
 */
public class BasePreferenceFragment extends PreferenceFragment {

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(getClass().getName()); //统计页面
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(getClass().getName());
    }

}
