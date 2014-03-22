package alexclin.ui.preferences;

import umeox.xmpp.base.BaseApp;
import alexclin.base.MyApplication;
import alexclin.xmpp.jabberim.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class MainPrefs extends PreferenceActivity{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.act_mainprefs);
	}

}
