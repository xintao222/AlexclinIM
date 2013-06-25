package alexclin.frame;

import java.io.File;

import alexclin.frame.CrashHandler.CrashAble;
import alexclin.xmpp.androidclient.AimConfiguration;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;

public class Application extends android.app.Application implements CrashAble {
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
	private AimConfiguration mConfig;

	public Application() {
		super();
	}

	@Override
	public void onCreate() {
		mConfig = new AimConfiguration(
				PreferenceManager.getDefaultSharedPreferences(this));
		Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance(this, this));
	}

	public static Application getApp(Context ctx) {
		return (Application) ctx.getApplicationContext();
	}

	public static AimConfiguration getConfig(Context ctx) {
		return getApp(ctx).mConfig;
	}

	@Override
	public void sendErrorToServer(String errorInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub
		
	}
}
