package uk.ac.mas.dare;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;



/**
 * 
 * This class represents an agent (implemented as a thread) which is responsible
 * for reading in and managing a number of alignments that it is aware of, and
 * building up an Alignment Store from which negotiations can take place.  The majority
 * of the code is responsible for managing the dialogue that the agent uses to communicate
 * with other agents.
 * 
 * @author      Terry Payne
 * @version     %I%, %G%
 * @since       1.0
 *
 */

public class Agent extends Thread {

	private CommsChannel comms;					// Communication Channel (shared Signleton)
	private RepairManager repair_manager;		// Repair Manager (shared Singleton)
	private String agentName;					// This agent's name
	private String collaboratorAgentName;		// The name of the interlocutor
	private AlignmentStore alignmentStore;		// The store for the alignment
	private CommitmentStore commitmentStore;
	
	private double evidenceThreshold;
	private boolean verbose;
	private boolean useUpperBoundFlag;
	private boolean isAlice;

	// The current proposer is tracked by both agents.  That way, agents
	// can take it in turns to make proposals/assertions.
	private boolean sentLastProposal;
	
	// The probability value of the last ASSERT from the opponent is stored, as it
	// can be used to predict the maximum value that future ASSERTIONS (or even
	// OBJECTIONS) could be.  This should be set to zero when there are no more
	// assertions to be made
	private double opponentsUpperBoundProbability;
	
	private AlignmentStore archiveStore;	// Archive of the original alignment store

	// private URI sourceOntologyURI;			// A URI to the source ontology
	// private URI targetOntologyURI;			// A URI to the target ontology for signature etc
	private URI referenceURIAlignmentURI;		// A URI to the reference URI used for testing
	
	/**
	 * Getter to see if this agent made the most recent proposal 
	 * @return true if this agent made the most recent proposal
	 */
	private boolean didSendLastProposal() {
		return sentLastProposal;
	}

	/**
	 * Setter for the current turn object (this might change soon)
	 * @param currentTurn true if the agent just made a proposal, otherwise false
	 */
	private void setSentLastProposal(boolean sentLastProposal) {
		this.sentLastProposal = sentLastProposal;
	}

	// ==============================================================
	// CONSTRUCTOR
	// ==============================================================

	/**
	 * Creates the Agent object, and sets up the communications channels and
	 * Agent name, myName.
	 * @param cc the synchronised CommsChannel shared with other agents - this
	 * is used to facilitate communication between agents.
	 * @param myName name of the agent (as a string)
	 */
	public Agent(CommsChannel cc, RepairManager rep_man, String myName, URI referenceURI,
			 AlignmentStore as, double eThreshold, boolean verboseFlag, boolean isAlice) {
		
		this.comms = cc;
		this.repair_manager = rep_man;
		this.agentName = myName;
		this.alignmentStore = as;
		this.verbose = verboseFlag;
		this.evidenceThreshold = eThreshold;
		this.useUpperBoundFlag = true;			// JUST USE IT FOR THESE EXPERIMENTS !!!
		this.isAlice = isAlice;
		
		// create a copy of as for the archive store
		this.archiveStore = new AlignmentStore();
		this.archiveStore.loadAlignmentsFromUriList(as.getUriList(), false);
		// Because we have to filter the mappings, replace the candidate alignment
		CandidateAlignment archCA = new CandidateAlignment(as.getCandidateAlignment());
		this.archiveStore.setCandidateAlignment(archCA);
		
		// this.sourceOntologyURI = ontSourceURI;
		// this.targetOntologyURI = ontTargetURI;
		
		this.commitmentStore = new CommitmentStore();
//		this.extensionAlignment = new CandidateAlignment();
//		this.extensionURIAlignmemt = null;
		this.referenceURIAlignmentURI = referenceURI;

		// set the default agent flags
		this.collaboratorAgentName = null;
		this.setSentLastProposal(false);
		this.opponentsUpperBoundProbability = 1.0;
		
		// Load in the ontology ready for inclusion checks
		/*
		IRI sourceIRI = IRI.create(sourceOntologyURI);
		this.sourceOntologyManager = OWLManager.createOWLOntologyManager();
		try {
			this.sourceOntology = this.sourceOntologyManager.loadOntology(sourceIRI);
		} catch (OWLOntologyCreationException e) {
			this.sourceOntology = null;
			System.err.println("Agent.java (constructor) for "+this.agentName+" failed to load the source ontology "+sourceOntologyURI.toString());
			// e.printStackTrace();
		}
		*/
	
	}
	
