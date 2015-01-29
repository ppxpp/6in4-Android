package cn.edu.bupt.niclab.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.MobclickAgentJSInterface;

import java.lang.ref.WeakReference;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ResourceFragment extends BaseFragment {

    private String tag = "ResourceFragment";
    private WebView mWebView;

    //private View content;
    private WeakReference<View> contentRef;
    private View waitView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (contentRef == null || contentRef.get() == null) {
            View view = inflater.inflate(R.layout.fragment_resource, container, false);
            waitView = view.findViewById(R.id.waitLayout);
            mWebView = (WebView) view.findViewById(R.id.webview);
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setAppCacheEnabled(true);
            SharedPreferences sp = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, getActivity().MODE_PRIVATE);
            long last_load_time = sp.getLong("load_time", -1);
            long curt_time = System.currentTimeMillis();
            long out_of_date = 1000 * 60 * 60 * 10;//10 hours
            if (last_load_time == -1 || curt_time - last_load_time >= out_of_date){
                mWebView.clearCache(true);
                SharedPreferences.Editor editor = sp.edit();
                editor.putLong("load_time", curt_time);
                editor.commit();
            }
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            //mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            MWebViewClient client = new MWebViewClient();
            mWebView.setWebViewClient(client);
            mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            mWebView.getSettings().setLoadWithOverviewMode(false);
            new MobclickAgentJSInterface(getActivity(),mWebView);
            String url = MobclickAgent.getConfigParams(getActivity(), Constants.PARAM_KEY_IPV6_RESOURCE);
            Log.d(tag, "res url = " + url);
            if (TextUtils.isEmpty(url)){
                url = Constants.URL_IPv6_RESOURCE;
                Log.d(tag, "get res url faild, set url to " + url);
            }
            //url = "http://6able.com/nav.html";
            mWebView.loadUrl(url);
            Log.d(tag, "onCreateView, this = " + this);
            contentRef = new WeakReference<View>(view);
        }else{
            Log.d(tag, "onCreateView, cache, this = " + this);
        }
        return contentRef.get();
    }

    private class MWebViewClient extends WebViewClient{
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(tag, "onPageStarted");
            waitView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url); //url为你要链接的地址
            Intent intent =new Intent(Intent.ACTION_VIEW, uri);
            if (getActivity() != null)
                startActivity(intent);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(tag,"onPageFinished");
            waitView.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.d(tag,"onReceivedError");
            waitView.setVisibility(View.GONE);
        }
    }

}
