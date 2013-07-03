package alexclin.xmpp.androidclient.ui.login;

import alexclin.util.ToastUtil;
import alexclin.xmpp.androidclient.IXMPPRosterCallback;
import alexclin.xmpp.androidclient.R;
import alexclin.xmpp.androidclient.service.IXMPPRosterService;
import alexclin.xmpp.androidclient.service.XMPPService;
import alexclin.xmpp.androidclient.ui.MainTabActivity;
import alexclin.xmpp.androidclient.ui.preferences.AccountPrefs;
import alexclin.xmpp.androidclient.util.PreferenceConstants;
import alexclin.xmpp.androidclient.util.XMPPHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * 登陆界面
 * 
 * @author alex
 * 
 */
public class LoginActivity extends Activity implements OnClickListener, ServiceConnection{
	private EditText mUserNameEdt;
	private EditText mPasswordEdt;
	private Button mSubmitBtn;
	private Button mRegisterBtn;
	private Button mFrogetPWBtn;
	private Button mServSetBtn;
	private ProgressDialog mDailog;
	
	SharedPreferences sp ;
	private Intent mXmppServiceIntent;
	private IXMPPRosterService mStub;
	private IXMPPRosterCallback.Stub callback;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.act_login);
		super.onCreate(savedInstanceState);
		initViewAndListener();
		mXmppServiceIntent = new Intent(this, XMPPService.class);
		sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		if(autoLogin(sp)){
			return;
		}else{
			sp.edit().putBoolean(PreferenceConstants.CONN_STARTUP, false).commit();
		}
		callback = new IXMPPRosterCallback.Stub(){
			@Override
			public void connectionStatusChanged(boolean isConnected,
					boolean willReconnect) throws RemoteException {
				mDailog.dismiss();
				if(isConnected){	
					sp.edit().putBoolean(PreferenceConstants.CONN_STARTUP, true).commit();
					startActivity(new Intent(LoginActivity.this, MainTabActivity.class));
					LoginActivity.this.finish();
				}else{
					ToastUtil.toastShort(LoginActivity.this, R.string.LoginFailedNotify);
					
				}				
			}			
		};
		bindService(mXmppServiceIntent, this, BIND_AUTO_CREATE);
	}

	private boolean autoLogin(SharedPreferences sp) {
		String jid = sp.getString(PreferenceConstants.JID, "");
		String password = sp.getString(PreferenceConstants.PASSWORD, "");
		String customServer = sp.getString(PreferenceConstants.CUSTOM_SERVER,
				"");
		if(!XMPPHelper.verifyUserAndPW(jid, password)||customServer.equals("")){
			return false;
		}else{
			startService(mXmppServiceIntent);
			startActivity(new Intent(this, MainTabActivity.class));
			this.finish();
			return true;
		}		
	}

	@Override
	protected void onDestroy() {
		if(mStub!=null){
			try {
				mStub.unregisterRosterCallback(callback);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			unbindService(this);
		}
		super.onDestroy();
	}

	private void initViewAndListener() {
		mUserNameEdt = (EditText) findViewById(R.id.UserNameInput_Login);
		mPasswordEdt = (EditText) findViewById(R.id.PasswordInput_Login);
		mSubmitBtn = (Button) findViewById(R.id.Submit_Login);
		mRegisterBtn = (Button) findViewById(R.id.Register_Login);
		mFrogetPWBtn = (Button) findViewById(R.id.Forget_Login);
		mServSetBtn = (Button) findViewById(R.id.ServerSetting_Login);
		mSubmitBtn.setOnClickListener(this);
		mRegisterBtn.setOnClickListener(this);
		mFrogetPWBtn.setOnClickListener(this);
		mServSetBtn.setOnClickListener(this);
		mDailog = new ProgressDialog(this);
		mDailog.setMessage("登录中。。。");
	}

	private void login() {
		String customServer = sp.getString(PreferenceConstants.CUSTOM_SERVER,
				"");
		if(customServer.equals("")){
			showNotifyDialog(R.string.NoServerSetting);
			return;
		}
		String jid = mUserNameEdt.getText().toString().trim();
		String password = mPasswordEdt.getText().toString();
		if (XMPPHelper.verifyUserAndPW(jid, password)) {
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(this);
			sp.edit().putString(PreferenceConstants.JID, jid)
					.putString(PreferenceConstants.PASSWORD, password).commit();
            //启动服务并监听登陆结果
			startService(mXmppServiceIntent);
			mDailog.show();
		} else {
			showNotifyDialog(R.string.WrongUserOrPW);
		}
	}

	private void showNotifyDialog(int res) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.OperationNotify)
				.setMessage(res).setPositiveButton(R.string.ok, null).create().show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.Submit_Login:
			login();
			break;
		case R.id.Register_Login:
			startActivity(new Intent(this, RegisterActivity.class));
			break;
		case R.id.Forget_Login:
			startActivity(new Intent(this, ForgetPwActivity.class));
			break;
		case R.id.ServerSetting_Login:
			startActivity(new Intent(this, AccountPrefs.class));
			break;
		}
	}
	
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			stopService(mXmppServiceIntent);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mStub = IXMPPRosterService.Stub.asInterface(service);
		try {
			mStub.registerRosterCallback(callback);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mStub = null;		
	}
	
}
