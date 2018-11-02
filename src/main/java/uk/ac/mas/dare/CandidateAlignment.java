package uk.ac.mas.dare;

import java.util.ArrayList;
import java.util.Random;

public class CandidateAlignment {
	
	// For now, our code provides an abstraction to the Alignment class used by the Ontology API
	// and essentially this construct maintains collections of mappings.  If necessary, we can
	// construct new Alignment API Alignments, and latter, the metadata could also be exploited.
	// The unknown issue as yet is what is actually needed to check the cohesion property of a
	// mapping given some alignment and the available ontologies.
	
	private ArrayList<CandidateMapping> candidateMappings;
	static Random randomGenerator;

	
	// ========================================================
	// Constructors
	// TODO Need to understand what constructors etc might be needed
	
	// Creates an empty CandidateAlignment
	public CandidateAlignment() {
		this.candidateMappings = new ArrayList<CandidateMapping>();
	}

	// Creates a CandidateAlignment given an array of Candidate Mappings
	public CandidateAlignment(ArrayList<CandidateMapping> cmlist) {
		this.candidateMappings = new ArrayList<CandidateMapping>();
		for (CandidateMapping cm:cmlist) {
			this.candidateMappings.add(cm);
		}
	}

	// Creates a duplicate of the CandidateAlignment
	public CandidateAlignment(CandidateAlignment ca) {
		this.candidateMappings = new ArrayList<CandidateMapping>();
		for (CandidateMapping cm:ca.getCandidateMappings()) {
			this.candidateMappings.add(cm);
		}
	}

	// ========================================================
	// Getters and Setters
	
	public ArrayList<CandidateMapping> getCandidateMappings() {
		return candidateMappings;
	}
	
	// ========================================================
	// toString - pretty prints
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
	    if (this.getCandidateMappings().size() > 0) {
	    	for (CandidateMapping cm:this.getCandidateMappings()) {
	    		sb.append(cm.toString());
	    		sb.append("\n");
	    	}
	    	sb.append("    ---------------\n");
	    } else {
	    	sb.append("    Empty Alignment\n");
	    }
    	return(sb.toString());
	}
	
	// ========================================================
	// score - scores the alignment by calculating the average
	// probability of the constituent mappings
	public double score() {
		double retVal = 0.0;
		int numMappings = this.getCandidateMappings().size();
		if (numMappings > 0) {
	    	for (CandidateMapping cm:this.getCandidateMappings()) {
	    		retVal += cm.getProbability();
	    	}
	    	retVal /= (double) numMappings;
		}
		return retVal;
	}
		
	// =================================================================================
	// hasMappings - returns true if we have mappings in the alignment
	public boolean hasMappings() {
		// Note that the original code for this checked if the candidateMappings
		// array was null; for the current version, I am now assuming that this
		// is always set, as there are no setters for this array (and therefore
		// it *shouldn't* in theory be changed to another value), and that all of
		// the constructors allocate this structure.
		
		// However, I've added the test, and inserted a warning to point this out
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:hasMappings");
			return false;
		}
		
		if (this.getCandidateMappings().size()>0) {
			return true;
		}
		return false;
	}
	
	// =================================================================================
	// head - return the first element of the candidate mapping
	public CandidateMapping head() {
		
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:head");
			return null;
		}
		
		if (this.getCandidateMappings().size()>0) {
			return this.getCandidateMappings().get(0);
		}
		return null;
	}

	// =================================================================================
	// deleteHead - delete the first element of the candidate mapping.  If successful, it
	// returns the head, which has been successfully deleted.  Otherwise, null is returned.
	// In many ways this acts like tail, except that tail constructs a new array, whereas
	// this modifies the stored array.
	public CandidateMapping deleteHead() {
		
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:head");
			return null;
		}
		
		CandidateMapping head = null;
		
		if (this.getCandidateMappings().size()>0) {
			head = this.getCandidateMappings().remove(0);
		}
		return head;
	}

	// =================================================================================
	// tail - return a candidate alignment containing all but the first element of the
	// candidate mapping as an array
	public CandidateAlignment tail() {
		
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:tail");
			return null;
		}
		
		int numMappings = this.getCandidateMappings().size();
		CandidateAlignment newAlignment = new CandidateAlignment();
		
		// Not sure I need the following test, but I've added it for clarity
		if (numMappings > 1) {
			for (int i = 1; i < numMappings; i++) {
				newAlignment.getCandidateMappings().add(
						this.getCandidateMappings().get(i));
			}
		}
		return newAlignment;
	}
	
	// =================================================================================
	// findMappingsMatching:mapping - return an array of all of the mappings that share
	// the same source, or target, of mapping.
	public ArrayList<CandidateMapping> findMappingsMatching(CandidateMapping mapping) {
		
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:findMappingsMatching");
			return null;
		}

		ArrayList<CandidateMapping> mappingList = new ArrayList<CandidateMapping>();
		
		for (CandidateMapping cm:this.getCandidateMappings()) {
			if(cm.getSourceConcept().equals(mapping.getSourceConcept())) {
				mappingList.add(cm);
			} else if(cm.getTargetConcept().equals(mapping.getTargetConcept())) {
				mappingList.add(cm);
			}
		}
		return mappingList;
	}

	// =================================================================================
	// findComplementOfMappingsMatching - return an array of all of the mappings that do
	// NOT share the same source, or target, of "mapping".
	public ArrayList<CandidateMapping> findComplementOfMappingsMatching(CandidateMapping mapping) {
		
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:findComplementOfMappingsMatching");
			return null;
		}

		ArrayList<CandidateMapping> complementOfArray = new ArrayList<CandidateMapping>();
		
		for (CandidateMapping cm:this.getCandidateMappings()) {
			if(cm.getSourceConcept().equals(mapping.getSourceConcept())) {
				; // do nothing
			} else if(cm.getTargetConcept().equals(mapping.getTargetConcept())) {
				; // do nothing
			} else {
				complementOfArray.add(cm);
			}
		}
		return complementOfArray;
	}

	// =================================================================================
	// lookup - check the mappings to see if there is one with the same source and target
	// and if found, returns the mapping.  If it cannot be found, then null is returned.
	
	public CandidateMapping lookup(CandidateMapping mapping) {
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:lookup");
			return null;
		}
		
		CandidateMapping returnMapping = null;
		for (CandidateMapping cm:this.getCandidateMappings()) {
			if((cm.getSourceConcept().equals(mapping.getSourceConcept())) &&
					(cm.getTargetConcept().equals(mapping.getTargetConcept()))) {
				returnMapping = cm;
				break;
			}
		}

		return returnMapping;
	}

	/**
	 * Deletes one of the mappings at random
	 * @return the mapping that was deleted
	 */
	public CandidateMapping deleteRandomMapping() {
		// See comment on the need for this test in "hasMappings"
		if (this.getCandidateMappings() == null) {
			System.err.println("Unexpected error - no candidateMappings array in method:deleteRandomMapping");
		}

		// Create the random number generator, if necessary.
		if (randomGenerator == null)
			randomGenerator = new Random();
		
		int alSize = this.getCandidateMappings().size();
		CandidateMapping cm = null;
		
		if (alSize>0) {
		    int victim = randomGenerator.nextInt(alSize);
			cm = this.getCandidateMappings().remove(victim);
			// System.out.println("Deleting victim number "+victim);
		}
		return cm;
		
	}


}
