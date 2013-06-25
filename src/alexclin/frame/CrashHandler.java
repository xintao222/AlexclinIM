package alexclin.frame;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class CrashHandler implements UncaughtExceptionHandler {
	private static final SimpleDateFormat dataFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss", Locale.CHINA);
	// 需求是 整个应用程序 只有一个 MyCrash-Handler
	private static CrashHandler myCrashHandler;
	private Context context;	
	private CrashAble mCrashAble;
	private UncaughtExceptionHandler mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();

	private CrashHandler(Context context,CrashAble crashAble){
		this.context = context;
		this.mCrashAble = crashAble;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		recordCrashToFile(ex);		
		try {
			// 1.获取当前程序的版本号. 版本的id
			PackageManager pm = context.getPackageManager();
			PackageInfo pinfo = pm.getPackageInfo(context.getPackageName(),
					PackageManager.GET_CONFIGURATIONS);
			String versionName = pinfo.versionName;
			int versionCode = pinfo.versionCode;
			String versioninfo = versionName + "/" + versionCode;
			// 2.获取手机的硬件信息.
			String mobileInfo = getMobileInfo();
			// 3.把错误的堆栈信息 获取出来
			String errorinfo = getErrorInfo(ex);
			if (errorinfo.length() > 1000) {
				errorinfo = errorinfo.substring(0, 1000);
			}
			// 4.把所有的信息 还有信息对应的时间 提交到服务器
			StringBuffer sb = new StringBuffer();
			sb.append("date:").append(dataFormat.format(new Date()))
					.append("\r\n").append("versioninfo:").append(versioninfo)
					.append("\r\n").append("mobileInfo:").append(mobileInfo)
					.append("\r\n").append("errorinfo:").append(errorinfo);
			mCrashAble.sendErrorToServer(sb.toString());
			// 干掉当前的程序
			mCrashAble.exit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mDefaultHandler.uncaughtException(thread, ex);
	}

	/**
	 * 将程序Crash的异常信息保存到文件中
	 * @param tw
	 */
	private void recordCrashToFile(Throwable tw) {
		Log.e("CrashException", "AppCrash NeedToRecordForCheck");
		if (tw == null) return;		
		String errorlog = context.getPackageName().replace(".", "_")+"_crashlog.txt";
		String savePath = "";
		String logFilePath = "";
		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			//判断是否挂载了SD卡
			String storageState = Environment.getExternalStorageState();		
			if(storageState.equals(Environment.MEDIA_MOUNTED)){
				savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Log/Crash/";
				File file = new File(savePath);
				if(!file.exists()){
					file.mkdirs();
				}
				logFilePath = savePath + errorlog;
			}
			//没有挂载SD卡，无法写文件
			if(logFilePath == ""){
				return;
			}
			File logFile = new File(logFilePath);
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			fw = new FileWriter(logFile,true);
			pw = new PrintWriter(fw);
			pw.println("---------START-----------"+(new Date().toLocaleString())+"---------------------\r\n");
			tw.printStackTrace(pw);
			pw.println("\r\n---------END  -----------"+(new Date().toLocaleString())+"---------------------\r\n");	
			pw.close();
			fw.close();
		} catch (Exception ex) {
			return;
		}finally{ 
			if(pw != null){ pw.close(); } 
			if(fw != null){ try { fw.close(); } catch (IOException ex) { }}
		}
		return;
		
	}

	/**
	 * 获取错误的信息
	 * 
	 * @param arg1
	 * @return
	 */
	private String getErrorInfo(Throwable arg1) {
		Writer writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		arg1.printStackTrace(pw);
		pw.close();
		String error = writer.toString();
		return error;
	}

	/**
	 * 获取手机的硬件信息
	 * 
	 * @return
	 */
	private String getMobileInfo() {
		StringBuffer sb = new StringBuffer();
		// 通过反射获取系统的硬件信息
		try {
			Field[] fields = Build.class.getDeclaredFields();
			for (Field field : fields) {
				// 暴力反射 ,获取私有的信息
				field.setAccessible(true);
				String name = field.getName();
				String value = field.get(null).toString();
				sb.append(name + "=" + value);
				sb.append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * 获取CrashHandler的单例对象
	 * @param context 设备上下文
	 * @param crashAble 程序崩溃时的回调，不能为null
	 * @return
	 */
	public static synchronized CrashHandler getInstance(Context context,CrashAble crashAble) {
		if (myCrashHandler != null) {
			return myCrashHandler;
		} else {
			myCrashHandler = new CrashHandler(context,crashAble);
			return myCrashHandler;
		}
	}

	public interface CrashAble {
		public void sendErrorToServer(String errorInfo);
		public void exit();
	}
	
	
}