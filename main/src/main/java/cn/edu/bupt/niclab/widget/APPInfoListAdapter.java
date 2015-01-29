package cn.edu.bupt.niclab.widget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.activities.APPDetailActivity;
import cn.edu.bupt.niclab.entity.APPInfo;
import cn.edu.bupt.niclab.event.FileDownloadEvent;
import cn.edu.bupt.niclab.fragments.Utils;
import cn.edu.bupt.niclab.services.FileDownloadService;
import de.greenrobot.event.EventBus;

/**
* Created by zhengmeng on 2015/1/19.
*/
public class APPInfoListAdapter extends BaseAdapter {

    private Context mContext;
    private List<APPInfoWrapper> mAPPInfoWrapperList;
    
    public APPInfoListAdapter(Context context, List<APPInfo> data){
        mContext = context;
        mAPPInfoWrapperList = new ArrayList<APPInfoWrapper>();
        for (APPInfo appInfo: data) {
            APPInfoWrapper wrapper = new APPInfoWrapper();
            wrapper.appInfo = appInfo;
            mAPPInfoWrapperList.add(wrapper);
        }
    }
    
    public void setData(List<APPInfo> data){
        if (mAPPInfoWrapperList != null){
            mAPPInfoWrapperList.clear();
        }else {
            mAPPInfoWrapperList = new ArrayList<APPInfoWrapper>();
        }
        for (APPInfo appInfo: data) {
            APPInfoWrapper wrapper = new APPInfoWrapper();
            wrapper.appInfo = appInfo;
            mAPPInfoWrapperList.add(wrapper);
        }
    }

    @Override
    public int getCount() {
        return mAPPInfoWrapperList == null ? 0 : mAPPInfoWrapperList.size();
    }
    
    public APPInfoWrapper getWrapperItemByFileURL(String fileUrl){
        for (APPInfoWrapper wrapper: mAPPInfoWrapperList){
            if (wrapper.appInfo.getDownloadUrl().equals(fileUrl)){
                return wrapper;
            }
        }
        return null;
    }

    public APPInfoWrapper getWrapperItemBy(int position){
        return mAPPInfoWrapperList == null ? null : mAPPInfoWrapperList.get(position);
    }

