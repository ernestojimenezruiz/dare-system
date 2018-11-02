package uk.ac.mas.dare;

import java.util.Comparator;
import java.util.Random;

public class CandidateMapComparator implements Comparator<CandidateMapping>{

	static Random randomGenerator;
	
	@Override
    public int compare(CandidateMapping cm1, CandidateMapping cm2) {
//		if (randomGenerator == null)
//			randomGenerator = new Random();
		
//		return (cm1.getProbability()>cm2.getProbability() ? -1 : (cm1.getProbability()>cm2.getProbability() ? 0 : 1));
				
		if (cm1.getProbability()<cm2.getProbability())
			return 1;
		if (cm1.getProbability()>cm2.getProbability())
			return -1;
		
		// They could be equal.  Determine order on lexographic ordering
		return cm1.getSourceConcept().compareTo(cm2.getSourceConcept());
	
	}
}
