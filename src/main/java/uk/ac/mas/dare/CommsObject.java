package uk.ac.mas.dare;

//import org.semanticweb.owl.align.Cell;

import java.util.Set;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class CommsObject {

	// ===================================================
	// Instance Variables
	private String sendingAgent;
	private String recipientAgent;
	private DialogueType dialogueType;
	private String message;
	private int messageID;
	private int subMessageID;
	private CandidateMapping mapping;
	private MappingObjectStr logMapMapping;
	private Set<MappingObjectStr> repairSet;
	// private CandidateMapping originalMapping;
	private CandidateMapping justification;

	private static int currentMessageID = 1;

	// ===================================================
	// Getters and Setters
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSendingAgent() {
		return sendingAgent;
	}

	public String getRecipientAgent() {
		return recipientAgent;
	}

	public DialogueType getDialogueType() {
		return dialogueType;
	}

	public void setDialogueType(DialogueType type) {
		dialogueType = type;
	}

	public Set<MappingObjectStr> getRepairSet() {
		return repairSet;
	}

	public void setRepairSet(Set<MappingObjectStr> repairSet) {
		this.repairSet = repairSet;
	}

	public int getMessageID() {
		return messageID;
	}

	public int getSubMessageID() {
		return subMessageID;
	}

	public synchronized int getNewMessageID() {
		return currentMessageID++;
	}

	public CandidateMapping getJustification() {
		return justification;
	}

	public void setJustification(CandidateMapping justification) {
		this.justification = justification;
	}

	public CandidateMapping getMapping() {
		return mapping;
	}

	public void setMapping(CandidateMapping mapping) {
		// This should create a brand new mapping which is then sent across the
		// comms channel
		this.mapping = mapping;
	}

	public MappingObjectStr getLogMapMapping() {
		return logMapMapping;
	}

	public void setLogMapMapping(MappingObjectStr logMapMapping) {
		this.logMapMapping = logMapMapping;
	}


	// ===================================================
	// Constructor Methods

	// The first constructor should be used when constructing
	// a new message between two agents. In many cases, the
	// agents will take turns sending messages to each other,
	// and in each case the comms will constitute a new
	// transaction, and hence necessitate a new messageID. The
	// subMessageID is set to 0 by default.

	public CommsObject(String a1, String a2, DialogueType type) {
		this.sendingAgent = a1;
		this.recipientAgent = a2;
		this.dialogueType = type;
		this.messageID = this.getNewMessageID();
		this.subMessageID = 0;
		this.mapping = null; // the Cell that we will communicate
		// this.originalMapping = null;// the actual candidate mapping; in case
		// we need to access the probabilities etc
		this.justification = null; // this will be used when proposing alternate
									// mappings
	}

	// The second constructor corresponds to the case when a message is being
	// responded to.  If followOn is true, then we are following on from the
	// previous message, rather than responding to it.
	
	public CommsObject(CommsObject co, DialogueType type, boolean followOn) {
		this.dialogueType = type;
		if (followOn == true) {
			this.sendingAgent = co.getSendingAgent();
			this.recipientAgent = co.getRecipientAgent();
		} else {
			this.sendingAgent = co.getRecipientAgent();
			this.recipientAgent = co.getSendingAgent();
		}
		// The following was done when we had the two level negotiation
//		this.messageID = co.getMessageID();
//		this.subMessageID = 1 + co.getSubMessageID(); // increment
		
		// This reverts to the notion of single level messages
		this.messageID = this.getNewMessageID();
		this.subMessageID = 0;


		// note that we may have to consider justifications, and thus
		// probabilities etc.
	}
	
	// This method is a shortcut (backwardly compatible) for the more
	// general constructor (above)
	public CommsObject(CommsObject co, DialogueType type) {
		this(co, type, false);
	}
	
	// ===================================================
	// Methods

	public String toString() {
		String result = new String();
//		result = this.messageID + " (" + this.subMessageID + ") "
//				+ this.sendingAgent + " -> " + this.recipientAgent + ":";
		result = this.messageID + " (" + this.subMessageID + "): "
				+ "<"+this.sendingAgent+", ";

		switch (this.dialogueType) {
		case JOIN:
			result += "JOIN, nil, nil, nil";
			break;
		case ASSERT:
			result += "ASSERT ";
			//result += this.mapping.toString();
			if (this.logMapMapping == null) {
				System.out.println("  No logMapMapping for the mapping "+this.mapping.toString());
				result += this.mapping.toString();
			} else {
				result += this.logMapMapping.toString();
			}
			result += ", " + this.getMapping().getProbability();
			if (this.repairSet == null)
				result += ", nil";
			else
				result += ", "+this.repairSet.toString()+"";
			break;
		case REJECTC:
			result += "REJECTC ";
			//result += this.mapping.toString();
			result += this.logMapMapping.toString();
			result += ", nil";
			break;
		case ACCEPTC:
			result += "ACCEPTC ";
			//result += this.mapping.toString();
			result += this.logMapMapping.toString();
			result += ", " + this.getMapping().getJointWeight();
			result += ", nil";
			break;
		case REJECTR:
			result += "REJECTR ";
			//result += this.mapping.toString();
			result += this.logMapMapping.toString();
			result += ", nil";
			break;
		case REPAIR:
			result += "REPAIR ";
			//result += this.mapping.toString();
			result += this.logMapMapping.toString();
			result += ", " + this.getMapping().getJointWeight();
			if (this.repairSet == null)
				result += ", nil";
			else
				result += ", "+this.repairSet.toString()+"";
			break;
		case ACCEPTR:
			result += "ACCEPTR ";
			//result += this.mapping.toString();
			result += this.logMapMapping.toString();
			result += ", nil";
			result += ", nil";
			break;
		case CLOSE:
			result += "CLOSE";
			break;
		case NONE:
			result += "NONE - wouldn't expect to see this!";
		case END:
			result += "END - shouldn't see this - all the agents have now closed the dialogue!";
		default:
			result += "default - Error???";
		}

		result +=">";
//		if (this.logMapMapping == null) {
//			result += " null";
//		} else {
//			result += this.logMapMapping.getConfidence();
//		}
		return result;

	}

}
