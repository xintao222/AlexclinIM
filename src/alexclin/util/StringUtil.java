package alexclin.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import android.text.Html;
import android.text.Spanned;

/**
 * 
 * @Title: StringUtil.java
 * @Package com.fullteem.utils
 * @Description: 字符串的处理类
 * @author zhouxin@easier.cn
 * @date 2012-11-22 下午4:35
 * @version V1.0
 */
public class StringUtil {
	private final static String[] CHINA_NUMBER = { "一", "二", "三", "四", "五",
			"六", "七", "八", "九", "十" };

	/**
	 * 判断是否为null或空值
	 * 
	 * @param str
	 *            String
	 * @return true or false
	 */
	public static boolean isNullOrEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}

	/**
	 * 判断str1和str2是否相同
	 * 
	 * @param str1
	 *            str1
	 * @param str2
	 *            str2
	 * @return true or false
	 */
	public static boolean equals(String str1, String str2) {
		return str1 == str2 || str1 != null && str1.equals(str2);
	}

	/**
	 * 判断str1和str2是否相同(不区分大小写)
	 * 
	 * @param str1
	 *            str1
	 * @param str2
	 *            str2
	 * @return true or false
	 */
	public static boolean equalsIgnoreCase(String str1, String str2) {
		return str1 != null && str1.equalsIgnoreCase(str2);
	}

	/**
	 * 判断字符串str1是否包含字符串str2
	 * 
	 * @param str1
	 *            源字符串
	 * @param str2
	 *            指定字符串
	 * @return true源字符串包含指定字符串，false源字符串不包含指定字符串
	 */
	public static boolean contains(String str1, String str2) {
		return str1 != null && str1.contains(str2);
	}

	/**
	 * 判断字符串是否为空，为空则返回一个空值，不为空则返回原字符串
	 * 
	 * @param str
	 *            待判断字符串
	 * @return 判断后的字符串
	 */
	public static String getString(String str) {
		return str == null ? "" : str;
	}

	private final static Pattern emailer = Pattern
			.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss",Locale.CHINA);
	private final static SimpleDateFormat dateFormater2 = new SimpleDateFormat(
			"yyyy-MM-dd",Locale.CHINA);
	private final static SimpleDateFormat dateFormater3 = new SimpleDateFormat(
			"HH:mm",Locale.CHINA);

	/**
	 * 将字符串转位日期类型
	 * 
	 * @param sdate
	 * @return
	 */
	public static Date toDate(String sdate) {
		try {
			return dateFormater.parse(sdate);
		} catch (ParseException e) {
			try {
				return dateFormater.parse(new Date(toLong(sdate) * 1000L)
						.toGMTString());
			} catch (ParseException e1) {
				return null;
			}
		}
	}

	public static String convertimeStumpToDate2(String time) {
		try {
			return dateFormater2.format(new Date(toLong(time) * 1000L));
		} catch (Exception e) {
			return null;
		}

	}

	public static String convertTimeStumpToDate(String time) {
		try {
			return dateFormater.format(new Date(toLong(time) * 1000L));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 以友好的方式显示时间
	 * 
	 * @param sdate
	 * @return
	 */
	public static String toFriendlyTimeStr(String sdate) {
		Calendar post = Calendar.getInstance();
		post.setTimeInMillis(toLong(sdate) * 1000L);

		Date time = post.getTime();

		String ftime = "";
		Calendar cal = Calendar.getInstance();

		// 判断是否是同一天
		String curDate = dateFormater2.format(cal.getTime());
		String paramDate = dateFormater2.format(time);
		if (curDate.equals(paramDate)) {
			int hour = (int) ((cal.getTimeInMillis() - time.getTime()) / 3600000);
			if (hour == 0)
				ftime = Math.max(
						(cal.getTimeInMillis() - time.getTime()) / 60000, 1)
						+ "分钟前";
			else
				ftime = hour + "小时前";
			return ftime;
		}

		long lt = time.getTime() / 86400000;
		long ct = cal.getTimeInMillis() / 86400000;
		int days = (int) (ct - lt);
		if (days == 0) {
			int hour = (int) ((cal.getTimeInMillis() - time.getTime()) / 3600000);
			if (hour == 0)
				ftime = Math.max(
						(cal.getTimeInMillis() - time.getTime()) / 60000, 1)
						+ "分钟前";
			else
				ftime = hour + "小时前";
		} else if (days == 1) {
			ftime = "昨天" + dateFormater3.format(time);
		} else if (days == 2) {
			ftime = "前天" + dateFormater3.format(time);
		} else if (days > 2 && days <= 10) {
			ftime = days + "天前";
		} else if (days > 10) {
			ftime = dateFormater2.format(time);
		}
		return ftime;
	}

	public static String getCurrentTimeStr(){
		return getTimeStr(System.currentTimeMillis());
	}
	
	public static String getTimeStr(long time){
		return dateFormater.format(new Date(time));
	}
	/**
	 * 以友好的方式显示时间
	 * 
	 * @param sdate
	 * @return
	 */
	public static String getChineseTime(long time) {
		if (time == 604800000) {
			return "一周";
		} else if (time == 86400000) {
			return "一天";
		}
		int yearCount = 0;
		int monthCount = 0;
		int dayCount = 0;
		int hourCount = 0;
		int minuteCount = 0;
		int secondCount = 0;
		yearCount = (int) time / (86400000 * 365);
		time = (int) time % (86400000 * 365);
		monthCount = (int) time / (86400000 * 30);
		time = (int) time % (86400000 * 30);
		dayCount = (int) time / (86400000);
		time = (int) time % (86400000);
		hourCount = (int) time / (3600000);
		time = (int) time % (3600000);
		minuteCount = (int) time / (60000);
		time = (int) time % (60000);
		secondCount = (int) time / (1000);
		String message = "";
		if (yearCount != 0) {
			message += yearCount + "年";
		}
		if (monthCount != 0) {
			message += monthCount + "月";
		}
		if (dayCount != 0) {
			message += dayCount + "天";
		}
		if (hourCount != 0) {
			message += hourCount + "小时";
		}
		if (minuteCount != 0) {
			message += minuteCount + "分钟";
		}
		if (secondCount != 0) {
			message += secondCount + "秒";
		}
		if (isNullOrEmpty(message)) {
			message = "即时";
		}
		return message;
	}

	public static String toFriendlyNumStr(long l) {
		if (l < 100)
			return l + "";
		else if (l < 1000)
			return l / 100 + "百";
		else if (l < 10000)
			return l / 1000 + "千";
		else if (l < 1000000)
			return l / 10000 + "万";
		else
			return "百万之上";
	}

	/**
	 * 判断给定字符串时间是否为今日
	 * 
	 * @param sdate
	 * @return boolean
	 */
	public static boolean isToday(String sdate) {
		boolean b = false;
		Date time = toDate(sdate);
		Date today = new Date();
		if (time != null) {
			String nowDate = dateFormater2.format(today);
			String timeDate = dateFormater2.format(time);
			if (nowDate.equals(timeDate)) {
				b = true;
			}
		}
		return b;
	}

	/**
	 * 判断给定字符串是否空白串。 空白串是指由空格、制表符、回车符、换行符组成的字符串 若输入字符串为null或空字符串，返回true
	 * 
	 * @param input
	 * @return boolean
	 */
	public static boolean isEmpty(String input) {
		if (input == null || "".equals(input))
			return true;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 判断是不是一个合法的电子邮件地址
	 * 
	 * @param email
	 * @return
	 */
	public static boolean isEmail(String email) {
		if (email == null || email.trim().length() == 0)
			return false;
		return emailer.matcher(email).matches();
	}

	/**
	 * 字符串转整数
	 * 
	 * @param str
	 * @param defValue
	 * @return
	 */
	public static int toInt(String str, int defValue) {
		try {
			return Integer.parseInt(str.replace("+", ""));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defValue;
	}

	/**
	 * 对象转整数
	 * 
	 * @param obj
	 * @return 转换异常返回 0
	 */
	public static int toInt(Object obj) {
		if (obj == null)
			return 0;
		return toInt(obj.toString(), 0);
	}

	/**
	 * 对象转整数
	 * 
	 * @param obj
	 * @return 转换异常返回 0
	 */
	public static long toLong(String obj) {
		try {
			return Long.parseLong(obj);
		} catch (Exception e) {
		}
		return 0;
	}

	/**
	 * 字符串转布尔值
	 * 
	 * @param b
	 * @return 转换异常返回 false
	 */
	public static boolean toBool(String b) {
		try {
			return Boolean.parseBoolean(b);
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * HTML化字符串
	 * 
	 * @param str
	 * @return
	 */
	public static Spanned fromHtml(String str) {
		if (!isEmpty(str)) {
			return Html.fromHtml(str);
		} else
			return Html.fromHtml("");
	}

	/**
	 * 判断两个字符串是否相等
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isEquals(String a, String b) {
		return a.compareTo(b) == 0;
	}

	/**
	 * 判断是否手机号
	 * 
	 * @param inputStr
	 * @return
	 */
	public static boolean isPhoneNumber(String inputStr) {
		if (inputStr == null) {
			return false;
		}
		if (inputStr.startsWith("1") && inputStr.length() == 11) {
			return true;
		}
		return false;
	}
	
	public static String getChineseNumber(int i){
		if(i==10){
			return CHINA_NUMBER[9];
		}
		String value = String.valueOf(i);
		char[] array = value.toCharArray();
		value = "";
		for(char c:array){
			value+=CHINA_NUMBER[Integer.parseInt(String.valueOf(c))-1];
		}
		return value;
	}
}
