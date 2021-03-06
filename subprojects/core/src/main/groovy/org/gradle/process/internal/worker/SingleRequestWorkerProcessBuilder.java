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

package org.gradle.process.internal.worker;

/**
 * Configures and builds single request workers. A single request worker runs each request in a separate forked worker process.
 *
 * <p>This builder produces instances of type {@link T}. Each method call on the returned object will start a worker process, run the method in the worker, stop the worker process and return the result.
 *
 * <p>The worker process runs the request using an instance of the implementation type specified as a parameter to {@link WorkerProcessFactory#create(Class, Class)}.</p>
 */
public interface SingleRequestWorkerProcessBuilder<T> extends WorkerProcessSettings {
    /**
     * Creates the worker. The returned value can be used to run multiple requests, each will run in a separate worker process.
     */
    T build();
}
