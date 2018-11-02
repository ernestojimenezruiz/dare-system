package uk.ac.mas.dare;

public enum AttackStatus {
	/**
	 * Status is not known
	 */
	UNKNOWN,
	
	/**
	 * Mapping survived attack, and is un
	 */
	IN,
	
	/**
	 * Mapping has been attacked and is out
	 */
	OUT,
	
	/**
	 * Mapping is blocked by another mapping - probably due to equal probabilities
	 */
	BLOCKED
}
