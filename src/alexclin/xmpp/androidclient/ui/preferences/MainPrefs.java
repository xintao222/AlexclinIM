package alexclin.xmpp.androidclient.ui.preferences;

import alexclin.xmpp.androidclient.R;
import alexclin.xmpp.androidclient.Application;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class MainPrefs extends PreferenceActivity{
	public void onCreate(Bundle savedInstanceState) {
		setTheme(Application.getConfig(this).getTheme());
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.act_mainprefs);
	}

}
