/*
 * Copyright, 1999-2016, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package org.joeyb.grpc.proxy;

import com.google.common.base.Objects;
import io.grpc.HandlerRegistry;
import io.grpc.ServerMethodDefinition;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * Created by rmichela on 2/6/17.
 */
public class CachingHandlerRegistry extends HandlerRegistry {
    private final ConcurrentMap<Key, ServerMethodDefinition<?,?>> services = new ConcurrentHashMap<>();

    private final BiFunction<String, String, ServerMethodDefinition<?,?>> notFoundSupplier;

    public CachingHandlerRegistry(BiFunction<String, String, ServerMethodDefinition<?,?>> notFoundSupplier) {
        this.notFoundSupplier = notFoundSupplier;
    }

    @Nullable
    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
        return services.computeIfAbsent(new Key(methodName, authority), k -> notFoundSupplier.apply(k.getMethodName(), k.getAuthority()));
    }


    private static class Key {
        private final String methodName;
        private final String authority;

        public Key(String methodName, String authority) {
            this.methodName = methodName;
            this.authority = authority;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getAuthority() {
            return authority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equal(methodName, key.methodName) &&
                    Objects.equal(authority, key.authority);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(methodName, authority);
        }
    }
}
