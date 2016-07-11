/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.mule.runtime.core.api.config.ConfigurationBuilder} that creates an {@link org.mule.runtime.extension.api.ExtensionManager}.
 * It reads the extension manifest file using the extension class loader that loads the extension annotated class and register the extension to the
 * manager.
 *
 * @since 4.0
 */
public class ClassLoaderIsolatedExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder
{

    private static Logger LOGGER = LoggerFactory.getLogger(ClassLoaderIsolatedExtensionsManagerConfigurationBuilder.class);

    private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
    private final String[] extensionClasses;
    private final List<ArtifactClassLoader> extensionClassLoaders;

    public ClassLoaderIsolatedExtensionsManagerConfigurationBuilder(String[] extensionClasses, List<ArtifactClassLoader> extensionClassLoaders)
    {
        this.extensionClasses = extensionClasses;
        this.extensionManagerAdapterFactory = new DefaultExtensionManagerAdapterFactory();
        this.extensionClassLoaders = extensionClassLoaders;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

        for (String extensionClass : extensionClasses)
        {
            ClassLoader extensionClassLoader = null;
            for (Object artifactClassLoader : extensionClassLoaders)
            {
                if(artifactClassLoader.getClass().getMethod("getArtifactName").invoke(artifactClassLoader).equals(extensionClass))
                {
                    extensionClassLoader = (ClassLoader) artifactClassLoader.getClass().getMethod("getClassLoader").invoke(artifactClassLoader);
                }
            }
            if (extensionClassLoader == null)
            {
                throw new IllegalStateException("There is no extension class loader by name: " + extensionClass);
            }

            // There will be more than one extension manifest file so we just filter by convention
            Method findResourceMethod = extensionClassLoader.getClass().getMethod("findResource", String.class);
            findResourceMethod.setAccessible(true);
            URL manifestUrl = (URL) findResourceMethod.invoke(extensionClassLoader, "META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Discovered extension " + extensionClass);
            }
            ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
            extensionManager.registerExtension(extensionManifest, extensionClassLoader);
        }
    }

    private ExtensionManagerAdapter createExtensionManager(MuleContext muleContext) throws InitialisationException
    {
        try
        {
            return extensionManagerAdapterFactory.createExtensionManager(muleContext);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, muleContext);
        }
    }
}
