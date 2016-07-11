/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.classloading.isolation.maven.MavenArtifactToClassPathURLResolver;

import java.io.File;
import java.util.function.Predicate;

/**
 * Just extends {@link ClassPathClassifierContext} to append specific data needed to classify the context internally in {@link MuleClassPathClassifier}.
 *
 * @since 4.0
 */
public class ExtendedClassPathClassifierContext
{
    private final ClassPathClassifierContext context;
    private final Predicate<MavenArtifact> exclusion;
    private final MavenArtifact compileArtifact;
    private final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver;
    private final File targetTestClassesFolder;

    /**
     * Creates a {@link ExtendedClassPathClassifierContext} used internally in {@link MuleClassPathClassifier} to do the classification.
     *
     * @param context the initial {@link ClassPathClassifierContext} context passed by {@link org.mule.functional.junit4.runners.ArtifactClassLoaderTestRunner}
     * @param exclusion the {@link Predicate<MavenArtifact>} built based on the exclusion patterns defined
     * @param compileArtifact the artifactId for the current maven artifact where the test belongs to
     * @param artifactToClassPathURLResolver resolves the {@link java.net.URL} from the class path for a given artifactId
     * @param targetTestClassesFolder the target/test-classes folder of the current artifact being tested
     */
    public ExtendedClassPathClassifierContext(final ClassPathClassifierContext context, final Predicate<MavenArtifact> exclusion, final MavenArtifact compileArtifact, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final File targetTestClassesFolder)
    {
        this.context = context;
        this.exclusion = exclusion;
        this.compileArtifact = compileArtifact;
        this.artifactToClassPathURLResolver = artifactToClassPathURLResolver;
        this.targetTestClassesFolder = targetTestClassesFolder;
    }

    public MavenArtifactToClassPathURLResolver getArtifactToClassPathURLResolver()
    {
        return artifactToClassPathURLResolver;
    }

    public MavenArtifact getCompileArtifact()
    {
        return compileArtifact;
    }

    public ClassPathClassifierContext getContext()
    {
        return context;
    }

    public Predicate<MavenArtifact> getExclusion()
    {
        return exclusion;
    }

    public File getTargetTestClassesFolder()
    {
        return targetTestClassesFolder;
    }
}
