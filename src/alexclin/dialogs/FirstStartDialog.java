package alexclin.dialogs;


import umeox.xmpp.aidl.XMPPRosterServiceAdapter;
import umeox.xmpp.base.UmeoxException;
import umeox.xmpp.util.PrefConsts;
import umeox.xmpp.util.XmppHelper;
import alexclin.ui.MainTabActivity;
import alexclin.ui.preferences.AccountPrefs;
import alexclin.util.StringUtil;
import alexclin.xmpp.jabberim.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class FirstStartDialog extends AlertDialog implements DialogInterface.OnClickListener,
		TextWatcher {

	private MainTabActivity mainWindow;
	private Button mOkButton;
	private EditText mEditJabberID;
	private EditText mEditPassword;
	private CheckBox mCreateAccount;
	private int themedTextColor;

	public FirstStartDialog(MainTabActivity mainWindow,
			XMPPRosterServiceAdapter serviceAdapter) {
		super(mainWindow);
		this.mainWindow = mainWindow;

		setTitle(R.string.StartupDialog_Title);

		LayoutInflater inflater = (LayoutInflater) mainWindow
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View group = inflater.inflate(R.layout.dialog_firststart, null, false);
		setView(group);

		setButton(BUTTON_POSITIVE, mainWindow.getString(android.R.string.ok), this);
		setButton(BUTTON_NEUTRAL, mainWindow.getString(R.string.StartupDialog_advanced), this);

		mEditJabberID = (EditText) group.findViewById(R.id.StartupDialog_JID_EditTextField);
		mEditPassword = (EditText) group.findViewById(R.id.StartupDialog_PASSWD_EditTextField);
		mCreateAccount = (CheckBox) group.findViewById(R.id.create_account);
		mEditJabberID.addTextChangedListener(this);
		TypedValue tv = new TypedValue();
		boolean found = mainWindow.getTheme().resolveAttribute(android.R.attr.editTextColor, tv, true);
		if (found) {
			// SDK 11+
			themedTextColor = mainWindow.getResources().getColor(tv.resourceId);
		} else {
			// SDK < 11
			themedTextColor = mainWindow.getResources().getColor(android.R.color.primary_text_light);
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mOkButton = getButton(BUTTON_POSITIVE);
		mOkButton.setEnabled(false);
	}


	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case BUTTON_POSITIVE:
			verifyAndSavePreferences();
			boolean create_account = mCreateAccount.isChecked();
			mainWindow.startConnection(create_account);
			break;
		case BUTTON_NEUTRAL:
			verifyAndSavePreferences();
			mainWindow.startActivity(new Intent(mainWindow, AccountPrefs.class));
			break;
		}
	}

	private void verifyAndSavePreferences() {
		String password = mEditPassword.getText().toString();
		String jabberID = mEditJabberID.getText().toString();

		savePreferences(jabberID, password);
		cancel();
	}

	public void afterTextChanged(Editable s) {
		if(StringUtil.isNullOrEmpty(s.toString())){
			mOkButton.setEnabled(true);
			mEditJabberID.setTextColor(themedTextColor);
		}else{
			mOkButton.setEnabled(false);
			mEditJabberID.setTextColor(Color.RED);
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	private void savePreferences(String jabberID, String password) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mainWindow);
		Editor editor = sharedPreferences.edit();

		editor.putString(PrefConsts.JID, jabberID);
		editor.putString(PrefConsts.PASSWORD, password);
		editor.putInt(PrefConsts.PORT, PrefConsts.DEFAULT_PORT);
		editor.commit();
	}

}
