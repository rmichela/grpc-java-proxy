/*
 * Copyright, 1999-2016, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package org.joeyb.grpc.proxy;

import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;

import java.util.function.BiConsumer;

/**
 * Created by rmichela on 2/9/17.
 */
public class ReverseProxyHandlerRegistry extends CachingHandlerRegistry {
    public ReverseProxyHandlerRegistry(String service, String knownDestination, ProxyChannelManager proxyChannelManager) {
        this(service, knownDestination, proxyChannelManager, (methodName, authority) -> {});
    }

    public ReverseProxyHandlerRegistry(String service, String knownDestination,
        ProxyChannelManager proxyChannelManager, BiConsumer<String, String> report) {
        super((methodName, authority) -> {
            // Does this registry apply to the incoming service request?
            if (methodName.startsWith(service + "/")) {
                report.accept(methodName, knownDestination);
                return ServerMethodDefinition.create(
                        MethodDescriptor.create(
                                MethodDescriptor.MethodType.UNKNOWN,
                                methodName,
                                InputStreamMarshaller.instance,
                                InputStreamMarshaller.instance
                        ),
                        new ProxyMethodServerCallHandler(authority, methodName,
                                // Override the provided authority with the known destination
                                proxyChannelManager.getChannel(knownDestination)));
            }
            return null;
        });
    }
}
