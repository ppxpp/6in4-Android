package cn.edu.bupt.niclab.event;

import java.io.File;

/**
 * Created by zhengmeng on 2015/1/19.
 */
public class FileDownloadEvent extends BaseEvent {
    
    public static final int EVENT_START = 6;
    public static final int EVENT_PROGRESS = 5;
    public static final int EVENT_PAUSE = 7;

    private long totalSize;
    private long downloadedSize;
    //本地文件存放路径
    private String localPath;
    //remote url
    private String fileUrl;
    
    //local file name
    private String localName;

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public FileDownloadEvent(int result){
        mResult = result;
    }
    
    public void setResult(int result){
        mResult = result;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(int downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
}
