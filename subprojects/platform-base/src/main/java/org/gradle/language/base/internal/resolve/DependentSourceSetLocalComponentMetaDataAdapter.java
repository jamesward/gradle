/*
 * Copyright 2015 the original author or authors.
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
package org.gradle.language.base.internal.resolve;

import org.gradle.api.artifacts.component.LibraryBinaryIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.LocalComponentMetaDataAdapter;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.language.base.internal.DependentSourceSetInternal;
import org.gradle.language.base.internal.model.DefaultLibraryLocalComponentMetaData;
import org.gradle.platform.base.DependencySpec;
import org.gradle.platform.base.DependencySpecContainer;

public class DependentSourceSetLocalComponentMetaDataAdapter implements LocalComponentMetaDataAdapter {


    @Override
    public boolean canConvert(Object source) {
        return source instanceof DependentSourceSetResolveContext;
    }

    @Override
    public DefaultLibraryLocalComponentMetaData convert(Object source) {
        DependentSourceSetResolveContext context = (DependentSourceSetResolveContext) source;
        LibraryBinaryIdentifier libraryBinaryIdentifier = context.getComponentId();
        DependentSourceSetInternal sourceSet = context.getSourceSet();
        TaskDependency buildDependencies = context.getSourceSet().getBuildDependencies();
        DefaultLibraryLocalComponentMetaData metaData = DefaultLibraryLocalComponentMetaData.newMetaData(libraryBinaryIdentifier, buildDependencies);
        addDependencies(libraryBinaryIdentifier.getProjectPath(), metaData, sourceSet.getDependencies());
        return metaData;
    }

    private void addDependencies(String defaultProject, DefaultLibraryLocalComponentMetaData metaData, DependencySpecContainer allDependencies) {
        for (DependencySpec dependency : allDependencies.getDependencies()) {
            metaData.addDependency(dependency, defaultProject);
        }
    }
}