    @Override
    public Object getItem(int position) {
        return mAPPInfoWrapperList == null ? null : mAPPInfoWrapperList.get(position).appInfo;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
        }
        APPInfoWrapper wrapper = getWrapperItemBy(position);
        APPInfo appInfo = wrapper.appInfo;
        
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageResource(R.drawable.icon_default);
        ImageLoader.getInstance().displayImage(appInfo.getIcon(), icon);
        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(appInfo.getName());
        TextView label = (TextView) view.findViewById(R.id.label);
        label.setText(String.format(mContext.getString(R.string.app_label), appInfo.getDownloadCount(), Utils.getReadableSize(appInfo.getSizeInKB())));
        Button installBtn = (Button) view.findViewById(R.id.install_btn);
        installBtn.setBackgroundResource(R.drawable.app_item_install_btn_bg);
        if (wrapper.curtDownloadState == null
                || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Finish
                || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Installed
                || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Downloading) {
            if (Utils.checkInstallAPP(mContext, appInfo.getPackageName())) {
                //已安装
                wrapper.curtDownloadState = APPDetailActivity.DownloadState.Installed;
                installBtn.setText(R.string.launch);
                installBtn.setBackgroundResource(R.drawable.app_item_launch_btn_bg);

            } else {
                //String fileUrl = wrapper.appInfo.getDownloadUrl();
                String localDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Constants.APKDir;
                String localName = wrapper.appInfo.getName();//fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                if (!localName.endsWith(".apk")){
                    localName = localName + ".apk";
                }
                String localPath = localDir + File.separator + localName;
                if (new File(localPath).exists()) {
                    //已下载，未安装
                    wrapper.curtDownloadState = APPDetailActivity.DownloadState.Finish;
                    installBtn.setText(R.string.install);
                } else {
                    //未下载
                    if (wrapper.curtDownloadState != APPDetailActivity.DownloadState.Downloading) {
                        wrapper.curtDownloadState = APPDetailActivity.DownloadState.Normal;
                        installBtn.setText(R.string.state_normal);
                    }else{
                        installBtn.setText(R.string.state_downloading);
                    }
                }

            }
        }else{
            switch (wrapper.curtDownloadState){
                case Starting:
                    installBtn.setText(R.string.state_starting);
                    break;
                case Downloading:
                    installBtn.setText(R.string.state_downloading);
                    break;
                case Stopping:
                    installBtn.setText(R.string.state_stopping);
                    break;
                case Pause:
                    installBtn.setText(R.string.state_pause);
                    break;
                case Finish:
                    installBtn.setText(R.string.state_finish);
                    break;
                case Failed:
                    installBtn.setText(R.string.state_failed);
                    break;
                case Normal:
                    installBtn.setText(R.string.state_normal);
                    break;
                case Installed:
                    installBtn.setText(R.string.state_installed);                
                    installBtn.setBackgroundResource(R.drawable.app_item_launch_btn_bg);
                    break;
            }
        }
        installBtn.setTag(wrapper);
        installBtn.setOnClickListener(onClickListener);

        NumberProgressBar mpb = (NumberProgressBar) view.findViewById(R.id.number_progress_bar);
        if (wrapper.curtDownloadState == APPDetailActivity.DownloadState.Downloading
                || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Pause
                || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Stopping){
            mpb.setVisibility(View.VISIBLE);
            int percent = (int)((double)wrapper.finished * 100 / (double)wrapper.total);
            mpb.setProgress(percent);
        }else{
            mpb.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            if (btn.getTag() != null && btn.getTag() instanceof APPInfoWrapper){
                APPInfoWrapper wrapper = (APPInfoWrapper) btn.getTag();
                String fileUrl = wrapper.appInfo.getDownloadUrl();
                String localDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Constants.APKDir;
                String localName = wrapper.appInfo.getName();//fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                if (!localName.endsWith(".apk")){
                    localName = localName + ".apk";
                }
                String localPath = localDir + File.separator + localName;
                if (wrapper.curtDownloadState == APPDetailActivity.DownloadState.Normal
                        || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Pause
                        || wrapper.curtDownloadState == APPDetailActivity.DownloadState.Failed) {
                    //download
                    FileDownloadService.startFileDownload(v.getContext(), fileUrl, localDir, localName);
                    wrapper.curtDownloadState = APPDetailActivity.DownloadState.Starting;
                    notifyDataSetChanged();
                }else if (wrapper.curtDownloadState == APPDetailActivity.DownloadState.Downloading){
                    //pause
                    FileDownloadService.pauseFileDownload(v.getContext(), fileUrl, localDir, localName);
                }else if (wrapper.curtDownloadState == APPDetailActivity.DownloadState.Finish){
                    //install
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(localPath)), "application/vnd.android.package-archive");
                    v.getContext().startActivity(intent);
                }else if (wrapper.curtDownloadState == APPDetailActivity.DownloadState.Installed){
                    //LAUNCH
                    Intent LaunchIntent = v.getContext().getPackageManager().getLaunchIntentForPackage(wrapper.appInfo.getPackageName());
                    v.getContext().startActivity(LaunchIntent);
                }
            }
        }
    };
    
    private class APPInfoWrapper{
        
        APPInfo appInfo;
        APPDetailActivity.DownloadState curtDownloadState;
        long finished;
        long total;
    }
    
    public void onActivityCreate(){
        EventBus.getDefault().register(this);
    }
    
    public void onActivityDestroy(){
        EventBus.getDefault().unregister(this);
    }
    
    public void onEventMainThread(FileDownloadEvent event){
        final String fileUrl = event.getFileUrl();
        final int result = event.getResult();
        APPInfoWrapper wrapper = getWrapperItemByFileURL(fileUrl);
        if (wrapper == null)
            return;

        if (event.getResult() == FileDownloadEvent.EVENT_START){
            //debug("onEventMainThread, start");
            //setInstallButtonState(APPDetailActivity.DownloadState.Downloading);
            //setInstallBtnBgProgress(0);
            wrapper.curtDownloadState = APPDetailActivity.DownloadState.Starting;

        }else if (event.getResult() == FileDownloadEvent.EVENT_PAUSE){
            //debug("onEventMainThread, pause");
            //setInstallButtonState(APPDetailActivity.DownloadState.Pause);
            wrapper.curtDownloadState = APPDetailActivity.DownloadState.Pause;

        }else if (event.getResult() == FileDownloadEvent.EVENT_SUCCESS){
            //debug("onEventMainThread, success");
            //setInstallButtonState(APPDetailActivity.DownloadState.Finish);
            wrapper.curtDownloadState = APPDetailActivity.DownloadState.Finish;
            //setInstallBtnBgProgress(1);

        }else if (event.getResult() == FileDownloadEvent.EVENT_FAILED){
            //debug("onEventMainThread, success");
            //setInstallButtonState(APPDetailActivity.DownloadState.Failed);
            //setInstallBtnBgProgress(1);
            wrapper.curtDownloadState = APPDetailActivity.DownloadState.Failed;

        }else if (event.getResult() == FileDownloadEvent.EVENT_PROGRESS){
            //debug("onEventMainThread, progress, finished = " + event.getDownloadedSize() + ", total = " + event.getTotalSize());
            long finished = event.getDownloadedSize();
            long total = event.getTotalSize();
            double percent = (double)finished / (double)total;
            //setInstallBtnBgProgress(percent);
            wrapper.finished = event.getDownloadedSize();
            wrapper.total = event.getTotalSize();
            wrapper.curtDownloadState = APPDetailActivity.DownloadState.Downloading;
        }
        notifyDataSetChanged();
    }
}
