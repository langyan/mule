/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.runners;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Set;

import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;

/**
 * Runner that does the testing of the class using a different {@link ClassLoader} from the one that launched the test.
 *
 * @since 4.0
 */
public class ArtifactClassloaderTestRunner extends AbstractRunnerDelegate
{

    private final Runner delegate;

    private volatile static Set<URL> classPathURLs;
    private volatile static LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies;

    static {
        classPathURLs = new DefaultClassPathURLsProvider().getURLs();
        allDependencies = new DependencyGraphMavenDependenciesResolver().buildDependencies();
    }

    private MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new MuleMavenMultiModuleArtifactMapping();
    private ClassLoaderRunnerFactory classLoaderRunnerFactory = new MuleClassLoaderRunnerFactory();
    private ClassPathClassifier classPathClassifier = new MuleClassPathClassifier();

    /**
     * Creates a Runner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public ArtifactClassloaderTestRunner(final Class<?> klass) throws Exception
    {
        ClassLoader classLoader = buildArtifactClassloader(klass);
        delegate = new ClassLoaderIsolatedTestRunner(classLoader, klass);
    }

    @Override
    protected Runner getDelegateRunner()
    {
        return delegate;
    }

    private ClassLoader buildArtifactClassloader(final Class<?> klass) throws IOException, URISyntaxException
    {
        ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(klass, classPathURLs, allDependencies, mavenMultiModuleArtifactMapping);
        return classLoaderRunnerFactory.createClassLoader(klass, artifactUrlClassification);
    }

}
