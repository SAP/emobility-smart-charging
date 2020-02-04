package com.sap.charging.util.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Configuration {
  private static final Map<String, Configuration> confStore = new ConcurrentHashMap<>();
  
  private final String path;
  private Map<String, String> properties;
  
  private Configuration(String path){
    this.path = path;
    properties = new ConcurrentHashMap<String, String>();
  }
  
  public static Configuration getDefault() throws IOException{
    return getConfiguration("/configuration.properties");
  }
  
  public static Configuration getConfiguration(String configPath) throws IOException{
    Configuration conf = confStore.get(configPath);
    if(conf==null){
      conf = new Configuration(configPath);
      
      loadConfiguration(configPath, conf);
    }
    
    return conf;
  }

  private static void loadConfiguration(String configPath, Configuration conf) throws IOException {
    InputStream is = Configuration.class.getResourceAsStream(configPath);
    Properties props = new Properties();
    props.load(is);
    
    for(Object key:props.keySet()){
      if(key instanceof String){
        final String strKey = (String)key;
        conf.properties.put(strKey, props.getProperty(strKey));
      }else{
        System.err.println("Illegal Key found!" + key);
      }
    }
  }
  
  public void reloadConfiguration() throws IOException{
    properties.clear();
    loadConfiguration(path, this);
  }
  
  public String getString(String key){
    return getString(key, null);
  }
  
  public String getString(String key, String defaultVal){
    String val = properties.get(key);
    return val!=null?val:defaultVal;
  }
  
  public Integer getInteger(String key){
    return getInteger(key, null);
  }
  
  public Integer getInteger(String key, Integer defaultVal){
    String val = properties.get(key).trim();
    if(val!=null){
      return Integer.parseInt(val);
    }
    return defaultVal;
  }
  
  public Long getLong(String key){
    return getLong(key, null);
  }
  
  public Long getLong(String key, Long defaultVal){
    String val = properties.get(key);
    if(val!=null){
      return Long.parseLong(val);
    }
    return defaultVal;
  }
  
  public Double getDouble(String key){
    return getDouble(key, null);
  }
  
  public Double getDouble(String key, Double defaultVal){
    String val = properties.get(key);
    if(val!=null){
      return Double.parseDouble(val);
    }
    return defaultVal;
  }
  
  public Boolean getBoolean(String key){
    return getBoolean(key, null);
  }
  
  public Boolean getBoolean(String key, Boolean defaultVal){
    String val = properties.get(key);
    if(val!=null){
      return Boolean.parseBoolean(val);
    }
    return defaultVal;
  }
}
