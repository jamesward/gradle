/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.messaging.remote.internal.hub;

import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;
import org.gradle.internal.serialize.SerializerRegistry;

class DefaultMethodArgsSerializer implements MethodArgsSerializer {
    private static final Object[] ZERO_ARGS = new Object[0];
    private final SerializerRegistry serializers;

    public DefaultMethodArgsSerializer(SerializerRegistry serializers) {
        this.serializers = serializers;
    }

    @Override
    public Serializer<Object[]> forTypes(Class<?>[] types) {
        if (types.length == 0) {
            return new EmptyArraySerializer();
        }
        final Serializer<Object>[] serializers = new Serializer[types.length];
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            serializers[i] = (Serializer<Object>) this.serializers.build(type);
        }
        return new ArraySerializer(serializers);
    }

    private static class ArraySerializer implements Serializer<Object[]> {
        private final Serializer<Object>[] serializers;

        ArraySerializer(Serializer<Object>[] serializers) {
            this.serializers = serializers;
        }

        @Override
        public Object[] read(Decoder decoder) throws Exception {
            Object[] result = new Object[serializers.length];
            for (int i = 0; i < serializers.length; i++) {
                result[i] = serializers[i].read(decoder);
            }
            return result;
        }

        @Override
        public void write(Encoder encoder, Object[] value) throws Exception {
            for (int i = 0; i < value.length; i++) {
                serializers[i].write(encoder, value[i]);
            }
        }
    }

    private class EmptyArraySerializer implements Serializer<Object[]> {
        @Override
        public Object[] read(Decoder decoder) {
            return ZERO_ARGS;
        }

        @Override
        public void write(Encoder encoder, Object[] value) {
        }
    }
}
