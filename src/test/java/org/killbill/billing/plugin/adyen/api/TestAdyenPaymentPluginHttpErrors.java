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

package org.killbill.billing.plugin.adyen.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.AdyenPluginMockBuilder;
import org.killbill.billing.plugin.adyen.TestWithEmbeddedDBBase;
import org.killbill.billing.util.callcontext.CallContext;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.killbill.billing.plugin.TestUtils.toProperties;
import static org.killbill.billing.plugin.adyen.api.TestAdyenPaymentPluginHttpErrors.WireMockHelper.doWithWireMock;
import static org.killbill.billing.plugin.adyen.api.TestAdyenPaymentPluginHttpErrors.WireMockHelper.wireMockUri;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Checks if the plugin could handle technical communication errors (strange responses, read/connect timeouts etc...) and map them to the correct PaymentPluginStatus.
 * <p/>
 * WireMock is used to create failure scenarios (toxiproxy will be used in the ruby ITs).
 * <p/>
 * Attention: If you have failing tests check first that you don't have a proxy configured (Charles, Fiddler, Burp etc...).
 */
public class TestAdyenPaymentPluginHttpErrors extends TestWithEmbeddedDBBase {

    private static final int OK = 200;
    private static final int UNAUTHORIZED = 401;
    private static final int NOT_FOUND = 404;
    private static final int MOVED = 301;
    private static final int SERVICE_UNAVAILABLE = 503;

    private static final String ADYEN_PATH = "/adyen";

