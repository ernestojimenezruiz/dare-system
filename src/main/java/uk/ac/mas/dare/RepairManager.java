/**
 * 
 */
package uk.ac.mas.dare;

//import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import auxStructures.RepairStatus;
import enumerations.REPAIR_METHOD;
import enumerations.VIOL_KIND;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import repair.MASRepair;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;

/**
 * @author trp
 *
 */
public class RepairManager {
	
	static int ALICE = 0;	// ID for Alice in arrays etc
	static int BOB = 1;		// ID for Bob in arrays etc
	
	private List<VIOL_KIND> violKinds = new ArrayList<>();
	private REPAIR_METHOD rm = REPAIR_METHOD.SUBEQ;

	private MASRepair masRepairAlice = null;	// These are absolute
	private MASRepair masRepairBob = null;		// These are absolute
	private MASRepair repairFacActive = null;	// These will vary depending on who
	private MASRepair repairFacPassive = null;	// uses an instance of this class
	
	private URI ontoAliceURI = null;
	private URI ontoBobURI = null;
	private URI ontoReference = null;

	private OWLOntology ontoAlice = null;		// Representation of Alice's ontology
	private OWLOntology ontoBob = null;			// Representation of Bob's ontology
	
	private OWLOntologyManager manager = null;	// Primary ontology manager
	
	// Final Solution Alignment
	// --- was commitmentStore in Alessandro's original example
	private Set<MappingObjectStr> solutionStore = null;
	private Set<MappingObjectStr> tmpSolStore = null;			// Temporary solution...
	/*
	 * The local commitment store takes in elements that are of type
	 * MappingObjectStr which are defined in Alessandro's jar.  So
	 * the things we need to do are:
	 * 1) Create an element of type MappingObjectStr from a CandidateMapping
	 * 2) Update the weight of a MappingObjectStr in the local commitment store
	 */
	
	private RepairStatus rsActive = null;	//repair status...
	private RepairStatus rsPassive = null;	//repair status...
//	private int activeAgentID = 0;
//	private int passiveAgentID = 1;

	// Logmap Alignment Stores for each agent
	// We manage them here, but produce cells for each agent and track their inclusion
	// in the two commitment stores.
	
//	private MASRepair alignmentManager = null;	// Keep this separate for loading alignments
//	private List<MappingObjectStr> alignmentAlice = null;
//	private List<MappingObjectStr> alignmentBob = null;

	// =============================================================================
	// Constructor
	// =============================================================================
	public RepairManager() {
		this.initRepair();
		this.solutionStore = new HashSet<>();

	}
	
	public URI getOntoReference() {
		return ontoReference;
	}

	public void setOntoReference(URI ontoReference) {
		this.ontoReference = ontoReference;
	}

	public URI getOntoAliceURI() {
		return ontoAliceURI;
	}

	public URI getOntoBobURI() {
		return ontoBobURI;
	}

	private void initRepair() {
		// Set up the Repair mechanism
		// List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);
		violKinds.add(VIOL_KIND.APPROX);
		violKinds.add(VIOL_KIND.CONSISTENCY);

		Params.fullReasoningRepair = false;
		Parameters.repair_heuristic = false;
		Params.suppressOutputFully();
		Params.storeViolations = true;
		
	}
		
	public void initiateRepairObjects(String [] ontologyPaths) {
		
		// ======================================================
		// Load in the ontologies
		
		// Get two managers - a local one for loading the ontologies
		// and a second for the repairs
		OWLOntologyManager managerTmp = OntoUtil.getManager(false); 
		this.manager = OntoUtil.getManager(true);

		try {

			this.ontoAliceURI = new File(ontologyPaths[ALICE]).toURI();					
			this.ontoBobURI = new File(ontologyPaths[BOB]).toURI();
			
			// load the two ontologies (the other only needed for the signature)
			this.ontoAlice = OntoUtil.load(ontologyPaths[ALICE], true, managerTmp);
			this.ontoBob = OntoUtil.load(ontologyPaths[BOB], true, managerTmp);

			this.masRepairAlice = new MASRepair(ontoAlice, ontoBob, true, true);
			this.masRepairBob = new MASRepair(ontoBob, ontoAlice, true, false);
			
//			this.alignmentManager = new MASRepair(ontoAlice, ontoBob, true, true);

			
		} catch (OWLOntologyCreationException e) {
			//fail("Exception while creating the \"hidden\" ontology: " + e.getMessage());
			System.err.println("Exception while creating the \"hidden\" ontology: " + e.getMessage());
		}

	}
	
	
	//==============
	//
	// The following methods need to be synchronised
	//
	//==============
    
