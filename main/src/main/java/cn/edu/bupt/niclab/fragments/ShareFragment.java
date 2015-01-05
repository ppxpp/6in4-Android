package cn.edu.bupt.niclab.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;

public class ShareFragment extends Fragment {


    public static ShareFragment newInstance(String param1, String param2) {
        ShareFragment fragment = new ShareFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    private GridView mGridView;
    private List<ShareItem> mItems;
    //private List<Map<String, Object>> mItems;
    //private SimpleAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        mGridView = (GridView) view.findViewById(R.id.grid);
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        queryAPP();
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(mOnItemClickListener);
        return view;
    }

    /**
     * 检索系统已安装的可以用于分享的APP
     */
    private void queryAPP(){

        if (mItems != null){
            mItems.clear();
            mItems = null;
        }
        mItems = new ArrayList<ShareItem>();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.setType("text/plain");
        List<ResolveInfo> infoList = getActivity().getPackageManager().queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (infoList != null){
            for (int i = 0; i < infoList.size(); i++) {
                ResolveInfo info = infoList.get(i);
                ShareItem item = new ShareItem();
                item.name = info.loadLabel(getActivity().getPackageManager()).toString();
                item.icon = info.loadIcon(getActivity().getPackageManager());
                item.packageInfo = info.activityInfo.packageName;
                item.className = info.activityInfo.name;
                mItems.add(item);
            }
        }
        mAdapter.notifyDataSetChanged();;
    }

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d("OnItemClickListener", "OnItemClickListener");
            String content = MobclickAgent.getConfigParams(getActivity(), Constants.PARAM_KEY_SHARE_TEXT);
            if (TextUtils.isEmpty(content)){
                content = getString(Constants.SHARE_TEXT_RESID);
            }
            ShareItem item = mItems.get(position);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, content);
            intent.setType("text/plain");
            intent.setPackage(item.packageInfo);
            intent.setClassName(item.packageInfo, item.className);
            startActivity(intent);
        }
    };

    private class ShareItem{
        public String name;
        public Drawable icon;
        public String packageInfo;
        public String className;
    }

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            if (mItems == null)
                return 0;
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            if (mItems == null)
                return null;
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_share_item,parent, false);
            }
            ShareItem item = (ShareItem) getItem(position);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            imageView.setImageDrawable(item.icon);
            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(item.name);

            return view;
        }
    };



}
