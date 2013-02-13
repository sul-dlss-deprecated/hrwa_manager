package edu.columbia.ldpd.hrwa;

import java.util.concurrent.TimeUnit;

public class TimeStringFormat {

    public static String getTimeString(long totalSeconds) {
        int days = (int) TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) -
                     TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) - 
                      TimeUnit.DAYS.toMinutes(days) -
                      TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.SECONDS.toSeconds(totalSeconds) -
                      TimeUnit.DAYS.toSeconds(days) -
                      TimeUnit.HOURS.toSeconds(hours) - 
                      TimeUnit.MINUTES.toSeconds(minutes);
        return  days + (days == 1 ? " day " : " days ") +
                hours + (hours == 1 ? " hour " : " hours ") +
                minutes + (minutes == 1 ? " minute " : " minutes ") +
                seconds + (seconds == 1 ? " second" : " seconds");
    }

}