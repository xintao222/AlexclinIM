package alexclin.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

	private ToastUtil() {
	}
	
	public static final void toastShort(Context context,String msg){
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}
	
	public static final void toastShort(Context context,int msg){
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

}
