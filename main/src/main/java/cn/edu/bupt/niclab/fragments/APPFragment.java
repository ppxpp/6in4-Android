package cn.edu.bupt.niclab.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.edu.bupt.niclab.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class APPFragment extends Fragment {


    public APPFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ap, container, false);
    }


}
