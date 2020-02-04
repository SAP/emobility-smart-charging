package com.sap.charging.opt.lp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.json.simple.JSONObject;

import com.sap.charging.dataGeneration.DataGenerator;
import com.sap.charging.model.Car;
import com.sap.charging.model.ChargingStation;
import com.sap.charging.model.EnergyPriceHistory;
import com.sap.charging.model.Fuse;
import com.sap.charging.model.FuseTree;
import com.sap.charging.model.FuseTreeNode;
import com.sap.charging.model.EnergyUtil.Phase;
import com.sap.charging.opt.CONSTANTS;
import com.sap.charging.opt.Instance;
import com.sap.charging.opt.lp.Equation.ConstraintType;
import com.sap.charging.opt.lp.util.Solver;
import com.sap.charging.opt.util.MethodTimer;
import com.sap.charging.util.Callback;
import com.sap.charging.util.FileIO;
import com.sap.charging.util.Loggable;
import com.sap.charging.util.Util;


/**
 * One instance describes one specific problem to be solved, e.g. a specific 
 * configuration of charging points, cars and other parameters.
 * 
 * From this, a matrix could be generated as input for a solver.
 * 
 */

public class InstanceLP extends Instance implements Loggable {
	
	public static int verbosity = 2;
	
	@Override
	public int getVerbosity() {
		return verbosity;
	}
	
	
	/**
	 * Maximizes the "fair share", meaning each EV receives a priority based on how close it is to its min SoC
	 */
	public final Objective objectiveFairShare = new Objective(1e12);
	
	/**
	 * Minimizes peaks, creates a smooth aggregated energy curve. Conflicts with energy price minimization
	 */
	public final Objective objectivePeakShaving = new Objective(0);
	
	/**
	 * Minimizes energy costs. Conflicts with peak shaving
	 */
	public final Objective objectiveEnergyCosts = new Objective(1); 
	
	/**
	 * Minimizes load imbalance of the three phases
	 */
	public final Objective objectiveLoadImbalance = new Objective(1e-3);
	
	
	/**
	 * Reduces the number of restrictions and variables output to the 
	 * CPLEX .lp file. This has no effect on the actual result, only on the 
	 * performance of generating the problem instance. Setting it to true may
	 * increase performance. 
	 * 
	 * For debugging purposes, this should be set to false so that all 
	 * variables and restrictions are output. 
	 */
	private final boolean reduceNumberOfRestrictions = true;	
	
	
	/**
	 * Should objective function components z1 to z4 be normalized to lie between 0 and 1?
	 */
	private boolean useNormalizingCoefficients = false;
	
	
	public InstanceLP(List<Car> cars, List<ChargingStation> chargingStations, EnergyPriceHistory energyPriceHistory,
			FuseTree fuseTree) {
		super(cars, chargingStations, energyPriceHistory, fuseTree);
		/*if (reduceNumberOfRestrictions==true) {
			this.trimEnergyPriceHistory();
		}*/
	}
	public InstanceLP(DataGenerator data) {
		super(data);
		
		/*if (reduceNumberOfRestrictions==true) {
			this.trimEnergyPriceHistory();
		}*/
	}
	
	
	
	public void setNormalizingCoefficients(boolean useNormalizingCoefficients) {
		this.useNormalizingCoefficients = useNormalizingCoefficients;
	}
	
	
	
	/**
	 * Variables of power P_{i,j,k}
	 */
	private MultiKeyMap<Integer,Variable> variablesP;
	/**
	 * Variables of where each car is X_{i,n} (assignment to charging stations)
	 */
	private MultiKeyMap<Integer,Variable> variablesX;
	/**
	 * Variables of difference for load distribution d^+_{k,j1,j2}
	 */
	private MultiKeyMap<Integer,Variable> variablesD;
	/**
	 * Variables of difference summed power assignment per timeslot to next timeslot
	 */
	private MultiKeyMap<Integer,Variable> variablesE;
	/**
	 * Variables of previously charged power Q_{i,k,n}
	 */
	private MultiKeyMap<Integer,Variable> variablesQ;
	/**
	 * Variables of charged power fractions (below and above min SoC) Q'_{n,above} and Q'_{n,below}
	 */
	private MultiKeyMap<Integer,Variable> variablesQPrime;
	/**
	 * Variables of isChargingNeeded U_{k,n}
	 */
	private MultiKeyMap<Integer,Variable> variablesU;
	/**
	 * Variables of is above min charging needs (minLoadingState)
	 */
	private MultiKeyMap<Integer,Variable> variablesUPrime;
	/**
	 * Variables of isAboveMin V_{k,n}
	 */
	private MultiKeyMap<Integer,Variable> variablesV;
	
	public MultiKeyMap<Integer,Variable> getVariablesX() {
		return variablesX;
	}
	public MultiKeyMap<Integer,Variable> getVariablesP() {
		return variablesP;
	}
	
