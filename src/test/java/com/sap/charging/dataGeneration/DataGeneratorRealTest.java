package com.sap.charging.dataGeneration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.charging.model.Car;
import com.sap.charging.model.CarFactory.CarType;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.util.Callback;

// Disable this class until a public/synthetic database is integrated
@Disabled
public class DataGeneratorRealTest {

	@Test
	public void testRetrieveEnergyPriceHistory() {
		// See https://www.epexspot.com/en/market-data/intradaycontinuous/intraday-table/2017-08-31/DE for data
		DataGeneratorReal data = new DataGeneratorReal("2017-08-31");
		data.generateEnergyPriceHistory(-1);
		
		EnergyPriceHistory result = data.getEnergyPriceHistory();
		assertEquals(96, result.getNTimeslots());
		assertEquals(37.64, result.getPrice(0), 1e-8);
		assertEquals(24.34, result.getPrice(1), 1e-8);
		assertEquals(8.97, result.getPrice(20), 1e-8);
		assertEquals(22.39, result.getPrice(95), 1e-8);
	}
	
	
	
	@Test
	public void testRetrieveEnergyPriceHistoryInvalidDate() {
		DataGeneratorReal data = new DataGeneratorReal("2000-01-01");
		try {
			data.generateEnergyPriceHistory(-1);
			fail("Should have generated an error.");
		}
		catch (NullPointerException e) {
			assertEquals("No data was found in energyPriceHistory for day=2000-01-01",
						 e.getMessage());
		}
	}
	
	@Test 
	public void testGenerateCarType() {
		DataGeneratorReal data = new DataGeneratorReal("2000-01-01");
		
		String typeB = "B";
		assertEquals(CarType.BEV, data.generateCarType(typeB));
		
		String typeP = "P";
		assertEquals(CarType.PHEV, data.generateCarType(typeP));
		
		String typeNA = "N/A";
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.PHEV, data.generateCarType(typeNA));
		assertEquals(CarType.BEV, data.generateCarType(typeNA));
	}

	@Test 
	public void testGeneratePossibleCarModels() {
		DataGeneratorReal data = new DataGeneratorReal("2000-01-01");
		
		double amountCharged = 40;
		String idNA = "N/A";
		assertEquals(CarType.BEV, data.generatePossibleCarModels(amountCharged, idNA, 3600)[0].carType);
	}
	
	@Test 
	public void testRetrieveCars() {
		DataGeneratorReal data = new DataGeneratorReal("2016-12-01");
		data.generateEnergyPriceHistory(-1).generateCars(-1);
		
		List<Car> cars = data.getCars();
		
		// There should be 24 cars charging on 2016-12-01 in wdf16
		assertEquals(24, cars.size());
		
		Car car = cars.get(0);
		System.out.println(car);
		assertEquals(0, car.getId());
		assertEquals(3.106*CONSTANTS.CHARGING_EFFICIENCY*1000/230, car.getMaxCapacity()-car.getCurrentCapacity(), 1e-8);
		assertEquals(6*3600+40*60, car.timestampArrival.toSecondOfDay());
		assertEquals(13*3600+51*60+10, car.timestampDeparture.toSecondOfDay());
		assertEquals(6*4+2, car.getFirstAvailableTimeslot());
		assertEquals(13*4+3, car.getLastAvailableTimeslot());
		
		car = cars.get(1);
		assertEquals(7.865*CONSTANTS.CHARGING_EFFICIENCY*1000/230, car.getMaxCapacity()-car.getCurrentCapacity(), 1e-8);
		assertEquals(7*3600+48*60, car.timestampArrival.toSecondOfDay());
		assertEquals(17*3600+13*60+2, car.timestampDeparture.toSecondOfDay());
		assertEquals(7*4+3, car.getFirstAvailableTimeslot());
		assertEquals(17*4+0, car.getLastAvailableTimeslot());
	}
	
	private int nChargingStations = 0;
	private void addNChargingStations() {
		nChargingStations++;
	}
	
	@Test
	public void testRetrieveFuseTree() {
		DataGeneratorReal data = new DataGeneratorReal("2000-01-01");
		data.generateChargingStations(-1)
			.generateFuseTree(-1, true);
		
		FuseTree fuseTree = data.getFuseTree();
		
		assertEquals(20, fuseTree.getNumberChargingStationsBottomLevel());
		fuseTree.traverseTree(new Callback<FuseTreeNode>() {
			@Override
			public void callback(FuseTreeNode item) {
				if (item instanceof ChargingStation) {
					addNChargingStations();
				}
			}
		});
		assertEquals(79, nChargingStations);
	}
	
	@Test
	public void testRetrieveChargingStations() {
		DataGeneratorReal data = new DataGeneratorReal("2000-01-01");
		data.generateChargingStations(-1)
			.generateFuseTree(-1, true);
		
		List<ChargingStation> stations = data.getChargingStations();
		
		assertEquals(79, stations.size());
		
		ChargingStation cs0 = stations.get(0);
		assertEquals(0, cs0.getId());
		assertEquals(true, cs0.isBEVAllowed);
		assertEquals(false, cs0.isPHEVAllowed);
		assertEquals(16, cs0.fusePhase1, 1e-8);
	}
	
	@Test
	public void testRetrieveCompleteDate() {
		DataGeneratorReal data = new DataGeneratorReal("2018-10-01");
		data.generateEnergyPriceHistory(-1)
			.generateCars(-1)
			.generateChargingStations(-1)
			.generateFuseTree(-1, true);
		
	}
	
	@Test
	public void testRetrieveAllDates() {
		DataGeneratorRealDB db = new DataGeneratorRealDB();
		ArrayList<String> dates = db.retrieveAvailableDates("CarPark1");
		
		System.out.println("First date: " + dates.get(0));
		System.out.println("Last date: " + dates.get(dates.size()-1));
		
		
		DataGeneratorReal data = new DataGeneratorReal(dates.get(0));
		data.generateAll();
		
		data = new DataGeneratorReal(dates.get(dates.size()-1));
		data.generateAll();
		
		assertEquals(565, dates.size());
		
		assertEquals("2016-09-28", dates.get(0));
		assertEquals("2018-06-27", dates.get(dates.size()-1));
	}
	
	@Test
	public void testRetrieveSomeData() {
		DataGeneratorRealDB db = new DataGeneratorRealDB();
		List<String> dates = db.retrieveAvailableDates("CarPark1").subList(0, 10);
		
		DataGeneratorReal data = null;
		String dateError = null;
		try {
			for (String date : dates) {
				dateError = date;
				data = new DataGeneratorReal(date);
				data.generateAll();
			}
		}
		catch (Exception e) {
			System.out.println("Error occured for date=" + dateError + ":");
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testCloneWithSeed() {
		int seed = 0;
		DataGeneratorReal data = new DataGeneratorReal("2016-12-01", seed, true, true);
		data.generateAll();
		DataGeneratorReal dataClone1 = data.clone();
		DataGeneratorReal dataClone2 = data.clone();
		for (int n=0;n<data.getCars().size();n++) {
			Car car = data.getCars().get(n);
			Car carClone1 = dataClone1.getCars().get(n);
			Car carClone2 = dataClone2.getCars().get(n);
			assertEquals(car.getCurrentCapacity(), carClone1.getCurrentCapacity(), 1e-9);
			assertEquals(car.getCurrentCapacity(), carClone2.getCurrentCapacity(), 1e-9);
			
		}
		
		
	}
	
}	















