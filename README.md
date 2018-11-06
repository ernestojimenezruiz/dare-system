# DARE System: Dialog-based Alignment Repair Engine

Ontology alignment (also called ontology matching) is the process of identifying correspondences between entities in different, possibly heterogeneous, ontologies. Traditional ontology alignment techniques rely on the full disclosure of the ontological models; however, within open and opportunistic environments, such approaches may not always be pragmatic or even acceptable (due to privacy concerns). Several studies have focused on collaborative, decentralized approaches to ontology alignment, where agents negotiate the acceptability of single correspondences acquired from past encounters, or try to ascertain novel correspondences on the fly. However, such approaches can lead to logical violations that may undermine their utility. The **DARE** system extends a dialogical approach to correspondence negotiation, whereby agents not only exchange details of possible correspondences, but also identify potential violations to the consistency and conservativity principles. Currently, the **DARE** relies on [LogMap's extension for the Conservativity Principle](https://github.com/ernestojimenezruiz/logmap-conservativity/).

## Use and Installation

* This project can be imported into Eclipse or other environments as a maven project or it can be cloned using `git clone https://github.com/ernestojimenezruiz/dare-system.git`
* This project depends on the _logmap-conservativity_ project: 
	1. Clone repository: [https://github.com/ernestojimenezruiz/logmap-conservativity](https://github.com/ernestojimenezruiz/logmap-conservativity)
 (e.g., `git clone https://github.com/ernestojimenezruiz/logmap-conservativity.git`)
	2. Run `mvn clean install` under the _logmap-conservativity_ project folder.
* To generate a JAR file for the **DARE** system, run `mvn clean install` under the _dare-system_ project folder.
* Dependencies:	

	1. Together with the JAR file maven will also generate a folder with the "java-dependencies" (inclusing logmap-conservativity library)
	2. The "lib" folder is also required. The "timeout" programs should be given execution permissions. 
	3. The "asp" folder contains the logic programs needed by the SCC repair algorithm
	4. The "resources" folder contains some example ontologies used in the tests

Check out the running example in the [kr-2016-release](https://github.com/ernestojimenezruiz/dare-system/releases/download/kr-2016-release/dare-kr2016-release.zip). `java -jar dare-system-1.0.0.jar` runs the class _uk.ac.mas.dare.KR15Environment_.

## References

- Ernesto Jiménez-Ruiz, Terry R. Payne, Alessandro Solimando, Valentina A. M. Tamma:
**Limiting Logical Violations in Ontology Alignnment Through Negotiation**. KR 2016: 217-226. ([PDF](http://www.cs.ox.ac.uk/files/8036/kr2016_jimenez-ruiz.pdf))([Slides](https://www.slideshare.net/ernestojimenezruiz/limiting-logical-violations-in-ontology-alignnment-through-negotiation)) 
- Terry R. Payne, Valentina A. M. Tamma:
**Negotiating over ontological correspondences with asymmetric and incomplete knowledge**. AAMAS 2014: 517-524. ([PDF](https://pdfs.semanticscholar.org/3e68/e33e6610e120027a613a7cad74a6e2467f35.pdf))
- Alessandro Solimando, Ernesto Jiménez-Ruiz, Giovanna Guerrini:
**Minimizing conservativity violations in ontology alignments: algorithms and evaluation**. Knowl. Inf. Syst. 51(3): 775-819 (2017). ([PDF](https://www.cs.ox.ac.uk/files/8299/kais-conservativity.pdf))
- [LogMap Conservativity](https://github.com/ernestojimenezruiz/logmap-conservativity/) source codes.


