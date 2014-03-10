package alexclin.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

/* 
 * 如果输出文件被写入外部存储， 
 * 本应用需要具有写外部存储的权限,  
 * 还要具有录音的权限．这些权限必须 
 * 在AndroidManifest.xml 文件中声明，像这样： 
 * 
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /> 
 * <uses-permission android:name="android.permission.RECORD_AUDIO" /> 
 * 
 */
public class AudioUtil {
	private static final String LOG_TAG = AudioUtil.class.getName();
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.CHINESE);// 设置日期格式
	private MediaRecorder mRecorder;
	private String dir;
	private String recordPath;
	public AudioUtil(String dirPath) {
		this.dir = dirPath;
		checkDirExists(dir);
		mRecorder = new MediaRecorder();
	}
	
	private void checkDirExists(String dir) {
		File file = new File(dir);
		if(!file.exists()){
		   file.mkdirs();
		}		
	}

	public void release(){
		mRecorder.release();
	}

	public void startRecording() {		
		// 设置音源为Micphone
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		// 设置封装格式
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		this.recordPath = dir + "/" + getTime() + ".amr";
		mRecorder.setOutputFile(recordPath);
		// 设置编码格式
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}
		mRecorder.start();
	}

	public String stopRecording() {
		mRecorder.stop();
		mRecorder.reset();
		return recordPath;
	}

	private String getTime() {		
		return df.format(new Date());// new Date()为获取当前系统时间
	}
}
