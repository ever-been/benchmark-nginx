package cz.cuni.mff.d3s.been.nginx;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.persistence.Query;
import cz.cuni.mff.d3s.been.persistence.ResultQueryBuilder;
import cz.cuni.mff.d3s.been.taskapi.ResultFacade;

/**
 * @author Kuba Brecka
 */
public class PlotGenerator {

	private static final Logger log = LoggerFactory.getLogger(PlotGenerator.class);

	public static void main(String[] args) throws IOException {
		PlotGenerator a = new PlotGenerator();
		a.generateRandomData();
		a.performHardcoreStatisticCalculations();
		BufferedImage image = a.generateChart();
		ImageIO.write(image, "png", new File("nginx-out.png"));
	}

	Integer[] revisions;
	Double[][] data;
	Double[] means;
	Double[] sds;

	public void generateRandomData() {
		revisions = new Integer[100];
		for (int i = 0; i < 100; i++)
			revisions[i] = i + 200;

		data = new Double[100][10];
		for (int rev = 0; rev < 100; rev++) {
			for (int i = 0; i < 10; i++) {
				data[rev][i] = 100 + Math.random() * 300;
			}
		}
	}

	public void retrieveData(ResultFacade resultFacade, String benchmarkId) {
		log.info("Retrieving data for benchmark {}", benchmarkId);

		// creates a query that will retrieve data from our results group and with only the specified benchmark ID
		Query query = new ResultQueryBuilder().on(HttperfResult.RESULT_GROUP).with("benchmarkId", benchmarkId).fetch();

		Collection<HttperfResult> results;
		try {
			// let's perform the query
			results = resultFacade.query(query, HttperfResult.class);
			if (results.size() == 0)
				throw new RuntimeException("No results found.");
		} catch (DAOException e) {
			log.error("Cannot retrieve result.", e);
			throw new RuntimeException(e);
		}

		// group by revision
		HashMap<Integer, ArrayList<Double>> map = new HashMap<>();
		for (HttperfResult result : results) {
			if (!map.containsKey(result.parameters.revision))
				map.put(result.parameters.revision, new ArrayList<Double>());

			map.get(result.parameters.revision).add(result.connectionTimeAvg);
		}

		// sort the keys
		SortedSet<Integer> keys = new TreeSet<>(map.keySet());
		revisions = new Integer[keys.size()];
		keys.toArray(revisions);
		data = new Double[keys.size()][];
		for (int i = 0; i < revisions.length; i++) {
			ArrayList<Double> dataForRevision = map.get(revisions[i]);
			data[i] = new Double[dataForRevision.size()];
			dataForRevision.toArray(data[i]);
		}
	}

	public void performHardcoreStatisticCalculations() {
		means = new Double[data.length];
		sds = new Double[data.length];

		for (int rev = 0; rev < data.length; rev++) {
			Double[] dataForRevision = data[rev];
			int numberOfSamples = dataForRevision.length;

			double sum = 0.0;
			for (int i = 0; i < numberOfSamples; i++) {
				sum += dataForRevision[i];
			}
			double mean = sum / numberOfSamples;

			double sqsum = 0.0;
			for (int i = 0; i < numberOfSamples; i++) {
				sqsum = (dataForRevision[i] - mean) * (dataForRevision[i] - mean);
			}

			means[rev] = mean;
			sds[rev] = Math.sqrt(sqsum / (numberOfSamples - 1));
		}
	}

	public BufferedImage generateChart() {
		// create dataset
		YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		YIntervalSeries s1 = new YIntervalSeries("Connection time (ms)");
		for (int i = 0; i < means.length; i++) {
			s1.add(revisions[i], means[i], means[i] - 1.96 * sds[i], means[i] + 1.96 * sds[i]);
		}
		dataset.addSeries(s1);

		// create chart
		NumberAxis xAxis = new NumberAxis("Revision number");
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis("Connection time (ms)");
		XYErrorRenderer renderer = new XYErrorRenderer();
		renderer.setBaseLinesVisible(true);
		renderer.setSeriesStroke(0, new BasicStroke(3.0f));
		renderer.setBaseShapesVisible(false);
		renderer.setErrorPaint(Color.blue);
		renderer.setErrorStroke(new BasicStroke(1.0f));
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		JFreeChart chart = new JFreeChart("Average connection time over revision number", plot);
		chart.setBackgroundPaint(Color.white);

		// create output image
		BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setBackground(Color.white);
		graphics.clearRect(0, 0, 800, 600);

		// render
		chart.draw(graphics, new Rectangle2D.Double(0, 0, 800, 600));

		return image;
	}
}
