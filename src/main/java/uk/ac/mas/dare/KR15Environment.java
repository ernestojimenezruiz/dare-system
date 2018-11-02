package uk.ac.mas.dare;

//import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import auxStructures.RepairStatus;
import repair.MASRepair;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;
import util.Util;
import enumerations.REPAIR_METHOD;
import enumerations.VIOL_KIND;
import static java.util.Arrays.asList;


public class KR15Environment {
	
	static String versionDetails = "1.0 (Nov 2015)";

	static int ALICE = 0;	// ID for Alice in arrays etc
	static int BOB = 1;		// ID for Bob in arrays etc
	static List<VIOL_KIND> violKinds = new ArrayList<>();
	static REPAIR_METHOD rm = REPAIR_METHOD.SUBEQ;
	
	static MASRepair masRepairAlice = null;
	static MASRepair masRepairBob = null;
	static MASRepair repairFacActive = null;
	static MASRepair repairFacPassive = null;
	
	static MASRepair alignmentStore = null;			// test to see if we still work without
													// having loaded the mappings into alice

	// ==========================================================================================
	// Machinery for handling OAEI Aggregated alignments
	
	static String fileRoot = System.getProperty("user.dir") + "/resources/";
	//static String fileRoot = "/Users/trp/Research.local/OAEI/2012/";
//	static String alignmentRoot = "conference-alignments-subset/";
	//static String alignmentRoot = "conference-alignments-cleanup/";
	static String alignmentRoot = fileRoot + "/OAEI/2012/conference/system-mappings/";
	static String referenceRoot = fileRoot + "/OAEI/2012/conference/reference-alignment/";
	static String ontologiesRoot = fileRoot + "/OAEI/2012/conference/ontologies/";
	
	static String kr16ExampleRoot = fileRoot + "/kr16/";
	
	
	static List<String> alignmentMethodNames = asList("AROMA", "ASE", "AUTOMSv2", "CODI", "GOMMA",
	"Hertuda", "HotMatch", "LogMap", "LogMapLt", "MEDLEY", "MaasMatch", "MapSSS",
	"Optima", "ServOMap", "ServOMapL", "WeSeE", "Wmatch", "YAM++");
	static List<String> ontologyNames = asList("cmt", "conference", "confof", "edas", "ekaw", "iasted", "sigkdd");
	static int overlap = 0;
	static int maxNumberOfAlignmentsPerAgent = 10;
	static int numberOfIterationsPerOntologyPair = 1;
	static Random randomGenerator;



	private static ArrayList<URI> generateFullURIList(String ontSource, String ontTarget) {
	    URI overlapUri;
	    ArrayList<URI> fullURIList = new ArrayList<URI>();
	    
		for (String name:alignmentMethodNames) {
		    overlapUri = new File(alignmentRoot + name +"-" + ontSource + "-" + ontTarget + ".rdf").toURI();
		    fullURIList.add(overlapUri);
		}
		return fullURIList;
	}
	

	
	// ==========================================================================================
	// ==========================================================================================
	// Test suite from Alessandro Solimando's test rig
	
	private enum MOVE {

		JOIN("1A","2B"),
		ASSERT("3A","6B"),
		ACCEPTC("7A","4B"),
		REJECTC("7A","4B"),
		REPAIR("7A","4B"),
		ACCEPTR("5A","8B"),
		REJECTR("5A","8B"),
		CLOSE("3A","6B");

		private String lblAgent1, lblAgent2;
		private String [] agentLabels = {"Alice","Bob"};

		private MOVE(String lblAgent1, String lblAgent2){
			this.lblAgent1 = lblAgent1;
			this.lblAgent2 = lblAgent2;
		}

		public String toString(int i){
			return "MOVE " + (i == 0 ? lblAgent1 : lblAgent2) + " " + 
					agentLabels[i] + " - " + name() + ": ";
		}
	}
	
