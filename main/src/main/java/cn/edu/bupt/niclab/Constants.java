package cn.edu.bupt.niclab;

/**
 * Created by ppxpp on 2014/10/11.
 */
public class Constants {

    //name of SharedPreference
    public static final String SHARED_PREFERENCE_NAME = "6in4sp";
    //key of last user id
    public static final String SP_KEY_LAST_USERID = "last_user_id";
    //whether vpn profile imported
    public static final String SP_KEY_PROFILE_IMPORTED = "profile_imported";
    //
    public static final String SERVER_IP = "www.6able.com";
    //检查账号的url
    public static final String URL_ACCOUNT_CHECK = "http://"+SERVER_IP+"/account/check";
    //IPv6资源页面
    public static final String URL_IPv6_RESOURCE = "http://"+SERVER_IP+"/res";
    //默认的用于检测ping6的参数
    public static final String URL_IPv6_HOST_FOR_TEST = "bt.byr.cn";
    //记录设备信息的url
    public static final String URL_RECORD_DEVICE = "http://"+SERVER_IP +"/record/device";
    //记录设备连接日志的url
    public static final String URL_RECORD_CONNECT_LOG = "http://" + SERVER_IP + "/record/connectlog";
    //获取APP列表接口
    public static final String URL_APP_RESOURCE = "http://" + SERVER_IP + "/app/list";
    
    
    public static final int SHARE_TITLE_RESID = R.string.share_title;
    //
    public static final int SHARE_TITLE_URL_RESID = R.string.share_title;
    //
    public static final int SHARE_TEXT_RESID = R.string.share_text;
    //
    public static final int SHARE_SITE_RESID = R.string.share_site;
    //
    public static final int SHARE_URL_RESID = R.string.share_url;

    //需与友盟后台填写的key一致
    public static final String PARAM_KEY_ACCOUNT_CHECK = "url_account_check";
    public  static final String PARAM_KEY_IPV6_RESOURCE = "url_ipv6_resource";
    public  static final String PARAM_KEY_APP_RESOURCE = "url_app_resource";
    public static final String PARAM_KEY_RECORD_DEVICE = "url_record_device";
    public static final String PARAM_KEY_RECORD_CONNECT_LOG = "url_record_connect_log";
    public static final String PARAM_KEY_USE_RSA = "use_rsa";
    public static final String PARAM_KEY_SHARE_TITLE = "share_title";
    public static final String PARAM_KEY_SHARE_TITLE_URL = "share_title_url";
    public static final String PARAM_KEY_SHARE_TEXT = "share_text";
    public static final String PARAM_KEY_SHARE_SITE = "share_site";
    public static final String PARAM_KEY_SHARE_URL = "share_url";
    public static final String PARAM_KEY_IPV6_HOST_FOR_TEST = "ipv6_host_for_test";


    //public static final boolean USE_RSA = true;
}
