package alexclin.base;

import alexclin.xmpp.jabberim.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
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

	@Override
	protected void initParam(SharedPreferences _prefs) {
		// TODO Auto-generated method stub
		
	}
	
	public static int getEditTextColor(Context ctx) {
		TypedValue tv = new TypedValue();
		boolean found = ctx.getTheme().resolveAttribute(android.R.attr.editTextColor, tv, true);
		if (found) {
			// SDK 11+
			return ctx.getResources().getColor(tv.resourceId);
		} else {
			// SDK < 11
			return ctx.getResources().getColor(android.R.color.primary_text_light);
		}
	}
}
