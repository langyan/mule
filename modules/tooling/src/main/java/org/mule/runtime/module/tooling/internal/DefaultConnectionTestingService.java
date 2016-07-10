/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.apache.commons.lang.StringUtils.join;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.module.tooling.api.ConnectionResult;
import org.mule.runtime.module.tooling.api.ConnectionTestingService;
import org.mule.runtime.module.tooling.api.DynamicParameterValue;
import org.mule.runtime.module.tooling.api.artifact.Artifact;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultConnectionTestingService implements ConnectionTestingService
{

    private final Artifact artifact;

    public DefaultConnectionTestingService(Artifact artifact)
    {
        //TODO connection testing service should work regardless of ExtensionManager.
        this.artifact = artifact;
    }

    public ConnectionResult testConnection(String extensionName, String vendor, String providerName, DynamicParameterValue... parametervalues)
    {
        return artifact.executeInArtifactContext(() -> {
            checkArgument(extensionName != null, "extension name must not be null");
            checkArgument(vendor != null, "vendor must not be null");
            checkArgument(providerName != null, "provider name must not be null");
            Set<RuntimeExtensionModel> extensions = artifact.getExtensionManager().getExtensions(extensionName);
            if (extensions.isEmpty())
            {
                throw new MuleRuntimeException(createStaticMessage("Extension with name %s was not found", extensionName));
            }
            Optional<RuntimeExtensionModel> vendorExtension = extensions.stream()
                    .filter(runtimeExtensionModel -> runtimeExtensionModel.getVendor().equals(vendor)).findAny();
            if (!vendorExtension.isPresent())
            {
                List<String> vendors = extensions.stream().map(extensionModel -> extensionModel.getVendor()).collect(Collectors.toList());
                throw new MuleRuntimeException(createStaticMessage("Extension with name %s and vendor %s was not found. You must specify a vendor. Vendor founds where %s",
                                                                   extensionName, vendor, join(vendors, ",")));
            }
            RuntimeExtensionModel extensionModel = vendorExtension.get();
            Optional<ConnectionProviderModel> connectionProviderModelOptional = extensionModel.getConnectionProviders().stream().filter(connectionProviderModel -> connectionProviderModel.getName().equals(providerName)).findFirst();
            if (!connectionProviderModelOptional.isPresent())
            {
                List<String> connectionProviderNames = extensionModel.getConnectionProviders().stream().map(connectionProviderModel -> connectionProviderModel.getName()).collect(Collectors.toList());
                throw new MuleRuntimeException(createStaticMessage("Extension with name %s and vendor %s does not have a provider with name %s. Providers founds where %s",
                                                                   extensionName, vendor, providerName, join(connectionProviderNames, ",")));
            }
            ConnectionProviderModel connectionProviderModel = connectionProviderModelOptional.get();
            return null;
        });

    }

}