	// #############################################################################################################
	// #############################################################################################################
	// ##
	// ## Helper  methods
	// ##
	// #############################################################################################################
	// #############################################################################################################

	/**
	 * Takes the CommsObject message, and after saving it in the
	 * Commitment Store, it sends the message.
	 * @return the DialogueType of the message.
	 */
	private DialogueType sendCo(CommsObject co) {
		if (co.getDialogueType() == DialogueType.ASSERT) {
			commitmentStore.newProposalSent(co);
			this.setSentLastProposal(true);
		} else {
			commitmentStore.newResponseSent(co);
		}
		if (this.verbose) {
			System.out.println(co.toString());
			//System.out.println("- - - - - - - - - - - - - - - - - -");
		}
		this.comms.send(co);
		return co.getDialogueType();
	}

	/**
	 * Returns either the individual probability or combined probability known
	 * for the mapping otherMapping.
	 * @param otherMapping
	 * @return either the individual or combined probability of otherMapping.
	 * If the mapping is not known, then 0.0 is returned.
	 */
	private double calculateAggregateUtility(CandidateMapping otherMapping) {
		
		double probability = 0.0;
		CandidateMapping resultAS = this.alignmentStore.lookup(otherMapping);
		CandidateMapping resultMyCS = this.commitmentStore.lookup(otherMapping, true);
		CandidateMapping resultOpponentCS = this.commitmentStore.lookup(otherMapping, false);
								
		AggregationSingleton ags = AggregationSingleton.getInstance();

		// First, check out cases if resultAS != null
		if (resultAS != null) {
			// Check the error case first - in AS and in myCS
			if (resultMyCS != null) {
				System.err.println("Agent.lookup found following mapping in both AS & myCS: "+otherMapping.toString());
			} else if (resultOpponentCS != null) {
				// If the mapping has been received, but the local probability not yet sent?				
				probability = ags.aggregate(resultAS.getProbability(), resultOpponentCS.getProbability());
			} else {
				// we only have resultAS to go on
				// TODO: should this be modified to consider the opponents last bid?
				// Use the Upper Bound
				probability = ags.aggregate(resultAS.getProbability(), this.opponentsUpperBoundProbability);
			}
		} else {
			// Nothing in AS...
			if (resultMyCS != null) {
				if (resultOpponentCS != null) {
					// Mapping was received and sent
					probability = ags.aggregate(resultMyCS.getProbability(), resultOpponentCS.getProbability());

				} else {
					// Mapping was sent, but not received
					// Thus - do we know what our opponent's probability would be?
					probability = ags.aggregate(resultMyCS.getProbability(), this.opponentsUpperBoundProbability);
				}
			} else {
				if (resultOpponentCS != null) {
					// Mapping was received, but not known locally
					probability = ags.aggregate(resultOpponentCS.getProbability(), 0);
				} else {
				}
			}
		}
		

		return probability;
	}


	/**
	 * Returns a {@link CandidateMapping} object, corresponding to
	 * an equivalent known mapping to the argument otherMapping, or
	 * null if no mapping can be found.
	 * <p>
	 * This method tries to ascertain if a mapping is known (based
	 * on its source and target strings), and if so, returns the
	 * object that matches the mapping.  This method always checks
	 * the Alignment Store first, but if nothing can be found, it then
	 * checks the local Commitment Store.  An additional test checks
	 * to see if the same mapping is found in both locations, and
	 * raises an error if found.
	 *
	 * @param otherMapping the candidate mapping that we are checking to see if
	 * it is known.  Note that the check will be based on string comparisons with
	 * the source and target atoms, and not object equivalence.
	 * @return a CandidateMapping object that shares the source and target strings with otherMapping
	 */
	public CandidateMapping lookup(CandidateMapping otherMapping) {
		
		CandidateMapping resultAS = this.alignmentStore.lookup(otherMapping);
		CandidateMapping resultCS = this.commitmentStore.lookup(otherMapping);
		
		// Sanity check - a mapping should never occur in both the Alignment
		// Store or the Commitment Store (containing the agent's own mappings)
		if ((resultAS != null) && (resultCS != null)) {
			System.err.println("Agent.lookup found following mapping in both CS and AS: "+otherMapping.toString());
		}
		
		// Check if something was found in the Alignment Store first!!!
		if (resultAS != null) {
			return resultAS;
		}
		
		// If nothing was in the Alignment Store, check the Commitment Store
		if (resultCS != null) {
			return resultCS;
		}
		
		// Ok, this mapping could not be found.  Return null!
		return null;
	}
	
	
	// #############################################################################################################
	// #############################################################################################################
	// ##
	// ## Dialogue Specific methods
	// ##
	// #############################################################################################################
	// #############################################################################################################

