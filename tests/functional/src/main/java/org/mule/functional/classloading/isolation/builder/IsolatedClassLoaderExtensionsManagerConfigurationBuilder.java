/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.builder;

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
public class IsolatedClassLoaderExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder
{

    private static Logger LOGGER = LoggerFactory.getLogger(IsolatedClassLoaderExtensionsManagerConfigurationBuilder.class);

    private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
    private final List<ArtifactClassLoader> pluginsClassLoaders;

    /**
     * Creates an instance of the builder with the list of plugin class loaders. If an {@link ArtifactClassLoader} has
     * a extension descriptor it will be registered as an extension if not it is assumed that it is not an extension plugin.
     * The extension will be loaded and registered with its corresponding class loader in order to get access
     * to the isolated {@link ClassLoader} defined for the extension.
     *
     * @param pluginsClassLoaders the list of {@link ArtifactClassLoader} created for each plugin found in the dependencies (either plugin or extension plugin).
     */
    public IsolatedClassLoaderExtensionsManagerConfigurationBuilder(List<ArtifactClassLoader> pluginsClassLoaders)
    {
        this.extensionManagerAdapterFactory = new DefaultExtensionManagerAdapterFactory();
        this.pluginsClassLoaders = pluginsClassLoaders;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

        for (Object pluginClassLoader : pluginsClassLoaders)
        {
            String artifactName = (String) pluginClassLoader.getClass().getMethod("getArtifactName").invoke(pluginClassLoader);
            ClassLoader classLoader = (ClassLoader) pluginClassLoader.getClass().getMethod("getClassLoader").invoke(pluginClassLoader);
            Method findResourceMethod = classLoader.getClass().getMethod("findResource", String.class);
            findResourceMethod.setAccessible(true);
            URL manifestUrl = (URL) findResourceMethod.invoke(classLoader, "META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
            if (manifestUrl != null)
            {
                // There will be more than one extension manifest file so we just filter by convention
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Discovered extension: {}", artifactName);
                }
                ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
                extensionManager.registerExtension(extensionManifest, classLoader);
            }
            else
            {
                LOGGER.debug("Discarding plugin artifact class loader with artifactName '{}' due to it doesn't have an extension descriptor", artifactName);
            }
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
