package csparql.ind;

import java.awt.Color;
import java.io.File;

import org.apache.http.message.BasicStatusLine;
import org.apache.log4j.PropertyConfigurator;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.Styler.LegendPosition;
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

			String fileOntology = "ContextOntology-COInd4.owl";

			// put static model
			engine.putStaticNamedModel("http://streamreasoning.org/ContextOntology-COInd4",CsparqlUtils.serializeRDFFile(fileOntology));
			
			/*
			String queryS6 = "REGISTER QUERY S6detection AS "
				+ "PREFIX : <http://semanticweb.org/STEaMINg/ContextOntology-COInd4#> "
				+ "PREFIX sosa: <http://www.w3.org/ns/sosa/> "
				+ "SELECT ?m ?pl "
				+ "FROM STREAM <Stream_S_temp> [RANGE 60s STEP 60s] "
				+ "FROM <http://streamreasoning.org/ContextOntology-COInd4> "
				+ "WHERE { "
				+ "{ ?m         		:isPartOf        ?pl ."
				+ "  ?m         		sosa:hosts       sosa:S_Temperature ." 
				+	"  :S_Temperature :madeObservation ?o1 ."
				+ "  ?o1        		:hasSimpleResult ?v1 ."
				+ " FILTER ( "
				+ "		?v1 > 30.0 "
				+ " ) . }"
				+ " } ";
			*/
			
			String queryS6 = "REGISTER QUERY S6detection AS "
				+ "PREFIX : <http://semanticweb.org/STEaMINg/ContextOntology-COInd4#> "
				+ "PREFIX sosa: <http://www.w3.org/ns/sosa/> "
				+ "SELECT ?s (AVG(?v1) as ?avgTemp) "
				+ "FROM STREAM <Stream_S_temp> [RANGE 10s STEP 10s] "
				+ "FROM <http://streamreasoning.org/ContextOntology-COInd4> "
				+ "WHERE { "
				+	"  ?s  :madeObservation ?o1 ."
				+ "  ?o1 :hasSimpleResult ?v1 ."
				+ " } "
				+ " GROUP BY ?s "
				+ " HAVING(AVG(?v1) > 30.0) ";

			double[] data_temp = new double[9000];
			
			double phase = 0;
			
			double[][] initdata_temp = getInitData(phase,data_temp);

			final XYChart chart = QuickChart.getChart("Capsule n°2,21.00.06.F9", "Time", "(°C)", "Temperature", initdata_temp[0], initdata_temp[1]);
			chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
			//XYSeries series_TG_temp = chart.addSeries("TG_temp", initdata_TG_temp[0],initdata_TG_temp[1]);
			//series_TG_temp.setMarker(SeriesMarkers.NONE);
			chart.getStyler().setYAxisMin(25.0);
			chart.getStyler().setYAxisMax(40.0);
			chart.getStyler().setCursorEnabled(true);
			chart.getStyler().setCursorColor(Color.GREEN);
			chart.getStyler().setCursorFontColor(Color.ORANGE);
			chart.getStyler().setCursorBackgroundColor(Color.BLUE);
			final SwingWrapper<XYChart> sw = new SwingWrapper<XYChart>(chart);
			sw.displayChart();

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLDataFactory factory = manager.getOWLDataFactory();
			String ontologyURI = "http://semanticweb.org/STEaMINg/ContextOntology-COInd4";
			String ns = ontologyURI + "#";
			final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(fileOntology));
			
			SensorsStreamer Stream_Temp = new SensorsStreamer("Stream_S_temp",ns,"Temperature",2,ontology,factory,data_temp,chart,sw);

			//Register new streams in the engine
			engine.registerStream(Stream_Temp);

			Thread Stream_C_Wtemp_Thread = new Thread(Stream_Temp);

			//Register new query in the engine
			CsparqlQueryResultProxy c_S6 = engine.registerQuery(queryS6, false);
			
			//Attach a result consumer to the query result proxy to print the results on the console
			c_S6.addObserver(new ConsoleFormatter("S6",ns,ontology,factory));	

			//Start streaming data
			Stream_C_Wtemp_Thread.start();

		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}
	
	private static double[][] getInitData(double phase, double[] values) {
		double[] xData = new double[1];
		double[] yData = new double[1];
		for (int i = 0; i < xData.length; i++) {
			xData[i] = phase+i;
			int index=(int) (phase+i);
			yData[i] = values[index];
		}
		return new double[][] { xData, yData };
	}
}