	// ==============================================================================
	// startDialogue
	// ==============================================================================

	/**
	 * Initiates the dialogue, by checking the comms channel and
	 * sending a JOIN message to the other agent (we assume bilateral
	 * communication for now). It returns true, to indicate that
	 * this agent was the first to communicate, or false to say that
	 * this agent should wait for a message to be posted to it, before
	 * doing anything.
	 * @return if the agent initiated the dialogue, then {@link true},
	 * otherwise {@link false}
	 */
	private boolean startDialogue() {
		// Create the join message
		boolean initiatedFirst = false;

		CommsObject co = new CommsObject(agentName, null, DialogueType.JOIN);
		// System.out.println(co.toString());	// DEBUG

		CommsObject response;
		if (this.comms.sendMessageIfPossibe(co)) {
			// this agent is the first to get the message out
			response = this.comms.receiveIfNotSender(this.agentName);
			initiatedFirst = true;
		} else {
			// message was already there, so retrieve it to get the name of
			// the transacting agent
			response = this.comms.receive();
			
			// Verify that the message is a join.  This is just a sanity check for now.
			if (response.getDialogueType() != DialogueType.JOIN) {
				System.err.println("Agent.startDialogue() - unexpected message: " + response.toString());
			}

			// note that we don't use sendCo as we are not storing
			// JOIN messages in the commitment store
			this.comms.send(co);
		}

		this.collaboratorAgentName = response.getSendingAgent();
		return initiatedFirst;
	}

	// ==============================================================================
	// assertOrClose
	// ==============================================================================
	
