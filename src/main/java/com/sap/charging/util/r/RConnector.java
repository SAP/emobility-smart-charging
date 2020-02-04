package com.sap.charging.util.r;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.sap.charging.util.Loggable;

public class RConnector implements Loggable {
	
	public static int verbosity = 1;
	
	private RConnection rConnection;
	
	private RConnection getRConnection() {
		if (rConnection == null || rConnection.isConnected() == false) {
			
			int port = (int) (6311 + Thread.currentThread().getId());
			//System.out.println(port);
			
			StartRserve.checkLocalRserve(port);
			/*if (StartRserve.isRserveRunning() == false) {
				StartRserve.launchRserve(cmd)
			}
			StartRserve.isRserveRunning()*/
			
			try {
				rConnection = new RConnection("127.0.0.1", port);
			} catch (RserveException e) {
				e.printStackTrace();
			} 
		}
		return rConnection;
	}
	
	public String getJavaWD() {
		String result = System.getProperty("user.dir").replace("\\", "/");
		return result;
	}
	
	public REXP evalRString(String rString) {
		try {
			REXP rexp = getRConnection().eval(rString);
			//getRConnection().close();
			return rexp;
		} catch (RserveException e) {
			System.out.println("Executed R string:");
			System.out.println(rString);
			e.printStackTrace();
		}
		return null;
	}
	
	public String addRTryCtach(String rCommand) {
		String rString = "tryCatch({\n"
				+ "setwd('" + getJavaWD() + "')\n"
				+ rCommand				
				+ "}, error=function(e) {\n"
				+ "\tas.character(e) \n" 
				+ "})";
		return rString;
	}
	
	
	public static void main(String[] args) throws REXPMismatchException {
		RConnector r = new RConnector();
		//System.out.println(r.getJavaWD());
		//r.getRConnection();
		
		String rCommand = r.addRTryCtach("temp <- 'hello world'\n temp");
		//rCommand = r.addRTryCtach(rCommand);
		REXP result = r.evalRString(rCommand);
		System.out.println(result.asString());
	}

	@Override
	public int getVerbosity() {
		return verbosity;
	}
	
}
