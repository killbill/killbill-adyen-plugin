/*
 * Copyright 2015 Groupon, Inc
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


import org.killbill.billing.account.api.Account;
import org.killbill.billing.payment.api.Payment;
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
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenHostedPaymentPageConfigurationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

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
    private Payment payment;
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

    public AdyenPluginMockBuilder withAdyenProperty(String key, String value) {
        adyenProperties.setProperty(key, value);
        return this;
    }

    public AdyenPluginMockBuilder withAccount(Account account) {
        this.account = account;
        return this;
    }

    public AdyenPluginMockBuilder withPayment(Payment payment) {
        this.payment = payment;
        return this;
    }

    public AdyenPaymentPluginApi build() throws Exception {
        AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(adyenProperties);

        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer(adyenConfigProperties);
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        final LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        final LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        final HttpHeaderInterceptor httpHeaderInterceptor = new HttpHeaderInterceptor();
        final PaymentPortRegistry adyenPaymentPortRegistry = new AdyenPaymentPortRegistry(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = new AdyenPaymentRequestSender(adyenPaymentPortRegistry);

        AdyenPaymentServiceProviderPort adyenPaymentServiceProviderPort = new AdyenPaymentServiceProviderPort(paymentInfoConverterManagement, adyenRequestFactory, adyenPaymentRequestSender);
        AdyenPaymentServiceProviderHostedPaymentPagePort adyenPaymentServiceProviderHostedPaymentPagePort = new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory);

        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account, TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency()), null);
        final OSGIKillbillLogService logService = TestUtils.buildLogService();

        AdyenConfigurationHandler adyenConfigurationHandler = new AdyenConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService);
        adyenConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderPort);

        AdyenHostedPaymentPageConfigurationHandler adyenHostedPaymentPageConfigurationHandler = new AdyenHostedPaymentPageConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService);
        adyenHostedPaymentPageConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderHostedPaymentPagePort);

        final Clock clock = new DefaultClock();

        final OSGIKillbillAPI killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, null);

        final OSGIConfigPropertiesService configPropertiesService = mock(OSGIConfigPropertiesService.class);

        return new AdyenPaymentPluginApi(adyenConfigurationHandler, adyenHostedPaymentPageConfigurationHandler, killbillApi, configPropertiesService, logService, clock, dao);
    }

    public AdyenPluginMockBuilder withDatabaseAccess(AdyenDao dao) {
        this.dao = dao;
        return this;
    }
}
