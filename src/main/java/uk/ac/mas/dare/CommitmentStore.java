package uk.ac.mas.dare;

import java.util.ArrayList;
/**
 * 
 * This class stores a list of {@link NegotiationRoundCommitmentStore} objects
 * representing the different communication rounds.
 *   
 * @author      Terry Payne
 * @version     %I%, %G%
 * @since       1.0
 *
 */
public class CommitmentStore {

	// We store mappings that are gleaned from the corresponding agents in a
	// CandidateAlignment
	// for convenience
	// private CandidateAlignment knownAlignment;

	// Need to store an Array of rounds, where each round contains a list of
	// messages.
	private ArrayList<NegotiationRoundCommitmentStore> negotiationRoundList;

	// ============================================================================
	// ============================================================================
	// Constructors
	
	/**
	 * Creates the CommitmentStore, and initializes the empty {@link NegotiationRoundCommitmentStore} list.
	 */
	public CommitmentStore() {
		this.negotiationRoundList = new ArrayList<NegotiationRoundCommitmentStore>();
	}

	// ============================================================================
	// ============================================================================
	// Getters and setters
	
	/**
	 * Getter that returns the negotiationRoundList.
	 * @return An array of {@link NegotiationRoundCommitmentStore} objects.
	 */
	public ArrayList<NegotiationRoundCommitmentStore> getNegotiationRoundList() {
		return negotiationRoundList;
	}
	
	/**
	 * Getter that returns the most recent {@link NegotiationRoundCommitmentStore} object
	 * from the negotiationRoundList.
	 * @return The most recent {@link NegotiationRoundCommitmentStore} object corresponding to the current negotiation round.
	 */
	public NegotiationRoundCommitmentStore getCurrentNegotiationRoundCS() {
		if (negotiationRoundList.size() <= 0) {
			// Note that this can happen when checking the probabilities of an assert, so
			// we no longer report a possivle problem
//			System.err.println("Problem in getCurrentNegotiationRoundCS - no current round)");
			return null;
		}
		// The size of the list must be at least one.  Therefore we shouldn't
		// get an error with invalid index here.
		return negotiationRoundList.get(negotiationRoundList.size()-1);
	}

	// ============================================================================
	// ============================================================================
	// Methods
	
	/**
	 * Creates a new {@link NegotiationRoundCommitmentStore} object that is added to our
	 * list of such objects, which represents the different rounds of the negotiation.
	 * The object is added to the end of the list, in which the message forms the first
	 * element of the objects messages.  In addition, the mappings found in that message
	 * are added to the appropriate CommitmentStores.  The object is then appended to
	 * the the {@link negotiationRoundList}.
	 * @param co The {@link CommsObject} object containing the message that initiated the negotiation round.
	 * @param received True if the agent received the message, or false if the agent sent the message.
	 */
	private void newProposal(CommsObject co, boolean received) {
		// Create a new element in the list, and pass on the message
		NegotiationRoundCommitmentStore newStore =
				new NegotiationRoundCommitmentStore(co, received);
		negotiationRoundList.add(newStore);
	}

	/**
	 * Wrapper method called when a new proposal message is created and sent to another agent.
	 * Results in the agent's commitment store creation for the new round.
	 * @param co The {@link CommsObject} object containing the message that initiated the negotiation round.
	 */
	public void newProposalSent(CommsObject co) {
		this.newProposal(co, false);
	}

	/**
	 * Wrapper method called when a new proposal message has been received by the agent.
	 * Results in the agent's commitment store creation for the new round.
	 * @param co The {@link CommsObject} object containing the message that initiated the negotiation round.
	 */
	public void newProposalReceived(CommsObject co) {
		this.newProposal(co, true);
	}
	
	// ====================================================================
	/**
	 * Adds a new entry to the current {@link NegotiationRoundCommitmentStore} object, and updates
	 * the relevant message stores and Commitment Stores.
	 * @param co The {@link CommsObject} object containing the new message.
	 * @param received True if the agent received the message, or false if the agent sent the message.
	 */
	public void newResponse(CommsObject co, boolean received) {
		
		NegotiationRoundCommitmentStore currentRoundCS = getCurrentNegotiationRoundCS();
		if (currentRoundCS != null) {
			currentRoundCS.addMessage(co, received);
		}
	}
	
	/**
	 * Wrapper method called when a new message is sent to or received by another agent.
	 * Results in the agent's commitment store update given this sent message.
	 * @param co The {@link CommsObject} object containing the received message.
	 */
	public void newResponseSent(CommsObject co) {

		this.newResponse(co, false);
	}
	/**
	 * Wrapper method called when a new message is sent to or received by another agent.
	 * Results in the agent's commitment store update given this received message.
	 * @param co The {@link CommsObject} object containing the message received.
	 */
	public void newResponseReceived(CommsObject co) {

		this.newResponse(co, true);
	}

	// ============================================================================

	/**
	 * Checks to see if the mapping otherMapping is in one of the CommitmentStores
	 * and if found, returns the corresponding mapping object.
	 * <P>
	 * This is the generic version of the other lookup functions in this class.
	 * @param mapping the candidate mapping that we are checking to see if
	 * it is known.  Note that the check will be based on string comparisons with
	 * the source and target atoms, and not object equivalence.
	 * @param inMyCS true if the lookup is in the local CS, and false if the lookup
	 * is to be in the opponent's CS.
	 * @return a CandidateMapping object that shares the source and target strings with otherMapping
	 */
	public CandidateMapping lookup(CandidateMapping mapping, boolean inMyCS) {
		NegotiationRoundCommitmentStore currentRoundCS = getCurrentNegotiationRoundCS();
		CandidateMapping result = null;
		if (currentRoundCS != null) {
			if (inMyCS == true) {
				result = currentRoundCS.getMyAlignment().lookup(mapping);
			} else {
				result = currentRoundCS.getOpponentsAlignment().lookup(mapping);
			}
		}
		return result;
	}
	// provides a shortcut to the lookup method in the candidateAlignment
	/**
	 * Checks to see if the mapping otherMapping is in the Agent's own CommitmentStore
	 * (as opposed to that containing received mappings from the collaborating agent),
	 * and if found, returns the corresponding mapping object.
	 * @param myMapping the candidate mapping that we are checking to see if
	 * it is known.  Note that the check will be based on string comparisons with
	 * the source and target atoms, and not object equivalence.
	 * @return a CandidateMapping object that shares the source and target strings with otherMapping
	 */
	public CandidateMapping lookup(CandidateMapping myMapping) {
		return this.lookup(myMapping, true);
	}
	
	/**
	 * Checks to see if the mapping otherMapping is in the opponent Agent's CommitmentStore
	 * and if found, returns the corresponding mapping object.
	 * @param otherMapping the candidate mapping that we are checking to see if
	 * it is known.  Note that the check will be based on string comparisons with
	 * the source and target atoms, and not object equivalence.
	 * @return a CandidateMapping object that shares the source and target strings with otherMapping
	 */
	public CandidateMapping lookupOpponent(CandidateMapping otherMapping) {
		return this.lookup(otherMapping, false);
	}


}
