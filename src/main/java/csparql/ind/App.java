package csparql.ind;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.opencsv.CSVReader;
import org.apache.log4j.PropertyConfigurator;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.larkc.csparql.common.utils.CsparqlUtils;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import csparql.ind.streamer.SensorsStreamer;

public class App {

	private static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {

		try{

			//Configure log4j logger for the csparql engine
			PropertyConfigurator.configure("log4j_configuration/csparql_readyToGoPack_log4j.properties");

			//Create csparql engine instance
			CsparqlEngineImpl engine = new CsparqlEngineImpl();
			//Initialize the engine instance
			//The initialization creates the static engine (SPARQL) and the stream engine (CEP)
			engine.initialize(true);

			//String fileOntology = "sicopad.owl";
			String fileOntology = "ContextOntology-COInd4.owl";

			// put static model
			engine.putStaticNamedModel("http://streamreasoning.org/sicopad",CsparqlUtils.serializeRDFFile(fileOntology));
			
			List<Date> axis_x_date = new ArrayList<>();
			List<Double> axis_y_temp = new ArrayList<>();
			int start = 4200;
			CSVReader reader = new CSVReader(new FileReader("example.csv"));
			List<String[]> allRows = reader.readAll();
			
			Double value =  Double.parseDouble(allRows.get(start)[3]);
			String date_str = allRows.get(start)[1].replace("/", "-") + " " + allRows.get(start)[2];
			SimpleDateFormat date_formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");  
			Date date = date_formatter.parse(date_str); 
			axis_x_date.add(date);
			axis_y_temp.add(value);

			final XYChart chart = new XYChartBuilder().width(800).height(600).theme(ChartTheme.XChart).title("Capsule n°2,21.00.06.F9").build();
			chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
			chart.setXAxisTitle("Time");
			chart.setYAxisTitle("°C");
			chart.addSeries("Temperature", axis_x_date, axis_y_temp).setMarker(SeriesMarkers.NONE);
			chart.getStyler().setYAxisMin(25.0);
			chart.getStyler().setYAxisMax(40.0);
			chart.getStyler().setXAxisLabelRotation(-90);
			chart.getStyler().setCursorEnabled(true);
			chart.getStyler().setCursorColor(Color.GREEN);
			chart.getStyler().setCursorFontColor(Color.BLACK);
			chart.getStyler().setCursorBackgroundColor(Color.WHITE);			
			chart.getStyler().setZoomEnabled(true);
			chart.getStyler().setZoomResetByDoubleClick(false);
			chart.getStyler().setZoomResetByButton(true);
			chart.getStyler().setZoomSelectionColor(new Color(0,0 , 192, 128));
			final SwingWrapper<XYChart> sw = new SwingWrapper<XYChart>(chart);
			sw.displayChart();

			String queryPrefievre = "REGISTER QUERY Prefievre AS "
				//+ "PREFIX : <http://semanticweb.org/mathieu/ontologies/2020/11/sicopad#> "
				+ "PREFIX : <http://semanticweb.org/STEaMINg/ContextOntology-COInd4#> "
				+ "PREFIX sosa: <http://www.w3.org/ns/sosa/> "
				+ "SELECT ?s (AVG(?v1) as ?avgTemp) "
				+ "FROM STREAM <Stream_S_temp> [RANGE 10s TUMBLING] "
				+ "FROM <http://streamreasoning.org/sicopad> "
				+ "WHERE { "
				+	"  ?s  :madeObservation ?o1 ."
				+ "  ?o1 :hasTime         ?t  ."
				+ "  ?t  :inXSDDateTime   ?ts  ."
				+ "  ?o1 :hasSimpleResult ?v1 ."
				+ " } "
				+ " GROUP BY ?s "
				+ " HAVING((AVG(?v1) > 37.8) && (AVG(?v1) < 38.0)) ";

			String queryPrefievreNuit = "REGISTER QUERY PrefievreNuit AS "
				//+ "PREFIX : <http://semanticweb.org/mathieu/ontologies/2020/11/sicopad#> "
				+ "PREFIX : <http://semanticweb.org/STEaMINg/ContextOntology-COInd4#> "
				+ "PREFIX sosa: <http://www.w3.org/ns/sosa/> "
				//+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT ?s (AVG(?v1) as ?avgTemp) "
				+ "FROM STREAM <Stream_S_temp> [RANGE 10s TUMBLING] "
				+ "FROM <http://streamreasoning.org/sicopad> "
				+ "WHERE { "
				+	"  ?s  :madeObservation ?o1 ."
				+ "  ?o1 :hasTime         ?t  ."
				+ "  ?t  :inXSDDateTime   ?ts  ."
				+ "  ?o1 :hasSimpleResult ?v1 ."
				+ " FILTER ( "
				//+ "       (hours(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime)	= 14 ) " 
				+ "         (hours(?ts)	> 0 ) "
				+ "         && (hours(?ts) < 7 ) " 
				+ " ) "
				+ " } "
				+ " GROUP BY ?s "
				+ " HAVING((AVG(?v1) > 37.6) && (AVG(?v1) < 37.8) "
				+ " ) ";
				

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLDataFactory factory = manager.getOWLDataFactory();
			//String ontologyURI = "http://www.semanticweb.org/mathieu/ontologies/2020/11/sicopad";
			String ontologyURI = "http://semanticweb.org/STEaMINg/ContextOntology-COInd4";
			String ns = ontologyURI + "#";
			final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(fileOntology));
			
			SensorsStreamer Stream_Temp = new SensorsStreamer("Stream_S_temp",ns,"Temperature",1,ontology,factory,axis_x_date,axis_y_temp,chart,sw,start);

			//Register new streams in the engine
			engine.registerStream(Stream_Temp);

			Thread Stream_temp_Thread = new Thread(Stream_Temp);

			//Register new query in the engine
			CsparqlQueryResultProxy c_queryPrefievre = engine.registerQuery(queryPrefievre, false);
			CsparqlQueryResultProxy c_queryPrefievreNuit = engine.registerQuery(queryPrefievreNuit, false);
			
			//Attach a result consumer to the query result proxy to print the results on the console
			c_queryPrefievre.addObserver(new ConsoleFormatterPreFievre("PREFIEVRE",ns,ontology,factory,axis_x_date,chart,sw));	
			c_queryPrefievreNuit.addObserver(new ConsoleFormatterPreFievreNuit("PREFIEVRE-NUIT",ns,ontology,factory,axis_x_date,chart,sw));

			//Start streaming data
			Stream_temp_Thread.start();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}