package cn.edu.bupt.niclab.account;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.activities.BaseActivity;

public class LoginActivity extends BaseActivity implements PlatformActionListener{
    private String tag = "LoginActivity";
    ImageView loginQQ, loginSinaWeibo;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        loginQQ = (ImageView) findViewById(R.id.login_qq);
        loginSinaWeibo = (ImageView) findViewById(R.id.login_sina_weibo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onPageStart("LoginActivity"); //统计页面
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd("LoginActivity"); //统计页面
    }

    public void loginByQQ(View view){
        ShareSDK.initSDK(this);
        Platform qq = new QQ(this);
        this.authorize(qq);
    }

    public void loginBySinaWeibo(View view){
        ShareSDK.initSDK(this);
        Platform sinaWeibo = new SinaWeibo(this);
        this.authorize(sinaWeibo);
    }

    private void authorize(Platform platform){
        Log.d(tag,"UI thread="+Thread.currentThread().getName());
        platform.removeAccount();
        if (platform.isValid()){
            String userId = platform.getDb().getUserId();
            if (!TextUtils.isEmpty(userId)){
                Log.d(tag, "userId="+userId);
                //用户名
                String userName = platform.getDb().getUserName();
                //头像url
                String head = platform.getDb().getUserIcon();
                //user id
                userId = (platform instanceof SinaWeibo ? Account.getIDPrefix(Account.AccountType.SinaWeibo) : Account.getIDPrefix(Account.AccountType.QQ)) + platform.getDb().getUserId();
                debug("name="+userName);
                debug("icon="+head);
                debug("id="+userId);
                Account account = new Account();
                account.userName = userName;
                account.headUrl = head;
                account.userId = userId;
                account.type = platform instanceof SinaWeibo ? Account.AccountType.SinaWeibo : Account.AccountType.QQ;
                AccountManager.getManager().setAccount(account);
                AccountManager.getManager().saveAccount(LoginActivity.this);
                setResult(RESULT_OK);
                finish();
                return;
            }
        }
        platform.setPlatformActionListener(this);
        // true不使用SSO授权，false使用SSO授权
        platform.SSOSetting(false);
        platform.showUser(null);

    }

    @Override
    public void onComplete(Platform platform, int action, HashMap<String, Object> res) {
        debug(res.toString());
        //用户名
        String userName = platform.getDb().getUserName();
        //头像url
        String head = platform.getDb().getUserIcon();
        //user id
        String userId = (platform instanceof SinaWeibo ? Account.getIDPrefix(Account.AccountType.SinaWeibo) : Account.getIDPrefix(Account.AccountType.QQ)) + platform.getDb().getUserId();
        debug("name="+userName);
        debug("icon="+head);
        debug("id="+userId);
        Account account = new Account();
        account.userName = userName;
        account.headUrl = head;
        account.userId = userId;
        account.type = platform instanceof SinaWeibo ? Account.AccountType.SinaWeibo : Account.AccountType.QQ;
        AccountManager.getManager().setAccount(account);

        Log.d(tag,"thread="+Thread.currentThread().getName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "run in ui thread");
                Toast.makeText(LoginActivity.this,R.string.login_success_hint, Toast.LENGTH_SHORT).show();
                AccountManager.getManager().saveAccount(LoginActivity.this);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void onError(Platform platform, int action, Throwable throwable) {
        debug("onError");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, R.string.oauth_error_hint, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCancel(Platform platform, int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this,R.string.oauth_cancel_hint,Toast.LENGTH_SHORT).show();
            }
        });
        debug("onCancel");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /*private void debug(String msg){
        Log.d(tag, msg);
    }*/
}
