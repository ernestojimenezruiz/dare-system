package uk.ac.mas.dare;

import org.semanticweb.owl.align.Cell;

public class CandidateMapping {

	// ====================================
	// Euzenat/Cell methods
	// ====================================
	private Cell mapping;
	
	private double probability;
	private double jointWeight;
	private int count;
	private AttackStatus argStatus;
	
	public CandidateMapping(Cell mapping, double probability) {
		this.mapping = mapping;
		this.probability = probability;
		this.jointWeight = -1.0;		// If the value is < 0 then ignore this.
		this.count = 1;
		this.argStatus = AttackStatus.UNKNOWN;
	}
	public CandidateMapping(Cell mapping) {
		this(mapping, 0.0);
	}
	
	public CandidateMapping(CandidateMapping cm) {
		this(cm.mapping, 0.0);
	}

	// Getters that abstract access to the elements of the cell.
	public String getSourceConcept() {
		return this.mapping.getObject1().toString();
	}
	public String getTargetConcept() {
		return this.mapping.getObject2().toString();
	}
	public String getRelationship() {
		return this.mapping.getRelation().getRelation();
	}
	
	public double getJointWeight() {
		return this.jointWeight;
	}

	public void setJointWeight(double weight) {
		this.jointWeight = weight;
	}

	public void copyStrengthToProbability() {
		this.probability = this.mapping.getStrength();
	}
	
	public Cell getMapping() {
		return mapping;
	}

	/*
	// ====================================
	// Logmap methods
	// ====================================
	private MappingObjectStr mapping;
	

	// ==============================================================

	public CandidateMapping(MappingObjectStr mapping, double probability) {
		this.mapping = mapping;
		this.probability = probability;
		this.count = 1;
		this.argStatus = AttackStatus.UNKNOWN;
	}
	
	public CandidateMapping(MappingObjectStr mapping) {
		this(mapping, 0.0);
	}

	public CandidateMapping(CandidateMapping cm) {
		this(cm.mapping, 0.0);
	}
	
	// Getters that abstract access to the elements of the cell.
	public String getSourceConcept() {
		return this.mapping.getIRIStrEnt1();
	}
	public String getTargetConcept() {
		return this.mapping.getIRIStrEnt2();
	}
	public String getRelationship() {
		return Integer.toString(this.mapping.getTypeOfMapping());
	}

	public void copyStrengthToProbability() {
		this.probability = this.mapping.getConfidence();
	}

	*/
	// ------------------------------------

	public double getProbability() {
		return probability;
	}

	
	/*
	 * m.getConfidence()
	 * int res = m1.getIRIStrEnt1().compareToIgnoreCase(m2.getIRIStrEnt1());
	 * 
	 */

	public int getCount() {
		return count;
	}

	public AttackStatus getArgStatus() {
		return argStatus;
	}

	public void setArgStatus(AttackStatus argStatus) {
		this.argStatus = argStatus;
	}

	// ==========================================================
	// Other Methods
	
	public String toString() {
//		String str = "<" + this.probability +
////				" (" + this.combinedProbability + ")" +
//				", {" + this.getSourceConcept() +
//				" " + this.getRelationship() +
//				" " + this.getTargetConcept() +
//				"}> ";
		
		String str = "<" +
				this.getSourceConcept() +
				" " + this.getRelationship() +
				" " + this.getTargetConcept() +
				">, ";
		
		if (this.jointWeight < 0)
			str += this.probability;
		else
			str += this.jointWeight;

		return str;
	}
	
	public void incrementCount() {
		this.count++;
	}
	public void updateProbabililty(int numberOfAlignments) {
		this.probability = ((double) this.count) / ((double) numberOfAlignments);
		this.mapping.setStrength(this.probability);
	}
	public void randomiseProbabililty() {
		this.probability = Math.random();	// a random number between 0..1
	}
	public void uniformProbabililty() {
		this.probability = 1.0;
	}
	
}
