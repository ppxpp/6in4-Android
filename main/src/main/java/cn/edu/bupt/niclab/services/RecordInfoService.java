package cn.edu.bupt.niclab.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.umeng.analytics.MobclickAgent;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.account.Account;
import cn.edu.bupt.niclab.account.AccountManager;
import cn.edu.bupt.niclab.account.Base64Coder;
import cn.edu.bupt.niclab.account.RSA;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class RecordInfoService extends IntentService {

    private String tag = getClass().getName();

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_SEND_LOG = "cn.edu.bupt.niclab.services.action.SENFD_LOG";
    private static final String ACTION_RECORD_INFO = "cn.edu.bupt.niclab.services.action.RECORD_INFO";

    public static final String ParamLogInfo = "LOG_INFO";

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRecordInfo(Context context) {
        Intent intent = new Intent(context, RecordInfoService.class);
        intent.setAction(ACTION_RECORD_INFO);
        context.startService(intent);
    }

    public static void startActionSendLog(Context context, String log) {
        Intent intent = new Intent(context, RecordInfoService.class);
        intent.setAction(ACTION_SEND_LOG);
        intent.putExtra(ParamLogInfo, log);
        context.startService(intent);
    }


    public RecordInfoService() {
        super("RecordInfoService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_RECORD_INFO.equals(action)) {
                handleActionRecordInfo();
            }else if (ACTION_SEND_LOG.equals(action)){
                String log = intent.getStringExtra(ParamLogInfo);
                handleActionSendLog(log);
            }
        }
    }

    private void handleActionRecordInfo(){
        Map<String, String> map = new HashMap<String, String>();
        String ip_a = executeCMD("ip a");
        map.put("ip_a", ip_a);
        //Log.d(tag, "ip_a = " + ip_a);

        String ip_6_rule = executeCMD("ip -6 rule");
        map.put("ip_6_rule", ip_6_rule);
        //Log.d(tag, "ip_6_rule = "+ ip_6_rule);

        String ip_6_route = executeCMD("ip -6 route");
        map.put("ip_6_route", ip_6_route);
        //Log.d(tag, "ip_6_route = " + ip_6_route);

        Log.d(tag, "model="+Build.MODEL);//Nexus5
        map.put("model", Build.MODEL);

        Log.d(tag, "manufacturer = " + Build.MANUFACTURER);//LGE
        map.put("manufacturer", Build.MANUFACTURER);

        Log.d(tag, "RELEASE = " + Build.VERSION.RELEASE);//4.4.4
        map.put("release", Build.VERSION.RELEASE);

        Account account = AccountManager.getManager().getCurtAccount();
        if (account != null){
            map.put("userId", account.userId);
            map.put("userName", account.userName);
        }

        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        String json = gson.toJson(map);
        //Log.d(tag, "json = " + json);
        String url = MobclickAgent.getConfigParams(RecordInfoService.this, Constants.PARAM_KEY_RECORD_DEVICE);
        //Log.d(tag, "url = " + url);
        if (TextUtils.isEmpty(url)){
            url = Constants.URL_RECORD_DEVICE;
        }
        postToServer(json, url);
    }

    private void handleActionSendLog(String log){
        Map<String, String> map = new HashMap<String, String>();

        Log.d(tag, "model="+Build.MODEL);//Nexus5
        map.put("model", Build.MODEL);

        Log.d(tag, "manufacturer = " + Build.MANUFACTURER);//LGE
        map.put("manufacturer", Build.MANUFACTURER);

        Log.d(tag, "RELEASE = " + Build.VERSION.RELEASE);//4.4.4
        map.put("release", Build.VERSION.RELEASE);

        Account account = AccountManager.getManager().getCurtAccount();
        if (account != null){
            map.put("userId", account.userId);
            map.put("userName", account.userName);
        }

        map.put("log", log);

        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        String json = gson.toJson(map);
        //Log.d(tag, "json = " + json);
        String url = MobclickAgent.getConfigParams(RecordInfoService.this, Constants.PARAM_KEY_RECORD_CONNECT_LOG);
        //Log.d(tag, "url = " + url);
        if (TextUtils.isEmpty(url)){
            url = Constants.URL_RECORD_CONNECT_LOG;
        }
        postToServer(json, url);
    }

    private String executeCMD(String cmd){
        Process process;
        StringBuilder sb = new StringBuilder();
        String rst;
        try {
            process = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((rst = br.readLine()) != null){
                sb.append(rst);
                sb.append("\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void postToServer(String json, String url){
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(json)){
            return;
        }
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair("data", json));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(param, HTTP.UTF_8));
            HttpResponse response = new DefaultHttpClient().execute(httpPost);
            Log.d(tag, "result = " + EntityUtils.toString(response.getEntity()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
