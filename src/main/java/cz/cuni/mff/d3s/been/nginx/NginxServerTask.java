package cz.cuni.mff.d3s.been.nginx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;

/**
 * Server task from the BEEN Nginx Sample Benchmark. This implements a BEEN task
 * that demonstrates various aspects of BEEN and the API for tasks.
 * 
 * This task will perform some synchronization between itself and the clients,
 * then it will start the HTTP server and notify the clients. After all clients
 * are finished, it will shut the server down and exit.
 * 
 * @author Kuba Brecka
 */
public class NginxServerTask extends Task {

	/**
	 * Slf4j logger, all logging inside BEEN is done by using standard Slf4j and
	 * by using LoggerFactory your logs will be automatically persisted and shown
	 * in the web interface.
	 */
	private static final Logger log = LoggerFactory.getLogger(NginxServerTask.class);

	/**
	 * Private helper class (specific to this benchmark).
	 */
	private final ServerHelper serverHelper = new ServerHelper(this);

	/**
	 * Should we really perform the benchmark or just a fake run?
	 */
	boolean fakeRun;

	/**
	 * The hostname where the HTTP server is started.
	 */
	String hostname;

	/**
	 * Port on which the HTTP server is started.
	 */
	int port;

	/**
	 * The main code of the task to perform. This implements the task that will
	 * first perform some synchronization between itself and the clients, then it
	 * will start the HTTP server and notify the clients. After all clients are
	 * finished, it will shut the server down and exit.
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

			// use getTaskProperty to retrieve a property of the current task
			fakeRun = Boolean.parseBoolean(this.getTaskProperty("fakeRun"));

			// logging can be done via the static `log` field
			log.info("Nginx Server Task started.");

			serverHelper.downloadSources();

			log.info("DownloadSources finished successfully.");

			serverHelper.buildSources();

			log.info("BuildSources finished successfully.");

			serverHelper.runServer();

			log.info("RunServer finished successfully.");
			log.info("Waiting for clients...");

			int numberOfClients = Integer.parseInt(this.getTaskProperty("numberOfClients"));

			// now we will use the `requestor` object to perform a rendez-vous synchronization
			// we set a latch to the number of clients and wait for the latch to become zero (every
			// client decreases the latch), but since a client might try to decrease the latch
			// before it is even set, we also use a checkpoint, for which all client waits before
			// they decrease the latch
			requestor.latchSet("rendezvous-latch", numberOfClients);
			requestor.checkPointSet("rendezvous-checkpoint", "ok");
			requestor.latchWait("rendezvous-latch");

			// now let's initialize a latch for the shutdown - every client decreases the latch
			// when it is finished, we will then wait for this latch
			requestor.latchSet("shutdown-latch", numberOfClients);

			// notify the clients about the server's hostname and port via a checkpoint
			requestor.checkPointSet("server-address", hostname + ":" + port);

			log.info("Server is running, waiting for clients to finish...");

			// wait for all clients to finish
			requestor.latchWait("shutdown-latch");

			log.info("All clients finished.");

			serverHelper.shutdownServer();

			log.info("ShutdownServer finished successfully.");
		} catch (MessagingException e) {
			throw new TaskException("A messaging exception has occurred.", e);
		}
	}

}