	public void constructVariables() {
		try(MethodTimer t=new MethodTimer(this.timeProblemConstruction)){
			log(1, "Constructing variables: "
					+ "count(X_i,n)=" + getCars().size()*getChargingStations().size() + "; " 
					+ "count(P_i,j,k)=" + getChargingStations().size()*3*energyPriceHistory.getNTimeslots() 
					+ "...");
			variablesP = new MultiKeyMap<>();
			variablesX = new MultiKeyMap<>();
			variablesD = new MultiKeyMap<>();
			variablesE = new MultiKeyMap<>();
			variablesQ = new MultiKeyMap<>();
			variablesQPrime = new MultiKeyMap<>();
			variablesU = new MultiKeyMap<>();
			variablesUPrime = new MultiKeyMap<>();
			variablesV = new MultiKeyMap<>();
			
			// Helper variables
			int kStart = getKStart();
			int kEnd = getKEnd();
			
			// Indexes reference:
			// i: Charging station/Ladestation
			// j: Phase (1,2,3)
			// k: Timeslot/Zeitintervall
			// n: Car name/ID
			
			log(1, "P_{i,j,k}... ", false, false);
			// Construct decision variables
			// P_{i,j,k} [real number]: Ladeleistung P 
			for (int i=0;i<chargingStations.size();i++) {
				for (int j=1;j<=3;j++) {
					for (int k=0;k<getEnergyPriceHistory().getNTimeslots();k++) {
						Variable newVariable = new Variable("P", false);
						newVariable.setIndex("i", i);
						newVariable.setIndex("j", j);
						newVariable.setIndex("k", k);
						variablesP.put(i, j, k, newVariable);
					}
				}
			}

			log(1, "X_{i,n}... ", false, false);
			// X_{i,n} [0,1]: Which station does the car park at?
			for (int i=0;i<chargingStations.size();i++) {
				for (int n=0;n<cars.size();n++) {
					Variable newVariable = new Variable("X", true);
					newVariable.setIndex("i", i);
					newVariable.setIndex("n", n);
					variablesX.put(i, n, newVariable);
				}
			}
			
			log(1, "D_{k,j1,j2}... ", false, false);
			// D_{k,j1,j2}: Help variables for difference for load distribution
			for (int k=0;k<getEnergyPriceHistory().getNTimeslots();k++) {
				for (int sign=1;sign>=0;sign--) {
					// Once loop iteration per sign 1 or -1
					Variable newVariableP1P2 = new Variable("D", false);
					newVariableP1P2.setIndex("k", k);
					newVariableP1P2.setIndex("sign", sign);
					newVariableP1P2.setIndex("jFrom", 1);
					newVariableP1P2.setIndex("jTo", 2);
					Variable newVariableP2P3 = new Variable("D", false);
					newVariableP2P3.setIndex("k", k);
					newVariableP2P3.setIndex("sign", sign);
					newVariableP2P3.setIndex("jFrom", 2);
					newVariableP2P3.setIndex("jTo", 3);
					Variable newVariableP1P3 = new Variable("D", false);
					newVariableP1P3.setIndex("k", k);
					newVariableP1P3.setIndex("sign", sign);
					newVariableP1P3.setIndex("jFrom", 1);
					newVariableP1P3.setIndex("jTo", 3);
					variablesD.put(k, sign, 1, 2, newVariableP1P2);
					variablesD.put(k, sign, 2, 3, newVariableP2P3);
					variablesD.put(k, sign, 1, 3, newVariableP1P3);
				}
			}
			
			log(1, "E_{k}... ", false, false);
			// E_{k}: Helper variables for difference of summed power of timeslot to next timeslot
			for (int k=getKStart();k<getKEnd()-1;k++) {
				for (int sign=1;sign>=0;sign--) { 
					Variable variableE = new Variable("E", false);
					variableE.setIndex("k", k);
					variableE.setIndex("sign", sign);
					//System.out.println("Adding variableE=" + variableE.getNameWithIndicesLong());
					variablesE.put(k, sign, variableE);
				}
				
			}
			
			log(1, "Q_{i,k,n}... ", false, false);
			// Q_{i,k,n}: Helper variables for previously charged power
			for (int i=0;i<chargingStations.size();i++) {
				for (int k=0;k<energyPriceHistory.getNTimeslots()+1;k++) {
					for (int n=0;n<cars.size();n++) {
						Variable variableQ = new Variable("Q", false);
						variableQ.setIndex("i", i);
						variableQ.setIndex("k", k);
						variableQ.setIndex("n", n);
						variablesQ.put(i, k, n, variableQ);
					}
				}
			}
			
			log(1, "Q'{n}... ", false, false);
			// Q'_{n,below} and Q'_{n,above}: Helper variables for fractions of charging at end
			for (int n=0;n<cars.size();n++) {
				Variable variableQPrimeBelow = new Variable("QPrime", false);
				variableQPrimeBelow.setIndex("n", n);
				variableQPrimeBelow.setIndex("below", 1);
				
				Variable variableQPrimeAbove = new Variable("QPrime", false);
				variableQPrimeAbove.setIndex("n", n);
				variableQPrimeAbove.setIndex("below", 0);
				
				variablesQPrime.put(n, 1, variableQPrimeBelow);
				variablesQPrime.put(n, 0, variableQPrimeAbove);
			}
			
			log(1, "U_{k,n}... ", false, false);
			// U_{k,n}: Helper variable for isChargingNeeded (binary)
			for (int n=0;n<cars.size();n++) {
				for (int k=0;k<energyPriceHistory.getNTimeslots();k++) {
					Variable variableU = new Variable("U", true);
					variableU.setIndex("k", k);
					variableU.setIndex("n", n);
					variablesU.put(k, n, variableU);
				}
			}
			
			log(1, "U'_{k,n}... ", false, false);
			// U'_{k,n}: Helper variable for is above min loading state (binary)
			for (int n=0;n<cars.size();n++) {
				//for (int k=0;k<energyPriceHistory.getNTimeslots()+1;k++) {
				int k = energyPriceHistory.getNTimeslots();
				Variable variableUPrime = new Variable("UPrime", true);
				variableUPrime.setIndex("k", k);
				variableUPrime.setIndex("n", n);
				variablesUPrime.put(k, n, variableUPrime);
				//}
			}
			
			log(1, "V_{k,n}... ", false, true);
			// V_{k,n}: Helper variable for isAboveMin charging current (binary)
			for (int n=0;n<cars.size();n++) {
				for (int k=kStart;k<kEnd;k++) {
					Variable variableV = new Variable("V", true);
					variableV.setIndex("k", k);
					variableV.setIndex("n", n);
					variablesV.put(k, n, variableV);
				}
			}
		} 
	}
	
	public void setAllVariablesXTo0() {
		log(1, "Setting all variableX to 0.");
		for (Variable variableX : variablesX.values()) {
			variableX.setValue(0);
		}
	}
	
	public void constructProblem() {
		log(1, "Constructing problem file...");
		try (MethodTimer t = new MethodTimer(this.timeProblemConstruction)) {
			String result = constructProblemLPFile(null);
			this.writeProblemLPFile(result);
		}
	}
	
	public void constructProblem(ArrayList<Equation> allRestrictions) {
		log(1, "Constructing problem file with previously constructed restrictions...");
		try (MethodTimer t = new MethodTimer(this.timeProblemConstruction)) {
			String result = constructProblemLPFile(allRestrictions);
			this.writeProblemLPFile(result);
		}
	}
	
	
	private int getKStart() {
		int earliestTimeslot = this.getEarliestCarTimeslot();
		return (reduceNumberOfRestrictions) ? earliestTimeslot : 0;
	}
	private int getKEnd() {
		int latestTimeslot = this.getLatestCarTimeslot();
		return (reduceNumberOfRestrictions) ? (latestTimeslot+1) : this.getEnergyPriceHistory().getNTimeslots();
	}
	
	
	/**
	 * Divide z1 by nCars * (1+c)
	 * @return
	 */
	public double getNormalizingCoefficientFairShare() {
		return 1.0 / (getCars().size() * (1 + 0.1));
	}
	
	/**
	 * Divide z2 by the maximum € we can spend, taking into account efficiency
	 * Approximation: 
	 * sum per car ( car.maxCurrent*230V*(1/4h) ) * max ( energyPrices ) 
	 * 
	 * @return
	 */
	public double getNormalizingCoefficientEnergyCosts() {
		// Get Ah needed for all cars
		double capacityNeededAh = getCars().stream().mapToDouble(car -> car.getMissingCapacity()).sum();
		double capacityNeededWh = capacityNeededAh * 230.0 / CONSTANTS.CHARGING_EFFICIENCY;
		double maxPriceEuroPerWh = energyPriceHistory.getHighestPrice() / 1000 / 1000; // Convert from MWh to Wh 
		double maxEuro =  capacityNeededWh * maxPriceEuroPerWh;
		double result = 1.0 / maxEuro; // 230V because this was not in the working previous result
		//System.out.println("Normalizinhg coefficient energy costs: " + result + ", maxPriceEuroPerWh: " + maxPriceEuroPerWh + ", capacityNeededWh: " + capacityNeededWh + ", maxEuro: " + maxEuro);
		return result;
	}
	
	/**
	 * Divide z3 by the maximum A of sum of differences we can have
	 * Approximation: 
	 * Each car has maxCurrent, per timeslot that it is there turn on and off
	 * ==> Difference to next timeslot is always car.maxCurrent
	 * 
	 */
	public double getNormalizingCoefficientPeakShaving() {
		double sumDifferences = 0;
		for (Car car : getCars()) {
			int availableTimeslots = car.getLastAvailableTimeslot() - car.getFirstAvailableTimeslot() + 1; // If it arrives at 0 and leaves at 0 it is there for 0
			sumDifferences += availableTimeslots * car.maxCurrent;
		}
		return 1.0 / sumDifferences;
	}
	
	/**
	 * Divide z4 by the maximum A of loadImbalance
	 * Approximation:
	 * Sum of all cars with non three-phase charging
	 * Car.maxCurrentPerPhase
	 * @return
	 */
	public double getNormalizingCoefficientLoadImbalance() {
		double sumLoadImbalance = getCars().stream()
				.filter(car -> car.sumUsedPhases < 3) 
				.mapToDouble(car -> (car.getLastAvailableTimeslot() - car.getFirstAvailableTimeslot() + 1) * car.maxCurrentPerPhase * 2)
				.sum();
		
		return 1.0 / sumLoadImbalance;
	}
	