    public synchronized MappingObjectStr getLogMapMapping(CandidateMapping cm)
    		throws Exception {
    	return LogMapWrapper.convertToLogMapMapping(ontoAlice, ontoBob, cm.getMapping());
    }
    
    public synchronized Set<MappingObjectStr> getPassiveRepair(CandidateMapping cm)
    		throws Exception {
    	
    	// Check to see if there is a violation to repair
		MappingObjectStr m = LogMapWrapper.convertToLogMapMapping(ontoAlice, ontoBob, cm.getMapping());
		
		// Add the mapping to a copy of the current alignment and assess...
		this.tmpSolStore = LogMapWrapper.cloneAlignment(this.solutionStore);
		this.tmpSolStore.add(m);

		this.rsPassive = this.repairFacPassive.assessMapping(manager, this.tmpSolStore, m, false, rm, violKinds);
		
		if (this.rsPassive.hasViolations()){
			Set<MappingObjectStr> repairAsReply = rsPassive.getRepair();
			
			// But is this a valid repair for the mapping cm?
			if (rsPassive.hasValidRepair()){
				// Yes, we can repair it
				// Add the repair to the temporary solution
				this.tmpSolStore = LogMapWrapper.applyRepair(this.tmpSolStore, repairAsReply);

				rsPassive = repairFacPassive.assessMapping(manager, 
						this.tmpSolStore, null, false, rm, violKinds);
				
				// Now, did the repair work???
				if(rsPassive.hasViolations()){
					// No - in which case don't return any repair :(
					return null;
				} else {
					// Yes - in which case this is our repair
					
					// TODO: NEW TEST - is the repair the removal of it...
					if(!LogMapWrapper.isContained(m, this.tmpSolStore)){
						// System.err.println("  The repair for including "+m+" is the removal of itself");
						return null;
					}
//					System.out.println("    repair" + repairAsReply);
					return repairAsReply;
				}
			}
		} 
		// If we are here, then there were no violations to repair
    	return null;
    }
	
    public synchronized boolean isRepairRational(CandidateMapping cm, Set<MappingObjectStr> repairSet)
			throws Exception {
		
		// Simple approach.  If a single mapping in the repair that is the deletion of an equivalence has
		// a strength that is greater than that of the candidate mapping (irrespective of the relationship
		// in the candidate mapping) then we consider the repair as irrational.
		boolean r = true;

		// We try to use the joint weight.  If not possible assume probability
		double strength = cm.getJointWeight();
		if (strength < 0)
			strength = cm.getProbability();
		
		if (repairSet != null)
			for (MappingObjectStr m:repairSet) {
				if ((m.getMappingDirection() == MappingObjectStr.EQ) && (m.getConfidence() > strength)) {
//					System.out.println("   FOUND ONE "+strength+" against "+m.toString()+m.getConfidence());
					r = false;
					break;
				}	
			}
		return r;	
	}
    
