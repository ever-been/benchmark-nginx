package cz.cuni.mff.d3s.been.nginx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Persister;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;

/**
 * Client task from the BEEN Nginx Sample Benchmark. This implements a BEEN task
 * that demonstrates various aspects of BEEN and the API for tasks.
 * 
 * This task will download a benchmark script (Httperf), build it and run it
 * against the server task's HTTP server. Then it will notify the server that
 * it's done and exit.
 * 
 * For the results from the benchmark, we use a {@link HttperfResult} class,
 * which is a simple data holder class and because it is a subclass of
 * {@link cz.cuni.mff.d3s.been.results.Result}, it is serializable and
 * persistable by BEEN.
 * 
 * @author Kuba Brecka
 */
public class NginxClientTask extends Task {

	/**
	 * Slf4j logger, all logging inside BEEN is done by using standard Slf4j and
	 * by using LoggerFactory your logs will be automatically persisted and shown
	 * in the web interface.
	 */
	private static final Logger log = LoggerFactory.getLogger(NginxClientTask.class);

	/**
	 * Should we really perform the benchmark or just a fake run?
	 */
	boolean fakeRun;

	/**
	 * This methods stores a result into the persistence layer.
	 * 
	 * @param result
	 *          the result to store
	 */
	private void storeResult(HttperfResult result) {

		// we can use the `results` field to create a persister object
		try (final Persister rp = results.createResultPersister(HttperfResult.RESULT_GROUP)) {

			// store the result
			rp.persist(result);

			log.info("Result stored.");
		} catch (DAOException e) {
			throw new RuntimeException("Cannot persist result.", e);
		}
	}

	/**
	 * This is the main method of this task, it will download a benchmark script
	 * (Httperf), build it and run it against the server task's HTTP server. Then
	 * it will notify the server that it's done and exit.
	 * 
	 * @param args
	 *          command-line arguments
	 * @throws TaskException
	 *           when any error occurs
	 */
	@Override
	public void run(String[] args) throws TaskException {

		// the CheckpointController is used for synchronization inside the task context
		try (CheckpointController requestor = CheckpointController.create()) {

			ClientHelper helper = new ClientHelper(this);

			// use getTaskProperty to retrieve a property of the current task
			fakeRun = Boolean.parseBoolean(this.getTaskProperty("fakeRun"));

			// logging can be done via the static `log` field
			log.info("Nginx Client Task started.");

			helper.downloadClientScript();

			log.info("DownloadClientScript finished.");

			// this performs a rendez-vous synchronization with the server task, by using a checkpoint
			// and a latch, see the comment in the server task about the details of the implementation
			requestor.checkPointWait("rendezvous-checkpoint");
			requestor.latchCountDown("rendezvous-latch");

			// let's get the server's IP and port from a checkpoint
			String serverAddress = requestor.checkPointWait("server-address");

			log.info("Client got server address: {}", serverAddress);

			int numberOfRuns = Integer.parseInt(this.getTaskProperty("numberOfRuns"));

			for (int i = 0; i < numberOfRuns; i++) {
				log.info("Starting run number {}", i);
				HttperfResult result = helper.runClientScript(serverAddress);
				storeResult(result);
			}

			log.info("Client finished benchmarking.");

			// notify the server that we have finished (by decreasing a latch, the server waits until it
			// is zero)
			requestor.latchCountDown("shutdown-latch");
		} catch (MessagingException e) {
			throw new TaskException("A messaging exception has occurred.", e);
		}
	}

}