    @Test(groups = "slow")
    public void testAuthorizeAdyenNotReachable() throws Exception {

        final String unReachableUri = "http://nothing-here.to";

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", unReachableUri)
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        assertEquals(result.getGatewayError(), "java.net.UnknownHostException: nothing-here.to");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.CANCELED);
        assertEquals(results.get(0).getGatewayError(), "java.net.UnknownHostException: nothing-here.to");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenConnectTimeout() throws Exception {

        final String freeLocalPort = "http://localhost:" + findFreePort();

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", freeLocalPort)
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        // Exact message depends on the JVM version
        assertTrue(result.getGatewayError().startsWith("java.net.ConnectException: Connection refused"));
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.CANCELED);
        // Exact message depends on the JVM version
        assertTrue(results.get(0).getGatewayError().startsWith("java.net.ConnectException: Connection refused"));
        assertNull(results.get(0).getGatewayErrorCode());
    }

    /**
     * Sanity check that a refused purchase (wrong cc data) still results in an error.
     */
    @Test(groups = "integration")
    public void testAuthorizeWithIncorrectValues() throws Exception {
        final String expectedGatewayError = "CVC Declined";
        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin(properties)
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, invalidCreditCardData(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, String.valueOf("1234")));
        assertEquals(result.getStatus(), PaymentPluginStatus.ERROR);
        assertEquals(result.getGatewayError(), expectedGatewayError);
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.ERROR);
        assertEquals(results.get(0).getGatewayError(), expectedGatewayError);
        assertNull(results.get(0).getGatewayErrorCode());
    }

    @Test(groups = "slow", enabled=false)
    public void testAuthorizeWithInvalidValues() throws Exception {
        final String expectedGatewayError = "validation Expiry month should be between 1 and 12 inclusive Card";
        final String expectedGatewayErrorCode = "o.a.cxf.binding.soap.SoapFault";
        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = authorizeCall(account, payment, callContext, pluginApi, invalidCreditCardData(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf("123")));
        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        assertEquals(result.getGatewayError(), expectedGatewayError);
        assertEquals(result.getGatewayErrorCode(), expectedGatewayErrorCode);

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.CANCELED);
        assertEquals(results.get(0).getGatewayError(), expectedGatewayError);
        assertEquals(results.get(0).getGatewayErrorCode(), expectedGatewayErrorCode);
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenReadTimeout() throws Exception {

        final int adyenClientReadTimeout = 100;
        final int adyenResponseDelay = adyenClientReadTimeout * 4;

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentReadTimeout", String.valueOf(adyenClientReadTimeout))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "java.net.SocketTimeoutException: Read timed out");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "java.net.SocketTimeoutException: Read timed out");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenBadResponse() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "java.io.IOException: Invalid Http response");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "java.io.IOException: Invalid Http response");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "integration")
    public void testRefundAdyenBadResponse() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi authorizeApi = AdyenPluginMockBuilder.newPlugin(properties)
                                                                         .withOSGIKillbillAPI(killbillAPI)
                                                                         .withDatabaseAccess(dao)
                                                                         .build();

        authorizeCall(account, payment, callContext, authorizeApi, creditCardPaymentProperties());

        final AdyenPaymentPluginApi refundApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
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
        assertEquals(result.getGatewayError(), "java.io.IOException: Invalid Http response");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = refundApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 2);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        assertNull(result.getGatewayErrorCode());
        assertNull(results.get(0).getGatewayError());
        assertEquals(results.get(1).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(1).getGatewayError(), "java.io.IOException: Invalid Http response");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenEmptyResponse() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "java.net.SocketException: Unexpected end of file from server");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "java.net.SocketException: Unexpected end of file from server");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenMalformedResponseChunk() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "java.io.IOException: Premature EOF");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "java.io.IOException: Premature EOF");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith401() throws Exception {
        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
                                                                      .build();

        final PaymentTransactionInfoPlugin result = doWithWireMock(new WithWireMock<PaymentTransactionInfoPlugin>() {
            @Override
            public PaymentTransactionInfoPlugin execute(final WireMockServer server) throws Exception {
                stubFor(post(urlEqualTo(ADYEN_PATH)).willReturn(
                        aResponse()
                                .withStatus(UNAUTHORIZED)));

                return authorizeCall(account, payment, callContext, pluginApi, creditCardPaymentProperties());
            }
        });
        assertEquals(result.getStatus(), PaymentPluginStatus.CANCELED);
        assertEquals(result.getGatewayError(), "o.a.c.t.http.HTTPException: HTTP response '401: Unauthorized' when communicating with " + wireMockUri(ADYEN_PATH));
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.CANCELED);
        assertEquals(results.get(0).getGatewayError(), "o.a.c.t.http.HTTPException: HTTP response '401: Unauthorized' when communicating with " + wireMockUri(ADYEN_PATH));
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith404() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "o.a.c.t.http.HTTPException: HTTP response '404: Not Found' when communicating with " + wireMockUri(ADYEN_PATH));
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "o.a.c.t.http.HTTPException: HTTP response '404: Not Found' when communicating with " + wireMockUri(ADYEN_PATH));
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith301() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "c.ctc.wstx.exc.WstxEOFException: Unexpected EOF in prolog\n" +
                                               " at [row,col {unknown-source}]: [1,0]");
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "c.ctc.wstx.exc.WstxEOFException: Unexpected EOF in prolog\n" +
                                                       " at [row,col {unknown-source}]: [1,0]");
        assertNull(result.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testAuthorizeAdyenRespondWith503() throws Exception {

        final Account account = defaultAccount();
        final OSGIKillbillAPI killbillAPI = TestUtils.buildOSGIKillbillAPI(account);
        final Payment payment = killBillPayment(account, killbillAPI);
        final AdyenCallContext callContext = newCallContext(account);

        final AdyenPaymentPluginApi pluginApi = AdyenPluginMockBuilder.newPlugin()
                                                                      .withAdyenProperty("org.killbill.billing.plugin.adyen.paymentUrl", wireMockUri(ADYEN_PATH))
                                                                      .withOSGIKillbillAPI(killbillAPI)
                                                                      .withDatabaseAccess(dao)
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
        assertEquals(result.getGatewayError(), "o.a.c.t.http.HTTPException: HTTP response '503: Service Unavailable' when communicating with " + wireMockUri(ADYEN_PATH));
        assertNull(result.getGatewayErrorCode());

        final List<PaymentTransactionInfoPlugin> results = pluginApi.getPaymentInfo(account.getId(), payment.getId(), ImmutableList.<PluginProperty>of(), callContext);
        assertEquals(results.size(), 1);
        assertEquals(results.get(0).getStatus(), PaymentPluginStatus.UNDEFINED);
        assertEquals(results.get(0).getGatewayError(), "o.a.c.t.http.HTTPException: HTTP response '503: Service Unavailable' when communicating with " + wireMockUri(ADYEN_PATH));
        assertNull(result.getGatewayErrorCode());
    }

    @DataProvider(name = "invalidCreditCardData")
    private Iterator<Object[]> invalidCreditCardDataDataProvider() {
        final List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] {invalidCreditCardData(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf("123")), "Expiry month should be between 1 and 12 inclusive Card"});
        data.add(new Object[] {invalidCreditCardData(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, String.valueOf("1234")), "CVC Declined"});
        return data.iterator();
    }

    private Iterable<PluginProperty> invalidCreditCardData(final String key, final String value) {
        final Map<String, String> invalidCreditCardData = new HashMap<String, String>(validCreditCardData());
        invalidCreditCardData.put(key, value);
        return toProperties(invalidCreditCardData);
    }

    private AdyenCallContext newCallContext(final Account account) {
        return new AdyenCallContext(DateTime.now(), account.getId(), UUID.randomUUID());
    }

    private Payment killBillPayment(final Account account, final OSGIKillbill killbillAPI) throws PaymentApiException {
        return TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency(), killbillAPI);
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
        Mockito.when(authorizationTransaction.getAmount()).thenReturn(BigDecimal.TEN);

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
