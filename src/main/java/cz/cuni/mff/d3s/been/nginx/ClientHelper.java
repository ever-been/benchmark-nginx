package cz.cuni.mff.d3s.been.nginx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHelper {

	private final NginxClientTask nginxClientTask;

	public ClientHelper(NginxClientTask nginxClientTask) {
		this.nginxClientTask = nginxClientTask;
	}

	void downloadClientScript() {
		if (nginxClientTask.fakeRun) return;

		MyUtils.exec(".", "wget", new String[]{"http://httperf.googlecode.com/files/httperf-0.9.0.tar.gz"});
		MyUtils.exec(".", "tar", new String[]{"xzvf", "httperf-0.9.0.tar.gz"});
		MyUtils.exec("./httperf-0.9.0", "./configure", new String[]{});
		MyUtils.exec("./httperf-0.9.0", "make", new String[]{});
	}

	HttperfResult runClientScript(String address) {
		if (nginxClientTask.fakeRun) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ignored) {
			}

			HttperfResult fakeResult = new HttperfResult(this.nginxClientTask);
			fakeResult.connectionTimeAvg = 100 + Math.random() * 300;
			return fakeResult;
		}

		String[] splitted = address.split(":");
		String hostname = splitted[0];
		int port = Integer.parseInt(splitted[1]);

		String output = MyUtils.exec("./httperf-0.9.0", "src/httperf", new String[]{"--client=0/1",
				"--server=" + hostname, "--port=" + port, "--uri=/", "--send-buffer=" + nginxClientTask.getTaskProperty("sendBuffer"),
				"--recv-buffer=" + nginxClientTask.getTaskProperty("recvBuffer"), "--num-conns=" + nginxClientTask.getTaskProperty("numberOfConnections"),
				"--num-calls=" + nginxClientTask.getTaskProperty("requestsPerConnection")});

		HttperfResult result = parseOutput(output);
		return result;
	}

	HttperfResult parseOutput(String output) {
		HttperfResult result = new HttperfResult(nginxClientTask);

		Matcher m = Pattern.compile(
				"Total: connections ([0-9]+) requests ([0-9]+) replies ([0-9]+) test-duration ([0-9.]+) s").matcher(output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.connections = Integer.parseInt(m.group(1));
		result.requests = Integer.parseInt(m.group(2));
		result.replies = Integer.parseInt(m.group(3));
		result.testDuration = Double.parseDouble(m.group(4));

		m = Pattern.compile("Connection rate: ([0-9.]+) conn/s").matcher(output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.connectionRate = Double.parseDouble(m.group(1));

		m = Pattern.compile(
				"Connection time \\[ms\\]: min ([0-9.]+) avg ([0-9.]+) max ([0-9.]+) median ([0-9.]+) stddev ([0-9.]+)").matcher(
				output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.connectionTimeMin = Double.parseDouble(m.group(1));
		result.connectionTimeAvg = Double.parseDouble(m.group(2));
		result.connectionTimeMax = Double.parseDouble(m.group(3));
		result.connectionTimeMedian = Double.parseDouble(m.group(4));
		result.connectionTimeStdDev = Double.parseDouble(m.group(5));

		m = Pattern.compile("Connection time \\[ms\\]: connect ([0-9.]+)").matcher(output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.connectionTimeConnect = Double.parseDouble(m.group(1));

		m = Pattern.compile("Request rate: ([0-9.]+) req/s").matcher(output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.requestRate = Double.parseDouble(m.group(1));

		m = Pattern.compile("Request size \\[B\\]: ([0-9.]+)").matcher(output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.requestSize = (int) Double.parseDouble(m.group(1));

		m = Pattern.compile("Reply size \\[B\\]: header ([0-9.]+) content ([0-9.]+) footer ([0-9.]+) \\(total ([0-9.]+)\\)").matcher(
				output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.replySizeTotal = (int) Double.parseDouble(m.group(4));

		m = Pattern.compile("Reply status: 1xx=([0-9]+) 2xx=([0-9]+) 3xx=([0-9]+) 4xx=([0-9]+) 5xx=([0-9]+)").matcher(
				output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.numberOf1xx = Integer.parseInt(m.group(1));
		result.numberOf2xx = Integer.parseInt(m.group(2));
		result.numberOf3xx = Integer.parseInt(m.group(3));
		result.numberOf4xx = Integer.parseInt(m.group(4));
		result.numberOf5xx = Integer.parseInt(m.group(5));

		m = Pattern.compile(
				"CPU time \\[s\\]: user ([0-9.]+) system ([0-9.]+) \\(user ([0-9.]+)% system ([0-9.]+)% total ([0-9.]+)%\\)").matcher(
				output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.cpuUser = Double.parseDouble(m.group(1));
		result.cpuSystem = Double.parseDouble(m.group(2));
		result.cpuPercentUser = Double.parseDouble(m.group(3));
		result.cpuPercentSystem = Double.parseDouble(m.group(4));
		result.cpuPercentTotal = Double.parseDouble(m.group(5));

		m = Pattern.compile("Net I/O: ([0-9.]+) KB/s").matcher(output);
		if (!m.find())
			throw new RuntimeException("Cannot parse result.");
		result.netIO = Double.parseDouble(m.group(1));

		return result;
	}
}