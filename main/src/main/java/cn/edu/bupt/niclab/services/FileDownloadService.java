package cn.edu.bupt.niclab.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.event.BaseEvent;
import cn.edu.bupt.niclab.event.FileDownloadEvent;
import de.greenrobot.common.StringUtils;
import de.greenrobot.event.EventBus;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class FileDownloadService extends Service {
    private String tag = getClass().getName();
    private void debug(String msg){Log.d(tag, msg);}
    
    
    private static final String ACTION_FILE_DOWNLOAD = "cn.edu.bupt.niclab.services.action.FILE_DOWNLOAD";

    private static final String PARAM_FILE_URL = "cn.edu.bupt.niclab.services.extra.PARAM_FILE_URL";
    private static final String PARAM_LOCAL_DIR = "cn.edu.bupt.niclab.services.extra.PARAM_LOCAL_DIR";
    private static final String PARAM_LOCAL_NAME = "cn.edu.bupt.niclab.services.extra.PARAM_LOCAL_NAME";

    public static void startFileDownload(Context context, String fileUrl, String localDir, String localName) {
        Intent intent = new Intent(context, FileDownloadService.class);
        intent.setAction(ACTION_FILE_DOWNLOAD);
        intent.putExtra(PARAM_FILE_URL, fileUrl);
        intent.putExtra(PARAM_LOCAL_DIR, localDir);
        intent.putExtra(PARAM_LOCAL_NAME, localName);
        context.startService(intent);
    }

    private static final String ACTION_PAUSE_DOWNLOAD = "cn.edu.bupt.niclab.services.action.PAUSE_DOWNLOAD";
    public static void pauseFileDownload(Context context, String fileUrl, String localDir, String localName) {
        Intent intent = new Intent(context, FileDownloadService.class);
        intent.setAction(ACTION_PAUSE_DOWNLOAD);
        intent.putExtra(PARAM_FILE_URL, fileUrl);
        intent.putExtra(PARAM_LOCAL_DIR, localDir);
        intent.putExtra(PARAM_LOCAL_NAME, localName);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //add running thread num
                //mRunningThreadNum.addAndGet(1);
                onHandleIntent(intent);
                //int a = mRunningThreadNum.decrementAndGet();
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }
    
    public class FileDownloadBinder extends Binder{
        FileDownloadService getService(){
            return FileDownloadService.this;
        }
    }
    
    private final IBinder mBinder = new FileDownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //@Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FILE_DOWNLOAD.equals(action)) {
                final String fileUrl = intent.getStringExtra(PARAM_FILE_URL);
                final String localDir = intent.getStringExtra(PARAM_LOCAL_DIR);
                final String localName = intent.getStringExtra(PARAM_LOCAL_NAME);
                handleFileDownload(fileUrl, localDir, localName);
            }else if (ACTION_PAUSE_DOWNLOAD.equals(action)) {
                final String fileUrl = intent.getStringExtra(PARAM_FILE_URL);
                final String localDir = intent.getStringExtra(PARAM_LOCAL_DIR);
                final String localName = intent.getStringExtra(PARAM_LOCAL_NAME);
                pauseFileDownload(fileUrl, localDir, localName);
            }
        }
    }

    private void pauseFileDownload(String fileUrl, String localDir, String localName) {
        String md5 = StringUtils.generateMD5String(fileUrl + localDir + localName);
        mPauseMap.put(md5, true);
        debug("====== pause file download =========");
    }

    
    
    private Map<String, Boolean> mPauseMap = new ConcurrentHashMap<String, Boolean>();
    
    private void handleFileDownload(String fileUrl, String localDir, String localName) {
        debug("====== handle file download =========");
        String md5 = StringUtils.generateMD5String(fileUrl + localDir + localName);
        mPauseMap.put(md5, false);
        
        //String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        File dir = new File(localDir);
        if (!dir.exists() || !dir.isDirectory()){
            dir.delete();
            if(!dir.mkdirs()) {
                //make dir error
                debug("mkdirs error, dir = " + dir.getAbsolutePath());
                FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_FAILED);
                event.setLocalName(localName);
                event.setFileUrl(fileUrl);
                EventBus.getDefault().post(event);
                return;
            }
        }
        //create http connection
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.connect();
            
            if (conn.getResponseCode() != 200){
                FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_FAILED);
                event.setLocalName(localName);
                event.setFileUrl(fileUrl);
                EventBus.getDefault().post(event);
                return;
            }
            
            int fileSize = conn.getContentLength();
            File localFile = new File(localDir + File.separator + localName);
            if (localFile.exists()){
                if (localFile.length() == fileSize) {
                    //already downloaded
                    FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_SUCCESS);
                    event.setLocalName(localName);
                    event.setFileUrl(fileUrl);
                    EventBus.getDefault().post(event);
                    return;
                }else{
                    localFile.delete();
                }
            }
            RandomAccessFile localTmpFile = new RandomAccessFile(localDir + File.separator + localName + ".tmp", "rw");
            if(localTmpFile.length() != fileSize) {
                localTmpFile.setLength(fileSize);
            }
            localTmpFile.close();
            final int DefaultThreadNum = 4;
            final int MinBlockSize = 1024000;//1mb
            int threadNum = DefaultThreadNum;
            if (threadNum * MinBlockSize > fileSize){
                threadNum = (fileSize % MinBlockSize == 0) ? (fileSize / MinBlockSize) : (fileSize / MinBlockSize + 1);
            }
            int blockSize = fileSize / threadNum;
            DownloadInfo[] dlInfos;
            dlInfos = readDownloadInfo(fileUrl, localDir, localName);
            if (dlInfos == null){
                dlInfos = new DownloadInfo[threadNum];
                for (int i = 0; i < dlInfos.length; i++){
                    DownloadInfo info = new DownloadInfo();
                    info.idx = i;
                    info.url = fileUrl;
                    info.localFile = localDir + File.separator + localName + ".tmp";
                    info.startPos = i * blockSize;
                    info.downloadedSize = 0;
                    info.blockSize = (i < (dlInfos.length - 1)) ? blockSize : (fileSize - i * blockSize);
                    dlInfos[i] = info;
                }
            }
            //publish start event
            FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_START);
            event.setLocalName(localName);
            event.setFileUrl(fileUrl);
            EventBus.getDefault().post(event);
            
            DownloadThread[] threads = new DownloadThread[threadNum];
            for (int i = 0; i < threadNum; i++){
                DownloadThread thread = new DownloadThread(dlInfos[i]);
                threads[i] = thread;
                thread.start();
            }
            int finishedSize = 0;
            while (finishedSize < fileSize){
                try {
                    Thread.sleep(1000);
                    //check progress
                    finishedSize = 0;
                    for (int i = 0; i < threadNum; i++){
                        finishedSize += threads[i].getFinishedSize();
                    }
                    //check if error
                    for (int i = 0; i < threadNum; i++){
                        if (threads[i].isError){
                            debug("restart thread " + i);
                            DownloadInfo info = threads[i].mInfo;
                            threads[i].terminate();
                            dlInfos[i] = info;
                            DownloadThread thread = new DownloadThread(dlInfos[i]);
                            threads[i] = thread;
                            thread.start();
                        }
                    }
                    //publish progress event
                    event.setResult(FileDownloadEvent.EVENT_PROGRESS);
                    event.setDownloadedSize(finishedSize);
                    event.setTotalSize(fileSize);
                    EventBus.getDefault().post(event);
                    debug("download " + finishedSize + " of " + fileSize);
                    
                    //check pause
                    if (mPauseMap.get(md5) == true){
                        for (int i = 0; i < threadNum; i++){
                            threads[i].terminate();
                        }
                        debug("pause download");
                        break;
                    }
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (finishedSize < fileSize){
                //wait all thread to stop
                for (int i = 0; i < threadNum; i++){
                    threads[i].join();
                }
                //send pause event
                event.setResult(FileDownloadEvent.EVENT_PAUSE);
                EventBus.getDefault().post(event);
                
                //write download info
                debug("write download info");
                writeDownloadInfo(fileUrl, localDir, localName, dlInfos);
            }else{
                //finish download
                //rename tmp file
                String fullPath = localDir + File.separator + localName;
                String tmpFileName = fullPath + ".tmp";
                File file = new File(tmpFileName);
                file.renameTo(new File(fullPath));
                //send finish event
                event.setResult(FileDownloadEvent.EVENT_SUCCESS);
                event.setLocalPath(fullPath);
                EventBus.getDefault().post(event);
                
                debug("finish download, delete download info");
                deleteDownloadInfo(fileUrl, localDir, localName);
            }
            mPauseMap.remove(md5);

            
        } catch (MalformedURLException e) {
            debug("MalformedURLException");
            e.printStackTrace();
        } catch (ConnectException e){
            debug("ConnectException");
            FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_FAILED);
            event.setLocalName(localName);
            event.setFileUrl(fileUrl);
            EventBus.getDefault().post(event);
        } catch (IOException e) {
            debug("IOException");
            e.printStackTrace();
            FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_FAILED);
            event.setLocalName(localName);
            event.setFileUrl(fileUrl);
            EventBus.getDefault().post(event);
            debug("IOException post event");
        } catch (InterruptedException e) {
            debug("InterruptedException");
            e.printStackTrace();
            FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_FAILED);
            event.setLocalName(localName);
            event.setFileUrl(fileUrl);
            EventBus.getDefault().post(event);
        }catch (Exception e){
            debug("Exception");
            e.printStackTrace();
            FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_FAILED);
            event.setLocalName(localName);
            event.setFileUrl(fileUrl);
            EventBus.getDefault().post(event);
        }

    }
    
    public static class DownloadInfo implements Serializable{
        int idx;
        //开始位置
        int startPos;
        //已下载的长度
        int downloadedSize;
        //总长度
        int blockSize;
        //文件url
        String url;
        //本地存储的文件路径
        String localFile;
    }
    
    class DownloadThread extends Thread{
        private String tag = Thread.currentThread().getName();
        private void debug(String msg){Log.d(tag, msg);}
        
        private DownloadInfo mInfo;
        private boolean terminate;
        private boolean isStop;
        private boolean isError;
        public DownloadThread(DownloadInfo info){
            //super("download--" + info.idx);
            mInfo = info;
            terminate = false;
            isStop = false;
            isError = false;
        }
        
        public int getFinishedSize(){
            return mInfo.downloadedSize;
        }
        
        public void terminate(){
            terminate = true;
        }
        
        public boolean isStop(){
            return isStop;
        }
        
        public void run(){
            debug("Thread " + mInfo.idx + " download from " + mInfo.startPos + " to " + (mInfo.startPos + mInfo.blockSize - 1) +",  size = " + mInfo.blockSize + ", downloaded = " + mInfo.downloadedSize);
            //open file
            try {
                RandomAccessFile file = new RandomAccessFile(mInfo.localFile, "rw");
                file.seek(mInfo.startPos + mInfo.downloadedSize);
                
                //open http connection
                URL url = new URL(mInfo.url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                debug("Range:"+"bytes="+(mInfo.startPos + mInfo.downloadedSize) + "-" + (mInfo.startPos + mInfo.blockSize - 1));
                conn.setRequestProperty("Range", "bytes="+(mInfo.startPos + mInfo.downloadedSize) + "-" + (mInfo.startPos + mInfo.blockSize - 1));

                InputStream in = conn.getInputStream();
                
                byte[] buf = new byte[409600];//400KB
                int count = 0;
                while (terminate == false && (count = in.read(buf, 0, 4096)) > 0){
                    file.write(buf, 0, count);
                    mInfo.downloadedSize += count;
                    debug("Thread " + mInfo.idx + ", download " + mInfo.downloadedSize + " of " + mInfo.blockSize);
                }
                
                in.close();
                file.close();
                conn.disconnect();
            } catch (FileNotFoundException e) {
                isError = true;
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                isError = true;
            }catch (Exception e){
                isError = true;
                e.printStackTrace();
            }
            /*while (terminate == false && mInfo.downloadedSize < mInfo.blockSize){
                try {
                    Thread.sleep(1000);
                    mInfo.downloadedSize += 300000;
                    if (mInfo.downloadedSize > mInfo.blockSize){
                        mInfo.downloadedSize = mInfo.blockSize;
                    }
                    debug("Thread " + mInfo.idx + ", download " + mInfo.downloadedSize + " of " + mInfo.blockSize);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
            debug("Thread " + mInfo.idx + "terminate download");
            isStop = true;
        }
        
    }
    
    private DownloadInfo[] readDownloadInfo(String fileUrl, String localDir, String localName){
        String md5 = StringUtils.generateMD5String(fileUrl + localDir + localName);
        try {
            FileInputStream fis = openFileInput(md5);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object object = ois.readObject();
            DownloadInfo[] out = (DownloadInfo[]) object;
            return out;
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch (ClassCastException e){
            e.printStackTrace();
        }
        return null;
    }
    
    private void deleteDownloadInfo(String fileUrl, String localDir, String localName){
        String md5 = StringUtils.generateMD5String(fileUrl + localDir + localName);
        deleteFile(md5);
    }

    private void writeDownloadInfo(String fileUrl, String localDir, String localName, DownloadInfo[] infos){
        String md5 = StringUtils.generateMD5String(fileUrl + localDir + localName);
        try {
            FileOutputStream fos = openFileOutput(md5, MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(infos);
            oos.flush();
            oos.close();
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (ClassCastException e){
            e.printStackTrace();
        }
    }


    //int mNotificationId = 0001;
    Map<String, Integer> mNotificationIdMap = new WeakHashMap<String, Integer>();
    public void onEventMainThread(FileDownloadEvent event) {
        //根据event的fileUrl参数来确定notificationId
        int notificationId = -1;
        if (mNotificationIdMap.containsKey(event.getLocalName())){
            notificationId = mNotificationIdMap.get(event.getLocalName());
        }else{
            notificationId = (int)System.currentTimeMillis();
            mNotificationIdMap.put(event.getLocalName(), notificationId);
        }
        String name = event.getLocalName();//.getFileUrl();
        //String name = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        if (!name.endsWith(".apk")){
            name = name + ".apk";
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(String.format(getString(R.string.download_title), name))
                .setContentText("0%");
        // Creates an Intent for the Activity
        Intent notifyIntent = new Intent();
        if (event.getResult() == BaseEvent.EVENT_SUCCESS){
            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notifyIntent.setAction(Intent.ACTION_VIEW);
            notifyIntent.setDataAndType(Uri.fromFile(new File(event.getLocalPath())), "application/vnd.android.package-archive");
        }else{
            //notifyIntent.setComponent((new ComponentName(this, DownloadDialogActivity.class)));
            // Sets the Activity to start in a new, empty task
            //notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
            //        Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        // Creates the PendingIntent
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // Puts the PendingIntent into the notification builder
        mBuilder.setContentIntent(notifyPendingIntent);

        if (event.getResult() == FileDownloadEvent.EVENT_START) {
            //setup notification

        } else if (event.getResult() == FileDownloadEvent.EVENT_PROGRESS) {
            //update progress
            mBuilder.setContentText(event.getDownloadedSize() * 100 / event.getTotalSize() + "%");
            mBuilder.setProgress((int)event.getTotalSize(), (int)event.getDownloadedSize(), false);
        } else if (event.getResult() == FileDownloadEvent.EVENT_SUCCESS) {

            mBuilder.setContentTitle(String.format(getString(R.string.notification_title_downloaded), name));
            mBuilder.setContentText(getString(R.string.click_to_install));
            mBuilder.setProgress(0, 0, false);
            mBuilder.setContentIntent(notifyPendingIntent);
        }
        mBuilder.setAutoCancel(true);
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (event.getResult() != BaseEvent.EVENT_FAILED) {
            mNotifyMgr.notify(notificationId, mBuilder.build());
        }else {
            mNotifyMgr.cancel(notificationId);
        }
    }
    
    
}
