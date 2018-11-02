package uk.ac.mas.dare;

import java.math.BigDecimal;

/**
 * This is a singleton for the Aggregation functions. It provides a single call to select the type of
 * aggregation function required, and then gives a single point of access to the aggregation function.
 *  
 * @author trp
 *
 */
public class AggregationSingleton {
	
	/**
	 * Set an enum to restrict the type of aggregation function used.
	 * Currently based on the ISWC 2014 submission which defines two
	 * Aggregation Functions:
	 * 	MAX - returns the maximum of the two values supplied
	 * 	AVG - returns an average (BigDecimal-based) of the two values supplied
	 *  CREDULOUS - returns an average of the two values if both are known;
	 *  			otherwise it returns the value of one if the other is zero
	 *  SCEPTICAL - returns an average of the two values if both are known;
	 *  			otherwise it returns -1.0 (i.e. should be below threshold)
	 * @author trp
	 *
	 */
	public enum FnType {
		   MAX, AVG, CREDULOUS, SCEPTICAL
		 }
	
	// private static final long PROBABILITYSCALEFACTOR = 100000;		// Some scalefactor to get a decent precision on our fudged division
	private static AggregationSingleton instance = null;				// The instance of the singleton, constructed using lazy evaluation.
	private static FnType fntype = FnType.AVG;					// The type of function to be used by the singleton; by default, CREDULOUS.
	private static double FAIL = -1.0;
	/**
	 * Create the getters/setters for the function type
	 * @return
	 */
	public FnType getFntype() {
		return fntype;
	}

	public void setFntype(FnType fntype) {
		AggregationSingleton.fntype = fntype;
	}

	/**
	 * Manage the construction of the singleton, following the standard
	 * design pattern.  The code here is based on the example at:
	 * http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
	 */
	protected AggregationSingleton() {
		// Exists only to defeat instantiation.
	}
	
	public static AggregationSingleton getInstance() {
		if(instance == null) {
			instance = new AggregationSingleton();
		}
		return instance;
	}

	/**
	 * Aggregates two doubles, using the currently selected aggregation function.
	 * NOTE: use the method setFntype(FnType) to set this to either FnType.MAX or FnType.AVG
	 * @param num1
	 * @param num2
	 * @return the two doubles, aggregated using the selected aggregation function.
	 */
	public double aggregate (double num1, double num2)
	{
		double retVal = num1;		// Set default return value
		
		switch (fntype) {
		case AVG:
			retVal = getAverage(num1, num2);
			break;
		case MAX:
			retVal = getMax(num1, num2);
			break;
		case CREDULOUS:
			// NOTE THAT THIS DOES NOT DIFFERENTIATE BETWEEN 0 (unknown) and 0 (upperbound)
			if (0==num1) {
				retVal = num2;
			} else if (0==num2) {
				retVal = num1;
			} else {
				retVal = getAverage(num1, num2);
			}
			break;
		case SCEPTICAL:
			if ((0!=num1) && (0!=num2)) {
				retVal = getAverage(num1, num2);
			} else {
				retVal = FAIL;
			}
			break;

		default:
			System.err.println("AggregationSingleton: no default function selected; returning num1");
			break;
		}
		
		// System.out.println("Agg "+fntype+" ("+num1+","+num2+") = "+retVal);
		return retVal;
	}
	
	/**
	 * Generates the average of the two inputs, using BigDecimal
	 * @param first double value
	 * @param second double value
	 * @return the mean of the two values
	 */
	private double getAverage(double first, double second) {
				
        BigDecimal decimal1 = new BigDecimal(first);  
        BigDecimal decimal2 = new BigDecimal(second); 
        BigDecimal sum = decimal1.add(decimal2);
        BigDecimal divisor = new BigDecimal(2.0);
        return sum.divide(divisor).doubleValue();
	}
	
	/**
	 * Generates the maximum of the two input values
	 * @param first double value
	 * @param second double value
	 * @return the max of the two values
	 */
	private double getMax(double first, double second) {

		// The following code replaces the average result with the maximum of the
		// two input values.
		return (first>second)?first:second;
		
	}


}
