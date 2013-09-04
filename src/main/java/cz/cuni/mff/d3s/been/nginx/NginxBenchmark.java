package cz.cuni.mff.d3s.been.nginx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.benchmarkapi.Benchmark;
import cz.cuni.mff.d3s.been.benchmarkapi.BenchmarkException;
import cz.cuni.mff.d3s.been.benchmarkapi.ContextBuilder;
import cz.cuni.mff.d3s.been.core.task.TaskContextDescriptor;
import cz.cuni.mff.d3s.been.core.task.TaskContextState;

/**
 * Benchmark generator task for BEEN Nginx Sample Benchmark. This implements a
 * very simple generator, that will create contexts for all revisions within the
 * user-specified range.
 * 
 * @author Kuba Brecka
 */
public class NginxBenchmark extends Benchmark {

	/**
	 * Slf4j logger, all logging inside BEEN is done by using standard Slf4j and
	 * by using LoggerFactory your logs will be automatically persisted and shown
	 * in the web interface.
	 */
	private static final Logger log = LoggerFactory.getLogger(NginxBenchmark.class);

	/**
	 * The main method that needs to be implemented and that generates task
	 * contexts. This method is called automatically by BEEN.
	 * 
	 * For this benchmark we simply store a value `currentRevision` and increase
	 * it every time this method is called. Then we create a task context using
	 * the {@link ContextBuilder} class and set its property to the current
	 * revision number.
	 * 
	 * @return a newly generated task context or null when the benchmark should
	 *         end
	 * @throws BenchmarkException
	 *           when any error occurs
	 */
	@Override
	public TaskContextDescriptor generateTaskContext() throws BenchmarkException {

		int fromRevision = Integer.parseInt(this.getTaskProperty("fromRevision"));
		int toRevision = Integer.parseInt(this.getTaskProperty("toRevision"));
		boolean fakeRun = Boolean.parseBoolean(this.getTaskProperty("fakeRun"));

		// for storing any benchmark state we use `storageGet` and `storageSet`
		// to ensure the state is persisted even when the generator task is interrupted
		int currentRevision = Integer.parseInt(this.storageGet("currentRevision", Integer.toString(fromRevision)));

		// we'll return null if we should end the benchmark
		if (currentRevision > toRevision)
			return null;

		log.info("Generating context for revision {}", currentRevision);

		// let's create a new task context from a template from a resource file with the specified name
		ContextBuilder contextBuilder = ContextBuilder.createFromResource(NginxBenchmark.class, "Nginx.tcd.xml");

		// let's assign these properties to the newly created task context
		contextBuilder.setProperty("revision", Integer.toString(currentRevision));
		contextBuilder.setProperty("fakeRun", Boolean.toString(fakeRun));

		// generate the context
		TaskContextDescriptor taskContextDescriptor = contextBuilder.build();

		currentRevision++;

		// stores the `currentRevision` value to the benchmark persistent storage
		this.storageSet("currentRevision", Integer.toString(currentRevision));

		return taskContextDescriptor;
	}

	/**
	 * This method can be used for notification about resubmits (when the
	 * generator task is interrupted), for this sample benchmark we don't do
	 * anything when a resubmit occurs.
	 */
	@Override
	public void onResubmit() {

	}

	/**
	 * This method is a notification about a finished task context. For this
	 * sample benchmark we don't do anything when a task context finishes.
	 * 
	 * @param taskContextId
	 *          ID of the currently finished context
	 * @param taskContextState
	 *          state with which the context finished
	 */
	@Override
	public void onTaskContextFinished(String taskContextId, TaskContextState taskContextState) {

	}

}
