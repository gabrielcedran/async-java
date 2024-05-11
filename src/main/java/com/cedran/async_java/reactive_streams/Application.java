package com.cedran.async_java.reactive_streams;

import com.cedran.async_java.raw_handling.BlockingServlet;
import com.cedran.async_java.raw_handling.NonBlockingServlet;
import com.cedran.async_java.raw_handling.SlowDownstreamServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;

public class Application {

    private Server server;

    private Server downstreamServer;

    public void run() throws Exception {

        ServletContextHandler servletHandler = new ServletContextHandler();

        servletHandler.addServlet(ReactiveStreamsServlet.class, "/reactive-streams");
        this.server = startServer(8080, servletHandler);

        ServletContextHandler downstreamServletHandler = new ServletContextHandler();

        downstreamServletHandler.addServlet(SlowDownstreamServlet.class, "/slow-downstream-service");

        this.downstreamServer = startServer(8081, downstreamServletHandler);

        server.join();
    }

    private Server startServer(int port, ServletContextHandler servletHandler) throws Exception {
        var server = new Server(port);
        final ThreadPool.SizedThreadPool threadPool = (ThreadPool.SizedThreadPool) server.getThreadPool();
        threadPool.setMaxThreads(20);
        threadPool.setMinThreads(10);

        server.setHandler(servletHandler);
        server.start();
        server.dumpStdErr();

        return server;
    }

    public void stop() throws Exception
    {
        server.stop();
        downstreamServer.stop();
    }

    public static void main(String[] args) throws Exception {
        var app = new Application();
        app.run();
    }
}
