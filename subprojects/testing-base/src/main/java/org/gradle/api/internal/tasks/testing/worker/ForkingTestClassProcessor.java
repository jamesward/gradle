/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.internal.tasks.testing.worker;

import com.google.common.collect.Lists;
import org.gradle.api.Action;
import org.gradle.api.internal.classpath.ModuleRegistry;
import org.gradle.api.internal.tasks.testing.TestClassProcessor;
import org.gradle.api.internal.tasks.testing.TestClassRunInfo;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory;
import org.gradle.messaging.remote.ObjectConnection;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.internal.worker.WorkerProcess;
import org.gradle.process.internal.worker.WorkerProcessBuilder;
import org.gradle.process.internal.worker.WorkerProcessFactory;
import org.gradle.util.CollectionUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

public class ForkingTestClassProcessor implements TestClassProcessor {
    private final WorkerProcessFactory workerFactory;
    private final WorkerTestClassProcessorFactory processorFactory;
    private final JavaForkOptions options;
    private final Iterable<File> classPath;
    private final Action<WorkerProcessBuilder> buildConfigAction;
    private final ModuleRegistry moduleRegistry;
    private RemoteTestClassProcessor remoteProcessor;
    private WorkerProcess workerProcess;
    private TestResultProcessor resultProcessor;

    public ForkingTestClassProcessor(WorkerProcessFactory workerFactory, WorkerTestClassProcessorFactory processorFactory, JavaForkOptions options, Iterable<File> classPath, Action<WorkerProcessBuilder> buildConfigAction, ModuleRegistry moduleRegistry) {
        this.workerFactory = workerFactory;
        this.processorFactory = processorFactory;
        this.options = options;
        this.classPath = classPath;
        this.buildConfigAction = buildConfigAction;
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public void startProcessing(TestResultProcessor resultProcessor) {
        this.resultProcessor = resultProcessor;
    }

    @Override
    public void processTestClass(TestClassRunInfo testClass) {
        if (remoteProcessor == null) {
            remoteProcessor = forkProcess();
        }

        remoteProcessor.processTestClass(testClass);
    }

    RemoteTestClassProcessor forkProcess() {
        WorkerProcessBuilder builder = workerFactory.create(new TestWorker(processorFactory));
        builder.setBaseName("Gradle Test Executor");
        builder.setImplementationClasspath(getTestWorkerImplementationClasspath());
        builder.applicationClasspath(classPath);
        options.copyTo(builder.getJavaCommand());
        buildConfigAction.execute(builder);

        workerProcess = builder.build();
        workerProcess.start();

        ObjectConnection connection = workerProcess.getConnection();
        connection.useParameterSerializers(TestEventSerializer.create());
        connection.addIncoming(TestResultProcessor.class, resultProcessor);
        RemoteTestClassProcessor remoteProcessor = connection.addOutgoing(RemoteTestClassProcessor.class);
        connection.connect();
        remoteProcessor.startProcessing();
        return remoteProcessor;
    }

    List<URL> getTestWorkerImplementationClasspath() {
        return Lists.newArrayList(
            CollectionUtils.flattenCollections(URL.class,
                moduleRegistry.getModule("gradle-core").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getModule("gradle-messaging").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getModule("gradle-base-services").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getModule("gradle-cli").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getModule("gradle-native").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getModule("gradle-testing-base").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getModule("gradle-testing-jvm").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("guava-jdk5").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("slf4j-api").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("jul-to-slf4j").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("native-platform").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("kryo").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("commons-lang").getImplementationClasspath().getAsURLs(),
                moduleRegistry.getExternalModule("junit").getImplementationClasspath().getAsURLs()
            )
        );
    }

    @Override
    public void stop() {
        if (remoteProcessor != null) {
            remoteProcessor.stop();
            workerProcess.waitForStop();
        }
    }
}
