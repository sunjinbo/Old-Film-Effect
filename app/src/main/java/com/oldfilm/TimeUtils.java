package com.oldfilm;

import java.math.BigDecimal;

/**
 * TimeUtils class.
 */
public class TimeUtils {

    public static String formatNumberToHourMinuteSecond(Double dateDou){
        String ft = "00:00:00";//没匹配上时:1.等于0时; 2.大于等于86400时.
        BigDecimal d = new BigDecimal(dateDou).setScale(0, BigDecimal.ROUND_HALF_UP);//四舍五入
        int date = Integer.valueOf(d.toString());
        if(date > 0 && date < 60){
            ft = date < 10 ? "00:00:0" + date : "00:00:" + date;
        }else if(date >= 60 && date < 3600){
            ft = "00:" + (date/60>=10?date/60:"0"+date/60) + ":" + (date%60>=10?date%60:"0"+date%60);
        }else if(date >= 3600 && date < 86400 ){
            ft = (date/3600>=10?date/3600:"0"+date/3600) + ":" + (date%3600/60>=10?date%3600/60:"0"+date%3600/60) + ":" + (date%60>=10?date%60:"0"+date%60);
        }
        return ft;
    }
}