	public void printNormalizingCoefficients() {
		System.out.println("Using normalizing coefficients: fairShare=" + getNormalizingCoefficientFairShare() + 
				", energyCosts=" + getNormalizingCoefficientEnergyCosts() +
				", peakShaving=" + getNormalizingCoefficientPeakShaving() + 
				", loadImbalance=" + getNormalizingCoefficientLoadImbalance());
	}
	
	
	
	private Equation constructObjectiveFunction() {
		if (variablesP == null) {
			log(0, "ERROR: Call constructVariables first!");
			throw new NullPointerException();
		}
		
		// Helper variables
		int kStart = getKStart();
		int kEnd = getKEnd();
				
		
		// OBJECTIVE FUNCTION
		// P = power
		Equation objectiveFunction = new Equation("OBJ", Equation.ConstraintType.OBJ);
		
		if (useNormalizingCoefficients == true && verbosity >= 2) {
			printNormalizingCoefficients();
		}
		
		if (this.objectiveFairShare.getWeight() >= 0) {
			
			for (Variable variableQPrime : variablesQPrime.values()) {
				double weight = (variableQPrime.getIndex("below") == 1) ? 
						objectiveFairShare.getWeight() :
						objectiveFairShare.getWeight() / 10;
						
				if (useNormalizingCoefficients == true) {
					weight *= getNormalizingCoefficientFairShare(); 
				}
				
				objectiveFunction.cloneAndAddVariable(variableQPrime, weight);
				
			}
			
			
			/*for (int i=0;i<chargingStations.size();i++) {
				for (int k=kStart;k<kEnd;k++) {
					for (int n=0;n<cars.size();n++) {
						
					}
				}
			}
			objectiveFunction.cloneAndAddVariable(power, -1.0 * 0.25 * CONSTANTS.PENALTY_NOT_CHARGING);*/
			// Part 2: Penalty for not loading with a price higher than the price for energy
						// sum_n (c'*b_n) - sum_i sum_j sum_k (c'*P_i,j,k)
						/*for (int n=0;n<cars.size();n++) {
							// Constants are left out, ignored by solvers
						}*/
		}
		
		if (this.objectiveEnergyCosts.getWeight() > 0) {
			
			double weight = objectiveEnergyCosts.getWeight();
			
			if (useNormalizingCoefficients == true) {
				weight *= getNormalizingCoefficientEnergyCosts(); 
			}
			
			// Minimize energy prices
			for (int i=0;i<getChargingStations().size();i++) {
				for (int j=1;j<=3;j++) {
					for (int k=kStart; k<kEnd; k++) {
						Variable power = getVariableP(i, j, k);
						
						double energyPrice;
						if (useNormalizingCoefficients) {
							// Retrieve energy price of this variable's timeslot k
							energyPrice = energyPriceHistory.getPrice(k); // Convert from MWh to Wh with / 10^6
							
							// [A]*230V * 1/[€] * [€/Wh]
							// [Wh] * 1/[€] * [€/Wh]
							objectiveFunction.cloneAndAddVariable(power, weight * 0.25 * energyPrice);
						}
						else {
							energyPrice = energyPriceHistory.getPrice(k);
							objectiveFunction.cloneAndAddVariable(power, objectiveEnergyCosts.getWeight() * 0.25 * energyPrice);
						}
							
					}
				}
			}
		}
		
		if (this.objectivePeakShaving.getWeight() > 0) {
			double weight = objectivePeakShaving.getWeight();
			
			if (useNormalizingCoefficients == true) {
				weight *= getNormalizingCoefficientPeakShaving();
			}
			
			for (Variable variableE : variablesE.values()) {
				
				objectiveFunction.cloneAndAddVariable(variableE, weight);
			}
		}
		
		if (this.objectiveLoadImbalance.getWeight() > 0) {
			// Penalty for bad load distribution (Schieflast), load imbalance
			
			double weight = objectiveLoadImbalance.getWeight();
			
			if (useNormalizingCoefficients == true) {
				weight *= getNormalizingCoefficientLoadImbalance();
			}
			
			for (Variable variableD : variablesD.values()) {
				if (variableD.getIndex("k") >= kStart && variableD.getIndex("k") < kEnd) {
					// Only add relevant ones
					
					if (useNormalizingCoefficients==true) {
						objectiveFunction.cloneAndAddVariable(variableD, weight * 0.5);
					}
					else {
						objectiveFunction.cloneAndAddVariable(variableD, weight);
					}
			
				}
			}
		}
		return objectiveFunction;
	}
	
