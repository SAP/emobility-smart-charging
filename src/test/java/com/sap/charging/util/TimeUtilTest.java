package com.sap.charging.util;

import java.time.LocalTime;

import org.junit.Assert;
import org.junit.jupiter.api.Test;


import com.sap.charging.util.TimeUtil;

public class TimeUtilTest {


	@Test
	public void testTimestampFromTimeslot() {
		LocalTime t1 = TimeUtil.getTimestampFromTimeslot(0);
		Assert.assertEquals("00:00", t1.toString());
		
		LocalTime t2 = TimeUtil.getTimestampFromTimeslot(3);
		Assert.assertEquals("00:45", t2.toString());
		
		LocalTime t3 = TimeUtil.getTimestampFromTimeslot(20);
		Assert.assertEquals("05:00", t3.toString());
		
		LocalTime t4 = TimeUtil.getTimestampFromTimeslot(37);
		Assert.assertEquals("09:15", t4.toString());
		
	}

	@Test
	public void testTimestampFromSeconds() {
		LocalTime t1 = TimeUtil.getTimestampFromSeconds(0);
		Assert.assertEquals("00:00", t1.toString());
		
		LocalTime t2 = TimeUtil.getTimestampFromSeconds(10);
		Assert.assertEquals("00:00:10", t2.toString());
		
		LocalTime t3 = TimeUtil.getTimestampFromSeconds(70);
		Assert.assertEquals("00:01:10", t3.toString());
		
		LocalTime t4 = TimeUtil.getTimestampFromSeconds(3661);
		Assert.assertEquals("01:01:01", t4.toString());
	}
	
	@Test
	public void testTimeslotFromSeconds() {
		int t1 = TimeUtil.getTimeslotFromSeconds(0);
		Assert.assertEquals(0, t1);
	
		int t2 = TimeUtil.getTimeslotFromSeconds(899);
		Assert.assertEquals(0, t2);
		
		int t3 = TimeUtil.getTimeslotFromSeconds(900);
		Assert.assertEquals(1, t3);
		
		int t4 = TimeUtil.getTimeslotFromSeconds(901);
		Assert.assertEquals(1, t4);
		
		int t5 = TimeUtil.getTimeslotFromSeconds(7200);
		Assert.assertEquals(8, t5);
	}
	
	@Test 
	public void testTimeDiffInMS() {
		new TimeUtil();
		long diff = TimeUtil.getDifferenceInMS(LocalTime.of(1, 0, 0),
											   LocalTime.of(0, 0,0));
		Assert.assertEquals(60*60*1000, diff);
	}
}
