package cn.edu.bupt.niclab.fragments;


import android.app.Fragment;
import android.util.Log;

import com.umeng.analytics.MobclickAgent;


/**
 * A simple {@link Fragment} subclass.
 */
public class BaseFragment extends Fragment {
    protected String tag = getClass().getName();
    @Override
    public void onResume() {
        super.onResume();
        Log.d(tag, "onResume");
        MobclickAgent.onPageStart(getClass().getName()); //统计页面
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(tag, "onPause");
        MobclickAgent.onPageEnd(getClass().getName());
    }

    @Override
    public void onDetach() {
        Log.d(tag, "onDetach");
        super.onDetach();
    }
}
