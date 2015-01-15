package cn.edu.bupt.niclab.fragments;


import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.nostra13.universalimageloader.core.ImageLoader;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.account.Base64Coder;
import cn.edu.bupt.niclab.account.RSA;
import cn.edu.bupt.niclab.entity.APPInfo;

/**
 * A simple {@link Fragment} subclass.
 */
public class APPFragment extends Fragment {
    private String tag = getClass().getName();
    protected void debug(String msg){Log.d(tag, msg);}

    public APPFragment() {
        // Required empty public constructor
    }
    
    private List<APPInfo> mAPPInfoList;
    private ListAdapter mListAdapter;
    private ListView mListView;
    private View mWaitView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        debug("APPFragment: onCreateView");
        mAPPInfoList = new ArrayList<APPInfo>();
        View view = inflater.inflate(R.layout.fragment_ap, container, false);
        mWaitView = view.findViewById(R.id.waiting_view);
        mWaitView.setVisibility(View.VISIBLE);
        mListView = (ListView) view.findViewById(R.id.app_list);
        mListAdapter = new ListAdapter();
        mListView.setAdapter(mListAdapter);
        //API:http://www.6able.com/app/list
        new MAsyncTask().execute(null);
        return view;
    }
    
    private class ListAdapter extends BaseAdapter{


        @Override
        public int getCount() {
            return mAPPInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAPPInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null){
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
            }
            APPInfo appInfo = (APPInfo) getItem(position);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setImageResource(R.drawable.icon_default);
            ImageLoader.getInstance().displayImage(appInfo.getIcon(), icon);
            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(appInfo.getName());
            TextView label = (TextView) view.findViewById(R.id.label);
            label.setText(String.format(getString(R.string.app_label), appInfo.getDownloadCount(), Utils.getReadableSize(appInfo.getSizeInKB())));
            Button installBtn = (Button) view.findViewById(R.id.install_btn);
            if (Utils.checkInstallAPP(getActivity(), appInfo.getPackageName())){
                installBtn.setText(R.string.launch);
                installBtn.setBackgroundResource(R.drawable.app_item_launch_btn_bg);
            }else{
                installBtn.setText(R.string.install);
                installBtn.setBackgroundResource(R.drawable.app_item_install_btn_bg);
            }
            installBtn.setTag(appInfo);
            installBtn.setOnClickListener(onClickListener);
            return view;
        }
        
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                if (btn.getTag() != null && btn.getTag() instanceof APPInfo){
                    APPInfo appInfo = (APPInfo) btn.getTag();
                    if (btn.getText().equals(getString(R.string.launch))){
                        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(appInfo.getPackageName());
                        startActivity( intent );
                    }
                }
            }
        };
    }
    
    private class MAsyncTask extends AsyncTask{
        private String tag = getClass().getName();

        @Override
        protected Object doInBackground(Object[] params) {
            //check cache
            String json = getJsonCache();
            if (TextUtils.isEmpty(json)){
                //load from server
                String url = MobclickAgent.getConfigParams(getActivity(), Constants.PARAM_KEY_APP_RESOURCE);
                Log.d(tag, "app url = " + url);
                if (TextUtils.isEmpty(url)){
                    url = Constants.URL_APP_RESOURCE;
                    Log.d(tag, "get app url failed, set url to " + url);
                }

                Request request = new Request.Builder().url(url).build();
                try {
                    Response response = new OkHttpClient().newCall(request).execute();
                    json = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //save json to cache
                saveToCache(json);
            }
            Log.d(tag, "result=" + json + ";+|");
            Gson gson = new Gson();
            Type strMap = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> map = gson.fromJson(json, strMap);
            //Log.d(tag, "map = " + map);
            List<Map<String, Object>> apps = (List<Map<String, Object>>) map.get("apps");
            List<APPInfo> appInfos = new ArrayList<APPInfo>();
            for (Map<String, Object> item : apps){
                APPInfo appInfo = new APPInfo();
                appInfo.setId(Integer.valueOf((String) item.get("id")));
                appInfo.setName((String) item.get("name"));
                appInfo.setIcon((String) item.get("icon"));
                appInfo.setDownloadUrl((String) item.get("download_url"));
                appInfo.setPackageName((String) item.get("package"));
                appInfo.setDescription((String) item.get("description"));
                String screen_shot_images = (String) item.get("screen_shot");
                appInfo.setScreenShots(screen_shot_images.split("|"));
                appInfo.setVersionName((String) item.get("version_name"));
                appInfo.setSizeInKB(Integer.valueOf((String) item.get("size")));
                appInfo.setDownloadCount(Integer.valueOf((String)item.get("download_count")));
                appInfos.add(appInfo);
            }
            return appInfos;
        }

        @Override
        protected void onPostExecute(Object o) {
            List<APPInfo> appInfos = (List<APPInfo>) o;
            mAPPInfoList.addAll(appInfos);
            mListAdapter.notifyDataSetChanged();
            mWaitView.setVisibility(View.GONE);
        }

        private String getJsonCache(){
            StringBuffer sb = new StringBuffer();
            String path = getActivity().getCacheDir().getAbsoluteFile() + File.separator + "app.cache";
            File cache = new File(path);
            if (!cache.exists()){
                return null;
            }else{
                long last = cache.lastModified();
                long delay = 1000 * 60;// * 60 * 1;//1 hour
                if ((System.currentTimeMillis() - last) > delay){
                    cache.delete();
                    return null;
                }
            }
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(cache));
                byte[] buf = new byte[1024];
                int count = 0;
                while ((count = bis.read(buf, 0, 1024)) > 0){
                    sb.append(new String(buf, 0, count));
                }
                bis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }


        private void saveToCache(String content){
            String path = getActivity().getCacheDir().getAbsoluteFile() + "/app.cache";
            Log.d(tag, "cache file path = " + path);
            File cache = new File(path);
            if (cache.exists()){
                cache.delete();
            }
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cache));
                bos.write(content.getBytes());
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    


}
