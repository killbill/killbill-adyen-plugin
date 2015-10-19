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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.AdyenPluginMockBuilder;
import org.killbill.billing.plugin.adyen.EmbeddedDbHelper;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.util.callcontext.CallContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.google.common.collect.ImmutableMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.killbill.billing.plugin.TestUtils.toProperties;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.CC_EXPIRATION_MONTH;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.CC_EXPIRATION_YEAR;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.CC_NUMBER;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.CC_TYPE;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.CC_VERIFICATION_VALUE;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.DEFAULT_COUNTRY;
import static org.killbill.billing.plugin.adyen.TestRemoteBase.DEFAULT_CURRENCY;
import static org.killbill.billing.plugin.adyen.api.TestAdyenPaymentPluginHttpErrors.WireMockHelper.doWithWireMock;
import static org.killbill.billing.plugin.adyen.api.TestAdyenPaymentPluginHttpErrors.WireMockHelper.wireMockUri;
import static org.testng.Assert.assertEquals;

/**
 * Checks if the plugin could handle technical communication errors (strange responses, read/connect timeouts etc...) and map them to the correct PaymentPluginStatus.
 * <p/>
 * WireMock is used to create failure scenarios (toxiproxy will be used in the ruby ITs).
 * <p/>
 * Attention: If you have failing tests check first that you don't have a proxy configured (Charles, Fiddler, Burp etc...).
 */
public class TestAdyenPaymentPluginHttpErrors {

    private static final int OK = 200;
    private static final int NOT_FOUND = 404;
    private static final int MOVED = 301;
    private static final int SERVICE_UNAVAILABLE = 503;

    private static final String ADYEN_PATH = "/adyen";

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

        final String unReachableUri = "http://nothing-here.to";

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", unReachableUri)
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());

        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenConnectTimeout() throws Exception {

        final String freeLocalPort = "http://localhost:" + findFreePort();

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", freeLocalPort)
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());

        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
    }

    /**
     * Sanity check that a refused purchase (wrong expiry month) still results in an error.
     */
    @Test(groups = "slow")
    public void testAuthorizeWithIncorrectValuesExpMonth() throws Exception {

        final Iterable<PluginProperty> inValidPurchaseData = invalidCreditCardDataExpMonth();

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, inValidPurchaseData);

        assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        assertEquals(result.getGatewayErrorCode(), "Expiry month should be between 1 and 12 inclusive");
    }

    /**
     * Sanity check that a refused purchase (wrong cvv) still results in an error.
     */
    @Test(groups = "slow")
    public void testAuthorizeWithIncorrectValuesCVV() throws Exception {

        final Iterable<PluginProperty> inValidPurchaseData = invalidCreditCardDataCVV();

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, inValidPurchaseData);

        assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        assertEquals(result.getGatewayErrorCode(), "CVC Declined");
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenReadTimeout() throws Exception {

        final int adyenClientReadTimeout = 100;
        final int adyenResponseDelay = adyenClientReadTimeout * 4;

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentReadTimeout", String.valueOf(adyenClientReadTimeout))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws PaymentPluginApiException {

                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withStatus(OK)
                                .withFixedDelay(adyenResponseDelay)));

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
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                                .withStatus(OK)));

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
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .withDatabaseAccess(dao)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                                .withStatus(OK)));

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
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withFault(Fault.EMPTY_RESPONSE)
                                .withStatus(OK)));

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
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
                                .withStatus(OK)));

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
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withStatus(NOT_FOUND)));

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
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withStatus(MOVED)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());

            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith503() throws Exception {

        final Account account = defaultAccount();
        final Payment payment = killBillPayment(account);
        final AdyenCallContext callContext = newCallContext();

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAccount(account)
                                                                      .withPayment(payment)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withStatus(SERVICE_UNAVAILABLE)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());

            }
        });

        assertEquals(result.getStatus(), PaymentPluginStatus.UNDEFINED);
    }

    private Iterable<PluginProperty> invalidCreditCardDataExpMonth() {
        final Map<String, String> invalidCreditCardData = new HashMap<String, String>(validCreditCardData());
        invalidCreditCardData.put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf("123"));
        return toProperties(invalidCreditCardData);
    }

    private Iterable<PluginProperty> invalidCreditCardDataCVV() {
        final Map<String, String> invalidCreditCardData = new HashMap<String, String>(validCreditCardData());
        invalidCreditCardData.put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, String.valueOf("1234"));
        return toProperties(invalidCreditCardData);
    }

    private AdyenCallContext newCallContext() {
        return new AdyenCallContext(DateTime.now(), UUID.randomUUID());
    }

    private Payment killBillPayment(final Account account) {
        return TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency());
    }

    private Iterable<PluginProperty> creditCardPaymentProperties() {
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

    private PaymentTransactionInfoPlugin authorizeCall(final Account account, final Payment payment, final CallContext callContext, final PaymentPluginApi pluginApi, final Iterable<PluginProperty> pluginProperties) throws PaymentPluginApiException {
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

        private static final WireMockHelper INSTANCE = new WireMockHelper();

        public static WireMockHelper instance() {
            return INSTANCE;
        }

        private int freePort = -1;

        private synchronized int getFreePort() throws IOException {
            if (freePort == -1) {
                freePort = findFreePort();
            }
            return freePort;
        }

        public static String wireMockUri(final String path) throws IOException {
            return "http://localhost:" + WireMockHelper.instance().getFreePort() + path;
        }

        public static <T> T doWithWireMock(final WithWireMock<T> command) throws Exception {
            final int wireMockPort = WireMockHelper.instance().getFreePort();
            final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(wireMockPort));
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockPort);
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

    static int findFreePort() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final int freePort = serverSocket.getLocalPort();
        serverSocket.close();
        return freePort;
    }
}
