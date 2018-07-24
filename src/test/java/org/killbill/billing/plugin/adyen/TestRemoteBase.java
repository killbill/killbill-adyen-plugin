/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

import java.util.Properties;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.TestUtils;
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
import org.killbill.billing.plugin.adyen.client.payment.service.DirectoryClient;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.killbill.billing.plugin.adyen.client.recurring.AdyenRecurringClient;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigPropertiesConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenHostedPaymentPageConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenRecurringConfigurationHandler;
import org.testng.annotations.BeforeClass;

public abstract class TestRemoteBase {

    // To run these tests, you need a properties file in the classpath (e.g. src/test/resources/adyen.properties)
    // See README.md for details on the required properties
    private static final String PROPERTIES_FILE_NAME = "adyen.properties";

    // Simulate payments using a credit card in Germany (requires credentials for a German merchant account)
    public static final Currency DEFAULT_CURRENCY = Currency.EUR;
    public static final String DEFAULT_COUNTRY = "DE";
    // Magic details at https://www.adyen.com/home/support/knowledgebase/implementation-articles.html
    // Note: make sure to use the Amex one, as Visa/MC is not always configured by default
    public static final String CC_NUMBER = "370000000000002";
    public static final int CC_EXPIRATION_MONTH = 8;
    public static final int CC_EXPIRATION_YEAR = 2018;
    public static final String CC_VERIFICATION_VALUE = "7373";
    public static final String CC_TYPE = "amex";

    public static final String CC_3DS_NUMBER = "5212345678901234";
    public static final String CC_3DS_VERIFICATION_VALUE = "737";

    public static final String CC_AVS_NUMBER = "5500000000000004";
    public static final String CC_CVV_VERIFICATION_VALUE = "737";

    public static final String DD_IBAN = "DE87123456781234567890";
    public static final String DD_BIC = "TESTDE01XXX";
    public static final String DD_HOLDER_NAME = "A. Schneider";

    public static final String ELV_KONTONUMMER = "1234567890";
    public static final String ELV_BANKLEITZAHL = "12345678";

    protected AdyenConfigProperties adyenConfigProperties;
    protected AdyenConfigurationHandler adyenConfigurationHandler;
    protected AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler;
    protected AdyenHostedPaymentPageConfigurationHandler adyenHostedPaymentPageConfigurationHandler;
    protected AdyenRecurringConfigurationHandler adyenRecurringConfigurationHandler;
    protected AdyenPaymentServiceProviderPort adyenPaymentServiceProviderPort;
    protected AdyenPaymentServiceProviderHostedPaymentPagePort adyenPaymentServiceProviderHostedPaymentPagePort;
    protected AdyenRecurringClient adyenRecurringClient;
    protected OSGIKillbillLogService logService;

    protected Properties properties;
    protected String merchantAccount;

    @BeforeClass(groups = "slow")
    public void setUpBeforeClassCommon() throws Exception {
        logService = TestUtils.buildLogService();
    }

    @BeforeClass(groups = "integration")
    public void setUpBeforeClass() throws Exception {
        setUpBeforeClassCommon();

        properties = TestUtils.loadProperties(PROPERTIES_FILE_NAME);
        adyenConfigProperties = getAdyenConfigProperties();

        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer();
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        final LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        final LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        final HttpHeaderInterceptor httpHeaderInterceptor = new HttpHeaderInterceptor();
        final PaymentPortRegistry adyenPaymentPortRegistry = new AdyenPaymentPortRegistry(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = new AdyenPaymentRequestSender(adyenPaymentPortRegistry);

        adyenPaymentServiceProviderPort = new AdyenPaymentServiceProviderPort(adyenRequestFactory, adyenPaymentRequestSender);
        final DirectoryClient directoryClient = new DirectoryClient(adyenConfigProperties.getDirectoryUrl(),
                                                                    adyenConfigProperties.getProxyServer(),
                                                                    adyenConfigProperties.getProxyPort(),
                                                                    !adyenConfigProperties.getTrustAllCertificates(),
                                                                    Integer.valueOf(adyenConfigProperties.getPaymentConnectionTimeout()),
                                                                    Integer.valueOf(adyenConfigProperties.getPaymentReadTimeout()));
        adyenPaymentServiceProviderHostedPaymentPagePort = new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory, directoryClient);

        adyenRecurringClient = new AdyenRecurringClient(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);

        final Account account = TestUtils.buildAccount(Currency.BTC, "US");
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);

        adyenConfigurationHandler = new AdyenConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, null);
        adyenConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderPort);

        adyenConfigPropertiesConfigurationHandler = new AdyenConfigPropertiesConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, null);
        adyenConfigPropertiesConfigurationHandler.setDefaultConfigurable(adyenConfigProperties);

        adyenHostedPaymentPageConfigurationHandler = new AdyenHostedPaymentPageConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, null);
        adyenHostedPaymentPageConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderHostedPaymentPagePort);

        adyenRecurringConfigurationHandler = new AdyenRecurringConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService, null);
        adyenRecurringConfigurationHandler.setDefaultConfigurable(adyenRecurringClient);

        merchantAccount = adyenConfigProperties.getMerchantAccount(DEFAULT_COUNTRY);
    }

    private AdyenConfigProperties getAdyenConfigProperties() {
        return new AdyenConfigProperties(properties);
    }
}
