package cn.edu.bupt.niclab.entity;

import java.io.Serializable;

/**
 * Created by zhengmeng on 2015/1/14.
 */
public class APPInfo implements Serializable {
    
    private int id;
    private String name;
    private String icon;
    private String downloadUrl;
    private String packageName;
    private String description;
    private String[] screenShots;
    private String versionName;
    private int sizeInKB;
    private int downloadCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getScreenShots() {
        return screenShots;
    }

    public void setScreenShots(String[] screenShots) {
        this.screenShots = screenShots;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getSizeInKB() {
        return sizeInKB;
    }

    public void setSizeInKB(int sizeInKB) {
        this.sizeInKB = sizeInKB;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }
}
