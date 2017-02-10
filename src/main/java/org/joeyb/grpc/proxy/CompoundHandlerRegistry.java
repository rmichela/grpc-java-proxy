/*
 * Copyright, 1999-2016, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package org.joeyb.grpc.proxy;

import io.grpc.HandlerRegistry;
import io.grpc.ServerMethodDefinition;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Created by rmichela on 2/9/17.
 */
public class CompoundHandlerRegistry extends HandlerRegistry {
    private final List<HandlerRegistry> registries;
    private final BiConsumer<String, String> preLookup;

    public CompoundHandlerRegistry(List<HandlerRegistry> registries) {
        this(registries, (methodName, authority) -> {});
    }

    public CompoundHandlerRegistry(List<HandlerRegistry> registries, BiConsumer<String, String> report) {
        this.registries = registries;
        this.preLookup = report;
    }

    @Nullable
    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
        preLookup.accept(methodName, authority);
        for(HandlerRegistry registry : registries) {
            ServerMethodDefinition<?, ?> method = registry.lookupMethod(methodName, authority);
            if (method != null) {
                return method;
            }
        }
        return null;
    }
}
