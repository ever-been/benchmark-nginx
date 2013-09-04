package cz.cuni.mff.d3s.been.nginx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ServerHelper {

	private final NginxServerTask nginxServerTask;

	private File workingDirectory = new File(".");

	Thread runnerThread;

	public ServerHelper(NginxServerTask nginxServerTask) {
		this.nginxServerTask = nginxServerTask;
	}

	void downloadSources() {
		if (nginxServerTask.fakeRun) return;

		String hgPath = nginxServerTask.getTaskProperty("hgPath");
		int currentRevision = Integer.parseInt(nginxServerTask.getTaskProperty("revision"));

		MyUtils.exec(".", "hg", new String[]{"clone", hgPath, "nginx"});
		MyUtils.exec("./nginx", "hg", new String[]{"update", "-r", Integer.toString(currentRevision)});
	}

	void buildSources() {
		if (nginxServerTask.fakeRun) return;

		File sourcesDir = new File(workingDirectory, "nginx");

		MyUtils.exec("./nginx", "auto/configure", new String[]{});
		MyUtils.exec("./nginx", "make", new String[]{});
	}

	void runServer() {
		if (nginxServerTask.fakeRun) return;

		// create logs dir
		try {
			final File sourcesDir = new File(workingDirectory, "nginx");
			Files.createDirectory(new File(sourcesDir, "logs").toPath());
		} catch (IOException e) {
			throw new RuntimeException("Cannot create logs directory.", e);
		}

		nginxServerTask.port = MyUtils.findRandomPort();
		nginxServerTask.hostname = MyUtils.getHostname();

		String configuration = MyUtils.getResourceAsString("nginx.conf");
		configuration = configuration.replace("listen 8000", "listen " + nginxServerTask.port);
		MyUtils.saveFileFromString(new File("./nginx/conf/nginx.conf"), configuration);

		// run the server from a separate thread
		runnerThread = (new Thread() {
			@Override
			public void run() {
				MyUtils.exec("./nginx", "objs/nginx", new String[]{"-p", "."});
			}
		});

		runnerThread.start();
	}

	void shutdownServer() {
		if (nginxServerTask.fakeRun) return;

		MyUtils.exec("./nginx", "objs/nginx", new String[]{"-p", ".", "-s", "stop"});
		try {
			runnerThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("Cannot join runnerThread.", e);
		}
	}
}