    public synchronized boolean isRepairOk(CandidateMapping cm, Set<MappingObjectStr> repairSet)
			throws Exception {		
		// Add the mapping to a temporary version of the solution store
		MappingObjectStr m = LogMapWrapper.convertToLogMapMapping(ontoAlice, ontoBob, cm.getMapping());
		this.tmpSolStore = LogMapWrapper.cloneAlignment(this.solutionStore);
		this.tmpSolStore.add(m);
		
		boolean r = true;
		
		// if there is a repair, then apply it...
		if (repairSet != null) {
//			System.out.println("   Checking repair set: "+repairSet.toString());
			// Check rationality
			if(isRepairRational(cm, repairSet)) {
//				System.out.println("*** REPAIR IS RATIONAL");
				

				// Note that this happens after adding the mapping, as the repair may modify it...
				this.tmpSolStore = LogMapWrapper.applyRepair(this.tmpSolStore, repairSet);

				rsActive = repairFacActive.assessMapping(manager, this.tmpSolStore, m, false, rm, violKinds);
		
				// Will the repair be accepted???
				if(rsActive.hasViolations()){
					// NO...
					r = false;
				}
				else {
					// YES...
					this.solutionStore = this.tmpSolStore;
					r = true;
				}
			} else {
//				System.out.println("*** REPAIR IS IRRATIONAL <-------");
				r = false;
			}	
		} else
			System.err.println("isRepairOk() was passed a null repairSet!!!");
		
		return r;
    }
	
	public synchronized boolean doesReceivedMappingCauseViolations(CandidateMapping cm, Set<MappingObjectStr> repairSet)
			throws Exception {		

		// Add the mapping to a temporary version of the solution store
		MappingObjectStr m = LogMapWrapper.convertToLogMapMapping(ontoAlice, ontoBob, cm.getMapping());
		this.tmpSolStore = LogMapWrapper.cloneAlignment(this.solutionStore);
		this.tmpSolStore.add(m);
		
		// if there is a repair, then apply it...
		if (repairSet != null) {
			
			//System.out.println("   Applying repair set: "+repairSet.toString());
			// Note that this happens after adding the mapping, as the repair may modify it...
			this.tmpSolStore = LogMapWrapper.applyRepair(this.tmpSolStore, repairSet);

//		} else {
//			System.out.println("   doesReceivedMappingCauseViolations with no repair set...");
		}

		// Check the effect of the mapping on the current alignment and ontologies
		// TODO: DEBUG - error with cmt and edas when checking...
		// 83 (0): <Alice, ASSERT <http://cmt#Co-author<->http://edas#TPCMember>, 0.05555555555555555, nil>
		// 84 (0): <Bob, ACCEPTC <http://cmt#Co-author<->http://edas#TPCMember>, 0.027777777777777776, nil>

		//  System.out.println("        - "+m.toString());
		if (this.tmpSolStore == null)
			System.out.println("       - tmpSolStore null");
		this.rsPassive = this.repairFacPassive.assessMapping(manager, this.tmpSolStore, m, false, rm, violKinds);

		// Note that we only check here, and don't instigate repairs...
		if(this.rsPassive.hasViolations()){
			return true;
		}
		
		// If the mapping does not cause violations, then at this point we are keeping it!
		// TODO: it might be that we should move this elsewhere?
		this.solutionStore = this.tmpSolStore;

		return false;
	}
	
	
    public synchronized Set<MappingObjectStr> getAssertedRepair(CandidateMapping cm)
    		throws Exception {
    	
		MappingObjectStr m = LogMapWrapper.convertToLogMapMapping(ontoAlice, ontoBob, cm.getMapping());
		
		// Add the mapping to a copy of the current alignment and assess...
		this.tmpSolStore = LogMapWrapper.cloneAlignment(this.solutionStore);
		this.tmpSolStore.add(m);
		
		Set<MappingObjectStr> repairActive = null;

    	// Check to see if there is a violation to repair
		if(this.rsActive.hasValidRepair()){

			repairActive = this.rsActive.getRepair();

			// m could be modified!
			this.tmpSolStore = LogMapWrapper.applyRepair(this.tmpSolStore, repairActive);
			
			// We have a possible repair - does it fix things for the active agent?
			this.rsActive = repairFacActive.assessMapping(manager, 
					this.tmpSolStore, m, false, rm, violKinds);
			
			if(this.rsActive.hasViolations()) {
				// Uh oh, still a violation, so best just skip it...
//				System.out.println("Uh oh... this agent" + 
//						" cannot solve his/her " + 
//						this.rsActive.getViolationsNumber() + 
//						" violation(s) for " + m + ", not proposing it");
				
				repairActive = null;
				
			} else {
				// Ok - no violation, so check if the mapping is still in the new
				// commitment store?  If not, then it was deleted by the repair
				if(!LogMapWrapper.isContained(m, this.tmpSolStore)){
					// Yep - would have been deleted.  So don't assert!
//					System.out.println("The repair would remove the mapping itself, not proposing it");
					repairActive = null;

				}
				// No - would have been ok, so propose...
			}
		}
		
		return repairActive;
    }

