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
public class ForwardProxyHandlerRegistry extends CachingHandlerRegistry {
    public ForwardProxyHandlerRegistry(ProxyChannelManager proxyChannelManager) {
        this(proxyChannelManager, (methodName, authority) -> {});
    }

    public ForwardProxyHandlerRegistry(ProxyChannelManager proxyChannelManager, BiConsumer<String, String> report) {
        super((methodName, authority) -> {
            report.accept(methodName, authority);
            return ServerMethodDefinition.create(
                    MethodDescriptor.create(
                            MethodDescriptor.MethodType.UNKNOWN,
                            methodName,
                            InputStreamMarshaller.instance,
                            InputStreamMarshaller.instance
                    ),
                    new ProxyMethodServerCallHandler(authority, methodName,
                            proxyChannelManager.getChannel(authority)));
        });
    }
}
