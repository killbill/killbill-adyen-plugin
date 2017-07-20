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

package org.killbill.billing.plugin.adyen;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.jaxws.HttpHeaderInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingInInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingOutInterceptor;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.converter.impl.PaymentInfoConverterService;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentRequestSender;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.killbill.billing.plugin.adyen.client.recurring.AdyenRecurringClient;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigPropertiesConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenHostedPaymentPageConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenRecurringConfigurationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

/**
 * First version of a test builder with a fluent api.
 * Goal is to have a more configurable test setup.
 * <p/>
 * TODO remove parts which are redundant with TestRemoteBase
 */
public class AdyenPluginMockBuilder {

    // To run these tests, you need a properties file in the classpath (e.g. src/test/resources/adyen.properties)
    // See README.md for details on the required properties
    private static final String PROPERTIES_FILE_NAME = "adyen.properties";
    private final Properties adyenProperties;
    private Account account;
    private OSGIKillbillAPI killbillAPI;
    private AdyenDao dao;

    private AdyenPluginMockBuilder() throws IOException, SQLException {
        adyenProperties = getDefaultAdyenConfigProperties();
        dao = mock(AdyenDao.class);

    }

    public static AdyenPluginMockBuilder newPlugin() throws Exception {
        return new AdyenPluginMockBuilder();
    }

    private static Properties getDefaultAdyenConfigProperties() throws IOException {
        return TestUtils.loadProperties(PROPERTIES_FILE_NAME);
    }

    public AdyenPluginMockBuilder withAdyenProperty(final String key, final String value) {
        adyenProperties.setProperty(key, value);
        return this;
    }

    public AdyenPluginMockBuilder withAccount(final Account account) {
        this.account = account;
        return this;
    }

    public AdyenPluginMockBuilder withOSGIKillbillAPI(final OSGIKillbillAPI killbillAPI) {
        this.killbillAPI = killbillAPI;
        return this;
    }

    public AdyenPaymentPluginApi build() throws Exception {
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(adyenProperties, adyenProperties);

        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer();
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        final LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        final LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        final HttpHeaderInterceptor httpHeaderInterceptor = new HttpHeaderInterceptor();
        final PaymentPortRegistry adyenPaymentPortRegistry = new AdyenPaymentPortRegistry(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = new AdyenPaymentRequestSender(adyenPaymentPortRegistry);

        final AdyenPaymentServiceProviderPort adyenPaymentServiceProviderPort = new AdyenPaymentServiceProviderPort(adyenRequestFactory, adyenPaymentRequestSender);
        final AdyenPaymentServiceProviderHostedPaymentPagePort adyenPaymentServiceProviderHostedPaymentPagePort = new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory, null);
        final AdyenRecurringClient adyenRecurringClient = new AdyenRecurringClient(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);

        final OSGIKillbillLogService logService = TestUtils.buildLogService();
        final OSGIConfigPropertiesService osgiConfigPropertiesService = Mockito.mock(OSGIConfigPropertiesService.class);
        Mockito.when(osgiConfigPropertiesService.getProperties()).thenReturn(adyenProperties);

        final AdyenConfigurationHandler adyenConfigurationHandler = new AdyenConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, osgiConfigPropertiesService);
        adyenConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderPort);

        final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler = new AdyenConfigPropertiesConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, osgiConfigPropertiesService);
        adyenConfigPropertiesConfigurationHandler.setDefaultConfigurable(adyenConfigProperties);

        final AdyenHostedPaymentPageConfigurationHandler adyenHostedPaymentPageConfigurationHandler = new AdyenHostedPaymentPageConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, osgiConfigPropertiesService);
        adyenHostedPaymentPageConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderHostedPaymentPagePort);

        final AdyenRecurringConfigurationHandler adyenRecurringConfigurationHandler = new AdyenRecurringConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, osgiConfigPropertiesService);
        adyenRecurringConfigurationHandler.setDefaultConfigurable(adyenRecurringClient);

        final Clock clock = new DefaultClock();

        final OSGIConfigPropertiesService configPropertiesService = mock(OSGIConfigPropertiesService.class);

        return new AdyenPaymentPluginApi(adyenConfigurationHandler,
                                         adyenConfigPropertiesConfigurationHandler,
                                         adyenHostedPaymentPageConfigurationHandler,
                                         adyenRecurringConfigurationHandler,
                                         killbillAPI,
                                         configPropertiesService,
                                         logService,
                                         clock,
                                         dao);
    }

    public AdyenPluginMockBuilder withDatabaseAccess(final AdyenDao dao) {
        this.dao = dao;
        return this;
    }
}
