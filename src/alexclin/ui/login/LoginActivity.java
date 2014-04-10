package alexclin.ui.login;

import umeox.xmpp.aidl.IXMPPRosterService;
import umeox.xmpp.aidl.IXMPPStateCallback;
import umeox.xmpp.service.Smackable.ConnectionState;
import umeox.xmpp.service.XMPPService;
import umeox.xmpp.util.PrefConsts;
import umeox.xmpp.util.ToastUtil;
import umeox.xmpp.util.XmppHelper;
import alexclin.base.JimService;
import alexclin.ui.MainTabActivity;
import alexclin.ui.preferences.AccountPrefs;
import alexclin.xmpp.jabberim.R;
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
import android.widget.EditText;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

public class LoginActivity extends Activity implements ServiceConnection{
	@ViewInject(R.id.UserNameInput_Login)
	private EditText mUserEdt;
	@ViewInject(R.id.PasswordInput_Login)
	private EditText mPassEdt;

	private ProgressDialog mDailog;

	private SharedPreferences sp;
	private Intent mXmppServiceIntent;
	private IXMPPRosterService mStub;
	private IXMPPStateCallback.Stub callback;
	private Intent mActivityIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.act_login);
		super.onCreate(savedInstanceState);
		ViewUtils.inject(this);		
		mDailog = new ProgressDialog(this);
		mDailog.setMessage("登录中。。。");
		mXmppServiceIntent = new Intent(this, JimService.class);
		mActivityIntent = new Intent(this, MainTabActivity.class);
		sp = PreferenceManager.getDefaultSharedPreferences(this);		
		if(!autoLogin()){
			sp.edit().putBoolean(PrefConsts.CONN_STARTUP, false).commit();
			callback = new IXMPPStateCallback.Stub(){
				@Override
				public void connectionStateChanged(int connectionstate,String msg) throws RemoteException {
					mDailog.dismiss();
					if(connectionstate == ConnectionState.ONLINE.ordinal()){	
						startMainActivity();
					}else if(connectionstate == ConnectionState.OFFLINE.ordinal()){						
						ToastUtil.toastShort(LoginActivity.this, R.string.LoginFailedNotify);					
					}				
				}			
			};
			bindService(mXmppServiceIntent, this, BIND_AUTO_CREATE);
		}		
	}

	@OnClick({ R.id.Submit_Login, R.id.Register_Login, R.id.Forget_Login,R.id.ServerSetting_Login })
	public void onClick(View v) {
		Intent intent = new Intent();
		switch (v.getId()) {
		case R.id.Submit_Login:
			login();
			break;
		case R.id.Register_Login:
			intent.setClass(LoginActivity.this, RegisterActivity.class);
			startActivity(intent);
			break;
		case R.id.Forget_Login:
			intent.setClass(LoginActivity.this, ForgetPwActivity.class);
			startActivity(intent);
			break;
		case R.id.ServerSetting_Login:
			intent.setClass(LoginActivity.this, AccountPrefs.class);
			startActivity(intent);
			break;
		}
	}

	private boolean autoLogin() {
		String jid = sp.getString(PrefConsts.JID, "");
		String password = sp.getString(PrefConsts.PASSWORD, "");
		mUserEdt.setText(jid);
		mPassEdt.setText(password);
		String customServer = sp.getString(PrefConsts.CUSTOM_SERVER,
				"");
		if(!XmppHelper.verifyUserAndPW(jid, password)||customServer.equals("")){
			return false;
		}else{
			sp.edit().putBoolean(PrefConsts.CONN_STARTUP, true).commit();
			startService(mXmppServiceIntent);
			startActivity(mActivityIntent);
			this.finish();
			return true;
		}
	}
	
	

	private void startMainActivity() {
		if(!isFinishing()){
			sp.edit().putBoolean(PrefConsts.CONN_STARTUP, true).commit();			
			mXmppServiceIntent.setAction(XMPPService.ACTION_REFRESH);
			startService(mXmppServiceIntent);
			startActivity(mActivityIntent);
			finish();
		}		
	}

	@Override
	protected void onDestroy() {
		if(mStub!=null){
			try {
				mStub.unregisterStateCallback(callback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			unbindService(this);
		}		
		super.onDestroy();
	}

	private void login() {
		String customServer = sp.getString(PrefConsts.CUSTOM_SERVER,"");
		LogUtils.e("customServer:"+customServer);
		if(customServer.equals("")){
			showNotifyDialog(R.string.NoServerSetting);
			return;
		}
		String jid = mUserEdt.getText().toString().trim();
		String password = mPassEdt.getText().toString();
		if (XmppHelper.verifyUserAndPW(jid, password)) {
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(this);
			sp.edit().putString(PrefConsts.JID, jid)
					.putString(PrefConsts.PASSWORD, password).commit();
            //启动服务并监听登陆结果
			mXmppServiceIntent.setAction(XMPPService.ACTION_LOGIN);
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
			mStub.registerStateCallback(callback);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mStub = null;		
	}
	
}
