package com.sap.charging.util;

import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeUtil {
	
    public static final long MILLIS_PER_SECOND = 1000;
    public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    public static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
    public static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
    public static final long SECONDS_PER_MINUTE = 60;
    public static final long SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
    public static final long SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;
    
	/**
	 * Returns a continuous timestamp based on a discrete timeslot,
	 * in 15 minute intervals. Starts at 00:00. 
	 * timeslot=0 ==> 00:00:00
	 * timeslot=1 ==> 00:15:00
	 * @param timeslot
	 * @return
	 */
	public static LocalTime getTimestampFromTimeslot(int timeslot) {
		int h = timeslot / 4;
		int m = 15 * (timeslot % 4);
		int s = 0;
		return LocalTime.of(h, m, s);
	}
	
	public static int getSecondsFromTimeslot(int timeslot) {
		return timeslot * 15*60;
	}
	
	/**
	 * Returns a LocalTime object based on seconds of day
	 * @param seconds
	 * @return
	 */
	public static LocalTime getTimestampFromSeconds(int seconds) {
		return LocalTime.ofSecondOfDay(seconds);
	}
	
	/**
	 * Discretize continuous seconds into 15 min intervals.
	 * Discretizing is done by rounding (floor) down to nearest 15 mins. 
	 * @param seconds
	 * @return
	 */
	public static int getTimeslotFromSeconds(int seconds) {
		/*LocalTime t = getTimestampFromSeconds(seconds);
		int h = t.getHour();
		int m = (int) (Math.floor(t.getMinute() / 15));
		
		int timeslot = h*4 + m;*/
		int timeslot = seconds / 900;  // Much faster
		return timeslot;
	}
	
	/**
	 * Returns the delta in time in milliseconds of the two timestamps
	 * 
	 * Result can be negative if t2 is after t1
	 * @param otherEvent
	 * @return
	 */
	public static long getDifferenceInMS(LocalTime t1, LocalTime t2) {
		return t2.until(t1, MILLIS);
	}
	
	/**
	 * Returns midnight (start of day) of current day
	 * @return
	 */
	public static LocalDateTime getCurrentDayMidnight() {
		LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
		LocalDateTime result = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0);
		return result;
	}
	
}
