package org.joeyb.grpc.proxy;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import com.google.common.annotations.VisibleForTesting;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.ServerCall;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code HostHeaderProxyChannelManager} is an implementation of {@link ProxyChannelManager} that uses a request header
 * in order to route the incoming request to the correct server.
 */
public class HostHeaderProxyChannelManager implements AutoCloseable, ProxyChannelManager {

    @VisibleForTesting
    final Map<String, ManagedChannel> channels;

    @VisibleForTesting
    final BiConsumer<String, ManagedChannelBuilder<?>> remoteChannelBuilderConfigurer;

    public HostHeaderProxyChannelManager(BiConsumer<String, ManagedChannelBuilder<?>> remoteChannelBuilderConfigurer) {

        this.channels = new ConcurrentHashMap<>();

        this.remoteChannelBuilderConfigurer = checkNotNull(
                remoteChannelBuilderConfigurer,
                "remoteChannelBuilderConfigurer");
    }

    /**
     * Shuts down all {@link Channel} instances that are maintained by this {@link ProxyChannelManager}.
     */
    @Override
    public void close() {
        channels.values().forEach(ManagedChannel::shutdown);
        channels.values().forEach(c -> {
            try {
                c.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                c.shutdownNow();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel getChannel(String authority) {
        return channels.computeIfAbsent(authority, h -> createAndConfigureChannelBuilder(h).build());
    }


    /**
     * Returns a {@link ManagedChannelBuilder} for creating a {@link Channel} to a "remote" service (i.e. NOT the
     * service instance that is paired with this proxy instance).
     *
     * @param authority the remote authority name
     */
    protected ManagedChannelBuilder<?> createRemoteChannelBuilder(String authority) {
        return ManagedChannelBuilder.forTarget(authority);
    }

    private ManagedChannelBuilder<?> createAndConfigureChannelBuilder(String authority) {
        ManagedChannelBuilder<?> builder = createRemoteChannelBuilder(authority);
        remoteChannelBuilderConfigurer.accept(authority, builder);
        return builder;
    }
}
