package cn.edu.bupt.niclab.account;

import java.io.Serializable;

/**
 * Created by ppxpp on 2014/10/11.
 */
public class Account implements Serializable{

    public String userName;
    public String password;
    //shareSDK返回的用户id，微博用户为"sina_weibo_{id}",QQ用户为"qq_{id}"
    public String userId;
    public String headUrl;
    public AccountType type;
    public String serverIP;
    public String serverPort;
    //private boolean isOutofdate = true;

    public static enum AccountType{
        QQ,//qq用户
        SinaWeibo//新浪微博用户
    }

    public static String getIDPrefix(AccountType type){
        String prefix = null;
        switch (type){
            case QQ:
                prefix = "qq_";
                break;
            case SinaWeibo:
                prefix = "sina_weibo_";
                break;
        }

        return prefix;
    }
}
