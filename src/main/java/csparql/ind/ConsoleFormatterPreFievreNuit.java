package csparql.ind;

import eu.larkc.csparql.common.RDFTable;
import eu.larkc.csparql.core.ResultFormatter;
import java.awt.Color;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import org.knowm.xchart.AnnotationLine;
import org.knowm.xchart.AnnotationTextPanel;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class ConsoleFormatterPreFievreNuit extends ResultFormatter {

  private String situationName;
  private String baseUri;
  private OWLOntology ontology;
  private OWLDataFactory factory;
  private List<Date> dataXlabel;
  private XYChart chart;
  private SwingWrapper<XYChart> sw;

  public ConsoleFormatterPreFievreNuit(String situationName, String baseUri, OWLOntology ontology, OWLDataFactory factory,
      List<Date> dataXlabel, XYChart chart, SwingWrapper<XYChart> sw) {
    this.situationName = situationName;
    this.baseUri = baseUri;
    this.ontology = ontology;
    this.factory = factory;
    this.dataXlabel = dataXlabel;
    this.chart = chart;
    this.sw = sw;
  }

  @Override
  public void update(Observable o, Object arg) {
    RDFTable rdfTable = (RDFTable) arg;
    System.out.println();
    if (rdfTable.size() == 0)
      System.out.println("TEMPERATURE NORMAL");
    else {
      System.out.println(situationName + " DETECTED. ");
      rdfTable.stream().forEach((t) -> {
        System.out.println(t.get(0) + " --- " + t.get(1) + " avg temp.");
        AnnotationLine xLine1 = new AnnotationLine(dataXlabel.get(dataXlabel.size() - 1).getTime(), true, false);
        xLine1.setColor(Color.RED);
        chart.addAnnotation(xLine1);
        AnnotationTextPanel tp = new AnnotationTextPanel("Pre-fievre-Nuit", dataXlabel.get(dataXlabel.size() - 1).getTime(), 39, false);
        chart.addAnnotation(tp);
        sw.repaintChart();

        OWLClass Situation = factory.getOWLClass(IRI.create(baseUri + "Situation"));
        OWLIndividual sit = factory.getOWLNamedIndividual(IRI.create(baseUri,situationName + "-" + System.currentTimeMillis()));
        OWLClassAssertionAxiom sitType = factory.getOWLClassAssertionAxiom(Situation, sit);
        ontology.add(sitType);

        try {
					ontology.saveOntology();
				} catch (OWLOntologyStorageException e1) {
					e1.printStackTrace();
        }
      });
    }
  }
}