	private static void initRepair() {
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

	private static void initiateRepairObject(
			String [] paths,
			List<OWLOntology> ontos,
			OWLOntologyManager mgr) throws Exception {
		
		// load the two ontologies (the other only needed for the signature)
		OWLOntology ontoAlice = OntoUtil.load(paths[0], true, mgr);
		OWLOntology ontoBob = OntoUtil.load(paths[1], true, mgr);
		
		ontos.add(ontoAlice);
		ontos.add(ontoBob);
		
		try {
			masRepairAlice = new MASRepair(ontoAlice, ontoBob, true, true);
			masRepairBob = new MASRepair(ontoBob, ontoAlice, true, false);
			alignmentStore = new MASRepair(ontoAlice, ontoBob, true, true);
		} catch (OWLOntologyCreationException e) {
			//fail("Exception while creating the \"hidden\" ontology: " + e.getMessage());
			System.err.println("Exception while creating the \"hidden\" ontology: " + e.getMessage());
		}

	}
	
	private static void owledValidate() throws Exception {
		
		// ======================================================
		// initialise the repair settings
		initRepair();

		// ======================================================
		// Load in the ontologies and alignment
		OWLOntologyManager managerTmp = OntoUtil.getManager(false), 
				manager = OntoUtil.getManager(true);

		String [] agentLabels = {"Alice","Bob"};
		String [] ontoPaths = {"owled15/aliceNODISJ.owl", "owled15/bob.owl"};
		
		List<OWLOntology> ontos = new ArrayList<>(2);

		initiateRepairObject(ontoPaths, ontos, managerTmp);
		
		String alignPath = "owled15/align2.rdf";
		List<MappingObjectStr> mappings = alignmentStore.loadMappings(alignPath);
		// List<MappingObjectStr> mappings = masRepairAlice.loadMappings(alignPath);
//		Collections.swap(mappings, 1, 3);
//		Collections.swap(mappings, 2, 3);
//		Collections.swap(mappings, 3, 4);

		Collections.swap(mappings, 1, 3);
		System.out.println("Loaded set of mappings:\n"+Util.prettyPrint(mappings));

		// ======================================================
		// Now iterate through the possible moves
		// ======================================================
		//
		// Commitment Store
		Set<MappingObjectStr> commitmentStore = new HashSet<>();

		// States
		int c = 0;				// Context State !!!
		RepairStatus rsActive = null;	//repair status...
		RepairStatus rsPassive = null;	//repair status...
		int activeAgentID = 0;
		int passiveAgentID = 1;

		System.out.println("");
		System.out.println("    "+MOVE.JOIN.toString(0));
		System.out.println("    "+MOVE.JOIN.toString(1));
		System.out.println("");
		
		Iterator<MappingObjectStr> itr = mappings.iterator();
		while (itr.hasNext()) {

			// =======================================================================
			// Change Perspectives for different agents (i.e. who sender vs recipient)
			if(c % 2 == 0){// || c == 5){
				repairFacActive = masRepairAlice;
				repairFacPassive = masRepairBob;
				activeAgentID = 0;
				passiveAgentID = 1;
			}
			else {
				repairFacActive = masRepairBob;
				repairFacPassive = masRepairAlice;
				activeAgentID = 1;
				passiveAgentID = 0;
			}
			c++;  // Increment c
			// =======================================================================
			
			MappingObjectStr m = itr.next();
			System.out.println("===================================================");
			System.out.println("@@@ c="+c+" with "+agentLabels[activeAgentID]+" considers move with correspondence:");
			System.out.println("@@@ "+m+"\n");
					
//			if(c == 2){
//				c++;
//				System.out.println("Skipping " + m);
//				continue;
//			}


			// =======================================================================
			// Report on Commitment Store as part of the debugging process
			System.out.println("    CS: " + 
			commitmentStore.toString().replace(", ", ",\n         ")+"\n");
			// =======================================================================

			
			// =======================================================================
			// FIXED POINT - in the trace Bob believes the correspondence has a
			// confidence above threshold, but Alice believes it to be below, and
			// therefore rejects it 
			if(m.getConfidence() < 0.45){
				System.out.println("    "+MOVE.ASSERT.toString(passiveAgentID) + m);
				System.out.println("    "+MOVE.REJECTC.toString(passiveAgentID) + m);
				continue;
			}
			// =======================================================================

			
			// =======================================================================
			// Check if the next correspondence causes a violation for the active agent
			// System.out.println(agentLabels[activeAgentID] + " checks before asserting");
			System.out.print("    MASRepair(A1): ");
			rsActive = repairFacActive.assessMapping(manager, commitmentStore, 
					m, false, rm, violKinds);
			System.out.println("");

			// WHAT EXACTLY IS NEXTCS AND HOW IS IT USED???
			// Hypothesis - next commitment store; so we clone the existing
			// commitment store and operate on that for now???
			Set<MappingObjectStr> nextCS = LogMapWrapper.cloneAlignment(commitmentStore);
			nextCS.add(m);
			
			if(rsActive.hasViolations()){
				// ===============================================================
				// Assert with Repair

				System.out.println("    "+MOVE.ASSERT.toString(activeAgentID) + m + 
						" with REPAIR, " + rsActive.getViolationsNumber() + 
						" violation(s)");
				
				// Need to see if we have a valid repair...
				if(rsActive.hasValidRepair()){
					Set<MappingObjectStr> repairActive = rsActive.getRepair();

					// m could be modified!
					nextCS = LogMapWrapper.applyRepair(nextCS, repairActive);
										
					// ===============================================================
					// We have a possible repair - does it fix things for the active agent?
					System.out.println("    " + agentLabels[activeAgentID] + 
							" checks his/her own repair before proposing it");
					System.out.println("    " + agentLabels[activeAgentID] + 
							"'s repair: " + repairActive.toString().replace(", ", ",\n    "));

					// Now check whether there is still a violation post repair
					System.out.print("    MASRepair(A2): ");
					rsActive = repairFacActive.assessMapping(manager, 
							nextCS, m, false, rm, violKinds);
					
					if(rsActive.hasViolations()) {
						// Uh oh, still a violation, so best just skip it...
						System.out.println(agentLabels[activeAgentID] + 
								" cannot solve his/her " + 
								rsActive.getViolationsNumber() + 
								" violation(s) for " + m + ", not proposing it");
					} else {
						// Ok - no violation, so check if the mapping is still in the new
						// commitment store?  If not, then it was deleted by the repair
						if(!LogMapWrapper.isContained(m, nextCS)){
							// Yep - would have been deleted.  So don't assert!
							System.out.println("The repair would remove the mapping itself, not proposing it");
							continue;
						} else {
							// No - would have been ok, so propose...
							System.out.println("    No problem now... carry on...");
							System.out.println("    "+MOVE.ASSERT.toString(activeAgentID) + m + 
									" with REPAIR, " + rsActive.getViolationsNumber() + 
									" violation(s)");
						}
					}
				}

				else {
					System.out.println(agentLabels[activeAgentID] + 
							" cannot solve his/her " + 
							rsActive.getViolationsNumber() + 
							" violation(s) for " + m + ", not proposing it");
					continue;
				}
			}
			else {
				// =======================================================================
				// Assert without repair
				System.out.println("    "+MOVE.ASSERT.toString(activeAgentID) + m);
			}
			// =======================================================================

			// RECIPIENT'S RESPONSE TO SENDER'S ACTION
			
			// =======================================================================
			System.out.println("---------------------------------------------------");
			System.out.println("@@@ c="+c+" with "+agentLabels[passiveAgentID]+" considers move...");
			// System.out.println("@@@ <<insert retrieved move here>>>\n");

			// notice: m could have been weakened!
			
			// System.out.println(agentLabels[passiveAgentID] + " checks before accepting");

			// we indirectly "apply" the suggested repair by using the repaired CS
			System.out.print("    MASRepair(P1): ");
			rsPassive = repairFacPassive.assessMapping(manager, nextCS, 
					m, false, rm, violKinds);
			System.out.println("");
			
			// Check if the asserted correspondence causes a violation for the passive agent
			if(rsPassive.hasViolations()){
				System.out.println("    "+ agentLabels[passiveAgentID] + " detects a violation... ");

				Set<MappingObjectStr> repairAsReply = rsPassive.getRepair();

				// ===============================================================
				// We know accepting the correspondence causes a violation!
				// Can a repair be found?
				if(rsPassive.hasValidRepair()){
														
					System.out.println("    .... and identifies a valid repair:\n    " + 
							repairAsReply.toString().replace(", ", ",    \n"));
					
					nextCS = LogMapWrapper.applyRepair(nextCS, repairAsReply);

					System.out.print("    MASRepair(P2): ");
					rsPassive = repairFacPassive.assessMapping(manager, 
							nextCS, null, false, rm, violKinds);
					System.out.println("");

					if(rsPassive.hasViolations()){
						// ======================================================
						// Oh bother, even the repair causes further violations!!!
						// Ok - quit whilst we are ahead...
						System.out.println(m + ": " + agentLabels[passiveAgentID] + 
								" has " + rsPassive.getViolationsNumber() + 
								" violation(s) but cannot compute a repair, rejecting");
						System.out.println("\n"+MOVE.REJECTC.toString(passiveAgentID) + m);
						continue;
					}
					else {
						// ======================================================
						// Yay - the repair is ok; lets propose it and see...
						System.out.println("    "+MOVE.REPAIR.toString(passiveAgentID) 
								+ repairAsReply);

						// let's see if the repair is fine also for the other agent
						System.out.println("\n    " + agentLabels[activeAgentID] + 
								" checks repair before accepting it");
						
						System.out.print("    MASRepair(A3): ");
						rsActive = repairFacActive.assessMapping(manager, 
								nextCS, m, false, rm, violKinds);
						System.out.println("");
				
						// Will the repair be accepted???
						if(rsActive.hasViolations()){
							// NO...
							System.out.println("    "+MOVE.REJECTR.toString(activeAgentID) + m);
							continue;
						}
						else {
							// YES...
							System.out.println("    "+MOVE.ACCEPTR.toString(activeAgentID));
							commitmentStore = nextCS;
							continue;
						}
					}										
				} else {
					// No repair to the violation could be found; thus reject the correspondence 
					System.out.println("    "+MOVE.REJECTC.toString(passiveAgentID) + m);
					continue;
				}
			}
			else {
				// =======================================================================
				// Accept without repair
				System.out.println("    Move (without repair suggested or required) looks good - lets accept");
				System.out.println("    "+MOVE.ACCEPTC.toString(passiveAgentID) + m);
				commitmentStore = nextCS;
				continue;
			}	
		}
		// =======================================================================

		
		
		// =======================================================================
		// Closing off the remaining moves
		System.out.println("    "+MOVE.CLOSE.toString(1));
		System.out.println("    "+MOVE.CLOSE.toString(0));
		// =======================================================================

		
		
		// =======================================================================
		// All done!
		System.out.println("\n");
		System.out.println("Final CS: " + 
				commitmentStore.toString().replace(", ", ",\n           "));
		
		System.out.println("END");

	}
	
	

	
	
	// ==========================================================================================
	// ==========================================================================================
	
	private static void runTest(double evidenceThreshold, boolean verboseFlag) {
		
		String [] agentLabels = {"Alice","Bob"};

		// ==================================
		// OWL-ED walkthrough example
//		String [] ontoPaths = {"kr16/aliceNODISJ.owl", "kr16/bob.owl"};
//		String [] alignPaths = {"kr16/aliceAlign.rdf", "kr16/bobAlign.rdf"};

		// ==================================
		// KR walkthrough example
		//String [] ontoPaths = {"kr16/alicekr.owl", "kr16/bobkr.owl"};
		//String [] alignPaths = {"kr16/aliceAlignkr.rdf", "kr16/bobAlignkr.rdf"};
	
		String [] ontoPaths = {kr16ExampleRoot + "alicekr.owl", kr16ExampleRoot + "bobkr.owl"};
		String [] alignPaths = {kr16ExampleRoot + "aliceAlignkr.rdf", kr16ExampleRoot + "bobAlignkr.rdf"};
	
		
		
		// ==================================
		// OAEI examples
//		String root="/Users/trp/Research/OAEI/2014/";
//		String [] ontoPaths = {root+"conference/confOf.owl", root+"conference/ekaw.owl"};
//		String [] alignPaths = {
//				root+"reference-alignment/confOf-ekaw.rdf", 
//				root+"reference-alignment/confOf-ekaw.rdf"};
		
		// ==================================
		// OAEI 2013 Anatomy example
//		String root="/Users/trp/Research/OAEI/2013/anatomy-dataset/";
//		String [] ontoPaths = {root+"mouse.owl", root+"human.owl"};
//		String [] alignPaths = {
//				root+"reference.rdf", 
//				root+"reference.rdf"};

		// ======================================================
		// Set up the alignment stores
		AlignmentStore asAlice = new AlignmentStore();				// Create store
		
		ArrayList<URI> uriListAlice = new ArrayList<URI>();
		uriListAlice.add(new File(alignPaths[ALICE]).toURI());		// As the alignments to the list
		asAlice.loadAlignmentsFromUriList(uriListAlice, true);		// and load them into the store

		
		AlignmentStore asBob = new AlignmentStore();
		ArrayList<URI> uriListBob = new ArrayList<URI>();
		uriListBob.add(new File(alignPaths[BOB]).toURI());
		asBob.loadAlignmentsFromUriList(uriListBob, true);
		
		CommsChannel cc = new CommsChannel();		// Set up a shared object for comms
		RepairManager repairMgr = new RepairManager();
		repairMgr.initiateRepairObjects(ontoPaths);
		
		//repairMgr.loadAlignments(alignPaths);
		
		Agent agent1 = new Agent(cc, repairMgr, agentLabels[ALICE], null, asAlice, evidenceThreshold, verboseFlag, true);
		Agent agent2 = new Agent(cc, repairMgr, agentLabels[BOB], null, asBob, evidenceThreshold, verboseFlag, false);

		agent1.start();
		agent2.start();

		// Wait until the threads have finished.
		try {
			agent1.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			agent2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.exit(0);

	}

	private static void runExperiment(String ont1, String ont2,
			int numExperiments, double evidenceThreshold, boolean verboseFlag) {
		
//		randomGenerator = new Random();
		String [] agentLabels = {"Alice","Bob"};
		//String root="/Users/trp/Research.local/OAEI/2012/conference/";
		String [] ontoPaths = {ontologiesRoot+ont1+".owl", ontologiesRoot+ont2+".owl"};

		CommsChannel cc = new CommsChannel();		// Set up a shared object for comms
		RepairManager repairMgr = new RepairManager();
		repairMgr.initiateRepairObjects(ontoPaths);
		
		URI uri = new File(referenceRoot + ont1 + "-" + ont2 + ".rdf").toURI();
		repairMgr.setOntoReference(uri);

		// ======================================================
		// Set up the alignment stores
		
		// Create a temporary store which can load in the alignments
		// and then check their semantic validity
		
		AlignmentStore as = new AlignmentStore();
		ArrayList<URI> myURIList = generateFullURIList(ont1, ont2);
		as.loadAlignmentsFromUriList(myURIList, false);
		CandidateAlignment ca = new CandidateAlignment();
		
		int origMappingCount = as.getCandidateAlignment().getCandidateMappings().size();
		int deletedMappingCount = 0;

//		System.out.print("Filtering ... ");
		for (CandidateMapping cm:as.getCandidateAlignment().getCandidateMappings()) {
//			System.out.println("checking: "+cm.toString());
			MappingObjectStr m;
			try {
				m = repairMgr.getLogMapMapping(cm);
				if (m==null) {
					deletedMappingCount++;
//					System.out.println(" >>> Failed to get Logmap equivalent!  Deleting...");
				} else {
					ca.getCandidateMappings().add(cm);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		as.setCandidateAlignment(ca);
		
//		System.out.println("from "+origMappingCount+
//				" mappings, we deleted "+deletedMappingCount+
//				" leaving only "+as.getCandidateAlignment().getCandidateMappings().size()+" mappings\n");
		

		// The following is inefficient, but we live with it.  Create the alignment stores
		// for each agent, and then replace the candidate mappings.
		// We explicitly create a new CA in each case to avoid shared data.
				
		AlignmentStore asAlice = new AlignmentStore();				// Create store		
		asAlice.loadAlignmentsFromUriList(myURIList, false);
		CandidateAlignment aliceCa = new CandidateAlignment(ca);
		asAlice.setCandidateAlignment(aliceCa);
		
		AlignmentStore asBob = new AlignmentStore();
		asBob.loadAlignmentsFromUriList(myURIList, false);
		CandidateAlignment bobCa = new CandidateAlignment(ca);
		asBob.setCandidateAlignment(bobCa);

		Agent agent1 = new Agent(cc, repairMgr, agentLabels[ALICE], null, asAlice, evidenceThreshold, verboseFlag, true);
		Agent agent2 = new Agent(cc, repairMgr, agentLabels[BOB], null, asBob, evidenceThreshold, verboseFlag, false);

		agent1.start();
		agent2.start();

		// Wait until the threads have finished.
		try {
			agent1.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			agent2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		System.out.println("Starting KR16 Code: " +
//	              System.getProperty("user.dir"));
		
		double evidenceThreshold = 0.0;		// Threshold
        boolean vflag = false;				// Verbose
		String ont1 = "cmt";
		String ont2 = "ekaw";
		
        int i = 0;
        String arg;

        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

            if (arg.equals("-verbose")) {
                System.out.println("verbose mode on");
                vflag = true;
            }
            else if (arg.equals("-ont1")) {
                if (i < args.length)
                    ont1 = args[i++];
                else
                    System.err.println("-ont1 requires an ontology name");
                if (vflag)
                    System.out.println("ont1 ontology = " + ont1);
            }
            else if (arg.equals("-ont2")) {
                if (i < args.length)
                    ont2 = args[i++];
                else
                    System.err.println("-ont2 requires an ontology name");
                if (vflag)
                    System.out.println("ont2 ontology = " + ont2);
            }
            else if (arg.equals("-t")) {
                if (i < args.length)
                	try {
                		evidenceThreshold = Double.parseDouble(args[i++]);
                		
                		if (evidenceThreshold>1.0)
                			evidenceThreshold=1.0;
                		
                		if (evidenceThreshold<0.0)
                			evidenceThreshold=0.0;
                		
                	} catch (NumberFormatException e) {
                	      //Will Throw exception!
                        System.err.println("-t 0..1 (double - num not valid, assuming value "+evidenceThreshold+")");
                	}
                else
                    System.err.println("-t requres a number");
                if (vflag)
                    System.out.println("t (threshold):" + evidenceThreshold);
            }
        }
		
        //runTest(0.1, true);
        vflag=true;
        runExperiment(ont1, ont2, overlap, evidenceThreshold, vflag);


//		int runType = 3;  // 1 - validation code; 2 - dialogue test; 3 - experiments
//		switch (runType) {
//		case 1:
//			// =====================================================
//			// Run the Validation Test Code
//			// =====================================================
//			try {
//				owledValidate();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			break;
//		case 2:
//			// =====================================================
//			// Run the dialogue test
//			// =====================================================
//			runTest(evidenceThreshold, vflag);
//			break;
//		case 3:
//			// =====================================================
//			// Run the dialogue over OAEI datasets
//			// =====================================================
//			evidenceThreshold = 0.0;
//			vflag=true;
////			runExperiment("cmt", "conference", overlap, evidenceThreshold, vflag);
////			runExperiment("cmt", "confOf", overlap, evidenceThreshold, vflag);
////			runExperiment("cmt", "edas", overlap, evidenceThreshold, vflag);
////			runExperiment("cmt", "ekaw", overlap, evidenceThreshold, vflag);
////			runExperiment("cmt", "iasted", overlap, evidenceThreshold, vflag);
////			runExperiment("cmt", "sigkdd", overlap, evidenceThreshold, vflag);
////
////			runExperiment("conference", "confOf", overlap, evidenceThreshold, vflag);
////			runExperiment("conference", "edas", overlap, evidenceThreshold, vflag);
////			runExperiment("conference", "ekaw", overlap, evidenceThreshold, vflag);
////			runExperiment("conference", "iasted", overlap, evidenceThreshold, vflag);
////			runExperiment("conference", "sigkdd", overlap, evidenceThreshold, vflag);
////
////			runExperiment("confOf", "edas", overlap, evidenceThreshold, vflag);
////			runExperiment("confOf", "ekaw", overlap, evidenceThreshold, vflag);
////			runExperiment("confOf", "iasted", overlap, evidenceThreshold, vflag);
//			runExperiment("confOf", "sigkdd", overlap, evidenceThreshold, vflag);
////
////			runExperiment("edas", "ekaw", overlap, evidenceThreshold, vflag);
////			runExperiment("edas", "iasted", overlap, evidenceThreshold, vflag);
////			runExperiment("edas", "sigkdd", overlap, evidenceThreshold, vflag);
////
////			runExperiment("ekaw", "iasted", overlap, evidenceThreshold, vflag);
////			runExperiment("ekaw", "sigkdd", overlap, evidenceThreshold, vflag);
////
////			runExperiment("iasted", "sigkdd", overlap, evidenceThreshold, vflag);
//
//			break;
//		default:
//			System.err.println("No valid run type specified");
//		}
	}
}
