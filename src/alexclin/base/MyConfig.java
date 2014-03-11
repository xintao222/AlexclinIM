package alexclin.base;

import alexclin.xmpp.jabberim.R;
import android.content.SharedPreferences;
import umeox.xmpp.base.BaseConfig;

public class MyConfig extends BaseConfig {

	public MyConfig(SharedPreferences _prefs) {
		super(_prefs);
	}

	@Override
	public String getIdentityName() {
		return "JabberIM";
	}

	@Override
	public String getIdentityType() {
		return "phone";
	}

	public int getTheme() {
		if (theme.equals("light")) {
			return R.style.YaximLightTheme;
		} else {
			return R.style.YaximDarkTheme;
		}
	}
}
