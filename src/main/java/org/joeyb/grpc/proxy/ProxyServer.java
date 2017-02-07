package org.joeyb.grpc.proxy;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerMethodDefinition;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

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

//        Map<String, String> routingTable = Arrays.stream(args)
//                                                 .skip(1)
//                                                 .map(s -> s.split("="))
//                                                 .collect(Collectors.toMap(a -> a[0], a -> a[1]));

        System.out.println("PORT = " + port);

        try (HostHeaderProxyChannelManager proxyChannelManager = new HostHeaderProxyChannelManager(
                (h, c) -> {
                    System.out.println("Allocating channel for " + h);
                    // TODO: Don't use plaintext
                    c.usePlaintext(true);
                })) {

            InputStreamMarshaller inputStreamMarshaller = new InputStreamMarshaller();

            HandlerRegistry registry = new CachingHandlerRegistry((methodName, authority) ->
                    ServerMethodDefinition.create(
                            MethodDescriptor.create(
                                    MethodDescriptor.MethodType.UNKNOWN,
                                    methodName,
                                    inputStreamMarshaller,
                                    inputStreamMarshaller
                            ),
                            new ProxyMethodServerCallHandler(authority, methodName, proxyChannelManager)));

            Server server = NettyServerBuilder.forPort(port)
                .fallbackHandlerRegistry(registry)
                .build();

            server.start();

            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