	public ArrayList<Equation> constructAllRestrictions() {
		// Helper variables
		int kStart = getKStart();
		int kEnd = getKEnd();
		log(1, "Running with kStart=" + kStart + ", kEnd=" + kEnd);
		
		// Construct restrictions
		// R1: One equation per car, station: Don't overload each car
		log(1, "Constructing R1... ", true, false);
		ArrayList<Equation> equationsR1 = new ArrayList<>();
		for (int i=0;i<chargingStations.size();i++) {
			for (int n=0;n<cars.size();n++) {
				Variable variableX = getVariableX(i, n);
				
				Equation equation = new Equation("R1", Equation.ConstraintType.LEQ);
				equation.setIndex("i", i);
				equation.setIndex("n", n);
				
				for (int j=1;j<=3;j++) {
					for (int k=kStart;k<kEnd;k++) {
						Variable power = getVariableP(i,j,k);
						double isAvailable = cars.get(n).isAvailableInt(k);
						// 0.25* w*n*d_{k,n}*P_{i,j,k}
						equation.cloneAndAddVariable(power, 0.25*CONSTANTS.CHARGING_EFFICIENCY*isAvailable);
					}
				}
				
				equation.cloneAndAddVariable(variableX, CONSTANTS.M_HIGH_BOUND);
				
				equation.RHS = cars.get(n).getMaxCapacity() - cars.get(n).getCurrentCapacity() + CONSTANTS.M_HIGH_BOUND; //b_n + M
				equationsR1.add(equation);
			}
		}
		
		
		// R2: One equation per i,j,k,n: Ein Auto steht da
		log(1, "R2... ", false, false);
		ArrayList<Equation> equationsR2 = new ArrayList<>();
		for (int i=0;i<getChargingStations().size();i++) {
			for (int j=1;j<=3;j++) {
				for (int k=kStart; k<kEnd;k++) {
					Variable power = getVariableP(i, j, k);
					
					Equation equation = new Equation("R2", Equation.ConstraintType.LEQ);
					equation.setIndex("i", i);
					equation.setIndex("j", j);
					equation.setIndex("k", k);
					
					equation.cloneAndAddVariable(power, 1);
					double fuse = chargingStations.get(i).getFusePhase(j); // -e_i,j * X_i,n
					
					for (int n=0;n<cars.size();n++) {
						int isAvailable = cars.get(n).isAvailableInt(k);
						equation.cloneAndAddVariable(getVariableX(i, n), -1 * fuse * isAvailable);
					}
					
					equation.RHS = 0;
					equationsR2.add(equation);
				}
			}
		}
		
		
		// R3: One equation per i,j,k,n: Sicherung 
		ArrayList<Equation> equationsR3 = new ArrayList<>();
		for (int i=0;i<getChargingStations().size();i++) {
			for (int j=1;j<=3;j++) {
				for (int k=kStart; k<kEnd;k++) {
					Variable power = getVariableP(i, j, k);
					
					Equation equation = new Equation("R3", Equation.ConstraintType.LEQ);
					equation.setIndex("i", i);
					equation.setIndex("j", j);
					equation.setIndex("k", k);
					
					double fuse = chargingStations.get(i).getFusePhase(j); // -e_i,j * X_i,n
					
					equation.cloneAndAddVariable(power, 1);
					equation.RHS = fuse;
					equationsR3.add(equation);
				}
			}
		}
		
		// R4: One equation per n: Jedes Auto max 1 Säule
		ArrayList<Equation> equationsR4 = new ArrayList<>();
		for (int n=0;n<cars.size();n++) {
			Equation equation = new Equation("R4", Equation.ConstraintType.LEQ);
			equation.setIndex("n", n);
			
			for (int i=0;i<chargingStations.size();i++) {
				Variable variableX = getVariableX(i, n);
				equation.cloneAndAddVariable(variableX, 1);
			}
			equation.RHS = 1;
			equationsR4.add(equation);
		}
		
		// R5: One equation per i,k: Jede Säule max 1 Auto zu Zeitpunkt k
		ArrayList<Equation> equationsR5 = new ArrayList<>();
		for (int i=0;i<chargingStations.size();i++) {
			for (int k=kStart;k<kEnd;k++) {
				Equation equation = new Equation("R5", Equation.ConstraintType.LEQ);
				equation.setIndex("i", i);
				equation.setIndex("k", k);
				
				for (int n=0;n<cars.size();n++) {
					//X_i,n * d_k,n
					Variable variableX = getVariableX(i, n);
					equation.cloneAndAddVariable(variableX, cars.get(n).isAvailableInt(k));
				}
								
				equation.RHS = 1;
				equationsR5.add(equation);
			}
		}
		
		
		// R6_1, R6_2: One equation per i,k,n Festlegen 1. Phase 2. Verhältnis UPPER/LOWER bound
		// R7_1, R6_3: One equation per i,k,n Festlegen 1. Phase 3. Verhältnis UPPER/LOWER bound
		log(1, "R6-R7... ", false, false);
		ArrayList<Equation> equationsR6 = new ArrayList<>();
		ArrayList<Equation> equationsR7 = new ArrayList<>();
		for (int i=0;i<chargingStations.size();i++) {
			for (int k=kStart;k<kEnd;k++) {
				Variable powerPhase1 = getVariableP(i, 1, k);
				Variable powerPhase2 = getVariableP(i, 2, k);
				Variable powerPhase3 = getVariableP(i, 3, k);
				
				for (int n=0;n<cars.size();n++) {
					Variable variableX = getVariableX(i, n);
					
					// Set upper bound for ratio of phase 1 to phase 2. If X_{i,n}=1, lower and upper bound should both be equal 0.
					// If X_{i,n}=0, lower and upper bound should be ignorable, which is why a large value is chosen. 
					Equation equationP1P2HIGH = new Equation("R6_1", Equation.ConstraintType.LEQ);
					equationP1P2HIGH.setIndex("i", i);
					equationP1P2HIGH.setIndex("k", k);
					equationP1P2HIGH.setIndex("n", n);
					int isAvailable = cars.get(n).isAvailableInt(k);
					
					equationP1P2HIGH.cloneAndAddVariable(powerPhase1, cars.get(n).canLoadPhase(2));
					equationP1P2HIGH.cloneAndAddVariable(powerPhase2, -1*cars.get(n).canLoadPhase(1));
					equationP1P2HIGH.cloneAndAddVariable(variableX, isAvailable * CONSTANTS.M_HIGH_BOUND);
					equationP1P2HIGH.RHS = CONSTANTS.M_HIGH_BOUND;
					equationsR6.add(equationP1P2HIGH);
					
					// Set lower bound for ratio of phase 1 to phase 2
					Equation equationP1P2LOW = new Equation("R6_2", Equation.ConstraintType.GEQ);
					equationP1P2LOW.setIndex("i", i);
					equationP1P2LOW.setIndex("k", k);
					equationP1P2LOW.setIndex("n", n);
					
					equationP1P2LOW.cloneAndAddVariable(powerPhase1, cars.get(n).canLoadPhase(2));
					equationP1P2LOW.cloneAndAddVariable(powerPhase2, -1*cars.get(n).canLoadPhase(1));
					equationP1P2LOW.cloneAndAddVariable(variableX, -1 * isAvailable * CONSTANTS.M_HIGH_BOUND);
					equationP1P2LOW.RHS = -1 * CONSTANTS.M_HIGH_BOUND;
					equationsR6.add(equationP1P2LOW);
					
					
					// Set upper bound for ratio of phase 1 to phase 3. If X_{i,n}=1, lower and upper bound should both be equal 0.
					// If X_{i,n}=0, lower and upper bound should be ignorable, which is why a large value is chosen. 
					Equation equationP1P3HIGH = new Equation("R7_1", Equation.ConstraintType.LEQ);
					equationP1P3HIGH.setIndex("i", i);
					equationP1P3HIGH.setIndex("k", k);
					equationP1P3HIGH.setIndex("n", n);
					
					equationP1P3HIGH.cloneAndAddVariable(powerPhase1, cars.get(n).canLoadPhase(3));
					equationP1P3HIGH.cloneAndAddVariable(powerPhase3, -1*cars.get(n).canLoadPhase(1));
					equationP1P3HIGH.cloneAndAddVariable(variableX, isAvailable * CONSTANTS.M_HIGH_BOUND);
					equationP1P3HIGH.RHS = CONSTANTS.M_HIGH_BOUND;
					equationsR6.add(equationP1P3HIGH);
					
					// Set lower bound for ratio of phase 1 to phase 2
					Equation equationP1P3LOW = new Equation("R7_2", Equation.ConstraintType.GEQ);
					equationP1P3LOW.setIndex("i", i);
					equationP1P3LOW.setIndex("k", k);
					equationP1P3LOW.setIndex("n", n);
					
					equationP1P3LOW.cloneAndAddVariable(powerPhase1, cars.get(n).canLoadPhase(3));
					equationP1P3LOW.cloneAndAddVariable(powerPhase3, -1*cars.get(n).canLoadPhase(1));
					equationP1P3LOW.cloneAndAddVariable(variableX, -1 * isAvailable * CONSTANTS.M_HIGH_BOUND);
					equationP1P3LOW.RHS = -1 * CONSTANTS.M_HIGH_BOUND;
					equationsR6.add(equationP1P3LOW);
				}
			}
		}
		
		// R8, R9: Minimale / Maximale Ladeleistung Auto (for each i, j, k, n)
		log(1, "R8-R9... ", false, false);
		ArrayList<Equation> equationsR8 = new ArrayList<>();
		ArrayList<Equation> equationsR9 = new ArrayList<>();
		for (int i=0;i<getChargingStations().size();i++) {
			for (int j=1;j<=3;j++) {
				for (int k=kStart;k<kEnd;k++) {
					Variable power = getVariableP(i, j, k);
					
					double fuse = chargingStations.get(i).getFusePhase(j);
					
					for (int n=0;n<cars.size();n++) {
						Car car = cars.get(n);
						Variable variableX = getVariableX(i, n);
						Variable variableU = getVariableU(k, n);
						Variable variableV = getVariableV(k, n);
						
						int isAvailable = cars.get(n).isAvailableInt(k);
						
						Equation equationMax = new Equation("R8", Equation.ConstraintType.LEQ);
						equationMax.setIndex("i", i);
						equationMax.setIndex("j", j);
						equationMax.setIndex("k", k);
						equationMax.setIndex("n", n);
						
						equationMax.cloneAndAddVariable(power, 1); // P_{i,j,k}
						equationMax.cloneAndAddVariable(variableX, isAvailable * fuse); // e_{i,j}*X_{i,n}
						
						
						equationMax.cloneAndAddVariable(variableX, -1 * isAvailable * car.maxCurrentPerPhase); // -f_{j,n}*X_{i,n}    

						equationMax.RHS = fuse; 
						
						equationsR8.add(equationMax);

						
						//double minAverageCurrent = 4*(car.maxCapacity-car.getCurrentCapacity()) / (car.getLastAvailableTimeslot()-car.getFirstAvailableTimeslot()+1);
						//int minAverageGreaterMinCurrent = (car.minCurrent <= minAverageCurrent) ? 1 : 0;
						//if (minAverageGreaterMinCurrent == 0) {
							//System.out.println(minAverageCurrent);
							//System.out.println(car);
						//}
						Equation equationMin = new Equation("R9", Equation.ConstraintType.GEQ);
						equationMin.setIndex("i", i);
						equationMin.setIndex("j", j);
						equationMin.setIndex("k", k);
						equationMin.setIndex("n", n);
						
						equationMin.cloneAndAddVariable(power, 1);
						equationMin.cloneAndAddVariable(variableX, -1*CONSTANTS.M_HIGH_BOUND);
						equationMin.cloneAndAddVariable(variableU, -1*CONSTANTS.M_HIGH_BOUND);
						int isSuspendable = (car.isSuspendable()) ? 1 : 0;
						equationMin.cloneAndAddVariable(variableV, -1*CONSTANTS.M_HIGH_BOUND*isSuspendable);
						
						equationMin.RHS = car.minCurrentPerPhase*car.canLoadPhase(j) 
								* car.isAvailableInt(k) - 2*CONSTANTS.M_HIGH_BOUND 
								- isSuspendable*CONSTANTS.M_HIGH_BOUND; 
						
						equationsR9.add(equationMin);
					}
				}
			}
		}
		
		// R10: Hierarchical fuse system
		// Traverse fuseTree: For each pre-fuse, get all bottom level nodes and create one restriction for it
		log(1, "R10... ", false, false);
		ArrayList<Equation> equationsR10 = new ArrayList<>();
		for (int kCounter=kStart;kCounter<kEnd;kCounter++) {
			final int k = kCounter;
			
			fuseTree.traverseTree(new Callback<FuseTreeNode>() {
				public void callback(FuseTreeNode item) {
					// Outer loop: Construct three equations (one per phase) per higher level fuse
					if (item instanceof Fuse) {
						final Fuse currentFuse = (Fuse) item;
						FuseTree subtree = fuseTree.getSubtree(currentFuse);
						
						for (int jCounter=1;jCounter<=3;jCounter++) {
							final int j = jCounter;
							
							final Equation equationPhaseJ = new Equation("R10", Equation.ConstraintType.LEQ);
							equationPhaseJ.setIndex("l", currentFuse.getId());
							equationPhaseJ.setIndex("j", j);
							equationPhaseJ.setIndex("k", k);
							equationPhaseJ.RHS = currentFuse.getFusePhase(Phase.getByInt(j));
							
							// Note: Repeatedly traversing the subtrees is probably less efficient 
							// than an recursive approach, but hopefully more readable...
							subtree.traverseTree(new Callback<FuseTreeNode>() {
								public void callback(FuseTreeNode item) {
									// Inner loop: Add variables to equation. Three variables per charging station
									if (item instanceof ChargingStation) {
										ChargingStation chargingStation = (ChargingStation) item;
										Variable powerConsumedPhaseJ = getVariableP(
												chargingStation.getId(), 
												chargingStation.getPhaseGridToChargingStation(Phase.getByInt(j)).asInt(), //chargingStation.getPhaseConsumed(Phase.getByInt(j)).asInt(), 
												k);
										/*if (k==43) {
											System.out.println("Fuse fuse id=" + currentFuse.getIndexL() + ", " + powerConsumedPhaseJ.getNameWithIndices());
										}*/
										
										//System.out.println("i=" + chargingStation.getIndexI() + ", j=" + j + ", k=" + k);
										//System.out.println(variablesX.values());
										equationPhaseJ.cloneAndAddVariable(powerConsumedPhaseJ, 1);
									}
								}
							});
							
							if (equationPhaseJ.hasVariables() == true) {
								equationsR10.add(equationPhaseJ);
							}
						}
						
					}
					
				}
			});
		}
		
		// R11: One equation per i, j, n:  Immediate start needed
		log(1, "R11... ", false, false);
		ArrayList<Equation> equationsR11 = new ArrayList<>();
		for (Variable variableX : variablesX.values()) {
			Car car = cars.get(variableX.getIndex("n"));
			int kFirst = car.getFirstAvailableTimeslot();
			for (int j=1;j<=3;j++) {
				Equation equationR11 = new Equation("R11", Equation.ConstraintType.GEQ);
				equationR11.setIndex("i", variableX.getIndex("i"));
				equationR11.setIndex("j", j);
				equationR11.setIndex("k", kFirst);
				equationR11.setIndex("n", variableX.getIndex("n"));
				
				Variable variableP = getVariableP(variableX.getIndex("i"), j, kFirst);
				equationR11.cloneAndAddVariable(variableP, 1);
				int immediateStart = car.isImmediateStartNeeded() ? 1 : 0;
				equationR11.cloneAndAddVariable(variableX, 
						-1*car.minCurrentPerPhase*car.canLoadPhase(j)*immediateStart);
				equationR11.RHS = 0;
				
				equationsR11.add(equationR11);
			}
		}

		// R12: Suspendable
		log(1, "R12... ", false, false);
		ArrayList<Equation> equationsR12 = new ArrayList<>();
		for (Car car : cars) {
			int n = car.getId();
			for (int i=0;i<getChargingStations().size(); i++) {
				for (int j=1;j<=3;j++) {
					for (int k=kStart; k<kEnd; k++) {
						Variable variableP = getVariableP(i, j, k);
						Variable variableX = getVariableX(i, n);
						Variable variableV = getVariableV(k, n);
						
						Equation equationR12 = new Equation("R12", ConstraintType.LEQ);
						equationR12.setIndex("i", i);
						equationR12.setIndex("j", j);
						equationR12.setIndex("k", k);
						equationR12.setIndex("n", n);
						
						equationR12.cloneAndAddVariable(variableP, 1);
						equationR12.cloneAndAddVariable(variableV, -1*CONSTANTS.M_HIGH_BOUND);
						equationR12.cloneAndAddVariable(variableX, CONSTANTS.M_HIGH_BOUND);
						
						int isSuspendable = (car.isSuspendable()) ? 1 : 0;
						equationR12.RHS = CONSTANTS.M_HIGH_BOUND*(1-isSuspendable) 
										  + CONSTANTS.M_HIGH_BOUND*(1-car.isAvailableInt(k))
										  + CONSTANTS.M_HIGH_BOUND;
						
						equationsR12.add(equationR12);
					}
				}
			}
		}
		
		// R13: Variable power allowed
		log(1, "R13... ", false, false);
		ArrayList<Equation> equationsR13 = new ArrayList<>();
		for (Car car : cars) {
			int n = car.getId();
			
			if (car.canUseVariablePower()) {
				// Only add these restrictions if car does NOT have variable power
				continue;
			}
			
			int canUseVariablePower = (car.canUseVariablePower()) ? 1 : 0;
			
			for (int i=0;i<chargingStations.size();i++) {
				for (int j=1;j<=3;j++) {
					for (int k=car.getFirstAvailableTimeslot();k<car.getLastAvailableTimeslot();k++) {
						Variable variableP0 = getVariableP(i, j, k);
					
						Variable variableP1 = getVariableP(i, j, k+1);
						Variable variableX = getVariableX(i,n);
						Variable variableU = getVariableU(k+1, n);
						
						Equation equationR13UP = new Equation("R13_1", ConstraintType.LEQ);
						equationR13UP.setIndex("i", i);
						equationR13UP.setIndex("j", j);
						equationR13UP.setIndex("k", k);
						equationR13UP.setIndex("n", n);
						Equation equationR13LO = new Equation("R13_2", ConstraintType.GEQ);
						equationR13LO.setIndex("i", i);
						equationR13LO.setIndex("j", j);
						equationR13LO.setIndex("k", k);
						equationR13LO.setIndex("n", n);
						
						equationR13UP.cloneAndAddVariable(variableP0, 1);
						equationR13UP.cloneAndAddVariable(variableP1, -1);
						equationR13UP.cloneAndAddVariable(variableX, CONSTANTS.M_HIGH_BOUND);
						equationR13UP.cloneAndAddVariable(variableU, CONSTANTS.M_HIGH_BOUND);
						equationR13UP.RHS = CONSTANTS.M_HIGH_BOUND * canUseVariablePower
											+ CONSTANTS.M_HIGH_BOUND * (1-car.isAvailableInt(k+1))
											+ 2*CONSTANTS.M_HIGH_BOUND;
						
						equationR13LO.cloneAndAddVariable(variableP0, 1);
						equationR13LO.cloneAndAddVariable(variableP1, -1);
						equationR13LO.cloneAndAddVariable(variableX, -1*CONSTANTS.M_HIGH_BOUND);
						equationR13LO.cloneAndAddVariable(variableU, -1*CONSTANTS.M_HIGH_BOUND);
						equationR13LO.RHS = - CONSTANTS.M_HIGH_BOUND * canUseVariablePower
								  		    - CONSTANTS.M_HIGH_BOUND * (1-car.isAvailableInt(k+1)) 
								  			- 2*CONSTANTS.M_HIGH_BOUND;
						
						equationsR13.add(equationR13UP);
						equationsR13.add(equationR13LO);
					}
				}
			}
		}
		
		// R14 and R15: BEV/PHEV allowed at each charging station
		log(1, "R14-R15... ", false, false);
		ArrayList<Equation> equationsR14 = new ArrayList<>();
		ArrayList<Equation> equationsR15 = new ArrayList<>();
		for (int i=0;i<chargingStations.size();i++) {
			for (int n=0;n<cars.size();n++) {
				Car car = cars.get(n);
				int isBEV = car.isBEV() ? 1 : 0;
				int isPHEV= car.isPHEV()? 1 : 0;
				
				ChargingStation chargingStation = chargingStations.get(i);
				int isBEVAllowed = chargingStation.isBEVAllowed ? 1 : 0;
				int isPHEVAllowed= chargingStation.isPHEVAllowed? 1 : 0;
				
				Variable variableX = getVariableX(i, n);
				
				Equation equationR14 = new Equation("R14", ConstraintType.LEQ);
				equationR14.setIndex("i", i);
				equationR14.setIndex("n", n);
				equationR14.cloneAndAddVariable(variableX, 1);
				equationR14.RHS = 1 - isBEV + isBEVAllowed;
				
				Equation equationR15 = new Equation("R15", ConstraintType.LEQ);
				equationR15.setIndex("i", i);
				equationR15.setIndex("n", n);
				equationR15.cloneAndAddVariable(variableX, 1);
				equationR15.RHS = 1 - isPHEV + isPHEVAllowed;
				
				
				equationsR14.add(equationR14);
				equationsR15.add(equationR15);
			}
		}
		
		
		
		
		// V1,V2,V3: Construct helper variables for distances to calculate good load distributions
		ArrayList<Equation> equationsV1 = new ArrayList<>();
		ArrayList<Equation> equationsV2 = new ArrayList<>();
		ArrayList<Equation> equationsV3 = new ArrayList<>();
		for (int k=kStart;k<kEnd; k++) {
			Equation equationP1P2 = new Equation("V1", ConstraintType.EQU);
			equationP1P2.setIndex("k", k);
			Equation equationP2P3 = new Equation("V2", ConstraintType.EQU);
			equationP2P3.setIndex("k", k);
			Equation equationP1P3 = new Equation("V3", ConstraintType.EQU);
			equationP1P3.setIndex("k", k);
			
			for (int i=0;i<getChargingStations().size();i++) {
				ChargingStation chargingStation = getChargingStations().get(i);
				//chargingStation.get
				//Variable power1 = getVariableP(i, chargingStation.getPhaseConsumed(1).asInt(), k);
				//Variable power2 = getVariableP(i, chargingStation.getPhaseConsumed(2).asInt(), k);
				//Variable power3 = getVariableP(i, chargingStation.getPhaseConsumed(3).asInt(), k);
				
				Variable power1 = getVariableP(i, chargingStation.getPhaseGridToChargingStation(Phase.getByInt(1)).asInt(), k);
				Variable power2 = getVariableP(i, chargingStation.getPhaseGridToChargingStation(Phase.getByInt(2)).asInt(), k);
				Variable power3 = getVariableP(i, chargingStation.getPhaseGridToChargingStation(Phase.getByInt(3)).asInt(), k);
				
				equationP1P2.cloneAndAddVariable(power1,  1);
				equationP1P2.cloneAndAddVariable(power2, -1);
				equationP2P3.cloneAndAddVariable(power2,  1);
				equationP2P3.cloneAndAddVariable(power3, -1);
				equationP1P3.cloneAndAddVariable(power1, -1);
				equationP1P3.cloneAndAddVariable(power3, -1);
			}
			
			
			equationP1P2.cloneAndAddVariable(getVariableD(k, 1, 1, 2), -1);
			equationP1P2.cloneAndAddVariable(getVariableD(k, 0, 1, 2),  1);
			equationP1P2.RHS = 0;

			equationP2P3.cloneAndAddVariable(getVariableD(k, 1, 2, 3), -1);
			equationP2P3.cloneAndAddVariable(getVariableD(k, 0, 2, 3),  1);
			equationP2P3.RHS = 0;

			
			equationP1P3.cloneAndAddVariable(getVariableD(k, 1, 1, 3), -1);
			equationP1P3.cloneAndAddVariable(getVariableD(k, 0, 1, 3),  1);
			equationP1P3.RHS = 0;
			
			
			equationsV1.add(equationP1P2);
			equationsV2.add(equationP2P3);
			equationsV3.add(equationP1P3);
			
			
			
		}
		
		
		
		// V4-V7: Helper variables for previously loaded power
		log(1, "V4-V7... ", false, false);
		ArrayList<Equation> equationsV4 = new ArrayList<>();
		ArrayList<Equation> equationsV5 = new ArrayList<>();
		ArrayList<Equation> equationsV6 = new ArrayList<>();
		ArrayList<Equation> equationsV7 = new ArrayList<>();
		for (Variable variableQ : variablesQ.values()) {
			int i = variableQ.getIndex("i");
			int k = variableQ.getIndex("k");
			int n = variableQ.getIndex("n");
			Variable variableX = getVariableX(i, n);
			if (!(variableX.hasValue() && variableX.getValue() == 0)) {
				// If X_i,n=0: Q_i,k,n is not restricted by V4 and V5
				Car car = cars.get(n);
				
				Equation equationV4 = new Equation("V4", ConstraintType.GEQ);
				equationV4.setIndex("i", i);
				equationV4.setIndex("k", k);
				equationV4.setIndex("n", n);
				Equation equationV5 = new Equation("V5", ConstraintType.LEQ);
				equationV5.setIndex("i", i);
				equationV5.setIndex("k", k);
				equationV5.setIndex("n", n);
				
				
				// iterate over timeslots BEFORE current k
				for (int kPrime=0;kPrime<k;kPrime++) {
					for (int j=1;j<=3;j++) {
						Variable variableP = getVariableP(i, j, kPrime);
						equationV4.cloneAndAddVariable(variableP, 0.25*CONSTANTS.CHARGING_EFFICIENCY*car.isAvailableInt(kPrime));
						equationV5.cloneAndAddVariable(variableP, 0.25*CONSTANTS.CHARGING_EFFICIENCY*car.isAvailableInt(kPrime));
					}
				}
				
				equationV4.cloneAndAddVariable(variableX, -1 * CONSTANTS.M_HIGH_BOUND);
				equationV4.cloneAndAddVariable(variableQ, -1);
				equationV4.RHS = -1 * CONSTANTS.M_HIGH_BOUND;
				equationsV4.add(equationV4);
				
				equationV5.cloneAndAddVariable(variableX, CONSTANTS.M_HIGH_BOUND);
				equationV5.cloneAndAddVariable(variableQ, -1);
				equationV5.RHS = CONSTANTS.M_HIGH_BOUND;
				equationsV5.add(equationV5);
			}
			
			if (!(variableX.hasValue() && variableX.getValue() == 1)) {
				// If X_i,n=1: Q_i,k,n is not restricted by V6 and V7
				// UP bound for Q
				Equation equationV6 = new Equation("V6", ConstraintType.LEQ);
				equationV6.setIndex("i", i);
				equationV6.setIndex("k", k);
				equationV6.setIndex("n", n);
				
				equationV6.cloneAndAddVariable(variableQ, 1);
				equationV6.cloneAndAddVariable(variableX, -1*CONSTANTS.M_HIGH_BOUND);
				equationV6.RHS = 0;
				equationsV6.add(equationV6);
				
				// LO bound for Q
				Equation equationV7 = new Equation("V7", ConstraintType.GEQ);
				equationV7.setIndex("i", i);
				equationV7.setIndex("k", k);
				equationV7.setIndex("n", n);
				
				equationV7.cloneAndAddVariable(variableQ, 1);
				equationV7.cloneAndAddVariable(variableX, CONSTANTS.M_HIGH_BOUND);
				equationV7.RHS = 0;
				equationsV7.add(equationV7);
			}
		}
		
		// V8-V9: Helper variables for isChargingNeeded
		log(1, "V8-V9... ", false, false);
		ArrayList<Equation> equationsV8 = new ArrayList<>();
		ArrayList<Equation> equationsV9 = new ArrayList<>();
		for (Variable variableU : variablesU.values()) {
			int k = variableU.getIndex("k");
			int n = variableU.getIndex("n");
			Car car = cars.get(n);
			
			Equation equationV8 = new Equation("V8", ConstraintType.GEQ);
			equationV8.setIndex("k", k);
			equationV8.setIndex("n", n);
			
			Equation equationV9 = new Equation("V9", ConstraintType.LEQ);
			equationV9.setIndex("k", k);
			equationV9.setIndex("n", n);
			
			equationV8.cloneAndAddVariable(variableU, car.getMaxCapacity());
			equationV9.cloneAndAddVariable(variableU, car.getMaxCapacity());
			
			for (int i=0;i<chargingStations.size();i++) {
				Variable variableQ = getVariableQ(i, k, n);
				equationV8.cloneAndAddVariable(variableQ, 1);
				equationV9.cloneAndAddVariable(variableQ, 1);
			}
			
			equationV8.RHS = car.getMaxCapacity()-car.getCurrentCapacity();
			equationV9.RHS = car.getMaxCapacity() * 1.999 - car.getCurrentCapacity();
			
			equationsV8.add(equationV8);
			equationsV9.add(equationV9);
		}
		
		// V10: Helper variable for summed difference of power between two timeslots
		// 2nd objective function minimizes this
		log(1, "V10... ", false, false);
		ArrayList<Equation> equationsV10 = new ArrayList<>();
		for (int k=getKStart();k<getKEnd()-1 && this.objectivePeakShaving.getWeight()>0;k++) {
			Equation equationV10 = new Equation("V10", ConstraintType.EQU);
			equationV10.setIndex("k", k);
			equationV10.RHS = 0;
			
			for (int i=0;i<chargingStations.size();i++) {
				for (int j=1;j<=3;j++) {
					Variable variableP = getVariableP(i, j, k);
					equationV10.cloneAndAddVariable(variableP, -1);
					
					Variable variablePNext = getVariableP(i, j, k+1);
					equationV10.cloneAndAddVariable(variablePNext, 1);
				}
			}
			
			equationV10.cloneAndAddVariable(getVariableE(k, 1), 1);
			equationV10.cloneAndAddVariable(getVariableE(k, 0), -1);
			equationsV10.add(equationV10);
		}
		
		
		// V11-12: Helper variable for is car below min charging needs
		// Used by objective function for fair share
		log(1, "V11-V12...", false, false);
		ArrayList<Equation> equationsV11 = new ArrayList<>();
		ArrayList<Equation> equationsV12 = new ArrayList<>();
		for (Variable variableUPrime : variablesUPrime.values()) {
			int k = variableUPrime.getIndex("k");
			int n = variableUPrime.getIndex("n");
			Car car = cars.get(n);
			
			Equation equationV11 = new Equation("V11", ConstraintType.GEQ);
			equationV11.setIndex("k", k);
			equationV11.setIndex("n", n);
			
			Equation equationV12 = new Equation("V12", ConstraintType.LEQ);
			equationV12.setIndex("k", k);
			equationV12.setIndex("n", n);
			
			equationV11.cloneAndAddVariable(variableUPrime, car.minLoadingState);
			equationV12.cloneAndAddVariable(variableUPrime, car.minLoadingState*CONSTANTS.M_HIGH_BOUND);
			
			for (int i=0;i<chargingStations.size();i++) {
				Variable variableQ = getVariableQ(i, k, n);
				equationV11.cloneAndAddVariable(variableQ, 1);
				equationV12.cloneAndAddVariable(variableQ, 1);
			}
			
			equationV11.RHS = car.minLoadingState - car.getCurrentCapacity();
			equationV12.RHS = car.minLoadingState - car.getCurrentCapacity() + car.minLoadingState*CONSTANTS.M_HIGH_BOUND;
			
			
			equationsV11.add(equationV11);
			equationsV12.add(equationV12);
		}
		
		// V13-V16: Helper variable for % minSoC reached or % above minSoC
		log(1, "V13-V16...", false, true);
		ArrayList<Equation> equationsV13 = new ArrayList<>();
		ArrayList<Equation> equationsV14 = new ArrayList<>();
		ArrayList<Equation> equationsV15 = new ArrayList<>();
		ArrayList<Equation> equationsV16 = new ArrayList<>();
		for (Variable variableQPrime : variablesQPrime.values()) {
			int kMax = energyPriceHistory.getNTimeslots(); // get timeslot after last one is finished, to include the last P_i,j,k
			int n = variableQPrime.getIndex("n");
			Car car = cars.get(n);
			int below = variableQPrime.getIndex("below");
			
			if (below == 1) {
				Equation equationV13 = new Equation("V13", ConstraintType.GEQ);
				equationV13.setIndex("n", n);
				
				equationV13.cloneAndAddVariable(variableQPrime, 1);
				for (int i=0;i<chargingStations.size();i++) {
					Variable variableQ = getVariableQ(i, kMax, n);
					equationV13.cloneAndAddVariable(variableQ, 1.0/car.minLoadingState);
				}
				Variable variableUPrime = getVariableUPrime(kMax, n);
				equationV13.cloneAndAddVariable(variableUPrime, -1 * CONSTANTS.M_HIGH_BOUND);
				equationV13.RHS = 1 - car.getCurrentCapacity()/car.minLoadingState - CONSTANTS.M_HIGH_BOUND;
				
				
				Equation equationV14 = new Equation("V14", ConstraintType.GEQ);
				equationV14.setIndex("n", n);
				
				equationV14.cloneAndAddVariable(variableQPrime, 1);
				equationV14.cloneAndAddVariable(variableUPrime, CONSTANTS.M_HIGH_BOUND);
				equationV14.RHS = 0;
				
				equationsV13.add(equationV13);
				equationsV14.add(equationV14);
			}
			else if (below == 0) {
				Equation equationV15 = new Equation("V15", ConstraintType.GEQ);
				equationV15.setIndex("n", n);
				
				equationV15.cloneAndAddVariable(variableQPrime, 1);
				for (int i=0;i<chargingStations.size();i++) {
					Variable variableQ = getVariableQ(i, kMax, n);
					equationV15.cloneAndAddVariable(variableQ, 1.0 / (car.getMaxCapacity() - car.minLoadingState));
				}
				Variable variableUPrime = getVariableUPrime(kMax, n);
				equationV15.cloneAndAddVariable(variableUPrime, CONSTANTS.M_HIGH_BOUND);
				equationV15.RHS = 1 + (-car.getCurrentCapacity() + car.minLoadingState )/ (car.getMaxCapacity() - car.minLoadingState); 
				
				Equation equationV16 = new Equation("V16", ConstraintType.GEQ);
				equationV16.setIndex("n", n);
				
				equationV16.cloneAndAddVariable(variableQPrime, 1);
				equationV16.cloneAndAddVariable(variableUPrime, -1 * CONSTANTS.M_HIGH_BOUND);
				equationV16.RHS = 1 - CONSTANTS.M_HIGH_BOUND;
				
				equationsV15.add(equationV15);
				equationsV16.add(equationV16);
			}
			
			
			
		}
		
		
		// Assignment variable values for X from previous algorithms
		ArrayList<Equation> assignmentsX = new ArrayList<>();
		for (Variable variableX : variablesX.values())  {
			if (variableX.hasValue()) {
				Equation equationAssignment = new Equation("AX", ConstraintType.EQU);
				equationAssignment.setIndex("i", variableX.getIndex("i"));
				equationAssignment.setIndex("n", variableX.getIndex("n"));
				equationAssignment.cloneAndAddVariable(variableX, 1);
				equationAssignment.RHS = variableX.getValue();
				assignmentsX.add(equationAssignment);
				if (variableX.getValue() == 1) {
					log(2, "Assigning X_i" + variableX.getIndex("i")+"_n" + variableX.getIndex("n")+"=" + variableX.getValue());
				}
			}
		}
		// Assignment variable values for V isAboveMin to 1 if car is not suspendable
		ArrayList<Equation> assignmentsV = new ArrayList<>();
		for (Variable variableV : variablesV.values()) {
			int n = variableV.getIndex("n");
			Car car = cars.get(n);
			if (car.isSuspendable() == false) {
				int k = variableV.getIndex("k");
				Equation equationAssigment = new Equation("AV", ConstraintType.EQU);
				equationAssigment.setIndex("k", k);
				equationAssigment.setIndex("n", n);
				equationAssigment.cloneAndAddVariable(variableV, 1);
				equationAssigment.RHS = 1;
				assignmentsV.add(equationAssigment);
				log(2, "Assigning V_k" + k+"_n" + k+"=" + 1);
			}
		}
		
		
		
		ArrayList<Equation> allRestrictions = new ArrayList<>();
		allRestrictions.addAll(equationsR1);
		allRestrictions.addAll(equationsR2);
		allRestrictions.addAll(equationsR3);
		allRestrictions.addAll(equationsR4);
		allRestrictions.addAll(equationsR5);
		allRestrictions.addAll(equationsR6);
		allRestrictions.addAll(equationsR7);
		allRestrictions.addAll(equationsR8);
		allRestrictions.addAll(equationsR9);
		allRestrictions.addAll(equationsR10);
		allRestrictions.addAll(equationsR11);
		allRestrictions.addAll(equationsR12);
		allRestrictions.addAll(equationsR13);
		allRestrictions.addAll(equationsR14);
		allRestrictions.addAll(equationsR15);
		allRestrictions.addAll(equationsV1);
		allRestrictions.addAll(equationsV2);
		allRestrictions.addAll(equationsV3);
		allRestrictions.addAll(equationsV4);
		allRestrictions.addAll(equationsV5);
		allRestrictions.addAll(equationsV6);
		allRestrictions.addAll(equationsV7);
		allRestrictions.addAll(equationsV8);
		allRestrictions.addAll(equationsV9);
		allRestrictions.addAll(equationsV10);
		allRestrictions.addAll(equationsV11);
		allRestrictions.addAll(equationsV12);
		allRestrictions.addAll(equationsV13);
		allRestrictions.addAll(equationsV14);
		allRestrictions.addAll(equationsV15);
		allRestrictions.addAll(equationsV16);
		
		allRestrictions.addAll(assignmentsX);
		allRestrictions.addAll(assignmentsV);
		
		return allRestrictions;
	}

	
	/**
	 * 
	 * @param allRestrictions OPTIONAL. 
	 * @return
	 */
	private String constructProblemLPFile(ArrayList<Equation> allRestrictions) {
		if (variablesP == null) {
			log(0, "ERROR: Call constructVariables first!");
			throw new NullPointerException();
		}
		
		
		Equation objectiveFunction = constructObjectiveFunction();
		if (allRestrictions == null) {
			allRestrictions = constructAllRestrictions();
		}
		
		
		ArrayList<Equation> allEquations = new ArrayList<>();
		allEquations.add(objectiveFunction);
		allEquations.addAll(allRestrictions);
		
		log(1, "--------------------------------------", false);
		log(1, "---- Number of variables: " 
				+ (variablesP.size()+variablesD.size()+variablesQ.size()+variablesQPrime.size()) + "cont+" 
				+ (variablesX.size()+variablesU.size()+variablesUPrime.size()+variablesV.size()) +"bin ----", false);
		log(1, "---- Number of restrictions: " + allRestrictions.size() + " ----", false);
		log(1, "--------------------------------------", false);
		
		log(1, "Constructing CPLEX file...");
		InstanceWriter instanceWriter = new InstanceWriter();
		String lpFileContent = instanceWriter.generateLPFile(objectiveFunction, 
				allRestrictions, variablesX, variablesU, variablesUPrime, variablesV);
		return lpFileContent;
	}
	
