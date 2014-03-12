package alexclin.http;

import java.lang.reflect.Type;

import android.util.SparseArray;

import com.google.gson.reflect.TypeToken;

public class ApiInt {
	public static final String MAIN_API_Url = "http://192.168.2.20:8080/shouwang/box/";
	public static final String FileHost = "http://192.168.2.20:8080/shouwang/";
	
	public static final int UploadFile = 1;
	public static final int DownFile = 2;
	public static final int AUTH = 3;
	
	private static SparseArray<String> mUrlMap = new SparseArray<String>();
	private static SparseArray<Type> mClassMap = new SparseArray<Type>();
	static {
		mUrlMap.put(UploadFile, FileHost+"upload");
		/*********************************************************************/
		mClassMap.put(UploadFile, new TypeToken<ReturnBean<UploadResult>>(){}.getType());
	}

	public static String getUrl(int apiInt) {
		return mUrlMap.get(apiInt);
	}
	
	public static Type getApiType(int apiInt){
		return mClassMap.get(apiInt);
	}
}
