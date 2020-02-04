package com.sap.charging.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FileIO {
	
	public static String readFile(String path) {
		String result = "";
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(path));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
 
	        while (line != null) {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	        result = sb.toString();
	    } catch (IOException e) { 
	    	e.printStackTrace();
	    } 
		finally {
	        try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
		
		return result;
	}
	
	public static JSONObject readJSONFile(String path) {
		JSONParser parser = new JSONParser();
		String content = readFile(path);
		JSONObject result = null;
		try {
			result = (JSONObject) parser.parse(content);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void writeFile(String path, String content, boolean appendCurrentDate) {
		PrintWriter out = null;
		
		if (appendCurrentDate == true) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd_HH-mm-ss").format(Calendar.getInstance().getTime());
			int lastPoint = path.lastIndexOf(".");
			String pathEnding = path.substring(lastPoint);
			path = path.substring(0, lastPoint);
			path += "-" + timeStamp + pathEnding;
		}
		
		try {
			//System.out.println("FileIO::WriteFile Writing file (tID=" + Thread.currentThread().getId() + ") to " + path + "...");
			out = new PrintWriter(path);
			out.print(content);
			out.flush();
			System.out.println("FileIO::WriteFile Wrote file (tID=" + Thread.currentThread().getId() + ") to " + path);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			out.close();
		}
	}
	
	public static void writeFile(String path, String content) {
		writeFile(path, content, false);
	}
	public static void writeFile(String path, JSONObject object) {
		writeFile(path, object, false);
	}
	
	public static void writeFile(String path, JSONObject object, boolean appendCurrentDate) {
		Writer writer = new JSONWriter();
		try {
			object.writeJSONString(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		writeFile(path, writer.toString(), appendCurrentDate);
	}

	public static boolean fileExists(String path) {
		File file = new File(path);
		return file.exists();
	}
	
	public static void createDir(String path) {
		File dir = new File(path);
		dir.mkdirs();
		System.out.println("FileIO::createDir Created dir: " + path);
	}
	
	
	/**
	 * Sample usage:
	 * <pre>
	 * Writer writer = new JSONWriter(); // this writer adds indentation
	 * jsonobject.writeJSONString(writer);
	 * System.out.println(writer.toString());
	 * </pre>
	 * 
	 * @author Elad Tabak
	 * @author Maciej Komosinski, minor improvements, 2015
	 * @since 28-Nov-2011
	 * @version 0.2
	 */
	public static class JSONWriter extends StringWriter
	{
		final static String indentstring = "  "; //define as you wish
		final static String spaceaftercolon = " "; //use "" if you don't want space after colon

		private int indentlevel = 0;

		@Override
		public void write(int c)
		{
			char ch = (char) c;
			if (ch == '[' || ch == '{')
			{
				super.write(c);
				super.write('\n');
				indentlevel++;
				writeIndentation();
			} else if (ch == ',')
			{
				super.write(c);
				super.write('\n');
				writeIndentation();
			} else if (ch == ']' || ch == '}')
			{
				super.write('\n');
				indentlevel--;
				writeIndentation();
				super.write(c);
			} else if (ch == ':')
			{
				super.write(c);
				super.write(spaceaftercolon);
			} else
			{
				super.write(c);
			}

		}

		private void writeIndentation()
		{
			for (int i = 0; i < indentlevel; i++)
			{
				super.write(indentstring);
			}
		}
	}

	
	
}	
