package alexclin.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.Handler;

public class DownloadUtil implements Runnable {
	public static final int OK = 0;
	public static final int NET_ERROR = 1;
	public static final int IO_ERROR = 2;
	public static final int URL_ERROR = 3;
	public static final int UNKOWN_ERROR = 3;
	private static Executor mPool;
	private static Handler mHandler;
	private String url;
	private String savePath;
	private CallBack callBack;
	

	public static boolean downloadFile(String url, String savePath,
			CallBack callBack) {
		if (mPool == null) {
			mPool = Executors.newFixedThreadPool(5);
		}
		if(mHandler == null){
			mHandler = new Handler();
		}
		if (checkPath(savePath)) {
			mPool.execute(new DownloadUtil(url, savePath, callBack));
			return true;
		} else {
			return false;
		}
	}

	public static interface CallBack {
		void onDownloadComplete(String url, String savePath, int errorCode);
	}

	public static boolean checkPath(String path) {
		File file = new File(path);
		if (file.canWrite() && file.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

	private DownloadUtil(String url, String savePath, CallBack callBack) {
		this.url = url;
		this.savePath = savePath;
		this.callBack = callBack;
	}

	@Override
	public void run() {
		int errorCode = OK;
		try {
			String fileName = url.substring(url.lastIndexOf("/"), url.length());
			if (savePath.endsWith("/")) {
				savePath = savePath + fileName;
			} else {
				savePath = savePath + File.separator + fileName;
			}
			File outfile = new File(savePath);
			if (outfile.exists()) {
				outfile.delete();
			}
			outfile.createNewFile();
			HttpURLConnection conn = null;
			URL fileurl = new URL(url);
			conn = (HttpURLConnection) fileurl.openConnection();
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(45000);
			conn.connect();
			int bytesum = 0;
			int byteread = 0;
			InputStream is = conn.getInputStream();
			FileOutputStream fos = new FileOutputStream(outfile);
			byte[] buffer = new byte[1204];
			while ((byteread = is.read(buffer)) != -1) {
				bytesum += byteread;
				System.out.println(bytesum);
				fos.write(buffer, 0, byteread);
			}
			is.close();
			fos.close();
		} catch (IndexOutOfBoundsException iobe) {
				errorCode = URL_ERROR;	
		} catch (FileNotFoundException fnfe) {
			errorCode = IO_ERROR;
		} catch (IOException fnfe) {
			errorCode = NET_ERROR;
		}catch (Exception e) {
			errorCode = UNKOWN_ERROR;
		}
		if(errorCode!=0){
			new File(savePath).delete();
		}
		final int error = errorCode;
		mHandler.post(new Runnable() {			
			@Override
			public void run() {
				callBack.onDownloadComplete(url, savePath, error);				
			}
		});		
	}
}
