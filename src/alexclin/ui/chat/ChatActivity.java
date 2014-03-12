package alexclin.ui.chat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import umeox.xmpp.aidl.IXMPPChatService;
import umeox.xmpp.aidl.XMPPChatServiceAdapter;
import umeox.xmpp.base.BaseApp;
import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.ChatProvider.ChatConstants;
import umeox.xmpp.data.RosterProvider;
import umeox.xmpp.util.PrefConsts;
import alexclin.base.GlobalConfig;
import alexclin.base.JimService;
import alexclin.http.BaseApi.Callback;
import alexclin.http.FileApi;
import alexclin.http.ReturnBean;
import alexclin.http.UploadResult;
import alexclin.mediatransfer.FileMessager;
import alexclin.mediatransfer.AudioUtil;
import alexclin.mediatransfer.MediaDB;
import alexclin.ui.MainTabActivity;
import alexclin.xmpp.jabberim.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;

/**
 * 聊天界面
 * 
 * @author alex
 * 
 */
/* recent ClipboardManager only available since API 11 */
public class ChatActivity extends SherlockListActivity implements
		OnKeyListener, TextWatcher, OnClickListener, OnTouchListener, Callback, OnPreparedListener {

	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class
			.getName() + ".username";
	public static final String INTENT_EXTRA_MESSAGE = ChatActivity.class
			.getName() + ".message";
	
	private static final String[] PROJECTION_FROM = new String[] {
		ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
		ChatProvider.ChatConstants.DIRECTION,
		ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
		ChatProvider.ChatConstants.DELIVERY_STATUS };

	private static final String TAG = "yaxim.ChatWindow";
	
	

	private ContentObserver mContactObserver = new ContactObserver();
	private TextView mTitle;
	private TextView mSubTitle;
	
	private Button mSendButton;
	private EditText mChatInput;
	
	private Button mVoiceBtn;
	
	private String mWithJabberID = null;
	private String mUserScreenName = null;
	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	private XMPPChatServiceAdapter mServiceAdapter;
	
	private AudioUtil mAudioUtil;
	public MediaDB mAudioDb;
	private MediaPlayer mPlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(((BaseApp) getApplication()).getConfig().getTheme());
		super.onCreate(savedInstanceState);
		mAudioUtil = new AudioUtil(GlobalConfig.VoiceCaheDir);
		mAudioDb= new MediaDB(this);
		mPlayer = new MediaPlayer();
		mPlayer.setOnPreparedListener(this);

		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.act_chat);

		getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mContactObserver);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		registerForContextMenu(getListView());
		setContactFromUri();
		registerXMPPService();
		setSendButton();
		setUserInput();

		String titleUserid;
		if (mUserScreenName != null) {
			titleUserid = mUserScreenName;
		} else {
			titleUserid = mWithJabberID;
		}

		setCustomTitle(titleUserid);
		Cursor c = managedQuery(ChatProvider.CONTENT_URI, PROJECTION_FROM,
				ChatConstants.JID + "='" + mWithJabberID + "'", null, null);
		setListAdapter(new ChatWindowAdapter(this,c));
	}

	@Override
	public void onDestroy() {
		mAudioUtil.release();
		mAudioDb.close();
		mPlayer.release();
		super.onDestroy();
		if (hasWindowFocus())
			unbindXMPPService();
		getContentResolver().unregisterContentObserver(mContactObserver);
	}

	private void setCustomTitle(String title) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.chat_action_title, null);
		mTitle = (TextView) layout.findViewById(R.id.action_bar_title);
		mSubTitle = (TextView) layout.findViewById(R.id.action_bar_subtitle);
		mTitle.setText(title);

		setTitle(null);
		getSupportActionBar().setCustomView(layout);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateContactStatus();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, JimService.class);
		Uri chatURI = Uri.parse(mWithJabberID);
		mServiceIntent.setData(chatURI);
		mServiceIntent.setAction("org.yaxim.androidclient.XMPPSERVICE");

		mServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				mServiceAdapter = new XMPPChatServiceAdapter(
						IXMPPChatService.Stub.asInterface(service),
						mWithJabberID);

				mServiceAdapter.clearNotifications(mWithJabberID);
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}

		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private void setSendButton() {
		mSendButton = (Button) findViewById(R.id.Chat_SendButton);
		mSendButton.setOnClickListener(this);
		mSendButton.setEnabled(false);
		mVoiceBtn = (Button)findViewById(R.id.Chat_VoiceBtn);
		findViewById(R.id.Chat_SwitchBtn).setOnClickListener(this);
		mVoiceBtn.setOnTouchListener(this);
	}

	private void setUserInput() {
		Intent i = getIntent();
		mChatInput = (EditText) findViewById(R.id.Chat_UserInput);
		mChatInput.addTextChangedListener(this);
		if (i.hasExtra(INTENT_EXTRA_MESSAGE)) {
			mChatInput.setText(i.getExtras().getString(INTENT_EXTRA_MESSAGE));
		}
	}

	private void setContactFromUri() {
		Intent i = getIntent();
		mWithJabberID = i.getDataString().toLowerCase(Locale.CHINA);
		if (i.hasExtra(INTENT_EXTRA_USERNAME)) {
			mUserScreenName = i.getExtras().getString(INTENT_EXTRA_USERNAME);
		} else {
			mUserScreenName = mWithJabberID;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.chat_menu, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		View target = ((AdapterContextMenuInfo) menuInfo).targetView;
//		TextView from = (TextView) target.findViewById(R.id.chat_from);
		//TODO 
//		getMenuInflater().inflate(R.menu.chat_contextmenu, menu);
//		if (!from.getText().equals(getString(R.string.chat_from_me))) {
//			menu.findItem(R.id.chat_contextmenu_resend).setEnabled(false);
//		}
	}

	private CharSequence getMessageFromContextMenu(MenuItem item) {
		View target = ((AdapterContextMenuInfo) item.getMenuInfo()).targetView;
//		TextView message = (TextView) target.findViewById(R.id.chat_message);
//		return message.getText();
		//TODO 
		return null;
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.chat_contextmenu_copy_text:
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(getMessageFromContextMenu(item));
			return true;
		case R.id.chat_contextmenu_resend:
			sendMessage(getMessageFromContextMenu(item).toString());
			Log.d(TAG, "resend!");
			return true;
		default:
			return super.onContextItemSelected((android.view.MenuItem) item);
		}
	}

	private void sendMessageIfNotNull() {
		if (mChatInput.getText().length() >= 1) {
			sendMessage(mChatInput.getText().toString());
		}
	}

	private void sendMessage(String message) {
		mChatInput.setText(null);
		mSendButton.setEnabled(false);
		mServiceAdapter.sendMessage(mWithJabberID, message);
		if (!mServiceAdapter.isServiceAuthenticated())
			showToastNotification(R.string.toast_stored_offline);
	}	

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER) {
			sendMessageIfNotNull();
			return true;
		}
		return false;

	}

	public void afterTextChanged(Editable s) {
		if (mChatInput.getText().length() >= 1) {
			mChatInput.setOnKeyListener(this);
			mSendButton.setEnabled(true);
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}

	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	private void showToastNotification(int message) {
		Toast toastNotification = Toast.makeText(this, message,
				Toast.LENGTH_SHORT);
		toastNotification.show();
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainTabActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static final String[] STATUS_QUERY = new String[] {
			RosterProvider.RosterConstants.STATUS_MODE,
			RosterProvider.RosterConstants.STATUS_MESSAGE, };

	private void updateContactStatus() {
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI,
				STATUS_QUERY, RosterProvider.RosterConstants.JID + " = ?",
				new String[] { mWithJabberID }, null);
		int MODE_IDX = cursor
				.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor
				.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			Log.d(TAG, "contact status changed: " + status_mode + " "
					+ status_message);
			mSubTitle.setVisibility((status_message != null && status_message
					.length() != 0) ? View.VISIBLE : View.GONE);
			mSubTitle.setText(status_message);
			getSupportActionBar().setIcon(getDrawableId(status_mode));
		}
		cursor.close();
	}

	private int getDrawableId(int status_mode) {
		switch (status_mode) {
		case PrefConsts.offline:
			return R.drawable.ic_status_offline;
		case PrefConsts.dnd:
			return R.drawable.ic_status_dnd;
		case PrefConsts.xa:
			return R.drawable.ic_status_xa;
		case PrefConsts.away:
			return R.drawable.ic_status_away;
		case PrefConsts.available:
			return R.drawable.ic_status_available;
		case PrefConsts.chat:
			return R.drawable.ic_status_chat;
		}
		return R.drawable.ic_status_offline;
	}

	private class ContactObserver extends ContentObserver {
		public ContactObserver() {
			super(new Handler());
		}

		public void onChange(boolean selfChange) {
			Log.d(TAG, "ContactObserver.onChange: " + selfChange);
			updateContactStatus();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.Chat_SendButton:
			sendMessageIfNotNull();
			break;
		case R.id.Chat_SwitchBtn:
			if(mVoiceBtn.getVisibility()==View.GONE){
				mVoiceBtn.setVisibility(View.VISIBLE);
				mSendButton.setVisibility(View.GONE);
				mChatInput.setVisibility(View.GONE);
			}else{
				mVoiceBtn.setVisibility(View.GONE);
				mSendButton.setVisibility(View.VISIBLE);
				mChatInput.setVisibility(View.VISIBLE);
			}
			break;
		}		
	}

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		if(e.getAction()==MotionEvent.ACTION_DOWN){
			mAudioUtil.startRecording();
		}else if(e.getAction()==MotionEvent.ACTION_UP){
			String filePath = mAudioUtil.stopRecording();
			FileApi.uploadFileAsync(this, filePath,filePath);
		}
		return true;
	}

	@Override
	public void onLoading(long total, long current, int apiInt,Object tag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStart(int apiInt,Object tag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFailure(int error, int apiInt,Object tag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSuccess(Object msg, int apiInt,Object tag) {
		ReturnBean<UploadResult> rb = (ReturnBean<UploadResult>)msg;
		if(rb.getErrorCode()==0){
			mAudioDb.insert(rb.getResult().getUrl(), (String)tag);
			sendMessage(FileMessager.wrapMessage(rb.getResult().getUrl(), FileMessager.TYPE_VOICE));
		}else{
			
		}		
	}
	
	public void playMedia(String path){
		if(mPlayer.isPlaying()){
			mPlayer.stop();
		}
		mPlayer.reset();
		try {
			mPlayer.setDataSource(path);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mPlayer.prepareAsync();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();		
	}
}
