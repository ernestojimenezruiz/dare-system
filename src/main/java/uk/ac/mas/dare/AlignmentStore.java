package uk.ac.mas.dare;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLOntology;

import repair.MASRepair;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

public class AlignmentStore {
	
	private CandidateAlignment candidateAlignment;
	private ArrayList<URI> uriList;
	private int originalNumberOfAlignments;
	private OWLOntology sourceOntology;
	private OWLOntology targetOntology;
	private MASRepair originalAlignmentStore;
	
	// ======================================================
	
	public AlignmentStore() {
		this.setCandidateAlignment(null);
		originalNumberOfAlignments = 0;
		this.setSourceOntology(null);
		this.setTargetOntology(null);
		this.setOriginalAlignmentStore(null);
	}

	public MASRepair getOriginalAlignmentStore() {
		return originalAlignmentStore;
	}

	public void setOriginalAlignmentStore(MASRepair originalAlignmentStore) {
		this.originalAlignmentStore = originalAlignmentStore;
	}

	public OWLOntology getSourceOntology() {
		return sourceOntology;
	}

	public void setSourceOntology(OWLOntology sourceOntology) {
		this.sourceOntology = sourceOntology;
	}

	public OWLOntology getTargetOntology() {
		return targetOntology;
	}

	public void setTargetOntology(OWLOntology targetOntology) {
		this.targetOntology = targetOntology;
	}

	public CandidateAlignment getCandidateAlignment() {
		return candidateAlignment;
	}

	public void setCandidateAlignment(CandidateAlignment candidateAlignment) {
		this.candidateAlignment = candidateAlignment;
	}

	public ArrayList<URI> getUriList() {
		return uriList;
	}

	public int getOriginalNumberOfAlignments() {
		return originalNumberOfAlignments;
	}
	
//	public void loadOntologies(String sourcePath, String targetPath) {
//		OWLOntologyManager managerTmp = OntoUtil.getManager(false);
//		try {
//			this.setSourceOntology(OntoUtil.load(sourcePath, true, managerTmp));
//			this.setTargetOntology(OntoUtil.load(targetPath, true, managerTmp));
//			this.setOriginalAlignmentStore(
//					new MASRepair(this.getSourceOntology(),
//								  this.getTargetOntology(),
//								  true, true));
//		} catch (OWLOntologyCreationException e) {
//			fail("Exception whilst loading the ontologies: " + e.getMessage());
//		}
//
//	}

	// ============================================================================
	// ============================================================================
	// The following method is responsible for parsing a list of alignments (as URIs)
	// and building a candidate alignment based on the mappings, and a probability score
	

	/**
	 * Loads the alignment files (stored in an array of URIs) using Jerome's Alignment
	 * API, and builds up the necessary internal structures
	 * @param uriList Array of URIs referencing files to be loaded.
	 * @param boolean averageWeights - average out the 
	 */

