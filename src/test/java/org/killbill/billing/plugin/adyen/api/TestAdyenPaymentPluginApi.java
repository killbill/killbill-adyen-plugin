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

package org.killbill.billing.plugin.adyen.api;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.killbill.adyen.recurring.RecurringDetail;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ning.http.util.UTF8UrlEncoder;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_ACCOUNT_NUMBER;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_BANK_IDENTIFIER_CODE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_HOLDER_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestAdyenPaymentPluginApi extends TestAdyenPaymentPluginApiBase {

    private static final long SLEEP_IN_MILLIS_FOR_RECURRING_DETAIL = 3000L; // 3 Seconds
    private static final int HTTP_200_OK = 200;
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    private Iterable<PluginProperty> propertiesWithCCInfo;
    private Iterable<PluginProperty> propertiesWith3DSInfo;
    private Iterable<PluginProperty> propertiesWithSepaInfo;
    private Map<String, String> propertiesForRecurring;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        super.setUp();

        propertiesWithCCInfo = toProperties(ImmutableMap.<String, String>builder()
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE).build());

        propertiesWith3DSInfo = toProperties(ImmutableMap.<String, String>builder()
                                                         .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                         .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Montblanc")
                                                         .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_3DS_NUMBER)
                                                         .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                         .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                         .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE)
                                                         .put(AdyenPaymentPluginApi.PROPERTY_USER_AGENT, "Java/1.8")
                                                         .put(AdyenPaymentPluginApi.PROPERTY_ACCEPT_HEADER, "application/json")
                                                         .put(AdyenPaymentPluginApi.PROPERTY_TERM_URL, "dummy://url")
                                                         .put(AdyenPaymentPluginApi.PROPERTY_THREE_D_THRESHOLD, "25000")
                                                         .build());

        propertiesWithSepaInfo = toProperties(ImmutableMap.<String, String>of());
        final String customerId = UUID.randomUUID().toString();
        propertiesForRecurring = ImmutableMap.of(AdyenPaymentPluginApi.PROPERTY_CUSTOMER_ID, customerId,
                                                 AdyenPaymentPluginApi.PROPERTY_EMAIL, customerId + "0@example.com");
    }

    @Test(groups = "slow")
    public void testPaymentMethodManagement() throws Exception {
        assertEquals(adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), false, ImmutableList.<PluginProperty>of(), context).size(), 0);
        assertEquals(adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), true, ImmutableList.<PluginProperty>of(), context).size(), 0);

        // The customerId passed during the addPaymentMethod call needs to match the one during the payment call
        final Iterable<PluginProperty> propertiesForAddPaymentMethod = toProperties(ImmutableMap.<String, String>builder()
                                                                                                .putAll(propertiesForRecurring)
                                                                                                .build());
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesForAddPaymentMethod, context);

        final List<PaymentMethodInfoPlugin> initialPaymentMethods = adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), false, ImmutableList.<PluginProperty>of(), context);
        assertEquals(initialPaymentMethods.size(), 1);
        assertEquals(initialPaymentMethods.get(0).getAccountId(), payment.getAccountId());
        assertEquals(initialPaymentMethods.get(0).getPaymentMethodId(), payment.getPaymentMethodId());
        assertNull(initialPaymentMethods.get(0).getExternalPaymentMethodId());
        assertEquals(adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), true, ImmutableList.<PluginProperty>of(), context), initialPaymentMethods);

        final Iterable<PluginProperty> propertiesWithCCForRecurring = toProperties(ImmutableMap.<String, String>builder()
                                                                                               .putAll(propertiesForRecurring)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_TYPE, "RECURRING")
                                                                                               .build());
        final PaymentTransaction authorizationTransaction1 = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransactionInfoPlugin authorizationInfoPlugin1 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction1.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction1.getAmount(),
                                                                                                             authorizationTransaction1.getCurrency(),
                                                                                                             propertiesWithCCForRecurring,
                                                                                                             context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction1, authorizationInfoPlugin1);

        // Sleep a few seconds to give Adyen's Test System time to process and create the RecurringDetails
        Thread.sleep(SLEEP_IN_MILLIS_FOR_RECURRING_DETAIL);

        // No change, unless refreshFromGateway is set
        assertEquals(adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), false, ImmutableList.<PluginProperty>of(), context), initialPaymentMethods);
        final List<PaymentMethodInfoPlugin> subsequentPaymentMethods = adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), true, ImmutableList.<PluginProperty>of(), context);
        assertEquals(subsequentPaymentMethods.size(), 1);
        assertEquals(subsequentPaymentMethods.get(0).getAccountId(), payment.getAccountId());
        assertEquals(subsequentPaymentMethods.get(0).getPaymentMethodId(), payment.getPaymentMethodId());
        // This points to the recurringDetailReference
        assertNotNull(subsequentPaymentMethods.get(0).getExternalPaymentMethodId());

        // Verify the token can be used for recurring payments
        final PaymentTransaction authorizationTransaction2 = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransactionInfoPlugin authorizationInfoPlugin2 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction2.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction2.getAmount(),
                                                                                                             authorizationTransaction2.getCurrency(),
                                                                                                             ImmutableList.<PluginProperty>of(),
                                                                                                             context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction2, authorizationInfoPlugin2);

        adyenPaymentPluginApi.deletePaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), ImmutableList.<PluginProperty>of(), context);

        assertEquals(adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), false, ImmutableList.<PluginProperty>of(), context).size(), 0);
        assertEquals(adyenPaymentPluginApi.getPaymentMethods(payment.getAccountId(), true, ImmutableList.<PluginProperty>of(), context).size(), 0);
    }

    @Test(groups = "slow")
    public void testAuthorizeAndMultipleCaptures() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfo, context);
        final PaymentTransaction authorizationTransaction = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransaction captureTransaction1 = buildPaymentTransaction(TransactionType.CAPTURE, new BigDecimal("5"));
        final PaymentTransaction captureTransaction2 = buildPaymentTransaction(TransactionType.CAPTURE, new BigDecimal("5"));

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                            payment.getId(),
                                                                                                            authorizationTransaction.getId(),
                                                                                                            payment.getPaymentMethodId(),
                                                                                                            authorizationTransaction.getAmount(),
                                                                                                            authorizationTransaction.getCurrency(),
                                                                                                            propertiesWithCCInfo,
                                                                                                            context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin);

        final PaymentTransactionInfoPlugin captureInfoPlugin1 = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                     payment.getId(),
                                                                                                     captureTransaction1.getId(),
                                                                                                     payment.getPaymentMethodId(),
                                                                                                     captureTransaction1.getAmount(),
                                                                                                     captureTransaction1.getCurrency(),
                                                                                                     propertiesWithCCInfo,
                                                                                                     context);
        verifyPaymentTransactionInfoPlugin(captureTransaction1, captureInfoPlugin1);

        final PaymentTransactionInfoPlugin captureInfoPlugin2 = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                     payment.getId(),
                                                                                                     captureTransaction2.getId(),
                                                                                                     payment.getPaymentMethodId(),
                                                                                                     captureTransaction2.getAmount(),
                                                                                                     captureTransaction2.getCurrency(),
                                                                                                     propertiesWithCCInfo,
                                                                                                     context);
        verifyPaymentTransactionInfoPlugin(captureTransaction2, captureInfoPlugin2);
    }

    @Test(groups = "slow")
    public void testAuthorizeAndVoid() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfo, context);
        final PaymentTransaction authorizationTransaction = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransaction voidTransaction = buildPaymentTransaction(TransactionType.VOID);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                            payment.getId(),
                                                                                                            authorizationTransaction.getId(),
                                                                                                            payment.getPaymentMethodId(),
                                                                                                            authorizationTransaction.getAmount(),
                                                                                                            authorizationTransaction.getCurrency(),
                                                                                                            propertiesWithCCInfo,
                                                                                                            context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin);

        final PaymentTransactionInfoPlugin voidInfoPlugin = adyenPaymentPluginApi.voidPayment(payment.getAccountId(),
                                                                                              payment.getId(),
                                                                                              voidTransaction.getId(),
                                                                                              payment.getPaymentMethodId(),
                                                                                              propertiesWithCCInfo,
                                                                                              context);
        verifyPaymentTransactionInfoPlugin(voidTransaction, voidInfoPlugin);
    }

    @Test(groups = "slow")
    public void testPurchaseAndRefund() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfo, context);
        final PaymentTransaction purchaseTransaction = buildPaymentTransaction(TransactionType.PURCHASE);
        final PaymentTransaction refundTransaction = buildPaymentTransaction(TransactionType.REFUND);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.purchasePayment(payment.getAccountId(),
                                                                                                           payment.getId(),
                                                                                                           purchaseTransaction.getId(),
                                                                                                           payment.getPaymentMethodId(),
                                                                                                           purchaseTransaction.getAmount(),
                                                                                                           purchaseTransaction.getCurrency(),
                                                                                                           propertiesWithCCInfo,
                                                                                                           context);
        verifyPaymentTransactionInfoPlugin(purchaseTransaction, authorizationInfoPlugin);

        final PaymentTransactionInfoPlugin refundInfoPlugin = adyenPaymentPluginApi.refundPayment(payment.getAccountId(),
                                                                                                  payment.getId(),
                                                                                                  refundTransaction.getId(),
                                                                                                  payment.getPaymentMethodId(),
                                                                                                  refundTransaction.getAmount(),
                                                                                                  refundTransaction.getCurrency(),
                                                                                                  propertiesWithCCInfo,
                                                                                                  context);
        verifyPaymentTransactionInfoPlugin(refundTransaction, refundInfoPlugin);
    }

    // Disabled by default since Card fund transfer (CFT) isn't enabled automatically on the sandbox
    @Test(groups = "slow", enabled = false)
    public void testCredit() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfo, context);
        final PaymentTransaction creditTransaction = buildPaymentTransaction(TransactionType.CREDIT);

        final PaymentTransactionInfoPlugin creditInfoPlugin = adyenPaymentPluginApi.creditPayment(payment.getAccountId(),
                                                                                                  payment.getId(),
                                                                                                  creditTransaction.getId(),
                                                                                                  payment.getPaymentMethodId(),
                                                                                                  creditTransaction.getAmount(),
                                                                                                  creditTransaction.getCurrency(),
                                                                                                  propertiesWithCCInfo,
                                                                                                  context);
        verifyPaymentTransactionInfoPlugin(creditTransaction, creditInfoPlugin);
    }

    @Test(groups = "slow")
    public void testAuthorizeAndMultipleCapturesSepaDirectDebit() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginSepaDirectDebit(), true, propertiesWithSepaInfo, context);

        final PaymentTransaction authorizationTransaction = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransaction captureTransaction1 = buildPaymentTransaction(TransactionType.CAPTURE);
        final PaymentTransaction captureTransaction2 = buildPaymentTransaction(TransactionType.CAPTURE);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                            payment.getId(),
                                                                                                            authorizationTransaction.getId(),
                                                                                                            payment.getPaymentMethodId(),
                                                                                                            authorizationTransaction.getAmount(),
                                                                                                            authorizationTransaction.getCurrency(),
                                                                                                            propertiesWithSepaInfo,
                                                                                                            context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin, false);

        final PaymentTransactionInfoPlugin captureInfoPlugin1 = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                     payment.getId(),
                                                                                                     captureTransaction1.getId(),
                                                                                                     payment.getPaymentMethodId(),
                                                                                                     captureTransaction1.getAmount(),
                                                                                                     captureTransaction1.getCurrency(),
                                                                                                     propertiesWithSepaInfo,
                                                                                                     context);
        verifyPaymentTransactionInfoPlugin(captureTransaction1, captureInfoPlugin1);

        final PaymentTransactionInfoPlugin captureInfoPlugin2 = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                     payment.getId(),
                                                                                                     captureTransaction2.getId(),
                                                                                                     payment.getPaymentMethodId(),
                                                                                                     captureTransaction2.getAmount(),
                                                                                                     captureTransaction2.getCurrency(),
                                                                                                     propertiesWithSepaInfo,
                                                                                                     context);
        verifyPaymentTransactionInfoPlugin(captureTransaction2, captureInfoPlugin2);
    }

    @Test(groups = "slow")
    public void testAuthorizeFailingInvalidCVV() throws Exception {
        final Iterable<PluginProperty> propertiesWithCCInfoWrongCVV = toProperties(ImmutableMap.<String, String>builder()
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, "1234")
                                                                                               .build());

        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfoWrongCVV, context);

        final PaymentTransaction authorizationTransaction = buildPaymentTransaction(TransactionType.AUTHORIZE);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                            payment.getId(),
                                                                                                            authorizationTransaction.getId(),
                                                                                                            payment.getPaymentMethodId(),
                                                                                                            authorizationTransaction.getAmount(),
                                                                                                            authorizationTransaction.getCurrency(),
                                                                                                            propertiesWithCCInfoWrongCVV,
                                                                                                            context);

        assertEquals(authorizationInfoPlugin.getGatewayError(), "CVC Declined");
        final List<PaymentTransactionInfoPlugin> fromDBList = adyenPaymentPluginApi.getPaymentInfo(payment.getAccountId(), payment.getId(), ImmutableList.<PluginProperty>of(), context);
        assertFalse(fromDBList.isEmpty());
        assertEquals(fromDBList.get(0).getGatewayError(), "CVC Declined");
    }

    @Test(groups = "slow")
    public void testAuthorizeRecurringDetailRecurring() throws Exception {
        final Iterable<PluginProperty> propertiesWithCCForRecurring = toProperties(ImmutableMap.<String, String>builder()
                                                                                               .putAll(propertiesForRecurring)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_TYPE, "RECURRING")
                                                                                               .build());

        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCForRecurring, context);

        final PaymentTransaction authorizationTransaction1 = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransaction authorizationTransaction2 = buildPaymentTransaction(TransactionType.AUTHORIZE);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin1 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction1.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction1.getAmount(),
                                                                                                             authorizationTransaction1.getCurrency(),
                                                                                                             propertiesWithCCForRecurring,
                                                                                                             context);

        verifyPaymentTransactionInfoPlugin(authorizationTransaction1, authorizationInfoPlugin1);

        // Sleep a few seconds to give Adyen's Test System time to process and create the RecurringDetails
        Thread.sleep(SLEEP_IN_MILLIS_FOR_RECURRING_DETAIL);

        final List<RecurringDetail> recurringDetailList = adyenRecurringClient.getRecurringDetailList(DEFAULT_COUNTRY,
                                                                                                      propertiesForRecurring.get(AdyenPaymentPluginApi.PROPERTY_CUSTOMER_ID),
                                                                                                      adyenConfigProperties.getMerchantAccount(DEFAULT_COUNTRY),
                                                                                                      "RECURRING");

        if (recurringDetailList.isEmpty()) {
            fail("No recurring details for " + propertiesForRecurring.get(AdyenPaymentPluginApi.PROPERTY_CUSTOMER_ID));
        }

        final Iterable<PluginProperty> propertiesWithRecurringDetailInfo = toProperties(ImmutableMap.<String, String>builder()
                                                                                                    .putAll(propertiesForRecurring)
                                                                                                    .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_DETAIL_ID, recurringDetailList.get(0).getRecurringDetailReference())
                                                                                                    .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_TYPE, "RECURRING")
                                                                                                    .build());

        // TODO: Doing the second Auth with the same Mocks (Payment, killBillApi, ...) is a bit hacky, but the SOAP request to Adyen Test goes out properly nevertheless, so we kept it like this for now
        final PaymentTransactionInfoPlugin authorizationInfoPlugin2 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction2.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction2.getAmount(),
                                                                                                             authorizationTransaction2.getCurrency(),
                                                                                                             propertiesWithRecurringDetailInfo,
                                                                                                             context);

        verifyPaymentTransactionInfoPlugin(authorizationTransaction2, authorizationInfoPlugin2);
    }

    @Test(groups = "slow")
    public void testAuthorizeRecurringDetailOneClick() throws Exception {
        final Iterable<PluginProperty> propertiesWithCCForRecurring = toProperties(ImmutableMap.<String, String>builder()
                                                                                               .putAll(propertiesForRecurring)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE)
                                                                                               .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_TYPE, "ONECLICK")
                                                                                               .build());

        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCForRecurring, context);

        final PaymentTransaction authorizationTransaction1 = buildPaymentTransaction(TransactionType.AUTHORIZE);
        final PaymentTransaction authorizationTransaction2 = buildPaymentTransaction(TransactionType.AUTHORIZE);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin1 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction1.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction1.getAmount(),
                                                                                                             authorizationTransaction1.getCurrency(),
                                                                                                             propertiesWithCCForRecurring,
                                                                                                             context);

        verifyPaymentTransactionInfoPlugin(authorizationTransaction1, authorizationInfoPlugin1);

        // Sleep a few seconds to give Adyen's Test System time to process and create the RecurringDetails
        Thread.sleep(SLEEP_IN_MILLIS_FOR_RECURRING_DETAIL);

        final List<RecurringDetail> recurringDetailList = adyenRecurringClient.getRecurringDetailList(DEFAULT_COUNTRY,
                                                                                                      propertiesForRecurring.get(AdyenPaymentPluginApi.PROPERTY_CUSTOMER_ID),
                                                                                                      adyenConfigProperties.getMerchantAccount(DEFAULT_COUNTRY),
                                                                                                      "ONECLICK");

        if (recurringDetailList.isEmpty()) {
            fail("No recurring details for " + propertiesForRecurring.get(AdyenPaymentPluginApi.PROPERTY_CUSTOMER_ID));
        }

        final Iterable<PluginProperty> propertiesWithRecurringDetailInfo = toProperties(ImmutableMap.<String, String>builder()
                                                                                                    .putAll(propertiesForRecurring)
                                                                                                    .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_DETAIL_ID, recurringDetailList.get(0).getRecurringDetailReference())
                                                                                                    .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE)
                                                                                                    .put(AdyenPaymentPluginApi.PROPERTY_RECURRING_TYPE, "ONECLICK")
                                                                                                    .build());

        // TODO: Doing the second Auth with the same Mocks (Payment, killBillApi, ...) is a bit hacky, but the SOAP request to Adyen Test goes out properly nevertheless, so we kept it like this for now
        final PaymentTransactionInfoPlugin authorizationInfoPlugin2 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction2.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction2.getAmount(),
                                                                                                             authorizationTransaction2.getCurrency(),
                                                                                                             propertiesWithRecurringDetailInfo,
                                                                                                             context);

        verifyPaymentTransactionInfoPlugin(authorizationTransaction2, authorizationInfoPlugin2);
    }

    @Test(groups = "slow")
    public void testAuthorizeWithContinuousAuthentication() throws Exception {

        final Iterable<PluginProperty> contAuthCcProperties = toProperties(ImmutableMap.<String, String>builder()
                                                                                       .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                                                       .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                                                       .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                                                       .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                                                       .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                                                       .put(AdyenPaymentPluginApi.PROPERTY_CONTINUOUS_AUTHENTICATION, "true").build());
        final PaymentTransaction authorizationTransaction = buildPaymentTransaction(TransactionType.AUTHORIZE);

        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, contAuthCcProperties, context);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                            payment.getId(),
                                                                                                            authorizationTransaction.getId(),
                                                                                                            payment.getPaymentMethodId(),
                                                                                                            authorizationTransaction.getAmount(),
                                                                                                            authorizationTransaction.getCurrency(),
                                                                                                            contAuthCcProperties,
                                                                                                            context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin);
    }

    @Test(groups = "slow")
    public void testAuthorizeAndComplete3DSecure() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWith3DSInfo, context);
        final PaymentTransaction authorizationTransaction = buildPaymentTransaction(TransactionType.AUTHORIZE, new BigDecimal("1000"));
        final PaymentTransaction captureTransaction = buildPaymentTransaction(TransactionType.CAPTURE, new BigDecimal("1000"));

        final PaymentTransactionInfoPlugin authorizationInfoPlugin1 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction.getAmount(),
                                                                                                             authorizationTransaction.getCurrency(),
                                                                                                             propertiesWith3DSInfo,
                                                                                                             context);

        assertEquals(authorizationInfoPlugin1.getGatewayErrorCode(), "RedirectShopper");
        assertEquals(authorizationInfoPlugin1.getStatus(), PaymentPluginStatus.PENDING);
        final URL issuerUrl = new URL(PluginProperties.findPluginPropertyValue("issuerUrl", authorizationInfoPlugin1.getProperties()));
        final String md = PluginProperties.findPluginPropertyValue("MD", authorizationInfoPlugin1.getProperties());
        final String paReq = PluginProperties.findPluginPropertyValue("PaReq", authorizationInfoPlugin1.getProperties());
        final String termUrl = PluginProperties.findPluginPropertyValue("TermUrl", authorizationInfoPlugin1.getProperties());

        final String responseHTML = given().log().all()
                                           .contentType(ContentType.URLENC)
                                           .accept(ContentType.HTML)
                                           .formParam("MD", md)
                                           .formParam("PaReq", paReq)
                                           .formParam("TermUrl", termUrl)
                                           .post(issuerUrl)
                                           .then().log().all()
                                           .statusCode(HTTP_200_OK)
                                           .extract().asString();

        final Map<String, String> formParams = extractForm(responseHTML);
        assertFalse(formParams.isEmpty(), "No FORM found in HTML response");

        final String formAction = rewriteFormURL(issuerUrl, formParams.remove("formAction"));
        formParams.put("username", "user");
        formParams.put("password", "password");

        final String redirectHTML = given().log().all()
                                           .contentType(ContentType.URLENC)
                                           .accept(ContentType.HTML)
                                           .formParams(formParams)
                                           .post(formAction)
                                           .then().log().all()
                                           .statusCode(HTTP_200_OK)
                                           .extract().asString();

        final Map<String, String> redirectFormParams = extractForm(redirectHTML);
        assertFalse(redirectFormParams.isEmpty(), "No FORM found in redirect HTML response");
        assertEquals(termUrl, redirectFormParams.remove("formAction"));
        // simulate url encoding that happens in the KillBill Client
        redirectFormParams.put("MD", UTF8UrlEncoder.encode(redirectFormParams.get("MD")));
        redirectFormParams.put("PaRes", UTF8UrlEncoder.encode(redirectFormParams.get("PaRes")));

        final List<PluginProperty> propertiesWithCompleteParams = PluginProperties.buildPluginProperties(redirectFormParams);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin2 = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                             payment.getId(),
                                                                                                             authorizationTransaction.getId(),
                                                                                                             payment.getPaymentMethodId(),
                                                                                                             authorizationTransaction.getAmount(),
                                                                                                             authorizationTransaction.getCurrency(),
                                                                                                             propertiesWithCompleteParams,
                                                                                                             context);

        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin2);
        assertEquals(authorizationInfoPlugin2.getFirstPaymentReferenceId(), authorizationInfoPlugin1.getFirstPaymentReferenceId());

        final PaymentTransactionInfoPlugin captureInfoPlugin = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                    payment.getId(),
                                                                                                    captureTransaction.getId(),
                                                                                                    payment.getPaymentMethodId(),
                                                                                                    captureTransaction.getAmount(),
                                                                                                    captureTransaction.getCurrency(),
                                                                                                    authorizationInfoPlugin2.getProperties(),
                                                                                                    context);

        verifyPaymentTransactionInfoPlugin(captureTransaction, captureInfoPlugin);

    }

    private Map<String, String> extractForm(final String html) {
        final Map<String, String> fields = new HashMap<String, String>();
        final Document doc = Jsoup.parse(html);
        final Elements forms = doc.getElementsByTag("form");
        for (final Element form : forms) {
            if ("post".equalsIgnoreCase(form.attr("method"))) {
                fields.put("formAction", form.attr("action"));
                final Elements inputs = form.getElementsByTag("input");
                for (final Element input : inputs) {
                    final String value = input.val();
                    if (value != null && !value.isEmpty() && !"Submit".equalsIgnoreCase(value)) {
                        fields.put(input.attr("name"), value);
                    }
                }
                return fields;
            }
        }
        return Collections.emptyMap();
    }

    private String rewriteFormURL(final URL issuerUrl, final String formAction) {
        if (formAction.startsWith("http")) {
            return formAction;
        } else {
            return issuerUrl.getProtocol() + "://" + issuerUrl.getHost() + (issuerUrl.getPort() != HTTP_PORT && issuerUrl.getPort() != HTTPS_PORT ? ":" + issuerUrl.getPort() : "") + formAction;
        }
    }

    private void verifyPaymentTransactionInfoPlugin(final PaymentTransaction paymentTransaction, final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin) throws PaymentPluginApiException {
        verifyPaymentTransactionInfoPlugin(paymentTransaction, paymentTransactionInfoPlugin, true);

        // Verify we can fetch the details
        final List<PaymentTransactionInfoPlugin> paymentTransactionInfoPlugins = adyenPaymentPluginApi.getPaymentInfo(payment.getAccountId(), paymentTransactionInfoPlugin.getKbPaymentId(), ImmutableList.<PluginProperty>of(), context);
        final PaymentTransactionInfoPlugin paymentTransactionInfoPluginFetched = Iterables.<PaymentTransactionInfoPlugin>find(Lists.<PaymentTransactionInfoPlugin>reverse(paymentTransactionInfoPlugins),
                                                                                                                              new Predicate<PaymentTransactionInfoPlugin>() {
                                                                                                                                  @Override
                                                                                                                                  public boolean apply(final PaymentTransactionInfoPlugin input) {
                                                                                                                                      return input.getKbTransactionPaymentId().equals(paymentTransaction.getId());
                                                                                                                                  }
                                                                                                                              });
        verifyPaymentTransactionInfoPlugin(paymentTransaction, paymentTransactionInfoPluginFetched, true);
    }

    /**
     * Verifies PaymentTransactionInfoPlugin.
     *
     * @param paymentTransaction           The PaymentTransaction
     * @param paymentTransactionInfoPlugin The PaymentTransactionInfoPlugin
     * @param authorizedProcessed          If {@code true} then the status for Authorize must be Processed, if {@code false} it could be Processed or Pending (e.g. for DirectDebit)
     */
    private void verifyPaymentTransactionInfoPlugin(final PaymentTransaction paymentTransaction, final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin, final boolean authorizedProcessed) {
        assertEquals(paymentTransactionInfoPlugin.getKbPaymentId(), payment.getId());
        assertEquals(paymentTransactionInfoPlugin.getKbTransactionPaymentId(), paymentTransaction.getId());
        assertEquals(paymentTransactionInfoPlugin.getTransactionType(), paymentTransaction.getTransactionType());
        if (TransactionType.VOID.equals(paymentTransaction.getTransactionType())) {
            assertNull(paymentTransactionInfoPlugin.getAmount());
            assertNull(paymentTransactionInfoPlugin.getCurrency());
        } else {
            assertEquals(paymentTransactionInfoPlugin.getAmount().compareTo(paymentTransaction.getAmount()), 0);
            assertEquals(paymentTransactionInfoPlugin.getCurrency(), paymentTransaction.getCurrency());
        }
        assertNotNull(paymentTransactionInfoPlugin.getCreatedDate());
        assertNotNull(paymentTransactionInfoPlugin.getEffectiveDate());

        final List<String> expectedGatewayErrorCodes;
        final List<PaymentPluginStatus> expectedPaymentPluginStatus;
        switch (paymentTransaction.getTransactionType()) {
            case PURCHASE:
            case AUTHORIZE:
                expectedGatewayErrorCodes = authorizedProcessed
                                        ? ImmutableList.of("Authorised")
                                        : ImmutableList.of("Authorised", "Received");
                expectedPaymentPluginStatus = authorizedProcessed
                                              ? ImmutableList.of(PaymentPluginStatus.PROCESSED)
                                              : ImmutableList.of(PaymentPluginStatus.PROCESSED, PaymentPluginStatus.PENDING);
                break;
            case CAPTURE:
                expectedGatewayErrorCodes = ImmutableList.of("[capture-received]");
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
            case REFUND:
                expectedGatewayErrorCodes = ImmutableList.of("[refund-received]");
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
            case VOID:
                expectedGatewayErrorCodes = ImmutableList.of("[cancel-received]");
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
            default:
                expectedGatewayErrorCodes = ImmutableList.of();
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
        }
        assertTrue(expectedGatewayErrorCodes.contains(paymentTransactionInfoPlugin.getGatewayErrorCode()), "was: " + paymentTransactionInfoPlugin.getGatewayErrorCode());
        assertTrue(expectedPaymentPluginStatus.contains(paymentTransactionInfoPlugin.getStatus()), "was: " + paymentTransactionInfoPlugin.getStatus());

        assertNull(paymentTransactionInfoPlugin.getGatewayError());
        assertNotNull(paymentTransactionInfoPlugin.getFirstPaymentReferenceId());
        // NULL for subsequent transactions (modifications)
        //Assert.assertNotNull(paymentTransactionInfoPlugin.getSecondPaymentReferenceId());
    }

    private Iterable<PluginProperty> toProperties(final Map<String, String> propertiesString) {
        return Iterables.transform(propertiesString.entrySet(),
                                   new Function<Map.Entry<String, String>, PluginProperty>() {
                                       @Override
                                       public PluginProperty apply(final Map.Entry<String, String> input) {
                                           return new PluginProperty(input.getKey(), input.getValue(), false);
                                       }
                                   });
    }

    private PaymentMethodPlugin adyenPaymentMethodPluginCC() {
        return adyenPaymentMethodPlugin(payment.getPaymentMethodId().toString(), null);
    }

    private PaymentMethodPlugin adyenPaymentMethodPluginSepaDirectDebit() {
        return adyenPaymentMethodPlugin(payment.getPaymentMethodId().toString(), "{"
                                                                                 + '"' + PROPERTY_DD_HOLDER_NAME + "\":\"" + DD_HOLDER_NAME + "\","
                                                                                 + '"' + PROPERTY_DD_ACCOUNT_NUMBER + "\":\"" + DD_IBAN + "\","
                                                                                 + '"' + PROPERTY_DD_BANK_IDENTIFIER_CODE + "\":\"" + DD_BIC + '"'
                                                                                 + '}');
    }

    private static PaymentMethodPlugin adyenPaymentMethodPlugin(final String paymentMethodId, final String additionalData) {
        final AdyenPaymentMethodsRecord record = new AdyenPaymentMethodsRecord();
        record.setKbPaymentMethodId(paymentMethodId);
        record.setIsDefault(AdyenDao.TRUE);
        if (!Strings.isNullOrEmpty(additionalData)) {
            record.setAdditionalData(additionalData);
        }
        return new AdyenPaymentMethodPlugin(record);
    }

}
