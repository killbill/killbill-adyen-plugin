/*
 * Copyright 2014-2015 Groupon, Inc
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
import java.util.Properties;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
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
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenHostedPaymentPageConfigurationHandler;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.testng.annotations.BeforeClass;

public abstract class TestRemoteBase {

    // To run these tests, you need a properties file in the classpath (e.g. src/test/resources/adyen.properties)
    // See README.md for details on the required properties
    private static final String PROPERTIES_FILE_NAME = "adyen.properties";

    // Simulate payments using a credit card in Germany (requires credentials for a German merchant account)
    protected static final Currency DEFAULT_CURRENCY = Currency.EUR;
    protected static final String DEFAULT_COUNTRY = "DE";

    // Magic details at https://www.adyen.com/home/support/knowledgebase/implementation-articles.html
    // Note: make sure to use the Amex one, as Visa/MC is not always configured by default
    protected static final String CC_NUMBER = "370000000000002";
    protected static final int CC_EXPIRATION_MONTH = 8;
    protected static final int CC_EXPIRATION_YEAR = 2018;
    protected static final String CC_VERIFICATION_VALUE = "7373";
    protected static final String CC_TYPE = "amex";

    protected static final String DD_IBAN = "DE87123456781234567890";
    protected static final String DD_BIC = "TESTDE01XXX";
    protected static final String DD_HOLDER_NAME = "A. Schneider";
    protected static final String DD_TYPE = "sepadirectdebit";

    protected static final String ELV_ACCOUNT_NUMBER = "1234567890";
    protected static final String ELV_BLZ = "12345678";
    protected static final String ELV_HOLDER_NAME = "Bill Killson";
    protected static final String ELV_TYPE = "elv";

    protected AdyenConfigProperties adyenConfigProperties;
    protected AdyenConfigurationHandler adyenConfigurationHandler;
    protected AdyenHostedPaymentPageConfigurationHandler adyenHostedPaymentPageConfigurationHandler;
    protected AdyenPaymentServiceProviderPort adyenPaymentServiceProviderPort;
    protected AdyenPaymentServiceProviderHostedPaymentPagePort adyenPaymentServiceProviderHostedPaymentPagePort;

    @BeforeClass(groups = "slow")
    public void setUpBeforeClass() throws Exception {
        adyenConfigProperties = getAdyenConfigProperties();

        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer(adyenConfigProperties);
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        final LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        final LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        final HttpHeaderInterceptor httpHeaderInterceptor = new HttpHeaderInterceptor();
        final PaymentPortRegistry adyenPaymentPortRegistry = new AdyenPaymentPortRegistry(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = new AdyenPaymentRequestSender(adyenPaymentPortRegistry);

        adyenPaymentServiceProviderPort = new AdyenPaymentServiceProviderPort(paymentInfoConverterManagement, adyenRequestFactory, adyenPaymentRequestSender);
        adyenPaymentServiceProviderHostedPaymentPagePort = new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory);

        final Account account = TestUtils.buildAccount(Currency.BTC, "US");
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account, TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency()), null);
        final OSGIKillbillLogService logService = TestUtils.buildLogService();

        adyenConfigurationHandler = new AdyenConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService);
        adyenConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderPort);

        adyenHostedPaymentPageConfigurationHandler = new AdyenHostedPaymentPageConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillAPI, logService);
        adyenHostedPaymentPageConfigurationHandler.setDefaultConfigurable(adyenPaymentServiceProviderHostedPaymentPagePort);
    }

    private AdyenConfigProperties getAdyenConfigProperties() throws IOException {
        final Properties properties = TestUtils.loadProperties(PROPERTIES_FILE_NAME);
        return new AdyenConfigProperties(properties);
    }
}