	public void loadAlignmentsFromUriList(ArrayList<URI> uriList, boolean useStrength) {

		Alignment myAlignment = null;
		this.uriList = uriList; 	// Just copying for now...
		
		CandidateAlignment newCA = new CandidateAlignment();
		
		for (URI uri:uriList) {
			try {
				AlignmentParser aparser = new AlignmentParser(0);
				myAlignment = aparser.parse(uri);
			} catch (AlignmentException e) {
				e.printStackTrace();
			}
			
			// If we have read in the alignment, process the alignment into our internal structures
			// so that we can determine our probabilities
			if (myAlignment != null) {
				try {
					for (Cell mapping:myAlignment) {
						boolean found = false;
//						originalNumberOfMappingsParsed++; // Increment our count
						// For each mapping found - check to see if it has been encountered before;
						// if so, increment count, otherwise, add it to our list
						for (CandidateMapping cm:newCA.getCandidateMappings()) {
							if ((cm.getSourceConcept().equals(mapping.getObject1().toString() )) &&
									(cm.getTargetConcept().equals(mapping.getObject2().toString() ))) {
								found = true;
								cm.incrementCount();
								break;
							}
						}
						if (!found) {
							// the mapping needs to be added to our candidate alignment
							CandidateMapping myCM = new CandidateMapping(mapping);
							newCA.getCandidateMappings().add(myCM);
//							originalNumberOfUniqueMappings++;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (useStrength) {
			// Use the original strenth in the alignment.  Note that this currently
			// only takes the latest strength recorded and doesn't do anything clever
			// with multiple alignments (e.g. averaging etc)
			for (CandidateMapping cm:newCA.getCandidateMappings())
				cm.copyStrengthToProbability();
		} else {
			// Now iterate through the counts and convert into the probability values
			originalNumberOfAlignments = uriList.size();
			for (CandidateMapping cm:newCA.getCandidateMappings()) {
				cm.updateProbabililty(originalNumberOfAlignments);
			}
		}
		
		// Finally, sort the data based on probabilities
		Collections.sort(newCA.getCandidateMappings(), new CandidateMapComparator());
		// Now we have generated our candidate alignment, we store it locally in the alignment.
		this.setCandidateAlignment(newCA);
	}

	


	// This is just a convenience method, to simplify the check.
	public boolean hasMappings() {
		return getCandidateAlignment().hasMappings();
	}

//	public CandidateMapping getProposal() {
//
//		// We assume that the candidateAlignment is always sorted
//		return this.candidateAlignment.head();
//		
//	}

	// ==========================================================================================
	// provides a shortcut to the lookup method in the candidateAlignment
	public CandidateMapping lookup(CandidateMapping otherMapping) {
		return this.candidateAlignment.lookup(otherMapping);
	}

	public void delete(CandidateMapping myLocalMapping) {
		// This is (sadly) more of a hack than anything.  It is assumed
		// that myLocalMapping is a mapping that is being stored in the
		// candidateAlignment.  If found, it is deleted.
		
		// Suppress the "unused" warning here, as we only use the variable success
		// when we uncomment the debugging code diagnostics, below
		@SuppressWarnings("unused")
		boolean success = this.candidateAlignment.getCandidateMappings().remove(myLocalMapping);
		
		// DEBUG
//		if (success) {
//			System.out.println(myLocalMapping.toString()+" deleted");
//		} else {
//			System.out.println("Failed to delete "+myLocalMapping.toString());
//		}
		
	}

	/**
	 * Provides a shortcut to the findMappingsMatching method.
	 * @param mapping The CandidateMapping object to be matched
	 * @return A list (ArrayList) of CandidateMapping objects that match mapping (i.e.
	 * that share the same string for the source or target of the mapping).  
	 */
	public ArrayList<CandidateMapping> findMappingsMatching(CandidateMapping mapping) {
		return this.candidateAlignment.findMappingsMatching(mapping);
	}
	
	/**
	 * Eliminates a random sample of the data, to simulate the affect of having
	 * knowledge poor agents.  Ideally, the agent should be built with several
	 * source alignments so that it can build a reasonable model of utilities for
	 * each correspondence.  However, to simulate the case where *few* correspondences
	 * are known, we then want to randomly eliminate them.
	 * @param pcCut A percentage of the mappings that should be removed.  This can be
	 * a number between 0.0 (nothing will be eliminated) to 1.0 (the entire alignment store
	 * will be eliminated).
	 */
	
	public void pruneRandomSample(double pcCut) {
		// bounds check
		if ((pcCut < 0.0) || (pcCut > 1.0)) {
			System.err.println("Warning: AlignmentStore.pruneRandomSample("+pcCut+") parameter invalid; should be between 0.0 and 1.0");
		} else {
			int originalSize = this.candidateAlignment.getCandidateMappings().size();
			int pruneNumber = (int) (pcCut * originalSize);

			// Do this the simple way.  Pick a number corresponding to one of the mappings, and
			// delete it.  Repeat until we've done them all
			for (;pruneNumber > 0; pruneNumber--) {
				this.candidateAlignment.deleteRandomMapping();
			}
		}
		
	}

	/**
	 * Iterates through the alignment, removing any mapping that is less than the
	 * evidenceThreshold.  This was implemented for the exhaustive tests, and should
	 * not be used for the decentralised, agent-based negotiation
	 * @param evidenceThreshold The evidence threshold.
	 */
	public void pruneWithThreshold(double evidenceThreshold) {
		// TODO Auto-generated method stub
		ArrayList<CandidateMapping> oldList = this.getCandidateAlignment().getCandidateMappings();
		CandidateAlignment newCA = new CandidateAlignment();
		for (CandidateMapping cm:oldList) {
			if (cm.getProbability() >= evidenceThreshold)
				newCA.getCandidateMappings().add(cm);
		}
		this.setCandidateAlignment(newCA);
	}
}
