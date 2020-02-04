package com.sap.charging.util.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CLITest {
	
	/**
	 * See
	 * https://stackoverflow.com/questions/1119385/junit-test-for-system-out-println
	 * For testing system.out statements
	 */
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	
	@BeforeEach
	public void setUpStreams() {
	    System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	public void cleanUpStreams() {
	    System.setOut(null);
	    System.setErr(null);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testHelp() throws CLArgumentMissing, CLArgumentInvalidValue, CLArgumentInvalidName, CLArgumentInvalidValueType, CLArgumentInvalidFormat {
		String argString = "--help";
		String[] args = argString.split(" ");
		CLArgumentsManager cli = new CLArgumentsManager();
		cli.setInTestingEnvironment();
		cli.init(args);
		
		String helpResult = outContent.toString();
		assertTrue(helpResult.contains("java -jar NAME.jar param1=value1 param2=value2"));
	
		for (CLArgument arg : cli.getPossibleArgs()) {
			assertTrue(helpResult.contains(arg.name));
		}
	}
	
	@Test
	public void testInit() throws CLArgumentMissing, CLArgumentInvalidValue, CLArgumentInvalidName, CLArgumentInvalidValueType, CLArgumentInvalidFormat {
		String method = "LP";
		int nCars = 10;
		int nTimeslots = 15;
		String argString = "method="+method+
				" nCars=" + nCars +
				" nTimeslots=" + nTimeslots;
		String[] args = argString.split(" ");
		CLArgumentsManager cli = new CLArgumentsManager();
		cli.init(args);
		
		assertEquals(method, cli.method.getValue());
		assertEquals(nCars, (int) cli.nCars.getValue());
		assertEquals(nTimeslots, (int) cli.nTimeslots.getValue());
	}
	
	@Test
	public void testInvalidValue() throws CLArgumentMissing, CLArgumentInvalidName, CLArgumentInvalidValueType, CLArgumentInvalidFormat {
		String argString = "method=invalid";
		try {
			String[] args = argString.split(" ");
			CLArgumentsManager cli = new CLArgumentsManager();
			cli.init(args);
			fail("method=invalid should have failed.");
		}
		catch (CLArgumentInvalidValue e) {
			String printed = outContent.toString();
			assertTrue(printed.contains("Argument method has invalid value: invalid"));
		}
	}
	
	@Test
	public void testInvalidName() throws CLArgumentMissing, CLArgumentInvalidValueType, CLArgumentInvalidFormat, CLArgumentInvalidValue {
		String argString = "invalid=LP";
		try {
			String[] args = argString.split(" ");
			CLArgumentsManager cli = new CLArgumentsManager();
			cli.init(args);
			fail("invalid=LP should have failed.");
		}
		catch (CLArgumentInvalidName e) {
			String printed = outContent.toString();
			assertTrue(printed.contains("Argument invalid is an invalid name."));
		}
	}

	@Test 
	public void testInvalidValueType() throws CLArgumentMissing, CLArgumentInvalidValue, CLArgumentInvalidName, CLArgumentInvalidFormat {
		String argString = "nCars=LP";
		try {
			String[] args = argString.split(" ");
			CLArgumentsManager cli = new CLArgumentsManager();
			cli.init(args);
			fail("nCars=LP should have failed.");
		}
		catch (CLArgumentInvalidValueType e) {
			String printed = outContent.toString();
			assertTrue(printed.contains("Invalid value type for nCars:"));
		}
	}
	
	@Test
	public void testInvalidFormat() throws CLArgumentMissing, CLArgumentInvalidValue, CLArgumentInvalidName, CLArgumentInvalidValueType {
		String argString = "nCars:10";
		try {
			String[] args = argString.split(" ");
			CLArgumentsManager cli = new CLArgumentsManager();
			cli.init(args);
			fail("nCars:10 should have failed.");
		}
		catch (CLArgumentInvalidFormat e) {
			String printed = outContent.toString();
			assertTrue(printed.contains("Invalid argument format for arg=nCars"));
		}
	}
	
	@Test
	public void testArgumentMissing() throws CLArgumentInvalidValue, CLArgumentInvalidName, CLArgumentInvalidValueType, CLArgumentInvalidFormat {
		CLArgument<Integer> temp = new CLArgument<Integer>(
				"temp", Integer.class,
				false, 0, null);
		try {
			String[] args = {};
			CLArgumentsManager cli = new CLArgumentsManager();
			cli.getPossibleArgs().add(temp);
			cli.init(args);
			fail("Missing 'temp' argument should have failed.");
		}
		catch (CLArgumentMissing e) {
			String printed = outContent.toString();
			assertTrue(printed.contains("Missing CL argument: temp"));
		}
	}
	
	
}
