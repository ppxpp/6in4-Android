package cn.edu.bupt.niclab.widget;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.entity.APPInfo;
import cn.edu.bupt.niclab.fragments.Utils;

/**
* Created by zhengmeng on 2015/1/19.
*/
public class APPInfoListAdapter extends BaseAdapter {

    private List<APPInfo> mAPPInfoList;
    public void setData(List<APPInfo> data){
        mAPPInfoList = data;
    }

    @Override
    public int getCount() {
        return mAPPInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mAPPInfoList.get(position);
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
        APPInfo appInfo = (APPInfo) getItem(position);
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageResource(R.drawable.icon_default);
        ImageLoader.getInstance().displayImage(appInfo.getIcon(), icon);
        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(appInfo.getName());
        TextView label = (TextView) view.findViewById(R.id.label);
        label.setText(String.format(getString(R.string.app_label), appInfo.getDownloadCount(), Utils.getReadableSize(appInfo.getSizeInKB())));
        Button installBtn = (Button) view.findViewById(R.id.install_btn);
        if (Utils.checkInstallAPP(getActivity(), appInfo.getPackageName())){
            installBtn.setText(R.string.launch);
            installBtn.setBackgroundResource(R.drawable.app_item_launch_btn_bg);
        }else{
            installBtn.setText(R.string.install);
            installBtn.setBackgroundResource(R.drawable.app_item_install_btn_bg);
        }
        installBtn.setTag(appInfo);
        installBtn.setOnClickListener(onClickListener);
        return view;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            if (btn.getTag() != null && btn.getTag() instanceof APPInfo){
                APPInfo appInfo = (APPInfo) btn.getTag();
                if (btn.getText().equals(getString(R.string.launch))){
                    Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(appInfo.getPackageName());
                    startActivity(intent);
                }
            }
        }
    };
}
