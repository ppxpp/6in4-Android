package cn.edu.bupt.niclab.account;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.edu.bupt.niclab.Constants;


/**
 * Created by ppxpp on 2014/10/11.
 */
public class AccountManager {

    private final String tag = getClass().getName();

    private final int STATUE_VALIDATED = 1;
    private final int STATUE_VALIDATING = 2;
    private final int STATUE_NOT_VALIDATE = 3;

    //private AtomicInteger mStatus;

    private static class Holder{
        private static AccountManager instance = new AccountManager();
    }

    private AccountManager(){
        //mStatus = new AtomicInteger(STATUE_NOT_VALIDATE);
    }

    public static AccountManager getManager(){
        return Holder.instance;
    }

    //当前登录用户
    private Account mCurtAccount;

    public void setAccount(Account account){
        this.mCurtAccount = account;
    }

    public Account getCurtAccount(){
        return mCurtAccount;
    }

    //从文件中读取缓存的用户信息
    public synchronized void loadAccount(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String lastUserId = sp.getString(Constants.SP_KEY_LAST_USERID, null);
        if (lastUserId == null){
            return;
        }
        File file = new File(context.getDir("account", Context.MODE_PRIVATE).getAbsolutePath() + File.separator + lastUserId);
        if (!file.exists()){
            return;
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            mCurtAccount = (Account) ois.readObject();
            //mStatus.set(STATUE_NOT_VALIDATE);
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized void deleteAccount(Context context){
        if (mCurtAccount == null){
            return;
        }
        File file = new File(context.getDir("account", Context.MODE_PRIVATE).getAbsolutePath() + File.separator + mCurtAccount.userId);
        if (file.exists()){
            file.delete();
        }
    }


    //将用户信息写入缓存
    public synchronized void saveAccount(Context context){
        if (mCurtAccount == null){
            return;
        }
        File file = new File(context.getDir("account", Context.MODE_PRIVATE).getAbsolutePath() + File.separator + mCurtAccount.userId);
        if (file.exists()){
            file.delete();
        }
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(mCurtAccount);
            oos.flush();
            oos.close();

            SharedPreferences sp = context.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(Constants.SP_KEY_LAST_USERID, mCurtAccount.userId);
            editor.commit();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
