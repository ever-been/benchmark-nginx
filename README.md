EverBEEN Nginx Sample Benchmark
===============================

This project contains a sample benchmark implemented using the EverBEEN framework. It performs a benchmark test using multiple instances of [Httperf](https://code.google.com/p/httperf/) measuring tool against an [Nginx](http://wiki.nginx.org/) HTTP server.

It requires a working toolchain (make, gcc, etc.) and a unix system.

Benchmark structure
-------------------

The benchmark consists of these BEEN tasks/benchmarks:

* **Client task** – a *task* that performs the client part of the benchmark. Implemented in the `NginxClientTask` class. The client will download the Httperf source code, compile it and it waits for the server to be running. Then it performs the measuring and ends.

* **Server task** - a *task* which runs the HTTP server, implemented in the `NginxServerTask` class. It will download a specific revision of Nginx, build it and run it on a random port. It then announces the clients the IP address and port of the server and waits for the clients to exit.

* **Benchmark generator task** - generates task contexts, one for each revision number within the specified range. Implemented in the `NginxBenchmark` class. Each generated context consists of one server task and two client tasks.

* **Evaluator** - a stand-alone task that will take results from one benchmark and plots a graph with the resulting data. Implemented in the `NginxEvaluator` class.

Besides these, there are a few important resource files:

* `NginxBenchmark.td.xml` – a XML task descriptor describing the whole benchmark and its parameters.
* `NginxEvaluator.td.xml` - a XML task descriptor for the stand-alone evaluator task.
* `Nginx.tcd.xml` - a task context descriptor that is used by the generator as a template context.

The benchmark must be packaged into a BPK file using BPK Maven plugin, which must be installed into the local Maven repository in advance. In the `pom.xml` Maven file, the project uses BEEN task and benchmark API and it also configures the BPK plugin to generate the correct package. Namely, it's important that it *publishes* the two task descriptor, so that these can be run from the web interface.
