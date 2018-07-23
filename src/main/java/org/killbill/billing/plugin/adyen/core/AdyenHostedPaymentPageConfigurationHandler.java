/*
 * Copyright 2015-2016 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.core;

import java.security.GeneralSecurityException;
import java.util.Properties;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.converter.impl.PaymentInfoConverterService;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.DirectoryClient;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenHostedPaymentPageConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<AdyenPaymentServiceProviderHostedPaymentPagePort> {

    private static final Logger logger = LoggerFactory.getLogger(AdyenHostedPaymentPageConfigurationHandler.class);

    private final String region;

    public AdyenHostedPaymentPageConfigurationHandler(final String pluginName,
                                                      final OSGIKillbillAPI osgiKillbillAPI,
                                                      final OSGIKillbillLogService osgiKillbillLogService,
                                                      final String region) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
        this.region = region;
    }

    @Override
    protected AdyenPaymentServiceProviderHostedPaymentPagePort createConfigurable(final Properties properties) {
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties, region);
        return initializeHppAdyenClient(adyenConfigProperties);
    }

    private AdyenPaymentServiceProviderHostedPaymentPagePort initializeHppAdyenClient(final AdyenConfigProperties adyenConfigProperties) {
        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer();
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        DirectoryClient directoryClient = null;
        if (adyenConfigProperties.getDirectoryUrl() != null) {
            try {
                directoryClient = new DirectoryClient(adyenConfigProperties.getDirectoryUrl(),
                                                      adyenConfigProperties.getProxyServer(),
                                                      adyenConfigProperties.getProxyPort(),
                                                      !adyenConfigProperties.getTrustAllCertificates(),
                                                      Integer.valueOf(adyenConfigProperties.getPaymentConnectionTimeout()),
                                                      Integer.valueOf(adyenConfigProperties.getPaymentReadTimeout()));
            } catch (final GeneralSecurityException e) {
                logger.warn("Unable to configure the directory client", e);
            }
        }
        return new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory, directoryClient);
    }
}

