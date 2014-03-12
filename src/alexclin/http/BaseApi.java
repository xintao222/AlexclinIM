package alexclin.http;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

/**
 * @Title: BaseHttpApi.java
 * @Description: TODO
 * @author 洪锦群
 * @date 2014-3-10 上午10:34:56
 * @version V1.0
 */
public class BaseApi {

	private static final int TIMEOUT = 10 * 1000;

	private static HttpUtils httpUtils;
	private static Gson gson;

	protected static HttpUtils getHttp() {
		if (httpUtils == null) {
			httpUtils = new HttpUtils();
			httpUtils.configTimeout(TIMEOUT);
		}
		return httpUtils;
	}	
	
	protected static Gson getGson(){
		if(gson==null){
			GsonBuilder gsonb = new GsonBuilder();
			gsonb.setDateFormat("yyyy-MM-dd HH:mm:ss");
			gson = gsonb.create();
		}
		return gson;
	}
	
	public interface Callback {
		public void onLoading(long total, long current, int apiInt,Object tag);

		public void onStart(int apiInt,Object tag);

		public void onFailure(int error, int apiInt,Object tag);

		public void onSuccess(Object result, int apiInt,Object tag);
	}
	
	protected static class StringCallback extends RequestCallBack<String> {
		private Callback callback;
		private int apiInt;
		private Object tag;

		public StringCallback(Callback callback, int apiInt) {
			super();
			this.callback = callback;
			this.apiInt = apiInt;
		}
		
		public StringCallback(Callback callback, int apiInt,Object tag) {
			super();
			this.callback = callback;
			this.apiInt = apiInt;
			this.tag = tag;
		}

		@Override
		public void onFailure(HttpException he, String msg) {
			callback.onFailure(he.getExceptionCode(), apiInt,tag);
		}

		@Override
		public void onSuccess(ResponseInfo<String> info) {
			Type t = ApiInt.getApiType(apiInt);
			if(t!=null){
				callback.onSuccess(getGson().fromJson(info.result,t), apiInt,tag);
			}else{
				callback.onSuccess(info.result, apiInt,tag);
			}			
		}

		@Override
		public void onLoading(long total, long current, boolean isUploading) {
			callback.onLoading(total, current, apiInt,tag);
		}

		@Override
		public void onStart() {
			callback.onStart(apiInt,tag);
		}
	}
}
