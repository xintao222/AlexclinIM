package alexclin.xmpp.androidclient;

import java.io.File;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;

public class Application extends android.app.Application {
	public static final int SDK_INT = Integer.valueOf(Build.VERSION.SDK);
	// identity name and type, see:
	// http://xmpp.org/registrar/disco-categories.html
	public static final String XMPP_IDENTITY_NAME = "yaxim";
	public static final String XMPP_IDENTITY_TYPE = "phone";
	/**
	 * Path to the trust store in this system.
	 */
	public final static String TRUST_STORE_PATH;
	static {
		String path = System.getProperty("javax.net.ssl.trustStore");
		if (path == null)
			TRUST_STORE_PATH = System.getProperty("java.home") + File.separator
					+ "etc" + File.separator + "security" + File.separator
					+ "cacerts.bks";
		else
			TRUST_STORE_PATH = path;
	}
	private YaximConfiguration mConfig;

	public Application() {
		super();
	}

	@Override
	public void onCreate() {
		mConfig = new YaximConfiguration(
				PreferenceManager.getDefaultSharedPreferences(this));
	}

	public static Application getApp(Context ctx) {
		return (Application) ctx.getApplicationContext();
	}

	public static YaximConfiguration getConfig(Context ctx) {
		return getApp(ctx).mConfig;
	}
}
