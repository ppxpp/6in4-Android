package cn.edu.bupt.niclab.app;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.entity.APPInfo;
import cn.edu.bupt.niclab.event.APPListEvent;
import cn.edu.bupt.niclab.event.BaseEvent;
import de.greenrobot.common.ObjectCache;
import de.greenrobot.common.io.FileUtils;
import de.greenrobot.event.EventBus;

/**
 * Created by zhengmeng on 2015/1/19.
 */
public class APPDataProvider {
    private String tag = getClass().getName();

    protected void debug(String msg) {
        Log.d(tag, msg);
    }
    //提供APP列表数据

    /**
     * *********************
     */
    private final String CacheFilePath;
    private static APPDataProvider instance;

    public static void init(Context context) {
        if (instance == null) {
            instance = new APPDataProvider(context);
        }
    }

    public static APPDataProvider getInstance() {
        return instance;
    }

    /**
     * *********************
     */

    private Context mContext;
    private List<APPInfo> mAPPInfoList;

    private APPDataProvider(Context context) {
        mContext = context;
        CacheFilePath = mContext.getCacheDir().getAbsolutePath() + File.separator + "app_list.cache";
        if ((mAPPInfoList = loadFromCache()) == null)
            mAPPInfoList = new Vector<APPInfo>();
    }

    private List<APPInfo> loadFromCache() {
        try {
            File cache = new File(CacheFilePath);
            //3 days
            if ((System.currentTimeMillis() - cache.lastModified()) > 1000 * 60 * 60 * 24 * 3){
                cache.delete();
                return null;
            }
            Object out = FileUtils.readObject(cache);
            if (out != null) {
                List<APPInfo> tmp = (List<APPInfo>) out;
                return tmp;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeToCache() {
        try {
            FileUtils.writeObject(new File(CacheFilePath), mAPPInfoList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFromServer() {
        //load from server
        String url = MobclickAgent.getConfigParams(mContext, Constants.PARAM_KEY_APP_RESOURCE);
        Log.d(tag, "app url = " + url);
        if (TextUtils.isEmpty(url)) {
            url = Constants.URL_APP_RESOURCE;
            Log.d(tag, "get app url failed, set url to " + url);
        }

        Request request = new Request.Builder().url(url).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                EventBus.getDefault().post(new APPListEvent(BaseEvent.EVENT_FAILED));
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String json = response.body().string();
                Gson gson = new Gson();
                Type strMap = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> map = gson.fromJson(json, strMap);
                List<Map<String, Object>> apps = (List<Map<String, Object>>) map.get("apps");
                List<APPInfo> appInfos = new Vector<APPInfo>();
                for (Map<String, Object> item : apps) {
                    APPInfo appInfo = new APPInfo();
                    appInfo.setId(Integer.valueOf((String) item.get("id")));
                    appInfo.setName((String) item.get("name"));
                    appInfo.setIcon((String) item.get("icon"));
                    appInfo.setDownloadUrl((String) item.get("download_url"));
                    appInfo.setPackageName((String) item.get("package"));
                    appInfo.setDescription((String) item.get("description"));
                    String screen_shot_images = ((String) item.get("screen_shot")).trim();
                    debug("screen_shot_images = " + screen_shot_images);
                    appInfo.setScreenShots(screen_shot_images.split("\\|"));
                    appInfo.setVersionName((String) item.get("version_name"));
                    appInfo.setSizeInKB(Integer.valueOf((String) item.get("size")));
                    appInfo.setDownloadCount(Integer.valueOf((String) item.get("download_count")));
                    appInfos.add(appInfo);
                }
                mAPPInfoList = appInfos;
                //write to cache
                writeToCache();
                EventBus.getDefault().post(new APPListEvent(BaseEvent.EVENT_SUCCESS));
            }
        });
    }

    public APPInfo getAPPInfoByID(int id){
        for(APPInfo info: mAPPInfoList){
            if (info.getId() == id){
                return info;
            }
        }
        return null;
    }
    
    public List<APPInfo> getAPPInfoList(){
        return mAPPInfoList;
    }
}