	private void writeProblemLPFile(String content) {
		String pathInstanceLPFile = CONSTANTS.PATH_DIR_GEN_TEMP + Util.generateGUID() + ".lp";
		this.pathInstanceLPFile = pathInstanceLPFile;
		FileIO.writeFile(pathInstanceLPFile, content);
	}

	public Variable getVariableX(int i, int n) {
		return variablesX.get(i, n);
	}
	public Variable getVariableP(int i, int j, int k) {
		return variablesP.get(i, j, k);
	}
	public Variable getVariableD(int k, int sign, int jFrom, int jTo) {
		return variablesD.get(k, sign, jFrom, jTo);
	}
	public Variable getVariableE(int k, int sign) {
		return variablesE.get(k, sign);
	}
	public Variable getVariableQ(int i, int k, int n) {
		return variablesQ.get(i, k, n);
	}
	public Variable getVariableU(int k, int n) {
		return variablesU.get(k, n);
	}
	public Variable getVariableUPrime(int k, int n) {
		return variablesUPrime.get(k, n);
	}
	public Variable getVariableV(int k, int n) {
		return variablesV.get(k, n);
	}
	
	
	private Solver solver;
	public String pathInstanceLPFile;
	public String pathOutputSolFile;
	
	public void setSolver(Solver solver) {
		this.solver = solver;
		this.pathOutputSolFile = CONSTANTS.PATH_DIR_GEN_TEMP + Util.generateGUID() + ".sol";
	}
	
	public void solveProblem() {
		try (MethodTimer t = new MethodTimer(this.timeSolution)) {
			solver.solveProblem(pathInstanceLPFile, pathOutputSolFile);
		}
	}
	
	@Override
	public JSONObject getSolutionJSON() {
		JSONObject result = solver.getSolutionJSON(this.pathOutputSolFile);
		return result;
	}


	@Override
	public String getMethod() {
		return "lp";
	}
}













