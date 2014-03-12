package alexclin.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

public class UnitUtil {
	public static int formatDipToPx(Context context, float dip) {
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		return (int) Math.ceil(dip * dm.density);
	}
}
