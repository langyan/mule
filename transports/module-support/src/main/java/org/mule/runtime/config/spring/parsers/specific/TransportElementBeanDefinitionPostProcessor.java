/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.runtime.core.endpoint.URIBuilder;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.util.StringUtils;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

public class TransportElementBeanDefinitionPostProcessor implements CommonBeanDefinitionCreator.BeanDefinitionPostProcessor
{

    @Override
    public void postProcess(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition)
    {
        if (componentModel.getIdentifier().getName().equals("inbound-endpoint") ||
            componentModel.getIdentifier().getName().equals("outbound-endpoint") ||
            componentModel.getIdentifier().getName().equals("endpoint"))
        {
            if (componentModel.getParameters().containsKey("ref"))
            {
                modelBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(componentModel.getParameters().get("ref")));
            }
            Object addressValue = componentModel.getParameters().get("address");
            if (addressValue != null)
            {
                addUriBuilderPropertyValue(modelBeanDefinition, addressValue);
            }
            processAddressAttribute(componentModel, modelBeanDefinition);
            String transformerRefs = componentModel.getParameters().get("transformer-refs");
            if (!StringUtils.isEmpty(transformerRefs))
            {
                String[] refs = transformerRefs.split(" ");
                ManagedList managedList = new ManagedList();
                for (String ref : refs)
                {
                    managedList.add(new RuntimeBeanReference(ref));
                }
                modelBeanDefinition.getPropertyValues().addPropertyValue("transformers", managedList);
            }
            String responseTransformerRefs = componentModel.getParameters().get("responseTransformer-refs");
            if (!StringUtils.isEmpty(responseTransformerRefs))
            {
                String[] refs = responseTransformerRefs.split(" ");
                ManagedList managedList = new ManagedList();
                for (String ref : refs)
                {
                    managedList.add(new RuntimeBeanReference(ref));
                }
                modelBeanDefinition.getPropertyValues().addPropertyValue("responseTransformers", managedList);
            }
            //Remove response message processor from message processors list
            ManagedList messageProcessors = (ManagedList) modelBeanDefinition.getPropertyValues().get("messageProcessors");
            if (messageProcessors != null)
            {
                Object lastMessageProcessor = messageProcessors.get(messageProcessors.size() - 1);
                if (lastMessageProcessor instanceof AbstractBeanDefinition)
                {
                    if (areMatchingTypes(MessageProcessorChainFactoryBean.class, ((AbstractBeanDefinition) lastMessageProcessor).getBeanClass()))
                    {
                        messageProcessors.remove(messageProcessors.size() - 1);
                    }
                }
            }
            //Take the <response> mps and add them.
            componentModel.getInnerComponents().stream().filter( innerComponent -> {
                return innerComponent.getIdentifier().getName().equals("response");
            }).findFirst().ifPresent( responseComponentModel -> {
                ManagedList responseMessageProcessorsBeanList = new ManagedList();
                responseComponentModel.getInnerComponents().forEach( responseProcessorComponentModel -> {
                    BeanDefinition beanDefinition = responseProcessorComponentModel.getBeanDefinition();
                    responseMessageProcessorsBeanList.add(beanDefinition != null ? beanDefinition : responseProcessorComponentModel.getBeanReference());
                });
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MessageProcessorChainFactoryBean.class);
                beanDefinitionBuilder.addPropertyValue("messageProcessors", responseMessageProcessorsBeanList);
                modelBeanDefinition.getPropertyValues().addPropertyValue("responseMessageProcessors", beanDefinitionBuilder.getBeanDefinition());
            });

            if (messageProcessors != null)
            {
                for (int i = 0; i < messageProcessors.size(); i++)
                {
                    Object processorDefinition = messageProcessors.get(i);
                    if (processorDefinition instanceof AbstractBeanDefinition && areMatchingTypes(AbstractRedeliveryPolicy.class, ((AbstractBeanDefinition) processorDefinition).getBeanClass()))
                    {
                        messageProcessors.remove(i);
                        modelBeanDefinition.getPropertyValues().addPropertyValue("redeliveryPolicy", processorDefinition);
                        break;
                    }
                }
            }
        }
        if (componentModel.getIdentifier().getName().endsWith("endpoint"))
        {
            //in the case of the endpoint element, all the properties must not be set to the {@code org.mule.runtime.core.endpoint.UrlEndpointURIBuilder}
            //but to the properties map inside it. So we need to revert previous properties processing in this particular case.
            ManagedMap propertiesMap = new ManagedMap();
            componentModel.getInnerComponents()
                    .stream()
                    .filter(innerComponent -> {
                        ComponentIdentifier identifier = innerComponent.getIdentifier();
                        return identifier.equals(SPRING_PROPERTY_IDENTIFIER) || identifier.equals(MULE_PROPERTY_IDENTIFIER);
                    }).forEach(propertyComponentModel -> {
                        PropertyValue propertyValue = CommonBeanDefinitionCreator.getPropertyValueFromPropertyComponent(propertyComponentModel);
                        modelBeanDefinition.getPropertyValues().removePropertyValue(propertyValue.getName());
                        propertiesMap.put(propertyValue.getName(), propertyValue.getValue());
                    });
            componentModel.getInnerComponents()
                    .stream()
                    .filter(innerComponent -> {
                        return innerComponent.getIdentifier().getName().equals("properties");
                    })
                    .findFirst()
                    .ifPresent(propertiesComponent -> {
                        CommonBeanDefinitionCreator.getPropertyValueFromPropertiesComponent(propertiesComponent).stream().forEach( propertyValue -> {
                            propertiesMap.put(propertyValue.getName(), propertyValue.getValue());
                        });
                    });
            if (!propertiesMap.isEmpty())
            {
                modelBeanDefinition.getPropertyValues().addPropertyValue("properties", propertiesMap);
            }
        }
    }

    private void addUriBuilderPropertyValue(AbstractBeanDefinition modelBeanDefinition, Object addressValue)
    {
        BeanDefinitionBuilder uriBuilderBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(URIBuilder.class);
        uriBuilderBeanDefinition.addConstructorArgValue(addressValue);
        uriBuilderBeanDefinition.addConstructorArgReference(OBJECT_MULE_CONTEXT);
        modelBeanDefinition.getPropertyValues().addPropertyValue("uRIBuilder", uriBuilderBeanDefinition.getBeanDefinition());
    }

    private void processAddressAttribute(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition)
    {
        if (componentModel.getIdentifier().getNamespace().equals("jms") && componentModel.getIdentifier().getName().endsWith("endpoint") && !componentModel.getParameters().containsKey("address") && !componentModel.getParameters().containsKey("ref"))
        {
            StringBuilder address = new StringBuilder("jms://");
            if (componentModel.getParameters().containsKey("queue"))
            {
                address.append(componentModel.getParameters().get("queue"));
            }
            else
            {
                address.append("queue/" + componentModel.getParameters().get("topic"));
            }
            addUriBuilderPropertyValue(modelBeanDefinition, address.toString());
        }
    }
}