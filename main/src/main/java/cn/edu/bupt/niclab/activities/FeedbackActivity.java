package cn.edu.bupt.niclab.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.DevReply;
import com.umeng.fb.model.Reply;
import com.umeng.fb.model.UserInfo;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import cn.edu.bupt.niclab.R;
import cn.edu.bupt.niclab.account.Account;
import cn.edu.bupt.niclab.account.AccountManager;
import cn.edu.bupt.niclab.account.LoginActivity;
import cn.edu.bupt.niclab.fragments.LogFragment;
import cn.edu.bupt.niclab.services.RecordInfoService;
import de.blinkt.openvpn.core.VpnStatus;

public class FeedbackActivity extends BaseActivity {


    private Account mAccount;
    private ListView mListView;
    private EditText mEditText;
    private Button mSendBtn;
    private ReplyListAadatper mAdatper;
    private FeedbackAgent agent;
    private Conversation defaultConversation;
    private TextView mHintTV;

    private Handler mHandler = new Handler();

    private AlphaAnimation inAnimation = new AlphaAnimation(0f, 1f);
    private AlphaAnimation outAnimation = new AlphaAnimation(1f, 0f);


    public FeedbackActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mEditText = (EditText) findViewById(R.id.edit_text);
        mSendBtn = (Button) findViewById(R.id.send_btn);
        mListView = (ListView) findViewById(R.id.list_view);
        mHintTV = (TextView) findViewById(R.id.hint);
        agent = new FeedbackAgent(this);
        defaultConversation = agent.getDefaultConversation();
        /*if (defaultConversation.getReplyList().size() == 0){
            mHintTV.setVisibility(View.VISIBLE);
            inAnimation.setDuration(1000);
            mHintTV.startAnimation(inAnimation);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    outAnimation.setDuration(1500);
                    outAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mHintTV.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    mHintTV.startAnimation(outAnimation);
                }
            }, 6000);
        }else{
            mHintTV.setVisibility(View.GONE);
        }*/
        mAdatper = new ReplyListAadatper();
        sync();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mAccount = AccountManager.getManager().getCurtAccount();
        if (mAccount == null && agent.getUserInfo() == null){
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.hint)
                    .setMessage(R.string.feedback_need_account)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(FeedbackActivity.this, LoginActivity.class));
                        }
                    }).create();
            dialog.show();
        }else{
            UserInfo info = agent.getUserInfo();
            if (info == null){
                info = new UserInfo();
                Map<String, String> contact = info.getContact();
                if (contact == null)
                    contact = new HashMap<String, String>();
                String contact_info = mAccount.userName + "(" + mAccount.userId + ")";
                contact.put("plain", contact_info);
                info.setContact(contact);
                agent.setUserInfo(info);
            }

            mListView.setAdapter(mAdatper);
            if (mAdatper.getCount() > 0)
                mListView.setSelection(mAdatper.getCount() - 1);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }else if (item.getItemId() == R.id.send_log){
            String log = getLogStr();
            RecordInfoService.startActionSendLog(this, log);
            defaultConversation.addUserReply("["+getString(R.string.send_log_file)+"]");
            sync();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.feedbackmenu, menu);
        return true;
    }

    public void onSendBtnCLicked(View view){
        String reply = mEditText.getText().toString().trim();
        if (TextUtils.isEmpty(reply)){
            return;
        }
        mEditText.getEditableText().clear();
        defaultConversation.addUserReply(reply);
        sync();
    }

    void sync() {
        Conversation.SyncListener listener = new Conversation.SyncListener() {

            @Override
            public void onSendUserReply(List<Reply> replyList) {
                mAdatper.notifyDataSetChanged();
                if (mAdatper.getCount() > 0)
                    mListView.setSelection(mAdatper.getCount() - 1);
            }

            @Override
            public void onReceiveDevReply(List<DevReply> replyList) {
                mAdatper.notifyDataSetChanged();
                if (mAdatper.getCount() > 0)
                    mListView.setSelection(mAdatper.getCount() - 1);
            }
        };
        defaultConversation.sync(listener);
    }


    public static final int TIME_FORMAT_NONE = 0;
    public static final int TIME_FORMAT_SHORT = 1;
    public static final int TIME_FORMAT_ISO = 2;

    String getLogStr() {
        Vector<VpnStatus.LogItem> allEntries=new Vector<VpnStatus.LogItem>();
        allEntries.clear();
        Collections.addAll(allEntries, VpnStatus.getlogbuffer());
        String str = "";
        for(VpnStatus.LogItem entry:allEntries) {
            str+=getTime(entry, TIME_FORMAT_ISO) + entry.getString(this) + '\n';
        }
        return str;
    }

    private String getTime(VpnStatus.LogItem le, int time) {
        if (time != TIME_FORMAT_NONE) {
            Date d = new Date(le.getLogtime());
            java.text.DateFormat timeformat;
            if (time== TIME_FORMAT_ISO)
                timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            else
                timeformat = DateFormat.getTimeFormat(this);

            return timeformat.format(d) + " ";

        } else {
            return "";
        }

    }

    private class ReplyListAadatper extends BaseAdapter{

        @Override
        public int getCount() {
            return defaultConversation.getReplyList() == null ? 0 : defaultConversation.getReplyList().size();
        }

        @Override
        public Object getItem(int position) {
            return defaultConversation.getReplyList().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            Reply reply = defaultConversation.getReplyList().get(position);
            if (reply instanceof DevReply){
                return 0;
            }else{
                return 1;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Reply reply = defaultConversation.getReplyList().get(position);
            View view = convertView;
            if (view == null){
                if (getItemViewType(position) == 0){
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.feedback_list_item_other, parent, false);
                }else{
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.feedback_list_item_self, parent, false);
                }
            }
            TextView content = (TextView) view.findViewById(R.id.content);
            TextView time = (TextView) view.findViewById(R.id.time);
            ImageView head = (ImageView) view.findViewById(R.id.head);
            content.setText(reply.getContent());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            time.setText(format.format(reply.getDatetime()));
            if (getItemViewType(position) == 1){
                //user
                if (mAccount == null){
                    head.setImageResource(R.drawable.ic_account_mything);
                }else{
                    ImageLoader.getInstance().displayImage(mAccount.headUrl, head);
                }
            }else{
                //dev
                head.setImageResource(R.drawable.icon);
            }
            return view;
        }
    }


}
