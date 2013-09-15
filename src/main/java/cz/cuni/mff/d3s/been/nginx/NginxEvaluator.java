package cz.cuni.mff.d3s.been.nginx;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import cz.cuni.mff.d3s.been.evaluators.EvaluatorResult;
import cz.cuni.mff.d3s.been.taskapi.Evaluator;

/**
 * The evaluator task from the BEEN Nginx Sample Benchmark. This implements a
 * stand-alone task that will retrieve results from a specific benchmark,
 * calculate some statistics (standard deviations) and plot the results into a
 * chart, which is then returned.
 * 
 * @author Kuba Brecka
 */
public class NginxEvaluator extends Evaluator {

	/**
	 * The main class of this task, it should return a {@link EvaluatorResult}
	 * object which basically represent a file and its data.
	 * 
	 * @return a new evaluation result to store
	 */
	@Override
	public EvaluatorResult evaluate() {

		// use getTaskProperty to retrieve a property of the current task
		String benchmarkId = this.getTaskProperty("benchmarkId");
		if (benchmarkId == null || benchmarkId.trim().isEmpty()) {
            // or try to get benchmark id from which this evaluator has been started
			benchmarkId = getBenchmarkId();
		}

		PlotGenerator plotGenerator = new PlotGenerator();

		// the `results` field provides access to the results, see the implementation
		// of `retrieveData` for more comments
		plotGenerator.retrieveData(results, benchmarkId);

		plotGenerator.performHardcoreStatisticCalculations();
		BufferedImage image = plotGenerator.generateChart();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String d = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());

		EvaluatorResult result = new EvaluatorResult();
		result.setBenchmarkId(benchmarkId);
		result.setMimeType(EvaluatorResult.MIME_TYPE_IMAGE_PNG);
		result.setFilename("nginx-" + d + ".png");
		result.setData(os.toByteArray());
		result.setTimestamp(new Date().getTime());

		return result;
	}

}
