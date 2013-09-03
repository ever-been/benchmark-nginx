package cz.cuni.mff.d3s.been.nginx;

import cz.cuni.mff.d3s.been.evaluators.EvaluatorResult;
import cz.cuni.mff.d3s.been.taskapi.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Kuba Brecka
 */
public class NginxEvaluator extends Evaluator {

	private static final Logger log = LoggerFactory.getLogger(NginxEvaluator.class);

	@Override
	public EvaluatorResult evaluate() {
		String benchmarkId = this.getTaskProperty("benchmarkId");

		PlotGenerator plotGenerator = new PlotGenerator();
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
