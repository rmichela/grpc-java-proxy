package org.joeyb.grpc.proxy;

import io.grpc.HandlerRegistry;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TODO: This will probably end up being a Scone app.
 */
public class ProxyServer {

    /**
     * Runs the proxy server. The expected arguments are the port number that the proxy server will run on and the local
     * server's host.
     *
     * @param args command-line args
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("ERROR: You must give the proxy port and optionally one or more routing instructions");
            System.exit(1);
            return;
        }

        final int port;

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Failed to parse given port number (" + args[0] + ").");
            System.exit(1);
            return;
        }

        System.out.println("PORT = " + port);


        Map<String, String> routingTable = Arrays.stream(args)
                .skip(1)
                .map(s -> s.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));

        routingTable.forEach((s, a) -> System.out.println(s + " -> " + a));


        PerAuthorityCachingProxyChannelManager proxyChannelManager = new PerAuthorityCachingProxyChannelManager(
                (h, c) -> {
                    System.out.println("Allocating channel for " + h);
                    // TODO: Don't use plaintext
                    c.usePlaintext(true);
                });

        List<HandlerRegistry> registries = routingTable
                .keySet()
                .stream()
                .map(service -> new ReverseProxyHandlerRegistry(service, routingTable.get(service),
                        proxyChannelManager, (methodName, authority) -> {
                    System.out.println("Reverse proxy: " + methodName + " -> " + authority);
                }))
                .collect(Collectors.toList());

        registries.add(new ForwardProxyHandlerRegistry(proxyChannelManager, (methodName, authority) -> {
            System.out.println("Forward proxy: " + methodName + " -> " + authority);
        }));

        HandlerRegistry compoundRegistry = new CompoundHandlerRegistry(registries, (methodName, authority) -> {
            System.out.println("Resolving: " + methodName + " -> " + authority);
        });

        Server server = NettyServerBuilder.forPort(port)
                .fallbackHandlerRegistry(compoundRegistry)
                .build();

        server.start();

        System.out.println("Press <enter> to terminate...");
        System.in.read();
        proxyChannelManager.close();
        server.shutdown();

    }
}
