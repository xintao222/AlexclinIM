package alexclin.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import android.content.Context;
import android.os.Environment;

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
	/** 前缀 */
	private static String mPrefix;
	/** 是否初始化了文件记录log */
	private static boolean isInit;
	private static Calendar mCal;
	/** 日志文件的路径 */
	private static String mLogFilePath;
	/** 打印到文件的输出流 */
	private static PrintWriter mPrintWriter;
	/** 是否debug */
	private static boolean isDebug;
	/** 是否记录所有日志 */
	private static boolean isRecord;

	public static void init(Context context, String prefix, boolean debug,boolean record){
		if (prefix != null) {
			mPrefix = prefix + ":";
		} else {
			mPrefix = "";
		}
		String logFileName = context.getPackageName().replace(".", "_");
		createLogFile(logFileName);
		isInit = true;
		isDebug = debug;
		isRecord =record;
	}
	
	public static void init(Context context,String prefix, boolean debug){
		init(context,prefix,debug,false);
	}
	
	public static void init(Context context,boolean debug){
		init(context,null,debug,false);
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

	public static void d(String tag, String msg) {
		if (isDebug)
			android.util.Log.d(tag, msg);
	}

	public static void w(String tag, String msg) {
		if (isDebug)
			android.util.Log.w(tag, msg);
		if (isInit&&isRecord)
			logFile("w", tag, msg);
	}

	public static void e(String tag, String msg) {
		if (isDebug)
			android.util.Log.e(tag, msg);
		if (isInit&&isRecord)
			logFile("e", tag, msg);
	}

	public static void v(String tag, String msg) {
		if (isDebug)
			android.util.Log.v(tag, msg);
		if (isInit&&isRecord)
			logFile("v", tag, msg);
	}

	/**
	 * 打印并记录异常信息到文件
	 * @param e
	 */
	public static void logError(Throwable e) {
		logError("", e);
	}

	public static void logError(String string, Throwable e) {
		if(isDebug){
			android.util.Log.e(string, e+"");
			e.printStackTrace();
		}
		if (e != null && isInit) {	
			mPrintWriter.write(string+":::");
			e.printStackTrace(mPrintWriter);			
		}
	}

	public static void close() {
		if (mPrintWriter != null) {
			mPrintWriter.flush();
			mPrintWriter.close();
		}
	}

	private static void createLogFile(String logFileName) {
		mCal = Calendar.getInstance();
		mLogFilePath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/Log/log/";
		logFileName = logFileName + "_" + mCal.get(Calendar.YEAR) + "-"
				+ (mCal.get(Calendar.MONTH) + 1) + "-"
				+ mCal.get(Calendar.DATE) + ".txt";
		try {
			File dirFile = new File(mLogFilePath);
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
			File file = new File(mLogFilePath + logFileName);
			if (!file.exists()) {
				if (file.createNewFile()) {
					mPrintWriter = new PrintWriter(new FileOutputStream(
							mLogFilePath), true);
				}
			} else {
				mPrintWriter = new PrintWriter(new FileOutputStream(
						mLogFilePath, true), true);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void logFile(String string, String tag, String msg) {
		if (mPrintWriter != null) {
			Date date = new Date();
			String log = string + " " + date.toLocaleString() + " " + tag + " "
					+ msg + "\r\n";
			mPrintWriter.write("UTF-8");
			mPrintWriter.write(log);
		}
	}

	private static String getObjectName(Object obj) {
		return obj.getClass().getSimpleName();
	}
}
