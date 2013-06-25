package alexclin.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import android.content.Context;

/**
 * 
 * @Title: LogUtil.java
 * @Package com.fullteem.utils
 * @Description: Android Log工具的封装，支持将Log打印到SD卡文件,
 * @author Alexclin
 * @date 2013-4-10 下午5:58:42
 * @version V1.0
 */
public final class LogUtil {
	/**前缀*/
	private static String mPrefix;
	/**是否初始化了文件记录log*/
	private static boolean isInit;
	private static Calendar mCal;
	/**日志文件的路径*/
	private static String mLogFilePath;
	/**打印到文件的输出流*/
	private static PrintStream mPrintStream;
	/**是否debug*/
	private static boolean isDebug;

	public static void init(Context context, String prefix, String appDir,boolean debug) {
		if (appDir != null) {
			if (prefix != null) {
				mPrefix = prefix + ":";
			}else{
				mPrefix = "";
			}
			String logFileName = context.getPackageName().replace(".", "_");
			createLogFile(logFileName, appDir);
			isInit = true;
			isDebug =debug;
		}
	}

	private static void createLogFile(String logFileName, String appDir) {
		mCal = Calendar.getInstance();
		mLogFilePath = appDir + "/log/" + logFileName + "_"
				+ mCal.get(Calendar.YEAR) + "-"
				+ (mCal.get(Calendar.MONTH) + 1) + "-"
				+ mCal.get(Calendar.DATE) + ".txt";
		try {
			File dirFile = new File(appDir+ "/log/");
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
			File file = new File(mLogFilePath);
			if (!file.exists()) {
				if (file.createNewFile()) {
					mPrintStream = new PrintStream(new FileOutputStream(
							mLogFilePath), true);
				}
			} else {
				mPrintStream = new PrintStream(new FileOutputStream(
						mLogFilePath, true), true);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void logFile(String string, String tag, String msg) {
		if (mPrintStream != null) {
			Date date = new Date();
			String log = string + " " + date.toLocaleString() + " " + tag + " "
					+ msg + "\n";
			try {
				mPrintStream.write(log.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static final void i(Object caller, String message) {
		LogUtil.i(mPrefix + getObjectName(caller), message);
	}

	public static final void v(Object caller, String message) {
		LogUtil.v(mPrefix + getObjectName(caller), message);
	}

	public static final void d(Object caller, String message) {
		LogUtil.d(mPrefix + getObjectName(caller), message);
	}

	public static final void w(Object caller, String message) {
		LogUtil.w(mPrefix + getObjectName(caller), message);
	}

	public static final void e(Object caller, String message) {
		LogUtil.e(mPrefix + getObjectName(caller), message);
	}

	public static void i(String tag, String msg) {
		if (isDebug)
			android.util.Log.i(tag, msg);
	}

	public static void w(String tag, String msg) {
		if (isDebug)
			android.util.Log.w(tag, msg);
		if (isInit)
			logFile("w", tag, msg);
	}

	public static void e(String tag, String msg) {
		if (isDebug)
			android.util.Log.e(tag, msg);
		if (isInit)
			logFile("e", tag, msg);
	}

	public static void d(String tag, String msg) {
		if (isDebug)
			android.util.Log.d(tag, msg);
	}

	public static void v(String tag, String msg) {
		if (isDebug)
			android.util.Log.v(tag, msg);
		if (isInit)
			logFile("v", tag, msg);
	}

	public static void logError(String string, Throwable e) {
		LogUtil.e(mPrefix + " error", e.getLocalizedMessage());
		if (e != null && isInit) {
			e.printStackTrace(mPrintStream);
			StackTraceElement[] ste = e.getStackTrace();
			for (StackTraceElement s : ste) {
				LogUtil.e(mPrefix, s.toString());
			}
		}
	}

	public static void close() {
		if (mPrintStream != null) {
			mPrintStream.flush();
			mPrintStream.close();
		}
	}

	public static void logError(Throwable e) {
		logError("", e);
	}

	private static String getObjectName(Object obj) {
		return obj.getClass().getSimpleName();
	}
}
