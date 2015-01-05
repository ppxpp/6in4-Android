package cn.edu.bupt.niclab.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;
import cn.sharesdk.douban.Douban;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.renren.Renren;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.tencent.weibo.TencentWeibo;
import cn.sharesdk.wechat.friends.Wechat;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShareAppFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShareAppFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ShareAppFragment.
     */
    public static ShareAppFragment newInstance() {
        ShareAppFragment fragment = new ShareAppFragment();
        return fragment;
    }

    public ShareAppFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.share_app);
        findPreference("qq").setOnPreferenceClickListener(this);
        findPreference("qzone").setOnPreferenceClickListener(this);
        findPreference("tecent_weibo").setOnPreferenceClickListener(this);
        findPreference("sinaweibo").setOnPreferenceClickListener(this);
        findPreference("wechat").setOnPreferenceClickListener(this);
        findPreference("douban").setOnPreferenceClickListener(this);
        findPreference("renren").setOnPreferenceClickListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("qq")){
            share(QQ.NAME);
        }else if (preference.getKey().equals("qzone")){
            share(QZone.NAME);
        }else if (preference.getKey().equals("tecent_weibo")){
            share(TencentWeibo.NAME);
        }else if (preference.getKey().equals("sinaweibo")){
            share(SinaWeibo.NAME);
        }else if (preference.getKey().equals("wechat")){
            share(Wechat.NAME);
        }else if (preference.getKey().equals("douban")){
            share(Douban.NAME);
        }else if (preference.getKey().equals("renren")){
            share(Renren.NAME);
        }

        return false;
    }

    private void share(String platformName){
        if (TextUtils.isEmpty(platformName)){
            return;
        }
        ShareSDK.initSDK(getActivity());
        Platform platform = ShareSDK.getPlatform(platformName);
        Platform.ShareParams sp = generateSP();
        platform.setPlatformActionListener(mActionListener);
        platform.share(sp);
    }

    PlatformActionListener mActionListener = new PlatformActionListener() {
        @Override
        public void onComplete(Platform platform, int i, HashMap<String, Object> stringObjectHashMap) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),R.string.share_finish, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onError(Platform platform, int i, Throwable throwable) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),R.string.share_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onCancel(Platform platform, int i) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),R.string.share_canceled, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Platform.ShareParams generateSP(){
        Platform.ShareParams sp = new Platform.ShareParams();
        String shareTitle = getParamStr(Constants.PARAM_KEY_SHARE_TITLE, Constants.SHARE_TITLE_RESID);
        String shareTitleUrl = getParamStr(Constants.PARAM_KEY_SHARE_TITLE_URL, Constants.SHARE_TITLE_URL_RESID);
        String shareText = getParamStr(Constants.PARAM_KEY_SHARE_TEXT, Constants.SHARE_TEXT_RESID);
        String shareSite = getParamStr(Constants.PARAM_KEY_SHARE_SITE, Constants.SHARE_SITE_RESID);
        String shareUrl = getParamStr(Constants.PARAM_KEY_SHARE_URL, Constants.SHARE_URL_RESID);
        sp.setTitle(shareTitle);
        sp.setTitleUrl(shareTitleUrl);
        sp.setText(shareText);
        sp.setSite(shareSite);
        sp.setSiteUrl(shareUrl);
        return sp;
    }

    private String getParamStr(String key, int resid){
        String str = MobclickAgent.getConfigParams(getActivity(), key);
        if (TextUtils.isEmpty(str)){
            str = getString(resid);
        }
        return str;
    }
}
