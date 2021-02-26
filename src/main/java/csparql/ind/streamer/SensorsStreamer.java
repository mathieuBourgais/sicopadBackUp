package csparql.ind.streamer;

import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	private List<Date> axis_x_values;
	private List<Double> axis_y_values;
	private XYChart chart;
	private SwingWrapper<XYChart> sw;
	private int start;

	public SensorsStreamer(String iri, String baseUri, String prop, long sleepTime,
			OWLOntology ontology, OWLDataFactory factory, List<Date> axis_x_values, List<Double> axis_y_values, 
			XYChart chart, SwingWrapper<XYChart> sw, int start) {
		super(iri);
		this.sleepTime = sleepTime;
		this.baseUri = baseUri;
		this.prop = prop;
		this.ontology = ontology;
		this.factory = factory;
		this.axis_x_values = axis_x_values;
		this.axis_y_values = axis_y_values;
		this.chart = chart;
		this.sw = sw;
		this.start = start;
	}

	public void run() {

		try {
			SimpleDateFormat date_formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");  
			CSVReader reader = new CSVReader(new FileReader("example.csv"));
			List<String[]> allRows = reader.readAll();
			Double value = 0.0;
			int observationIndex = 0;
			int timeIndex = 0;
			List<Date> data_x = new ArrayList<>();
			List<Double> data_y = new ArrayList<>();
			int i = 1;
			for (String[] row : allRows) {
				if (i > start) {
					if (!row[3].isEmpty()) {
						value = Double.parseDouble(row[3]);
					} else {
						value = 0.0;
					}
					String date_str = row[1].replace("/", "-") + " " + row[2];
					Date date = date_formatter.parse(date_str); 
					data_x.add(date);
					data_y.add(value);
					//System.out.println("Reading Values from file");
				}
				i++;
			}
			
			//String ontologyURI = "http://www.semanticweb.org/mathieu/ontologies/2020/11/sicopad"; 
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
					value =  data_y.get(observationIndex); //axis_y_values.get(observationIndex);   
					Date date = data_x.get(timeIndex); //axis_x_values.get(timeIndex);
					
					axis_y_values.add(value);
					axis_x_values.add(date);
					chart.updateXYSeries(prop, axis_x_values, axis_y_values, null);
					sw.repaintChart();

					RdfQuadruple q = new RdfQuadruple(baseUri + "S_" + prop, baseUri + "madeObservation", baseUri + "S_" + prop + "-Obs-" + 	observationIndex, System.currentTimeMillis());
					//System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "S_" + prop + "-Obs-" + observationIndex, baseUri + "observedProperty", baseUri + prop, System.currentTimeMillis());
					//System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "S_" + prop + "-Obs-" + observationIndex, baseUri + "hasSimpleResult", value + "^^http://www.w3.org/2001/XMLSchema#double", System.currentTimeMillis());
					System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "S_" + prop + "-Obs-" + observationIndex, baseUri + "hasTime", baseUri + "t-obs-S_" + prop + "-"+ timeIndex, System.currentTimeMillis());
					//System.out.println(q);
					this.put(q);
					q = new RdfQuadruple(baseUri + "t-obs-S_" + prop + "-"+ timeIndex, baseUri + "inXSDDateTime", date + "^^http://www.w3.org/2001/XMLSchema#dateTime", System.currentTimeMillis());
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

					OWLDataPropertyAssertionAxiom obshassimpleresult = factory.getOWLDataPropertyAssertionAxiom(hasSimpleResult, obs, value + "^^http://www.w3.org/2001/XMLSchema#double");
					ontology.add(obshassimpleresult);

					try {
						ontology.saveOntology();
					} catch (OWLOntologyStorageException e1) {
						e1.printStackTrace();
					}
				
					TimeUnit.MILLISECONDS.sleep(sleepTime);
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
}