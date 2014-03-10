package alexclin.dialogs;


import org.jivesoftware.smack.util.StringUtils;

import umeox.xmpp.aidl.XMPPRosterServiceAdapter;
import umeox.xmpp.util.XmppHelper;
import alexclin.ui.FriendsFragment;
import alexclin.xmpp.jabberim.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AddRosterItemDialog extends AlertDialog implements
		DialogInterface.OnClickListener, TextWatcher {

	private FriendsFragment mMainWindow;
	private XMPPRosterServiceAdapter mServiceAdapter;

	private Button okButton;
	private EditText userInputField;
	private EditText aliasInputField;
	private EditText verifyMsgEdt;
	private GroupNameView mGroupNameView;

	public AddRosterItemDialog(FriendsFragment mainWindow,
			XMPPRosterServiceAdapter serviceAdapter) {
		super(mainWindow.getActivity());
		mMainWindow = mainWindow;
		mServiceAdapter = serviceAdapter;

		setTitle(R.string.addFriend_Title);

		LayoutInflater inflater = (LayoutInflater) mainWindow.getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View group = inflater.inflate(R.layout.addrosteritemdialog, null, false);
		setView(group);

		userInputField = (EditText)group.findViewById(R.id.AddContact_EditTextField);
		aliasInputField = (EditText)group.findViewById(R.id.AddContactAlias_EditTextField);
		verifyMsgEdt = (EditText)group.findViewById(R.id.VerifyMsg_EditTextField);

		mGroupNameView = (GroupNameView)group.findViewById(R.id.AddRosterItem_GroupName);
		mGroupNameView.setGroupList(mMainWindow.getRosterGroups());

		setButton(BUTTON_POSITIVE, mainWindow.getString(android.R.string.ok), this);
		setButton(BUTTON_NEGATIVE, mainWindow.getString(android.R.string.cancel),
				(DialogInterface.OnClickListener)null);

	}
	public AddRosterItemDialog(FriendsFragment mainWindow,
			XMPPRosterServiceAdapter serviceAdapter, String jid) {
		this(mainWindow, serviceAdapter);
		userInputField.setText(StringUtils.parseName(jid));
	}

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		okButton = getButton(BUTTON_POSITIVE);
		afterTextChanged(userInputField.getText());

		userInputField.addTextChangedListener(this);
	}

	public void onClick(DialogInterface dialog, int which) {
		mServiceAdapter.addRosterItem(userInputField.getText()
				.toString(), aliasInputField.getText().toString(),
				mGroupNameView.getGroupName(),verifyMsgEdt.getText().toString());
	}

	public void afterTextChanged(Editable s) {
		if(!s.toString().matches(" *")){
			okButton.setEnabled(true);
			userInputField.setTextColor(XmppHelper.getEditTextColor(mMainWindow.getActivity()));
		}else{
			okButton.setEnabled(false);
			userInputField.setTextColor(Color.RED);
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

}
