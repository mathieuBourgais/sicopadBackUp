package csparql.ind;

import eu.larkc.csparql.common.RDFTable;
import eu.larkc.csparql.core.ResultFormatter;
import java.util.Observable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class ConsoleFormatter extends ResultFormatter {
    
    private String situationName;
    private String baseUri;
    private OWLOntology ontology;
    private OWLDataFactory factory;

	  public ConsoleFormatter(String  situationName, String baseUri, OWLOntology ontology, OWLDataFactory factory) {
      this.situationName = situationName;
      this.baseUri = baseUri;
      this.ontology = ontology;
      this.factory = factory;
    }

    @Override
    public void update(Observable o, Object arg) {

      RDFTable rdfTable = (RDFTable)arg;
      System.out.println();
      
      if (rdfTable.size()==0)
        System.out.println("NO RESULT");
      else {
        System.out.println(situationName + " DETECTED. "+ rdfTable.size() + " result at SystemTime: "+System.currentTimeMillis());
        rdfTable.stream().forEach((t) -> {
          System.out.println(t.get(0) + " --- " + t.get(1) + " avg temp. " + situationName);
          
          OWLClass Situation = factory.getOWLClass(IRI.create(baseUri + "Situation"));
          OWLIndividual sit = factory.getOWLNamedIndividual(IRI.create(baseUri,situationName + "-" + System.currentTimeMillis()));
          OWLClassAssertionAxiom sitType = factory.getOWLClassAssertionAxiom(Situation, sit);
          ontology.add(sitType);
          /*
          OWLClass Machine = factory.getOWLClass(IRI.create(baseUri + "Machine"));
          OWLIndividual M3 = factory.getOWLNamedIndividual(IRI.create(t.get(0)));
          OWLClassAssertionAxiom machineM3 = factory.getOWLClassAssertionAxiom(Machine, M3);
          ontology.add(machineM3);
          OWLClass Line = factory.getOWLClass(IRI.create(baseUri + "Line"));
          OWLIndividual PL1 = factory.getOWLNamedIndividual(IRI.create(t.get(1)));
          OWLClassAssertionAxiom pordLinePL1 = factory.getOWLClassAssertionAxiom(Line, PL1);
          ontology.add(pordLinePL1);

          OWLObjectProperty concernBy = factory.getOWLObjectProperty(IRI.create(baseUri + "concernBy"));
          OWLObjectPropertyAssertionAxiom concernByAssertM3 = factory.getOWLObjectPropertyAssertionAxiom(concernBy, M3, sit);
          ontology.add(concernByAssertM3);
          OWLObjectPropertyAssertionAxiom concernByAssertPL1 = factory.getOWLObjectPropertyAssertionAxiom(concernBy, PL1, sit);
          ontology.add(concernByAssertPL1);
          */
          try {
				  	ontology.saveOntology();
				  } catch (OWLOntologyStorageException e1) {
					  e1.printStackTrace();
          }
          
        });
      }
    }
}