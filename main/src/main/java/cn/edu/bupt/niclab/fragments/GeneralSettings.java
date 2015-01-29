/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package cn.edu.bupt.niclab.fragments;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;
import com.umeng.update.UpdateStatus;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import cn.edu.bupt.niclab.BuildConfig;
import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.LaunchVPN;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.VpnProfile;
import cn.edu.bupt.niclab.account.Account;
import cn.edu.bupt.niclab.account.AccountManager;
import cn.edu.bupt.niclab.account.Base64Coder;
import cn.edu.bupt.niclab.account.LoginActivity;
import cn.edu.bupt.niclab.account.RSA;
import cn.edu.bupt.niclab.activities.DisconnectVPN;
import cn.edu.bupt.niclab.activities.FeedbackActivity;
import cn.edu.bupt.niclab.activities.LogWindow;
import cn.edu.bupt.niclab.activities.MainActivity;
import cn.edu.bupt.niclab.activities.ShareAppActivity;
import cn.edu.bupt.niclab.services.RecordInfoService;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import de.blinkt.openvpn.api.ExternalAppDatabase;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

public class GeneralSettings extends BasePreferenceFragment implements OnPreferenceClickListener, OnClickListener {

	private ExternalAppDatabase mExtapp;
    private String tag = "GeneralSettings";
    private final int REQUEST_CODE_LOGIN = 14;
    private final int REQUEST_CODE_LAUNCH_VPN = 15;
    private ProgressDialog mPD;
    private PreferenceScreen mPScreen;
    //private PreferenceCategory mVPNPFC;
    private Preference mLogoutPF;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.general_settings);
        
        mPScreen = (PreferenceScreen) findPreference("screen");
        
        /*PreferenceCategory devHacks = (PreferenceCategory) findPreference("device_hacks");
        Preference loadtun = findPreference("loadTunModule");
		if(!isTunModuleAvailable()) {
			loadtun.setEnabled(false);
            devHacks.removePreference(loadtun);
        }

        CheckBoxPreference cm9hack = (CheckBoxPreference) findPreference("useCM9Fix");
        if (!cm9hack.isChecked() && (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            devHacks.removePreference(cm9hack);
        }
        
        if(devHacks.getPreferenceCount()==0)
            getPreferenceScreen().removePreference(devHacks);
        */

		mExtapp = new ExternalAppDatabase(getActivity());

        

        if (!"ovpn3".equals(BuildConfig.FLAVOR)) {
            PreferenceCategory appBehaviour = (PreferenceCategory) findPreference("app_behaviour");
            appBehaviour.removePreference(findPreference("ovpn3"));
        }
		//setClearApiSummary();

        //findPreference("share").setOnPreferenceClickListener(this);

        Preference logPF = findPreference("log");
        logPF.setOnPreferenceClickListener(this);

        //Preference accountPf = findPreference("account");
        //accountPf.setOnPreferenceClickListener(this);

        Preference vpnPF = findPreference("vpn");
        vpnPF.setOnPreferenceClickListener(this);

        Preference updatePF = findPreference("update");
        //get current version
        PackageManager packageManager = getActivity().getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(getActivity().getPackageName(), 0);
            updatePF.setSummary(getString(R.string.current_version_is) + info.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        updatePF.setOnPreferenceClickListener(this);

        Preference feedbackPF = findPreference("feedback");
        feedbackPF.setOnPreferenceClickListener(this);

        mLogoutPF = findPreference("logout");
        mLogoutPF.setOnPreferenceClickListener(this);
        //mVPNPFC = (PreferenceCategory) findPreference("app_vpn");
	}

    @Override
    public void onResume() {
        Log.d(tag, "onResume");
        VpnStatus.addStateListener(mStateListener);
        updateLogoutPreference();
        //updateAccountPF();
        updateVPNPF();
        super.onResume();

    }

    @Override
    public void onPause() {
        VpnStatus.removeStateListener(mStateListener);
        if (mPD != null && mPD.isShowing()){
            mPD.dismiss();;
            mPD = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mPD != null && mPD.isShowing()){
            mPD.dismiss();;
            mPD = null;
        }
        super.onDestroy();
    }

    private void updateLogoutPreference(){
        Account account = AccountManager.getManager().getCurtAccount();
        //Log.d(tag, "account = " + account +", mOtherPFC.getPreferenceCount()"+ mVPNPFC.getPreferenceCount());
        if (account == null/* && mVPNPFC.getPreferenceCount() == 3*/){
            //mVPNPFC.removePreference(mLogoutPF);
            mPScreen.removePreference(mLogoutPF);
        }else if(account != null /*&& mVPNPFC.getPreferenceCount() == 2*/){
            //mVPNPFC.addPreference(mLogoutPF);
            mPScreen.addPreference(mLogoutPF);
        }
    }

    /*private void updateAccountPF(){
        //set account
        Preference accountPf = findPreference("account");
        Account account = AccountManager.getManager().getCurtAccount();
        if (account == null){
            accountPf.setTitle(R.string.app_account_not_login_title);
            accountPf.setSummary(R.string.app_account_not_login_sumary);
        }else{
            accountPf.setTitle(account.userName);
            if (account.type == Account.AccountType.QQ){
                accountPf.setSummary(getString(R.string.from) + " QQ");
            }else if (account.type == Account.AccountType.SinaWeibo){
                accountPf.setSummary(getString(R.string.from) + " 新浪微博");
            }
        }
    }*/

    private void updateVPNPF(){
        Preference pf = findPreference("vpn");
        if (isConnected){
            if (mPD != null && mPD.isShowing()){
                mPD.dismiss();
                mPD = null;
            }
            String userName = AccountManager.getManager().getCurtAccount().userName;
            pf.setTitle(String.format(getString(R.string.app_vpn_connect_title), userName));
            //pf.setTitle(R.string.app_vpn_connect_title);
            pf.setSummary(R.string.app_vpn_connect_sumary);
        }else{
            pf.setTitle(R.string.app_vpn_not_connect_title);
            pf.setSummary(R.string.app_vpn_not_connect_sumary);
        }
    }


    private void setClearApiSummary() {
		Preference clearapi = findPreference("clearapi");

		if(mExtapp.getExtAppList().isEmpty()) {
			clearapi.setEnabled(false);
			clearapi.setSummary(R.string.no_external_app_allowed);
		} else { 
			clearapi.setEnabled(true);
			clearapi.setSummary(getString(R.string.allowed_apps,getExtAppList(", ")));
		}
	}

	private String getExtAppList(String delim) {
		ApplicationInfo app;
		PackageManager pm = getActivity().getPackageManager();

		String applist=null;
		for (String packagename : mExtapp.getExtAppList()) {
			try {
				app = pm.getApplicationInfo(packagename, 0);
				if (applist==null)
					applist = "";
				else
					applist += delim;
				applist+=app.loadLabel(pm);

			} catch (NameNotFoundException e) {
				// App not found. Remove it from the list
				mExtapp.removeApp(packagename);
			}
		}

		return applist;
	}

	private boolean isTunModuleAvailable() {
		// Check if the tun module exists on the file system
        return new File("/system/lib/modules/tun.ko").length() > 10;
    }

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference.getKey().equals("account")){
            Account account = AccountManager.getManager().getCurtAccount();
            if (account == null){
                startActivity(new Intent(getActivity(), LoginActivity.class));
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle("提示")
                        .setMessage("是否退出此账号?退出账号后，VPN连接也会断开!")
                        .setCancelable(true)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                breakConnect(false);
                                AccountManager.getManager().deleteAccount(getActivity());
                                AccountManager.getManager().setAccount(null);
                                //updateAccountPF();
                            }
                        });
                builder.create().show();
            }
        }else if (preference.getKey().equals("vpn")){
            if (!isConnected){
                //统计点击次数
                MobclickAgent.onEvent(getActivity(), "connect");
                Account account = AccountManager.getManager().getCurtAccount();
                if (account == null){
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_LOGIN);
                }else{
                    startConnect();
                }
            }else{
                //统计点击次数
                MobclickAgent.onEvent(getActivity(), "disconnect");
                breakConnect(true);
            }
        }else if (preference.getKey().equals("update")){
            //统计点击次数
            MobclickAgent.onEvent(getActivity(), "update");
            mPD = new ProgressDialog(getActivity());
            mPD.setMessage("正在检查更新...");
            mPD.setCancelable(true);
            mPD.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mPD.dismiss();
                    isCanceled = true;
                }
            });
            isCanceled = false;
            mPD.show();
            checkUpdate();
        }else if (preference.getKey().equals("feedback")){
            //FeedbackAgent agent = new FeedbackAgent(getActivity());
            //agent.startFeedbackActivity();
            startActivity(new Intent(getActivity(), FeedbackActivity.class));
            //统计点击次数
            MobclickAgent.onEvent(getActivity(), "feedback");
        }else if (preference.getKey().equals("log")){
            startActivity(new Intent(getActivity(), LogWindow.class));
        }else if (preference.getKey().equals("share")){
            String content = MobclickAgent.getConfigParams(getActivity(), Constants.PARAM_KEY_SHARE_TEXT);
            if (TextUtils.isEmpty(content)){
                content = getString(Constants.SHARE_TEXT_RESID);
            }
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, content);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_selector_title)));
            //startActivity(new Intent(getActivity(), ShareAppActivity.class));
        }else if(preference.getKey().equals("logout")){
            Account account = AccountManager.getManager().getCurtAccount();
            if (account != null && isConnected){
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.hint))
                        .setMessage(R.string.logout_confirm_message)
                        .setCancelable(true)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                breakConnect(false);
                                AccountManager.getManager().deleteAccount(getActivity());
                                AccountManager.getManager().setAccount(null);
                                //updateAccountPF();
                                Toast.makeText(getActivity(),getString(R.string.logout_success),Toast.LENGTH_SHORT).show();
                                updateLogoutPreference();
                            }
                        });
                builder.create().show();
            }else{
                AccountManager.getManager().deleteAccount(getActivity());
                AccountManager.getManager().setAccount(null);
                Toast.makeText(getActivity(),getString(R.string.logout_success),Toast.LENGTH_SHORT).show();
                updateLogoutPreference();
            }

        }
			
		return true;
	}

    //ProgressDialog mUpdatePD;
    boolean isCanceled = false;
    private void checkUpdate(){
        UmengUpdateAgent.setUpdateListener(new UmengUpdateListener() {
            @Override
            public void onUpdateReturned(int updateStatus, UpdateResponse updateInfo) {
                if (isCanceled){
                    return;
                }
                if (mPD != null && mPD.isShowing()){
                    mPD.dismiss();
                    mPD = null;
                }
                switch (updateStatus) {
                    case UpdateStatus.Yes: // has update
                        UmengUpdateAgent.showUpdateDialog(getActivity(), updateInfo);
                        break;
                    case UpdateStatus.No: // has no update
                        Toast.makeText(getActivity(), "没有更新", Toast.LENGTH_SHORT).show();
                        break;
                    case UpdateStatus.NoneWifi: // none wifi
                        Toast.makeText(getActivity(), "没有wifi连接， 只在wifi下更新", Toast.LENGTH_SHORT).show();
                        break;
                    case UpdateStatus.Timeout: // time out
                        Toast.makeText(getActivity(), "超时", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        UmengUpdateAgent.forceUpdate(getActivity());
    }


    @Override
	public void onClick(DialogInterface dialog, int which) {
		if( which == Dialog.BUTTON_POSITIVE){
			mExtapp.clearAllApiApps();
			setClearApiSummary();
		}
	}

    private void breakConnect(boolean showDialog){
        Intent intent = new Intent(getActivity(),DisconnectVPN.class);
        intent.putExtra("show_dialog", showDialog);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(tag, "onActivityResult, requestCode = " + requestCode + ", resuleCode="+resultCode);
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == getActivity().RESULT_OK){
            startConnect();
        }
        else if (requestCode ==REQUEST_CODE_LAUNCH_VPN && resultCode == getActivity().RESULT_OK){
            if (data != null){
                boolean success = data.getBooleanExtra("success", false);
                if (!success && mPD != null && mPD.isShowing()){
                    mPD.dismiss();
                    mPD = null;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    boolean isConnected = false;
    private VpnStatus.StateListener mStateListener = new VpnStatus.StateListener() {
        @Override
        public void updateState(String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
            if (level == VpnStatus.ConnectionStatus.LEVEL_CONNECTED){
                if (isConnected == false) {
                    //一次成功的VPN连接
                    final MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mainActivity);
                            if(sp.getBoolean("switch_to_res", true)) {
                                mainActivity.switchToResPage();
                            }
                            RecordInfoService.startActionRecordInfo(getActivity());
                        }
                    });
                    //MobclickAgent.onEventValue(mainActivity, "VPNConnect Success", null, 0);
                }
                isConnected = true;
            }else if (level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED){
                if (isConnected == true){
                    //一次断开连接
                }
                isConnected = false;
            }
            Log.d("", "logMessage = " + logmessage);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateVPNPF();
                }
            });
        }
    };

    private void startConnect(){
        if (mPD == null || !mPD.isShowing()){
            mPD = new ProgressDialog(getActivity());
            mPD.setCancelable(true);
            mPD.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    breakConnect(false);
                }
            });
        }
        mPD.setMessage("正在检查账号");
        mPD.show();
        new Thread(validateAccount).start();
    }

    private void doConnect(){
        VpnProfile profile = getVPNProfile();
        Account account = AccountManager.getManager().getCurtAccount();
        profile.mUsername = account.userId;
        profile.mPassword = account.password;
        //Log.d(tag, "serverName = " + profile.mServerName + ", serverPort = " + profile.mServerPort);
        profile.mServerName = account.serverIP;
        profile.mServerPort = account.serverPort;
        //Log.d(tag, "serverName = " + profile.mServerName + ", serverPort = " + profile.mServerPort);
        startVPN(profile);
    }

    private void startVPN(VpnProfile profile) {
        //clear previous log
        VpnStatus.clearLog();
        VpnStatus.logInfo(R.string.logCleared);

        Intent intent = new Intent(getActivity(),LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);

        startActivityForResult(intent, REQUEST_CODE_LAUNCH_VPN);
    }


    private VpnProfile getVPNProfile(){
        try {
            ProfileManager vpl = ProfileManager.getInstance(getActivity());
            if (vpl.getProfileByName(getString(R.string.app)) == null) {
                InputStream is = getActivity().getAssets().open("6kuaibo-6in4.ovpn");
                ConfigParser cp = new ConfigParser();
                InputStreamReader isr = new InputStreamReader(is);
                cp.parseConfig(isr);
                VpnProfile vp = cp.convertProfile();
                vp.mName = getResources().getString(R.string.app);
                is.close();

                vpl.addProfile(vp);
                vpl.saveProfile(getActivity(), vp);
                vpl.saveProfileList(getActivity());
                return vp;
            }
            return vpl.getProfileByName(getString(R.string.app));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigParser.ConfigParseError configParseError) {
            configParseError.printStackTrace();
        }
        return null;
    }

    Runnable validateAccount = new Runnable() {
        private String tag = "validateAccount";
        @Override
        public void run() {
            try {
                //暂停300ms，若不暂停，页面可能会出问题，原因不祥
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final Account account = AccountManager.getManager().getCurtAccount();
            if (account == null){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.account_expired_hint)
                                .setPositiveButton(R.string.account_expired_hint_btn,null);
                        builder.create().show();
                        AccountManager.getManager().setAccount(null);
                    }
                });
                return;
            }
            //{"id":"__id__","name":"__name__"}
            String json = "{\"id\":\"" + account.userId + "\",\"name\":\"" + account.userName + "\"}";
            //boolean useRSA = true;
            String useRSA = MobclickAgent.getConfigParams(getActivity(), Constants.PARAM_KEY_USE_RSA);
            if (useRSA == null){
                useRSA = "true";
            }
            final boolean rsa = Boolean.valueOf(useRSA);
            if (rsa){
                try {
                    char[] out = Base64Coder.encode(RSA.instance().encryptWithPublicKey(getActivity(), json));
                    json = new String(out);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            }
            //获取请求的URL
            String url = MobclickAgent.getConfigParams(getActivity(), Constants.PARAM_KEY_ACCOUNT_CHECK);
            if (url == null){
                url = Constants.URL_ACCOUNT_CHECK;
            }
            //Log.d(tag, "url = " + url);

            Ion.with(getActivity(), url).setBodyParameter("data", json)
                    .asString().setCallback(new FutureCallback<String>() {
                @Override
                public void onCompleted(Exception e, String result) {
                    if (e != null){
                        Log.d(tag, "error:" + e.toString());
                    }else {
                        Log.d(tag, "result=" + result);
                        if (rsa){
                            //decrypt
                            try {
                                result = RSA.instance().decryptWithPublicKey(getActivity(),Base64Coder.decode(result));
                            } catch (NoSuchAlgorithmException e1) {
                                e1.printStackTrace();
                            } catch (NoSuchPaddingException e1) {
                                e1.printStackTrace();
                            } catch (InvalidKeyException e1) {
                                e1.printStackTrace();
                            } catch (InvalidKeySpecException e1) {
                                e1.printStackTrace();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (IllegalBlockSizeException e1) {
                                e1.printStackTrace();
                            } catch (BadPaddingException e1) {
                                e1.printStackTrace();
                            }
                        }
                        GsonBuilder gb = new GsonBuilder();
                        Gson g = gb.create();
                        Map<String, String> map = g.fromJson(result, new TypeToken<Map<String, String>>() {
                        }.getType());
                        account.password = map.get("password");
                        //Log.d(tag, "password = " + map.get("password"));
                        account.serverIP = map.get("ip");
                        //Log.d(tag, "server ip = " + map.get("ip"));
                        account.serverPort = map.get("port");
                        //Log.d(tag, "server port = " + map.get("port"));
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mPD!= null && mPD.isShowing()){
                                    mPD.setMessage("正在连接VPN");
                                }
                                doConnect();
                            }
                        });
                    }
                }
            });
        }
    };

}