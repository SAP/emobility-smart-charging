package com.sap.charging.util.configuration;

import java.io.IOException;

public class Options {
  public final int nrSlots;
  public final int slotsPerHour;
  public final int slotMinutes;
  public final int plannedHours;
  public final double chargingEfficiency;
  
  public Options(int hours, int slotMinutes){
    this(hours, slotMinutes, 0.85);
  }
  
  public Options(int hours, int slotMinutes, double efficiency){
    slotsPerHour = 60/slotMinutes;
    nrSlots = hours*slotsPerHour;
    this.slotMinutes = slotMinutes;
    this.plannedHours = hours;
    this.chargingEfficiency = efficiency;
  }
  
  private static Options instance = null;
  
  public static Options get(){
    if(instance==null){
      Configuration conf;
      try {
        conf = Configuration.getDefault();
        instance = new Options(conf.getInteger("default.charging.planhours", 12), 
                                conf.getInteger("default.charging.slotminutes", 15),
                                conf.getDouble("default.charging.efficiency", 0.85));
      } catch (IOException | ExceptionInInitializerError | NullPointerException e) {
        //e.printStackTrace();
    	System.out.println("Error reading standard configuration file.");
        instance = new Options(12, 15);
      }
    }
    return instance;
  }
  
  public static void set(int hours, int slotMinutes, double efficiency){
    instance = new Options(hours, slotMinutes, efficiency);
  }
}