	/**
	 * Determines whether or not there are any further mappings that could be
	 * asserted (in which case a proposal/assert message is constructed and sent), and
	 * if not, the dialogue is closed (again, with a close-dialogue message being
	 * constructed).
	 * @param co either a message that a CLOSE could be responding to (if there are
	 * no more mappings to ASSERT), or (in most cases) null, as no reply should be generated.
	 * @return a new CommsObject message containing the proposal, or if no mappings
	 * can be proposed, then a CLOSE message will be generated.
	 * If a null message is generated, this could indicate an error...
	 */
	private CommsObject assertOrClose(CommsObject co) {
		// Check to see if we still have a candidate mapping. If not, then
		// close. Otherwise, assert the mapping.
				
		CommsObject response = null;

		while (alignmentStore.hasMappings()) {
			CandidateMapping proposedMapping = alignmentStore.getCandidateAlignment().head();
			if (proposedMapping == null) {
				System.err.println("alignmentStore.getProposal() failed to return a mapping");
			} else {
				// Need to decide what to do with this mapping...
				double probOfProposedMapping = this.calculateAggregateUtility(proposedMapping);
				
//				System.out.println("Considering "+proposedMapping.toString()+" with weight:"+probOfProposedMapping);
//				System.out.println(this.agentName + " is aware of an upperbound of "+this.opponentsUpperBoundProbability);

				if (probOfProposedMapping >= this.evidenceThreshold) {
					response = null;
					
					// We potentially have a mapping to propose:					
//					response = new CommsObject(agentName, this.collaboratorAgentName,
//							DialogueType.ASSERT);
					
					Set<MappingObjectStr> repairSet = null;
					// As the agent is sending something, check who is active
					try {
						if (this.isAlice) {
							this.repair_manager.aliceIsActive();
						} else {
							this.repair_manager.bobIsActive();
						}
						
						// Add the logMap version of the mapping
//						response.setLogMapMapping(this.repair_manager.getLogMapMapping(proposedMapping));

//						System.out.println(this.agentName + " checks out "+ response.getLogMapMapping().toString());

						if (this.repair_manager.willAssertedMappingCauseViolations(proposedMapping)) {
							// we have a potential violation
							repairSet = this.repair_manager.getAssertedRepair(proposedMapping);
							// if the repairSet is not null, then it is inlcuded in the assertion.
							// Otherwise we simply ignore this mapping and go onto the next one.
							// We should also check if the repair is rational
							
							if (repairSet != null) {
								if(!this.repair_manager.isRepairRational(proposedMapping, repairSet)) {
//									System.out.println("*** REPAIR IS IRRATIONAL <-------");
									
									// YES - THIS IS REPEATING THE CODE BELOW BUT LIVE WITH IT
									this.alignmentStore.delete(proposedMapping);
									response=null;
									continue;
								}
							
								// System.out.println("COULD CAUSE ASSERT VIOLATION: repair is:" +repairSet.toString());
								response = new CommsObject(agentName, this.collaboratorAgentName,
										DialogueType.ASSERT);
								response.setRepairSet(repairSet);
								response.setLogMapMapping(this.repair_manager.getLogMapMapping(proposedMapping));

							} else {
								// System.err.println("Agent.java: assertOrClose - violation caused but with no repair");
								// Examine the next possible mapping 
								this.alignmentStore.delete(proposedMapping);
								response=null;
								continue;
							}

						} else {
							response = new CommsObject(agentName, this.collaboratorAgentName,
									DialogueType.ASSERT);
							response.setLogMapMapping(this.repair_manager.getLogMapMapping(proposedMapping));
						}

						if (response != null) {
							// the message will be added to the Commitment Store when it is sent
							response.setMapping(proposedMapping);
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.err.println("WARNING: the dialogue will probably make an erroneous CLOSE move");
					}

					this.alignmentStore.delete(proposedMapping);
					break;
				} else {
					// Found a mapping but the probability is too low.  In which case
					// all other mappings will be too low. Delete for now
					this.alignmentStore.delete(proposedMapping);
//					System.out.println(this.agentName + " is deleting (under threshold) "
//							+ proposedMapping.toString() +" ("+probOfProposedMapping+")\n");					
				}
			}
		}
		// if we get here, then we either have our response, or are out of mappings
		if (response == null) {
//			 System.out.println(this.agentName + " has no more mappings... closing...");
			
			// Assume lazy evaluation to avoid null pointer exception
			if ((co==null) || (co.getDialogueType() != DialogueType.CLOSE)) {
				response = new CommsObject(agentName, this.collaboratorAgentName,
						DialogueType.CLOSE);
			} else {
				response = new CommsObject(co, DialogueType.CLOSE);
			}
		}
		return response;
	}

	// ==============================================================================
	// generateAcknowledgement
	// ==============================================================================

	/**
	 * Given a mapping, see if the equivalent mapping exists in the
	 * local candidate store, and then generate the response. If it
	 * can't be found, then generate an acceptance/acknowledgement with
	 * a zero valued probability.
	 * 
	 * @param co the previous CommsObject that this message is responding to
	 * @return the new CommsObject, or null if something goes wrong
	 */
	private CommsObject generateAcceptCMessage(CommsObject co) {

		CandidateMapping otherMapping = co.getMapping();
					
		// Look up the mapping for now
		CandidateMapping myLocalMapping = this.lookup(otherMapping);
		if (myLocalMapping == null) {
			myLocalMapping = new CandidateMapping(otherMapping);
		}
		
		// =============================================================
		// TODO: need to generate a REJECTC iff:
		//	1) Combined probabilities are below threshold
		//	2) Mapping symbol is not one known to the agent
		//	3) The Agent has some other reason not to accept the mapping
		// =============================================================
		// check probability now...
		double probability = this.calculateAggregateUtility(myLocalMapping);
		CommsObject response = null;
		
//		System.out.println("\n"+this.agentName+" checks out "+myLocalMapping+" as aggr("+
//						co.getMapping().getProbability()+","+
//						myLocalMapping.getProbability()+") = "+probability);

		
		if (probability <this.evidenceThreshold) {
			
//			System.out.println("==================");
//			repair_manager.printSolution();
//			System.out.print("\n"+this.agentName+" REJECTING "+myLocalMapping+" as its probability ("+probability+") is under threshold ("+this.evidenceThreshold+")");
			response = new CommsObject(co, DialogueType.REJECTC);
			// Update the old myLocalMapping object to set its attack status as out
			// myLocalMapping.setArgStatus(AttackStatus.OUT);
			
//			CandidateMapping myOpponentsMapping = this.commitmentStore.lookupOpponent(myLocalMapping);
//			if (myOpponentsMapping != null) {
//				System.out.println("My opponent knew of this as well, so resetting attack status");
//				// myOpponentsMapping.setArgStatus(AttackStatus.OUT);
//			}

			response.setMapping(myLocalMapping);
//			System.out.println("Rejected a mapping "+otherMapping.toString() +" with probability "+probability);
		} else {
			myLocalMapping.setJointWeight(probability);
			response = new CommsObject(co, DialogueType.ACCEPTC);
			response.setMapping(myLocalMapping);
		}

		// Ensure that the message also includes the logmap variant of the mapping
		try {
			response.setLogMapMapping(repair_manager.getLogMapMapping(myLocalMapping));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// =============================================================
		// Do any final housekeeping.  If the mapping was found in the Alignment Store
		// then it needs to be deleted.  When the mapping is acknowledged or rejected,
		// it will be added to the Commitment Store
		StoreType mappingKnown = this.isMappingKnown(otherMapping);
		// Note that this will affect any lookups to the probabililty until a message has been sent
		if (mappingKnown == StoreType.ALIGNMENTSTORE) {
			this.alignmentStore.delete(myLocalMapping);
		}

		return response;
	}
	
	/**
	 * Returns a StoreType object corresponding to whether or not
	 * the otherMapping argument exists in either the {@link AlignmentStore}
	 * object or the {@link CommitmentStore} object.  If the
	 * mapping is not found, then the StoreType UNKNOWN is returned.
	 * <p>
	 * Note that no sanity checks are performed by this method, and that the
	 * lookup methods in the two stores are used to determine the existence
	 * of a stored mapping that matches otherMapping.
	 * @param otherMapping the candidate mapping that we are checking to see if
	 * it is known.  Note that the check will be based on string comparisons with
	 * the source and target atoms, and not object equivalence.
	 * @return a value from the StoreType enum, corresponding to where the otherMapping
	 * object exists 
	 */
	public StoreType isMappingKnown(CandidateMapping otherMapping) {
		// Note that unlike lookup, we don't do any sanity checks here.
		// This is kept as simple as possible
		if (this.alignmentStore.lookup(otherMapping) != null)
			return StoreType.ALIGNMENTSTORE;
		
		// Note that this should look up the local mappings in
		// the commitment store
		if (this.commitmentStore.lookup(otherMapping) != null)
			return StoreType.COMMITMENTSTORE;
		
		return StoreType.UNKNOWN;	
	}

	
	// #############################################################################################################
	// #############################################################################################################
	// ##
	// ## Main Dialogue Handler Method
	// ##
	// #############################################################################################################
	// #############################################################################################################


	// ============================================================
	// ============================================================
	// This is the general message-handling code. It waits for a message
	// to arrive that is destined to that agent, and then responds accordingly.
	// The argument (closeDialogue) states whether a prior message exchange
	// returned a close dialogue message
	//

	/**
	 * This is the general message-handling code. It waits for a message
	 * to arrive that is destined to that agent, and then responds accordingly.
	 * The argument (closeDialogue) states whether a prior message exchange
	 * returned a close dialogue message
	 * 
	 * @param priorCloseDialogue
	 * @return the DialogueType of any message sent in response to the received
	 * message.  Otherwise, ENDASSERTION is returned
	 */
	private DialogueType handleMessage(DialogueType priorDialogueType) {
		
		// boolean closeDialogue = false;
		DialogueType currentDialogueType = DialogueType.CLOSE; // failsafe default???
		CommsObject coRecieved = this.comms.receiveFor(this.agentName);
		CommsObject newResponse = null;
		CommsObject co = null;
//		System.out.println(this.agentName+" processing message> \n    "+coRecieved.toString());
//		repair_manager.printSolution();
		
//		System.out.println("\nCandidate Alignment for " + this.agentName
//				+ " with threshold:" + this.evidenceThreshold+"\n"
//				+ this.alignmentStore.getCandidateAlignment().toString());

		switch (coRecieved.getDialogueType()) {
		case CLOSE:
			// We received a CLOSEDIALOGUE. If this follows a prior CLOSEDIALOGUE,
			// then simply return the close status, otherwise, attempt a new proposal
			this.opponentsUpperBoundProbability = 0.0;	// If there are no more mappings, upper bound is zero

			commitmentStore.newResponseReceived(coRecieved);
			
			if (priorDialogueType != DialogueType.CLOSE) {
				co = this.assertOrClose(coRecieved);	// if this sends a CLOSEDIALOGUE, then it should be a reply
				currentDialogueType = this.sendCo(co);
				if (currentDialogueType == DialogueType.CLOSE) {
					// Change currentDialogueType to END, as we have now finished.
					currentDialogueType = DialogueType.END;	// Terminate the dialogue handling
				}
			} else {
				currentDialogueType = DialogueType.END;	// Terminate the dialogue handling
			}
			break;
			
		case ASSERT: 
			// NOTE that we are not checking priorDialogueType right now... 
			
			Set<MappingObjectStr> repairSet = null;
			
			// Store the message in the commitment store
			commitmentStore.newProposalReceived(coRecieved);
			if (coRecieved.getMapping().getProbability() <= this.opponentsUpperBoundProbability) {
				
					this.opponentsUpperBoundProbability = coRecieved.getMapping().getProbability();
//				System.out.println(this.agentName + " spotted that " + this.collaboratorAgentName
//						+ "'s last probability was " + this.opponentsUpperBoundProbability);
			} else {
				System.err.println(this.agentName + " spotted that " + this.collaboratorAgentName
						+ "'s last probability unexpectedly increased from " + this.opponentsUpperBoundProbability
						+ " to: " + coRecieved.getMapping().getProbability());
			}

			// Lets see if we can accept this (based on thresholds etc)
			// If we can, then we need to know if this could result in a
			// violation...
			co = this.generateAcceptCMessage(coRecieved);
			
			/*
			 * on receiving an assert, then the agent should think about planning the next
			 * proposal if the mapping is accepted, but not if a repair is issued.
			 */
			
			if (co == null) {
				System.err.println("Failed to generate an acknowledgement or objection!!!");
			} else if (co.getDialogueType() == DialogueType.ACCEPTC) {
				// Only check if the mapping causes violations if we are thinking of accepting it...
				// If there is a repair within the assertion, this also needs to be
				// considered.			
				repairSet = coRecieved.getRepairSet();
				try {
					// If we have a repair, then lets at least check if it is rational first.  If it is not, then
					// simply return true (i.e. the mapping with the repair causes violation).  Whilst not
					// strictly true, it should also be then dealt with later...
					if(!this.repair_manager.isRepairRational(coRecieved.getMapping(), repairSet)) {
//						System.out.println("*** REPAIR IS IRRATIONAL <-------");
						co.setDialogueType(DialogueType.REJECTC);
					} else {						
						if(this.repair_manager.doesReceivedMappingCauseViolations(coRecieved.getMapping(), repairSet)){
							
							// At this point we know we have violations.  So we have a choice:
							//   1) Repair the violation
							//	 2) Or we reject the mapping
							repairSet = this.repair_manager.getPassiveRepair(coRecieved.getMapping());
							if (repairSet == null) {
//								System.out.println("   Asserted mapping causes violation, but no repair found");
								co.setDialogueType(DialogueType.REJECTC);
							
							} else {
//								System.out.println("    Asserted mapping causes violation, repair is:" +repairSet.toString());
								co.setDialogueType(DialogueType.REPAIR);
								co.setRepairSet(repairSet);
							
							}
						}
					}
					co.setLogMapMapping(this.repair_manager.getLogMapMapping(coRecieved.getMapping()));
					// If we are here then we are simply sending the Accepting the mapping
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} 
				
			if (co != null) {
				// Now send the message
				currentDialogueType = this.sendCo(co);
			}
			
			if ((currentDialogueType == DialogueType.ACCEPTC) || 
			    (currentDialogueType == DialogueType.REJECTC)) {
				// Now determine and act on turn order.
				if (this.didSendLastProposal()) {
					this.setSentLastProposal(false);
				} else {
					co = this.assertOrClose(null);	// should be a new message, not a reply
					currentDialogueType = this.sendCo(co);
				}
			}


			/*
			 * The code below checks for objections from the AAMAS code.
			 * We don't care about this...
			 * 

			// See if an objection can be raised to the message		
			// TODO: Might want to test here if we simply want to reject the mapping
			// for example if it isn't grounded by the agent's ontology
			co = this.generateObjection(coRecieved);
			if (co == null) {
				// no objection, therefore we acknowledge the message
				co = this.generateAcknowledgement(coRecieved);
			}

			if (co == null) {
				System.err.println("Failed to generate an acknowledgement or objection!!!");
			} else {
				currentDialogueType = this.sendCo(co);
			}
			 
			
			// ==============================================

			if ((co.getDialogueType() == DialogueType.ACKNOWLEDGE) ||
					(co.getDialogueType() == DialogueType.REJECT)){

				// As we have just issued an acknowledgement, we could consider any
				// further objections here.
				newResponse = this.generateAdditionalObjection(co);
				
				// New code to solve the Bob dilemma
				if (newResponse == null) {
					// What about an implicit objection
					newResponse = this.generateImplicitObjection(co);
				}
				if (newResponse == null) {
					// ok - mebbie time to end the round
					newResponse = new CommsObject(co, DialogueType.ENDASSERT, true);
				}
				currentDialogueType = this.sendCo(newResponse);
			}
			
			*/
			
//			// Now determine and act on turn order.
//			if (this.didSendLastProposal()) {
//				this.setSentLastProposal(false);
//			} else {
//				co = this.assertOrClose(null);	// should be a new message, not a reply
//				currentDialogueType = this.sendCo(co);
//			}
			
//			co = this.assertOrClose(null);	// should be a new message, not a reply
//			currentDialogueType = this.sendCo(co);

			
			break;
		case REPAIR:
			
			// ===================
			try {
			    if (repair_manager.isRepairOk(coRecieved.getMapping(), coRecieved.getRepairSet())) {
			    	// System.out.println("Repair is ok");
					co = new CommsObject(coRecieved, DialogueType.ACCEPTR);
			    } else {
			    	// System.out.println("Repair is NOT ok");		    	
					co = new CommsObject(coRecieved, DialogueType.REJECTR);
			    }
				co.setMapping(coRecieved.getMapping());
				co.setLogMapMapping(this.repair_manager.getLogMapMapping(coRecieved.getMapping()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			currentDialogueType = this.sendCo(co);

			// This is a guess...
			if (this.didSendLastProposal()) {
				this.setSentLastProposal(false);
			} else {
				co = this.assertOrClose(null);	// should be a new message, not a reply
				currentDialogueType = this.sendCo(co);
			}

			break;
		case REJECTR:			
		case REJECTC:
			
			// Update the attack status before storing
			// coRecieved.getMapping().setArgStatus(AttackStatus.OUT);
			// Update local status
			if (coRecieved.getDialogueType() == DialogueType.REJECTC) {
				CandidateMapping myLocalMapping = this.lookup(coRecieved.getMapping());
				if (myLocalMapping == null) {
					System.err.println("Rejected mapping "+coRecieved.getMapping()+" wasn't known!!!");
				} else {
					// myLocalMapping.setArgStatus(AttackStatus.OUT);
					// System.out.println("My mapping "+coRecieved.getMapping()+" was rejected");
				}
			}
			commitmentStore.newResponseReceived(coRecieved);
			
			// The following is technically incorrect, but is probably safe.
			currentDialogueType = coRecieved.getDialogueType();
			
			// This is a guess...
			if (this.didSendLastProposal()) {
				this.setSentLastProposal(false);
			} else {
				co = this.assertOrClose(null);	// should be a new message, not a reply
				currentDialogueType = this.sendCo(co);
			}

			break;	
			
		case ACCEPTC:
		case ACCEPTR:
			
			// If ACCEPTR is sent, then the repair has been applied.  Time to
			// make the next move...
			commitmentStore.newResponseReceived(coRecieved);

			// The following is technically incorrect, but is probably safe.
			currentDialogueType = coRecieved.getDialogueType();

			if (this.didSendLastProposal()) {
				this.setSentLastProposal(false);
			} else {
				co = this.assertOrClose(null);	// should be a new message, not a reply
				currentDialogueType = this.sendCo(co);
			}
			break;
		default:
			break;
		}
		return currentDialogueType;
	}
	
	
	// #############################################################################################################
	// #############################################################################################################
	// ##
	// ## Thread and Reporting Methods
	// ##
	// #############################################################################################################
	// #############################################################################################################

	// ============================================================
	// The remaining methods (below) are general support for the agent
	// including the run method, which initiates the agent when the
	// thread is created.
	// ============================================================
	/**
	 * This is the overloaded run method that is executed when the thread is
	 * created.  It starts by loading the alignments that an agent knows about. 
	 */
	public void run() {
		if (this.verbose) {
			System.out.println("\nCandidate Alignment for " + this.agentName
					+ " with threshold:" + this.evidenceThreshold+"\n"
					+ this.alignmentStore.getCandidateAlignment().toString());
		}
		
		CommsObject co = null;
		DialogueType coDT = DialogueType.JOIN;	// Starting state

		if (this.startDialogue()) {

			// this agent should initiate the next part of the dialogue,
			// which in this case should always be a proposal, as there
			// is no message to counter or reject.
			// However, if there are no mappings to propose/assert, then
			// a close should be sent.

			co = this.assertOrClose(null); 	// should be a new message, not a reply
			this.sendCo(co);
			coDT = co.getDialogueType();
		} 
		
		// Start the message handler
		while (coDT != DialogueType.END) {
			coDT = this.handleMessage(coDT);
		}

		if (this.agentName.equals("Alice")){
			String myResults = this.repair_manager.getOntoAliceURI(). toString();					// Alice's ontology
			myResults += "\t" + this.repair_manager.getOntoBobURI().toString();						// Bob's ontology
			myResults += "\t" + this.evidenceThreshold;
			myResults += "\t" + archiveStore.getCandidateAlignment().getCandidateMappings().size();	// # mappings in CA

			int numMappingsDisclosed = 0;
			for (NegotiationRoundCommitmentStore nrcs : commitmentStore.getNegotiationRoundList()) {
				numMappingsDisclosed+=nrcs.getMyAlignment().getCandidateMappings().size();
			}
			myResults += "\t" + numMappingsDisclosed;
			
			myResults += "\t" + this.repair_manager.getSizeOfSolution();
	
			myResults += reportOnAlignmentPR(repair_manager.generateSolutionAlignment(),
					repair_manager.getOntoReference());

			System.out.println(myResults);
		}
		// ============================================================
		// Report on the final structures
		
		// System.out.println(this.agentName+"'s "+repair_manager.getSolutionAsString());
		
		
//		repair_manager.printSolution();
		// For debugging purposes, look at the commitment stores
//		if (this.verbose) {
//			// Provide details about the different commitment stores
//			String result = new String();
//			for (NegotiationRoundCommitmentStore nrcs : this.commitmentStore
//					.getNegotiationRoundList()) {
//				result += nrcs.toString();
//			}
//			System.out.println("\n==================\n"
//					+ this.agentName + ": Final CS\n" + result);
//		}

		
	}
	
	
	// =====================================================================
	// Code for evaluating the resulting alignment
	// =====================================================================
	private String reportOnAlignmentPR(URIAlignment solnURIAlignment, URI refURI) {
		String resultStr = new String();
		if (refURI != null) {
			try {			
				// ===============================
				// Load the reference alignment
				AlignmentParser aparser = new AlignmentParser(0); // parameter "0" relates to debugging info
				Alignment reference = aparser.parse(refURI); 
				 
				PRecEvaluator evaluator = new PRecEvaluator(reference, solnURIAlignment);
				Properties p = new BasicParameters();
				evaluator.eval(p);
				resultStr += "\t" + evaluator.getExpected();	// Is this the number in the gold standard?
				resultStr += "\t" + evaluator.getPrecision();	// (Precision wrt the gold standard)
				resultStr += "\t" + evaluator.getRecall();		// (Recall wrt the gold standard)
				resultStr += "\t" + evaluator.getFmeasure();	// (f-measure wrt the gold standard)
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			resultStr += "\t\t\t";	// Empty values
		}
		return resultStr;
	}

	

}
