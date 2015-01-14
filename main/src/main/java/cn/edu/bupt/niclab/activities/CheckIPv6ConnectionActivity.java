package cn.edu.bupt.niclab.activities;

import android.content.Intent;
import android.os.*;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Process;

import cn.edu.bupt.niclab.Constants;
import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.services.RecordInfoService;

public class CheckIPv6ConnectionActivity extends BaseActivity {

    private View mItemView, mResultView;
    private Button mStartBtn;
    private TextView mResultTV;
    private final int FeedbackTag = 1, DiagnoseTag = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_ipv6_connection);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mItemView = findViewById(R.id.itemlist);
        mResultView = findViewById(R.id.result_container);
        mStartBtn = (Button) findViewById(R.id.start_btn);
        mResultTV = (TextView) findViewById(R.id.result);
        mResultTV.setMovementMethod(new ScrollingMovementMethod());

        mItemView.setVisibility(View.VISIBLE);
        mResultView.setVisibility(View.INVISIBLE);
    }

    public void startDiagnose(View v){
        if (mStartBtn.getTag() != null && (Integer)mStartBtn.getTag() == FeedbackTag) {
            //feedback
            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.putExtra("content", mResultTV.getText().toString());
            startActivity(intent);
        }else{
            switchToResultView();
            mStartBtn.setEnabled(false);
            mStartBtn.setText(getString(R.string.diagnosing));
            new MAsyncTask().execute(null);
        }
    }

    private void switchToResultView(){
        mItemView.setVisibility(View.INVISIBLE);
        mResultView.setVisibility(View.VISIBLE);
    }

    class MAsyncTask extends AsyncTask {

        @Override
        protected void onProgressUpdate(Object[] values) {
            String result = (String) values[0];
            mResultTV.append(result);
        }

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                String[] cmd = {"sh",  "-c", "ip -6 a | grep net6"};
                publishProgress("Local IPv6 address\n");
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuffer sb = new StringBuffer();
                String line = null;
                while ((line = reader.readLine()) != null){
                    line = line.trim();
                    //line = inet6 fe80::364d:f7ff:fe53:3e98/64 scope link
                    String[] segs = line.split(" ");
                    if (segs.length > 2){
                        sb.append(segs[1] + "\n");
                    }
                }
                reader.close();
                publishProgress(sb.toString());
                //get dns server
                String[] cmd2 = {"sh", "-c", "getprop | grep dns"};
                publishProgress("\n\nDNS Server:\n");
                process = Runtime.getRuntime().exec(cmd2);
                process.waitFor();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                sb = new StringBuffer();
                line = null;
                while ((line = reader.readLine()) != null){
                    line = line.trim();
                    //line = [dhcp.wlan0.dns1]: [192.168.2.1]
                    if (!line.contains("[]")){
                        sb.append(line + "\n");
                    }
                }
                reader.close();
                publishProgress(sb.toString());
                //ping host
                sb = new StringBuffer();

                String url = MobclickAgent.getConfigParams(CheckIPv6ConnectionActivity.this, Constants.PARAM_KEY_IPV6_HOST_FOR_TEST);
                if (TextUtils.isEmpty(url)){
                    url = Constants.URL_IPv6_HOST_FOR_TEST;
                }
                String command = "ping6 -c 4 " + url;
                publishProgress("\n\n" + command + "\n");
                process = Runtime.getRuntime().exec(command);
                process.waitFor();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                line = null;
                while ((line = reader.readLine()) != null){
                    sb.append(line + "\n");
                }
                reader.close();
                if (sb.length() == 0){
                    //no output in stdout
                    reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    line = null;
                    while ((line = reader.readLine()) != null){
                        sb.append(line + "\n");
                    }
                    reader.close();
                }
                publishProgress(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            mStartBtn.setEnabled(true);
            mStartBtn.setText(getString(R.string.diagnose_feedback));
            mStartBtn.setTag(FeedbackTag);
        }
    };

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}
