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
package org.killbill.billing.plugin.adyen.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.AdyenPluginMockBuilder;
import org.killbill.billing.plugin.adyen.EmbeddedDbHelper;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.killbill.billing.plugin.TestUtils.toProperties;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.*;
import static org.killbill.billing.plugin.adyen.api.TestAdyenPaymentPluginHttpErrors.WireMockHelper.doWithWireMock;
import static org.testng.Assert.assertEquals;

/**
 * Checks if the plugin could handle technical communication errors (strange responses, read/connect timeouts etc...) and map them to the correct PaymentPluginStatus.
 * <p/>
 * Wiremock is used to create failure scenarios (toxiproxy will be used in the ruby ITs).
 */
public class TestAdyenPaymentPluginHttpErrors {

    //TODO was not able to provoke a connection timeout (addRequestProcessingDelay() had no effect: http://wiremock.org/simulating-faults.html)

    private AdyenDao dao;

    @BeforeClass(groups = "slow")
    public void setUpBeforeClass() throws Exception {

        dao = EmbeddedDbHelper.instance().startDb();
    }

    @BeforeMethod(groups = "slow")
    public void setUpBeforeMethod() throws Exception {
        EmbeddedDbHelper.instance().resetDB();
    }

    @AfterClass(groups = "slow")
    public void tearDownAfterClass() throws Exception {
        EmbeddedDbHelper.instance().stopDB();
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenNotReachable() throws Exception {

        String unReachableUri = "http://nothing-here.to";

        final Account account = defaultAccount();
        Payment payment = killBillPayment(account);
        AdyenCallContext callContext = newCallContext();

        AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", unReachableUri)
                .withAccount(account)
                .withPayment(payment)
                .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());

        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
    }


    @Test(groups = "slow")
    public void testAuthorizeWithInvalidValues() throws Exception {

        final Account account = defaultAccount();
        Payment payment = killBillPayment(account);
        AdyenCallContext callContext = newCallContext();

        AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAccount(account)
                .withPayment(payment)
                .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, invalidCreditCardData());

        assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenReadTimeout() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentReadTimeout", "100")
                .withAccount(account)
                .withPayment(payment)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws PaymentPluginApiException {

                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withStatus(200)
                                .withFixedDelay(400)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenBadResponse() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAccount(account)
                .withPayment(payment)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                                .withStatus(200)));


                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    @Test(groups = "slow")
    public void testRefundAdyenBadResponse() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi authorizeApi = AdyenPluginMockBuilder.newPlugin()
                .withAccount(account)
                .withPayment(payment)
                .withDatabaseAccess(dao)
                .build();

        authorizeCall(account, payment, callContext, authorizeApi, creditCardPaymentProperties());

        final AdyenPaymentPluginApi refundApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAccount(account)
                .withPayment(payment)
                .withDatabaseAccess(dao)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                                .withStatus(200)));

                final PaymentTransaction refundTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.REFUND, Currency.EUR);

                return refundApi.refundPayment(account.getId(),
                        payment.getId(),
                        refundTransaction.getId(),
                        account.getPaymentMethodId(),
                        refundTransaction.getAmount(),
                        refundTransaction.getCurrency(),
                        creditCardPaymentProperties(),
                        callContext);
            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenEmptyResponse() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();


        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAccount(account)
                .withPayment(payment)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withFault(Fault.EMPTY_RESPONSE)
                                .withStatus(200)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }


    @Test(groups = "slow")
    public void testAuthorizeAdyenMalformedResponseChunk() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAccount(account)
                .withPayment(payment)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
                                .withStatus(200)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }


    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith404() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAccount(account)
                .withPayment(payment)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withStatus(404)));


                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith301() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", "http://localhost:8089/adyen")
                .withAccount(account)
                .withPayment(payment)
                .build();

        PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo("/adyen")).willReturn(
                        aResponse()
                                .withStatus(301)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());

            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    private List<PluginProperty> invalidCreditCardData() {
        HashMap<String, String> invalidCreditCardData = new HashMap<String, String>(validCreditCardData());
        invalidCreditCardData.put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf("123"));
        return toProperties(invalidCreditCardData);
    }

    private AdyenCallContext newCallContext() {
        return new AdyenCallContext(DateTime.now(), UUID.randomUUID());
    }


    private Payment killBillPayment(Account account) {
        return TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency());
    }

    private List<PluginProperty> creditCardPaymentProperties() {
        return toProperties(validCreditCardData());
    }

    private ImmutableMap<String, String> validCreditCardData() {
        return ImmutableMap.<String, String>builder()
                .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE).build();
    }

    private Account defaultAccount() {
        return TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
    }

    private PaymentTransactionInfoPlugin authorizeCall(Account account, Payment payment, AdyenCallContext callContext, AdyenPaymentPluginApi pluginApi, List<PluginProperty> pluginProperties) throws PaymentPluginApiException {
        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, Currency.EUR);

        return pluginApi.authorizePayment(account.getId(),
                payment.getId(),
                authorizationTransaction.getId(),
                account.getPaymentMethodId(),
                authorizationTransaction.getAmount(),
                authorizationTransaction.getCurrency(),
                pluginProperties,
                callContext);
    }

    private interface WithWireMock<T> {
        T execute(WireMockServer server) throws Exception;
    }

    static class WireMockHelper {

        private static final int WIRE_MOCK_PORT = 8089;

        public static <T> T doWithWireMock(WithWireMock<T> command) throws Exception {
            WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
            wireMockServer.start();
            WireMock.configureFor("localhost", WIRE_MOCK_PORT);
            try {
                return command.execute(wireMockServer);
            } finally {
                wireMockServer.shutdown();
                while (wireMockServer.isRunning()) {
                    Thread.sleep(1);
                }
            }
        }
    }
}
