package uk.ac.mas.dare;

import java.util.ArrayList;
/**
 * 
 * This class represents a negotiation round commitment store, which is the object
 * that maintains two variants of a commitment store (one for the local agent, and
 * one for the collaborating agent, and a list of the messages that were exchanged
 * during the current negotiation round.  A negotiation round consists of messages
 * starting from an ASSERT, and terminating with a ENDASSERTION.
 * 
 * @author      Terry Payne
 * @version     %I%, %G%
 * @since       1.0
 *
 */

public class NegotiationRoundCommitmentStore {	
	// We maintain two Candidate Alignment structures to store the mappings from
	// this agent and the collaborator agent, and methods will be provided to manage
	// this data.
	
	private CandidateAlignment myAlignment;
	private CandidateAlignment opponentsAlignment;
	
	// We also maintain a list of all of the messages that are sent back and forth
	// between the two agents
	
	private ArrayList<CommsObject> messageList;
	
	// Finally we maintain a reference to the argumentation framework, so that it
	// can be used to generate verbose output, etc.
	
	// private ArgumentationFramework myArgumentationFramework;
	
	// ============================================================================

	/**
	 * Getter for the {@link messageList} object.
	 * @return The array of CommsObject messages.
	 */
	public ArrayList<CommsObject> getMessageList() {
		return messageList;
	}

	/**
	 * Getter for the {@link myAlignment} object (i.e. the agent's own CommitmentStore).
	 * @return A {@link CandidateAlignment} object containing the elements of the Agent's own CommitmentStore.
	 */
	public CandidateAlignment getMyAlignment() {
		return myAlignment;
	}

	/**
	 * Getter for the {@link opponentsAlignment} object (i.e. the agent's opponents CommitmentStore).
	 * @return A {@link CandidateAlignment} object containing the elements of the Agent's opponents CommitmentStore.
	 */
	public CandidateAlignment getOpponentsAlignment() {
		return opponentsAlignment;
	}

	// ============================================================================

	/**
	 * @return the af
	 */
//	public ArgumentationFramework getMyArgumentationFramework() {
//		return myArgumentationFramework;
//	}

	/**
	 * @param af the af to set
	 */
//	public void setMyArgumentationFramework(ArgumentationFramework af) {
//		this.myArgumentationFramework = af;
//	}

	/**
	 * Constructor that creates a new instance, and stores the comms object as the
	 * first element of the message list.  It then unpacks the relevant data and
	 * updates the relevant stores.
	 * @param co The CommsObject message that was received. 
	 * @param received True if the message was received from another agent,
	 * and false if the message is one that the agent itself created.  This
	 * determines in which CommitmentStore the messages are stores.
	 */
	public NegotiationRoundCommitmentStore(CommsObject co, boolean received) {
		
		// Reset the argumentation framework reference - this is set once an argumentation
		// takes place
//		this.myArgumentationFramework = null;
		
		// Create the array to hold the messages in this round
		this.messageList = new ArrayList<CommsObject>();
		this.messageList.add(co);

		// Create the Alignments to store the details of the mappings exchanged
		myAlignment = new CandidateAlignment();
		opponentsAlignment = new CandidateAlignment();
		
		// Now handle the mapping that was exchanged in the message
		this.addMapping(co, received);
	}

	/**
	 * Adds the mappings (if any) found in the CommsObject message co to
	 * the CommitmentStore.
	 * @param co The CommsObject message that was received. 
	 * @param received True if the message was received from another agent,
	 * and false if the message is one that the agent itself created.  This
	 * determines in which CommitmentStore the messages are stores.
	 */
	public void addMessage(CommsObject co, boolean received) {
		
		this.messageList.add(co);
		this.addMapping(co, received);
	}
	
	/**
	 * Takes a CommsObject message, and looks to see if there is a mapping
	 * and justification mapping, and if so, adds these to the CommitmentStore.
	 * If either of the mappings already exist in the CommitmentStore, then
	 * nothing is added
	 * @param co The CommsObject message that was received.
	 * @param received True if the message was received from another agent,
	 * and false if the message is one that the agent itself created.  This
	 * determines in which CommitmentStore the messages are stores.
	 */
	private void addMapping(CommsObject co, boolean received) {
		
		// Now handle the mapping that was exchanged in the message
		CandidateAlignment localAlignment = null;
		if (received) {
			localAlignment = opponentsAlignment;
		} else {
			localAlignment = myAlignment;
		}
				
		// First, check that there was even a mapping to store.  This
		// might not be the case with CLOSE messages etc.
		CandidateMapping myMapping = co.getMapping();
		if (myMapping != null) {
			CandidateMapping cm = localAlignment.lookup(co.getMapping());
			if (cm == null) {
				localAlignment.getCandidateMappings().add(co.getMapping());
//			} else {
//				System.err.println("Mapping "+cm.toString() + " found in CS");
			}
		}
		
		// In the case of objections, there may be a justification,
		// which also needs to be added
		// NOTE THAT THE CODE (BELOW) is a repeat of the code above!!!
		CandidateMapping justification = co.getJustification();
		if (justification != null) {
			CandidateMapping cm = localAlignment.lookup(justification);
			if (cm == null) {
				localAlignment.getCandidateMappings().add(justification);
//			} else {
//				System.err.println("Mapping (justification) "+cm.toString() + " found in CS");
			}

		}

	}
	
	/**
	 * Constructs a pretty-print version of the contents of the current NegotiationRound object.
	 */
	public String toString() {
		String result = new String();
//		AggregationSingleton ags = AggregationSingleton.getInstance();
		
		result += messageList.size() + " messages\n";
		result += "==================\n";
		for (CommsObject co:messageList) {
			result += co.toString() + "\n";
		}
		// result += "==================\n";

		// This goes through the mappings in myAlignments, and generates stats for those mappings.
		// Checks are made on the number of mappings in both alignments being equal, and that the
		// mappings in myAlignments have a counterpart in opponentsAlignments.  However, the tests
		// are not exhaustive, and shouldn't be assumed to be complete.
		
		ArrayList<CandidateMapping> myMappings = this.myAlignment.getCandidateMappings();
		ArrayList<CandidateMapping> opponentsMappings = this.opponentsAlignment.getCandidateMappings();
		if (myMappings.size() != opponentsMappings.size()) {
			result += "Warning: the internal alignments differ in size";
		} else {
			for (CandidateMapping myCM:myMappings) {
				for (CandidateMapping oppCM:opponentsMappings) {
					if (myCM.getSourceConcept().equals(oppCM.getSourceConcept()) &&
							myCM.getTargetConcept().equals(oppCM.getTargetConcept())) {
						// we have our matching mappings
						result += myCM.getSourceConcept() + " " +
								myCM.getRelationship() + " " +
								myCM.getTargetConcept() + " " +
								myCM.getProbability() + "/" + oppCM.getProbability() +
								"\n";
//								" (" + ags.aggregate(myCM.getProbability(), oppCM.getProbability()) + ")\n";
						continue;
					}
				}
			}
		}
		return result;
	}

}
