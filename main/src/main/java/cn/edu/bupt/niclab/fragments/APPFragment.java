package cn.edu.bupt.niclab.fragments;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.umeng.analytics.MobclickAgent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.activities.APPDetailActivity;
import cn.edu.bupt.niclab.app.APPDataProvider;
import cn.edu.bupt.niclab.entity.APPInfo;
import cn.edu.bupt.niclab.event.APPListEvent;
import cn.edu.bupt.niclab.event.BaseEvent;
import cn.edu.bupt.niclab.widget.APPInfoListAdapter;
import de.greenrobot.event.EventBus;

/**
 * A simple {@link Fragment} subclass.
 */
public class APPFragment extends Fragment {
    private String tag = getClass().getName();
    protected void debug(String msg){Log.d(tag, msg);}


    public APPFragment() {
        // Required empty public constructor
    }
    
    private APPInfoListAdapter mListAdapter;
    private ListView mListView;
    private View mWaitView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ap, container, false);
        mWaitView = view.findViewById(R.id.waiting_view);
        mWaitView.setVisibility(View.VISIBLE);
        mListView = (ListView) view.findViewById(R.id.app_list);
        List<APPInfo> data = APPDataProvider.getInstance().getAPPInfoList();
        mListAdapter = new APPInfoListAdapter(getActivity(), data);
        mListView.setOnItemClickListener(mOnItemClickListener);
        mListView.setAdapter(mListAdapter);
        if (data == null || data.size() == 0){
            APPDataProvider.getInstance().loadFromServer();
        }else{
            mWaitView.setVisibility(View.GONE);
        }
        mListAdapter.onActivityCreate();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        if (mListAdapter != null) {
            mListAdapter.onActivityDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEventMainThread(APPListEvent event){
        debug("onEventMainThread");
        if (event.getResult() == BaseEvent.EVENT_SUCCESS){
            mListAdapter.setData(APPDataProvider.getInstance().getAPPInfoList());
            mListAdapter.notifyDataSetChanged();
            mWaitView.setVisibility(View.GONE);
        }
        
    }

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            APPInfo info = (APPInfo) mListAdapter.getItem(position);
            Intent intent = new Intent(getActivity(), APPDetailActivity.class);
            intent.putExtra("app_id", info.getId());
            startActivity(intent);
        }
    };
}
