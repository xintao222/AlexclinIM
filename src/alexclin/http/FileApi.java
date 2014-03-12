package alexclin.http;

import java.io.File;

import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

public class FileApi extends BaseApi{	

	public static final void uploadFileAsync(Callback callback,String path,Object tag) {
		RequestParams params = new RequestParams();		
		params.addBodyParameter("file", new File(path));
		getHttp().send(HttpRequest.HttpMethod.POST,
				ApiInt.getUrl(ApiInt.UploadFile), params,
				new StringCallback(callback, ApiInt.UploadFile,tag));
	}
	
	public static final HttpHandler<File> downloadFile(Callback callback,String url,String path){
		return getHttp().download(url, path, true, new DownCallback(callback, ApiInt.DownFile,url));
	}
	
	public static final HttpHandler<File> downloadFile(Callback callback,String url,String path,Object tag){
		return getHttp().download(url, path, true, new DownCallback(callback, ApiInt.DownFile,tag));
	}

	private static class DownCallback extends RequestCallBack<File> {
		private Callback callback;
		private int apiInt;
		private Object tag;

		public DownCallback(Callback callback, int apiInt) {
			super();
			this.callback = callback;
			this.apiInt = apiInt;
		}
		
		public DownCallback(Callback callback, int apiInt,Object tag) {
			super();
			this.callback = callback;
			this.apiInt = apiInt;
			this.tag = tag;
		}

		@Override
		public void onLoading(long total, long current, boolean isUploading) {
			callback.onLoading(total, current, apiInt,tag);
		}

		@Override
		public void onStart() {
			callback.onStart(apiInt,tag);
		}

		@Override
		public void onFailure(HttpException he, String msg) {
			callback.onFailure(he.getExceptionCode(), apiInt,tag);
		}

		@Override
		public void onSuccess(ResponseInfo<File> info) {
			callback.onSuccess(info.result.getAbsolutePath(), apiInt,tag);
		}
	}
}
