package cn.edu.bupt.niclab.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import cn.edu.bupt.niclab.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CurtDownloadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CurtDownloadFragment extends BaseFragment {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CurtDownloadFragment.
     */
    public static CurtDownloadFragment newInstance() {
        CurtDownloadFragment fragment = new CurtDownloadFragment();
        return fragment;
    }

    public CurtDownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    

    
    ListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_curt_download, container, false);
        mListView = (ListView) view.findViewById(R.id.list_view);
        
        return view;
    }


}
