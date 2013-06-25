package alexclin.xmpp.androidclient.ui.login;

import alexclin.xmpp.androidclient.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * 登陆界面
 * @author alex
 *
 */
public class LoginActivity extends Activity implements OnClickListener{
	private EditText mUserNameEdt;
	private EditText mPasswordEdt;
	private Button mSubmitBtn;
	private Button mCancelBtn;
	private Button mRegisterBtn;
	private Button mFrogetPWBtn;
	private Button mServSetBtn;
	private ProgressDialog mDailog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.act_login);
		super.onCreate(savedInstanceState);
		initViewAndListener();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void initViewAndListener() {
		mUserNameEdt = (EditText) findViewById(R.id.UserNameInput_Login);
		mPasswordEdt = (EditText) findViewById(R.id.PasswordInput_Login);
		mSubmitBtn = (Button) findViewById(R.id.Submit_Login);
		mCancelBtn = (Button) findViewById(R.id.Cancel_Login);
		mRegisterBtn = (Button) findViewById(R.id.Register_Login);
		mFrogetPWBtn = (Button) findViewById(R.id.Forget_Login);
		mServSetBtn = (Button) findViewById(R.id.ServerSetting_Login);
		mSubmitBtn.setOnClickListener(this);
		mCancelBtn.setOnClickListener(this);
		mRegisterBtn.setOnClickListener(this);
		mFrogetPWBtn.setOnClickListener(this);
		mServSetBtn.setOnClickListener(this);
		mDailog = new ProgressDialog(this);
		mDailog.setMessage("登录中。。。");
	}

	private void login() {
//		String userName = mUserNameEdt.getText().toString().toString();
//		String password = mPasswordEdt.getText().toString();
//		mDailog.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.Submit_Login:
            login();
			break;
		case R.id.Cancel_Login:
            this.finish();
			break;
		case R.id.Register_Login:

			break;
		case R.id.Forget_Login:

			break;
		case R.id.ServerSetting_Login:

			break;
		}

	}
}
