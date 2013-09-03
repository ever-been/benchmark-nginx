package cz.cuni.mff.d3s.been.nginx;

import java.util.Date;

import cz.cuni.mff.d3s.been.results.Result;

/**
 * @author Kuba Brecka
 */
public class HttperfResult extends Result {

	public static final String RESULT_GROUP = "nginx";

	static class MetaInfo {
		String hostname;
		Date timestamp;
		String taskId;
		String taskContextId;

		// JSON deserializer needs this constructor
		public MetaInfo() {}
	}

	MetaInfo metaInformation;

	static class Parameters {
		int revision;
		int numberOfClients;
		int numberOfRuns;
		int numberOfConnections;
		int requestsPerConnection;
		int sendBuffer;
		int recvBuffer;

		// JSON deserializer needs this constructor
		public Parameters() {}
	}

	Parameters parameters;

	// JSON deserializer needs this constructor
	public HttperfResult() {
	}

	public HttperfResult(NginxClientTask task) {
		this.created = new Date().getTime();
		this.benchmarkId = task.getBenchmarkId();
		this.contextId = task.getContextId();
		this.taskId = task.getId();

		metaInformation = new MetaInfo();
		metaInformation.timestamp = new Date();
		metaInformation.hostname = MyUtils.getHostname();
		metaInformation.taskId = task.getId();
		metaInformation.taskContextId = task.getContextId();

		parameters = new Parameters();
		parameters.revision = Integer.parseInt(task.getTaskProperty("revision"));
		parameters.numberOfClients = Integer.parseInt(task.getTaskProperty("numberOfClients"));
		parameters.numberOfRuns = Integer.parseInt(task.getTaskProperty("numberOfRuns"));
		parameters.numberOfConnections = Integer.parseInt(task.getTaskProperty("numberOfConnections"));
		parameters.requestsPerConnection = Integer.parseInt(task.getTaskProperty("requestsPerConnection"));
		parameters.sendBuffer = Integer.parseInt(task.getTaskProperty("sendBuffer"));
		parameters.recvBuffer = Integer.parseInt(task.getTaskProperty("recvBuffer"));
	}

	int connections;
	int requests;
	int replies;
	double testDuration;

	double connectionRate;
	double connectionTimeMin;
	double connectionTimeAvg;
	double connectionTimeMax;
	double connectionTimeMedian;
	double connectionTimeStdDev;
	double connectionTimeConnect;

	double requestRate;
	int requestSize;
	int replySizeTotal;

	int numberOf1xx;
	int numberOf2xx;
	int numberOf3xx;
	int numberOf4xx;
	int numberOf5xx;

	double cpuUser;
	double cpuSystem;

	double cpuPercentUser;
	double cpuPercentSystem;
	double cpuPercentTotal;

	double netIO;
}
