package cn.edu.bupt.niclab.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koushikdutta.async.util.FileUtility;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.app.APPDataProvider;
import cn.edu.bupt.niclab.entity.APPInfo;
import cn.edu.bupt.niclab.event.FileDownloadEvent;
import cn.edu.bupt.niclab.fragments.Utils;
import cn.edu.bupt.niclab.services.FileDownloadService;
import de.greenrobot.common.io.FileUtils;
import de.greenrobot.event.EventBus;

public class APPDetailActivity extends Activity {
    private String tag = getClass().getName();

    protected void debug(String msg) {
        Log.d(tag, msg);
    }
    
    
    private APPInfo mAPPInfo;
    
    private ImageView mIconIV;
    private TextView mNameTV, mLabelTV, mVersionTV;
    private ExpandableTextView mDetailTV;
    private Button mInstallBtn;
    private LinearLayout mScreenShotLayout;
    
    private String fileUrl;
    private String localDir;
    private String localName;
    private String localPath;
    
    public static enum  DownloadState{
        Starting,//正在准备下载
        Downloading,//正在下载
        Stopping,//正在准备停止下载
        Pause,//处于暂停状态
        Finish,//下载完成
        Failed,//下载失败
        Normal,//未下载
        Installed,//已完成安装
    }
    private DownloadState mCurtState;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_appdetail);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        int app_id = intent.getIntExtra("app_id", -1);
        mAPPInfo = APPDataProvider.getInstance().getAPPInfoByID(app_id);
        if (mAPPInfo == null){
            finish();
        }
        
        mIconIV = (ImageView) findViewById(R.id.icon);
        mNameTV = (TextView) findViewById(R.id.name);
        mLabelTV = (TextView) findViewById(R.id.label);
        mVersionTV = (TextView) findViewById(R.id.version);
        mDetailTV = (ExpandableTextView) findViewById(R.id.expand_text_view);
        mInstallBtn = (Button) findViewById(R.id.install);
        mScreenShotLayout = (LinearLayout) findViewById(R.id.screen_shot_layout);

        setupView();

        fileUrl = mAPPInfo.getDownloadUrl();
        localDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Constants.APKDir;
        localName = mAPPInfo.getName();//fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        if (!localName.endsWith(".apk")){
            localName = localName + ".apk";
        }
        localPath = localDir + File.separator + localName;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCurtState == null
                || mCurtState == APPDetailActivity.DownloadState.Finish
                || mCurtState == APPDetailActivity.DownloadState.Installed
                || mCurtState == DownloadState.Downloading) {
            if (Utils.checkInstallAPP(this, mAPPInfo.getPackageName())) {
                //已安装
                setInstallButtonState(DownloadState.Installed);
            } else {
                if (new File(localPath).exists()) {
                    //已下载，未安装
                    setInstallButtonState(DownloadState.Finish);
                } else {
                    //未下载
                    if (mCurtState != DownloadState.Downloading) {
                        setInstallButtonState(DownloadState.Normal);
                    }else{
                        setInstallButtonState(DownloadState.Downloading);
                    }
                }
            }
        }
    }

    private void setupView(){
        ImageLoader.getInstance().displayImage(mAPPInfo.getIcon(), mIconIV);
        mNameTV.setText(mAPPInfo.getName());
        mLabelTV.setText(String.format(getString(R.string.app_label), mAPPInfo.getDownloadCount(), Utils.getReadableSize(mAPPInfo.getSizeInKB())));
        mVersionTV.setText(String.format(getString(R.string.app_version), mAPPInfo.getVersionName()));
        mDetailTV.setText(mAPPInfo.getDescription());
        
        if (mAPPInfo.getScreenShots() != null){
            mScreenShotLayout.removeAllViews();
            for (String url: mAPPInfo.getScreenShots()){
                debug("url = " + url);
                ImageView imageView = new ImageView(this);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(Utils.dip2px(this, 120), Utils.dip2px(this, 160)));
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                mScreenShotLayout.addView(imageView);
                ImageLoader.getInstance().displayImage(url, imageView);
            }
        }
    }
    
    private void download(){
        //download
        debug("start download");
        FileDownloadService.startFileDownload(this, fileUrl, localDir, localName);
    }
    
    private void pause(){
        debug("pause download");
        FileDownloadService.pauseFileDownload(this, fileUrl, localDir, localName);
        
    }
    
    private void install(){
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(localPath)), "application/vnd.android.package-archive");
        startActivity(intent);
    }
    
    private void launch(){
        Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(mAPPInfo.getPackageName());
        startActivity(LaunchIntent);
        
    }
    
    
    private void setInstallButtonState(DownloadState state){
        if (mCurtState == state){
            return;
        }
        mCurtState = state;
        switch (state){
            case Starting:
                mInstallBtn.setText(R.string.state_starting);
                break;
            case Downloading:
                mInstallBtn.setText(R.string.state_downloading);
                break;
            case Stopping:
                mInstallBtn.setText(R.string.state_stopping);
                break;
            case Pause:
                mInstallBtn.setText(R.string.state_pause);
                break;
            case Finish:
                mInstallBtn.setText(R.string.state_finish);
                break;
            case Failed:
                mInstallBtn.setText(R.string.state_failed);
                break;
            case Normal:
                mInstallBtn.setText(R.string.state_normal);
                break;
            case Installed:
                mInstallBtn.setText(R.string.state_installed);
                break;
        }
    }
    
    public void onEventMainThread(FileDownloadEvent event){
        if (event.getResult() == FileDownloadEvent.EVENT_START){
            //debug("onEventMainThread, start");
            setInstallButtonState(DownloadState.Downloading);
            setInstallBtnBgProgress(0);
            
        }else if (event.getResult() == FileDownloadEvent.EVENT_PAUSE){
            //debug("onEventMainThread, pause");
            setInstallButtonState(DownloadState.Pause);
            
        }else if (event.getResult() == FileDownloadEvent.EVENT_SUCCESS){
            //debug("onEventMainThread, success");
            setInstallButtonState(DownloadState.Finish);
            setInstallBtnBgProgress(1);
            
        }else if (event.getResult() == FileDownloadEvent.EVENT_FAILED){
            //debug("onEventMainThread, success");
            setInstallButtonState(DownloadState.Failed);
            setInstallBtnBgProgress(1);

        }else if (event.getResult() == FileDownloadEvent.EVENT_PROGRESS){
            //debug("onEventMainThread, progress, finished = " + event.getDownloadedSize() + ", total = " + event.getTotalSize());
            long finished = event.getDownloadedSize();
            long total = event.getTotalSize();
            double percent = (double)finished / (double)total;
            setInstallBtnBgProgress(percent);
        }
    }
    
    public void onInstallBtnClicked(View view){
        if (mCurtState == DownloadState.Normal 
                || mCurtState == DownloadState.Pause
                || mCurtState == DownloadState.Failed) {
            //download
            setInstallButtonState(DownloadState.Starting);
            download();
        }else if (mCurtState == DownloadState.Downloading){
            //pause
            setInstallButtonState(DownloadState.Stopping);
            pause();
        }else if (mCurtState == DownloadState.Finish){
            //install
            install();
        }
    }
    
    private void setInstallBtnBgProgress(double percent){
        percent = percent > 1 ? 1 : percent;
        int width = (int) (mInstallBtn.getWidth() * percent);
        StateListDrawable sld = (StateListDrawable) mInstallBtn.getBackground();
        DrawableContainer.DrawableContainerState containerState = (DrawableContainer.DrawableContainerState) sld.getConstantState();
        Drawable[] children = containerState.getChildren();
        //debug("length = " + children.length + ", count = " + containerState.getChildCount());
        LayerDrawable ld = (LayerDrawable) children[1];
        Drawable d = ld.getDrawable(1);
        d.setBounds(0, 0, width, mInstallBtn.getHeight());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_appdetail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_download_manage) {
            startActivity(new Intent(this, APPManageActivity.class));
            return true;
        }else if (id == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
