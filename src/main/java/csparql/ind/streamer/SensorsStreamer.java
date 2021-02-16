package csparql.ind.streamer;

import java.io.FileReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.opencsv.CSVReader;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;

public class SensorsStreamer extends RdfStream implements Runnable {

	private long sleepTime;
	private String baseUri;
	private String prop;
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private double[] dataValues;
	private XYChart chart;
	private SwingWrapper<XYChart> sw;

	public SensorsStreamer(String iri, String baseUri, String prop, long sleepTime,
			OWLOntology ontology, OWLDataFactory factory, double[] dataValues, XYChart chart, SwingWrapper<XYChart> sw) {
		super(iri);
		this.sleepTime = sleepTime;
		this.baseUri = baseUri;
		this.prop = prop;
		this.ontology = ontology;
		this.factory = factory;
		this.dataValues = dataValues;
		this.chart = chart;
		this.sw = sw;
	}

	public void run() {

		try {
			CSVReader reader = new CSVReader(new FileReader("example.csv"));
			List<String[]> allRows = reader.readAll();

			//String result;
			Double result;
			int observationIndex = 0;
			int timeIndex = 0;

			String ontologyURI = "http://semanticweb.org/STEaMINg/ContextOntology-COInd4";
			String ns = ontologyURI + "#";
			String pre_SOSAOnt = "http://www.w3.org/ns/sosa/";
			String pre_TIME = "http://www.w3.org/2006/time#";

			OWLClass Sensor = factory.getOWLClass(IRI.create(pre_SOSAOnt + "Sensor"));
			OWLClass Observation = factory.getOWLClass(IRI.create(pre_SOSAOnt + "Observation"));
			OWLClass ObservableProperty = factory.getOWLClass(IRI.create(pre_SOSAOnt + "ObservableProperty"));
			OWLClass Instant = factory.getOWLClass(IRI.create(pre_TIME + "Instant"));

			OWLObjectProperty madeObservation = factory.getOWLObjectProperty(IRI.create(pre_SOSAOnt + "madeObservation"));
			OWLObjectProperty observedProperty = factory.getOWLObjectProperty(IRI.create(pre_SOSAOnt + "observedProperty"));
			OWLDataProperty hasSimpleResult = factory.getOWLDataProperty(IRI.create(pre_SOSAOnt + "hasSimpleResult"));
			OWLObjectProperty hasTime = factory.getOWLObjectProperty(IRI.create(ns,"hasTime"));
			OWLDataProperty inXSDDateTimeStamp = factory.getOWLDataProperty(IRI.create(pre_TIME + "inXSDDateTimeStamp"));

			while(true){
				try{
					result =  Double.parseDouble(allRows.get(observationIndex)[3]);
					Timestamp date = new Timestamp(System.currentTimeMillis());

					dataValues[observationIndex] = result;
					final double[][] data = getData(observationIndex,dataValues);
					chart.updateXYSeries(prop, data[0], data[1], null);
					sw.repaintChart();

					RdfQuadruple q = new RdfQuadruple(baseUri + "S_" + prop, baseUri + "madeObservation", baseUri + "S_" + prop + "-Obs-" + 	observationIndex, System.currentTimeMillis());
					System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "S_" + prop + "-Obs-" + observationIndex, baseUri + "observedProperty", baseUri + prop, System.currentTimeMillis());
					System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "S_" + prop + "-Obs-" + observationIndex, baseUri + "hasSimpleResult", result + "^^http://www.w3.org/2001/XMLSchema#double", System.currentTimeMillis());
					System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "S_" + prop + "-Obs-" + observationIndex, baseUri + "hasTime", baseUri + "t-obs-S_" + prop + "-"+ timeIndex, System.currentTimeMillis());
					System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "t-obs-S_" + prop + "-"+ timeIndex, baseUri + "inXSDDateTime", date + "^^http://www.w3.org/2001/XMLSchema#dateTimeStamp", System.currentTimeMillis());
					System.out.println(q);
					this.put(q);
				
					OWLIndividual sensor = factory.getOWLNamedIndividual(IRI.create(ns,"S_" + prop));
					OWLClassAssertionAxiom sensorType = factory.getOWLClassAssertionAxiom(Sensor, sensor);
					ontology.add(sensorType);
					OWLIndividual obs = factory.getOWLNamedIndividual(IRI.create(ns,"S_" + prop + "-Obs-" + observationIndex));
					OWLClassAssertionAxiom obsType = factory.getOWLClassAssertionAxiom(Observation, obs);
					ontology.add(obsType);
					OWLIndividual property = factory.getOWLNamedIndividual(IRI.create(ns,prop));
					OWLClassAssertionAxiom propType = factory.getOWLClassAssertionAxiom(ObservableProperty, property);
					ontology.add(propType);

					OWLObjectPropertyAssertionAxiom sensormadeobs = factory.getOWLObjectPropertyAssertionAxiom(madeObservation, sensor, obs);
					ontology.add(sensormadeobs);
					OWLObjectPropertyAssertionAxiom observedProp = factory.getOWLObjectPropertyAssertionAxiom(observedProperty, obs, property);
					ontology.add(observedProp);
					
					OWLIndividual time = factory.getOWLNamedIndividual(IRI.create(pre_TIME,"t-obs-S_" + prop + "-"+ timeIndex));        		
					OWLClassAssertionAxiom timeType = factory.getOWLClassAssertionAxiom(Instant, time);
					ontology.add(timeType);
					OWLObjectPropertyAssertionAxiom obshastime = factory.getOWLObjectPropertyAssertionAxiom(hasTime, obs, time);
					ontology.add(obshastime);
					OWLDataPropertyAssertionAxiom timehasdate = factory.getOWLDataPropertyAssertionAxiom(inXSDDateTimeStamp, time, date + "^^http://www.w3.org/2001/XMLSchema#dateTimeStamp");
					ontology.add(timehasdate);

					OWLDataPropertyAssertionAxiom obshassimpleresult = factory.getOWLDataPropertyAssertionAxiom(hasSimpleResult, obs, result + "^^http://www.w3.org/2001/XMLSchema#double");
					ontology.add(obshassimpleresult);

					try {
						ontology.saveOntology();
					} catch (OWLOntologyStorageException e1) {
						e1.printStackTrace();
					}

					TimeUnit.SECONDS.sleep(sleepTime);
					observationIndex++;
					timeIndex++;
				} catch(Exception e){
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			//TODO: handle exception
		}
	}

	private static double[][] getData(double phase,double[] values) {
		ArrayList<Double> xData1 = new ArrayList<Double>();
		ArrayList<Double> yData1 = new ArrayList<Double>();
		for (int i = 0; i < phase; i++) {
				xData1.add(i+0.0);
				int index=(int) (i);
				yData1.add(values[index]);
		}
		double[] xData = new double[xData1.size()];
		double[] yData = new double[yData1.size()];
		for (int i = 0; i < phase; i++) {
			xData[i] = xData1.get(i);
			yData[i] = yData1.get(i);
	 	}
		return new double[][] { xData, yData };
	}
}