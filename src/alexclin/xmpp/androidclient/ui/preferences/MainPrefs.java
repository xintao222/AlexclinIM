package alexclin.xmpp.androidclient.ui.preferences;

import alexclin.frame.Application;
import alexclin.xmpp.androidclient.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class MainPrefs extends PreferenceActivity{
	public void onCreate(Bundle savedInstanceState) {
		setTheme(Application.getConfig(this).getTheme());
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.act_mainprefs);
	}

}
