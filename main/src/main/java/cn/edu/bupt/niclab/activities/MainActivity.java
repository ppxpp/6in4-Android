/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package cn.edu.bupt.niclab.activities;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.util.Log;

import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

import cn.edu.bupt.niclab.BuildConfig;
import cn.edu.bupt.niclab.fragments.APPFragment;
import cn.edu.bupt.niclab.fragments.GeneralSettings;
import cn.edu.bupt.niclab.fragments.ResourceFragment;

import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.fragments.ShareFragment;

public class MainActivity extends BaseActivity {

    private String tag = getClass().getName();
    private Tab mResTab, mSettingTab, mShareTab;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        Tab resourcetab = bar.newTab().setText(R.string.title_resource);
        Tab appTab = bar.newTab().setText(R.string.title_app);
        Tab generalTab = bar.newTab().setText(R.string.generalsettings);
        Tab shareTab = bar.newTab().setText(R.string.title_share);

        resourcetab.setTabListener(new TabListener<ResourceFragment>("resource", ResourceFragment.class));
        appTab.setTabListener(new TabListener<APPFragment>("app", APPFragment.class));
        generalTab.setTabListener(new TabListener<GeneralSettings>("settings", GeneralSettings.class));
        shareTab.setTabListener(new TabListener<ShareFragment>("share", ShareFragment.class));

        bar.addTab(resourcetab);
        bar.addTab(appTab);
		bar.addTab(generalTab);
        bar.addTab(shareTab);

        mResTab = resourcetab;
        mSettingTab = generalTab;
        mShareTab = shareTab;

        //发送策略定义了用户由统计分析SDK产生的数据发送回友盟服务器的频率。
        MobclickAgent.updateOnlineConfig(this);
        MobclickAgent.openActivityDurationTrack(false);
        if (BuildConfig.DEBUG){
            Log.d("MainActivity", "in debug");
            MobclickAgent.setDebugMode(true);
        }
        //update application
        UmengUpdateAgent.update(this);
        Log.d(tag, "onCreate, task_id = " + getTaskId());
	}

    public void switchToResPage(){
        getActionBar().selectTab(mResTab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(tag, "onNewIntent, task_id = " + getTaskId());
    }

    protected class TabListener<T extends Fragment> implements ActionBar.TabListener
	{
		private Fragment mFragment;
		private String mTag;
		private Class<T> mClass;

        public TabListener(String tag, Class<T> clz) {
            mTag = tag;
            mClass = clz;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }
      
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                Log.d("TabListener", "create fragment " + mClass.getName());
                mFragment = Fragment.instantiate(MainActivity.this, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
                //ft.show(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
                //ft.hide(mFragment);
            }
        }


		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}



    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }*/


}