	/*
	 * This method checks to see if the new mapping causes violations, returning
	 * true if so, and false otherwise...
	 */
	public synchronized boolean willAssertedMappingCauseViolations(CandidateMapping cm)
			throws Exception {
		
		MappingObjectStr m = LogMapWrapper.convertToLogMapMapping(ontoAlice, ontoBob, cm.getMapping());
//		if (m == null)
//			System.out.println("NULL FOUND");
//		else
//			System.out.println("        - Calling assessMapping on "+m.toString()+" with strength "+m.getConfidence());
		// =======================================================================
		// Check the effect of the mapping on the current alignment and ontologies
		this.rsActive = this.repairFacActive.assessMapping(manager, this.solutionStore, m, false, rm, violKinds);
//		this.tmpSolStore = LogMapWrapper.cloneAlignment(this.solutionStore);
//		this.tmpSolStore.add(m);
		
		return this.rsActive.hasViolations();

	}
	
	public synchronized void aliceIsActive() {
		this.repairFacActive = masRepairAlice;
		this.repairFacPassive = masRepairBob;
//		this.activeAgentID = 0;
//		this.passiveAgentID = 1;
	}

	public synchronized void bobIsActive() {
		this.repairFacActive = masRepairBob;
		this.repairFacPassive = masRepairAlice;
//		this.activeAgentID = 1;
//		this.passiveAgentID = 0;
	}

	// ===========================================================================
	// Reporting methods
	// ===========================================================================

	public String getSolutionAsString() {
		String str = "Joint Solution Store:\n   ";
		str += this.solutionStore.toString().replace(", ", ",\n    ");
		str += "\n";
		int ceq, csub, csup;
		ceq = csub = csup = 0;

		for (MappingObjectStr m:this.solutionStore) {
//			System.out.println("Testing: "+m.toString()+" with relation "+m.getMappingDirection());
            if(m.getMappingDirection() == MappingObjectStr.EQ) ceq++;
            else if(m.getMappingDirection() == MappingObjectStr.SUB) csub++;
            else if(m.getMappingDirection() == MappingObjectStr.SUP) csup++;	
		}
		
		str += "Of " + this.solutionStore.size() + " mappings we have: "
				+ ceq + " <->; "
				+ csub + " ->; "
				+ csup + " <-; ";
		
		return str+"\n";
	}
//	public void printSolution() {
//		System.out.println("    Joint Solution Store: " + 
//				this.solutionStore.toString().replace(", ", ",\n           "));
//		System.out.println("\n");		
//	}

	public int getSizeOfSolution() {
		return this.solutionStore.size();
	}
	public URIAlignment generateSolutionAlignment() {
		URIAlignment solutionURIAlignment = new URIAlignment();

		try {
			solutionURIAlignment.init(ontoAliceURI, ontoBobURI);
			
			// add the cells
			for (MappingObjectStr m:this.solutionStore) {
				URI sourceURI = new URI(m.getIRIStrEnt1());
				URI targetURI = new URI(m.getIRIStrEnt2());
				solutionURIAlignment.addAlignCell(sourceURI, targetURI,"=", m.getConfidence());
				/*
				 * Note that we are *not* including anything wrt the relationship here and
				 * in fact we are explicitly turning every relationship into an equivalence one.
				 */
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// ==============================
		// NOTE: The following code could be used *somewhere* to output
		// the alignment in a usable form.  For now it is left as debug code
//		try {
//			//
//			// Outputing
//			PrintWriter writer;
//
//			writer = new PrintWriter (
//					  new BufferedWriter(
//			                   new OutputStreamWriter( System.out, "UTF-8" )), true);
//		    AlignmentVisitor renderer = new RDFRendererVisitor(writer);
//		    solutionURIAlignment.render(renderer);
//		    writer.flush();
//		    // writer.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// ==============================
		return solutionURIAlignment;
	}


}
