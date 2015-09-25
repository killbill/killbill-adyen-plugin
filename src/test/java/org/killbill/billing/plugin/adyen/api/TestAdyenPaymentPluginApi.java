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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Strings;
import org.killbill.adyen.recurring.RecurringDetail;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.adyen.recurring.AdyenRecurringClient;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_ACCOUNT_NUMBER;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_BANK_IDENTIFIER_CODE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_BANKLEITZAHL;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_HOLDER_NAME;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;

public class TestAdyenPaymentPluginApi extends TestWithEmbeddedDBBase {

    private static final long SLEEP_IN_MILLIS_FOR_RECURRING_DETAIL = 3000L; // 3 Seconds

    private Payment payment;
    private CallContext context;
    private AdyenPaymentPluginApi adyenPaymentPluginApi;
    private AdyenRecurringClient adyenRecurringClient;
    private Iterable<PluginProperty> propertiesWithCCInfo;
    private Iterable<PluginProperty> propertiesWithSepaInfo;
    private Iterable<PluginProperty> propertiesWithElvInfo;
    private Map<String, String> propertiesForRecurring;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        final Clock clock = new DefaultClock();

        context = Mockito.mock(CallContext.class);
        Mockito.when(context.getTenantId()).thenReturn(UUID.randomUUID());

        final Account account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
        payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency());
        final OSGIKillbillAPI killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, null);

        final OSGIKillbillLogService logService = TestUtils.buildLogService();

        final OSGIConfigPropertiesService configPropertiesService = Mockito.mock(OSGIConfigPropertiesService.class);
        adyenPaymentPluginApi = new AdyenPaymentPluginApi(adyenConfigurationHandler, adyenHostedPaymentPageConfigurationHandler, killbillApi, configPropertiesService, logService, clock, dao);

        adyenRecurringClient = new AdyenRecurringClient(adyenConfigProperties);

        propertiesWithCCInfo = toProperties(ImmutableMap.<String, String>builder()
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                                                        .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE).build());

        propertiesWithSepaInfo = toProperties(ImmutableMap.of(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, DD_TYPE));
        propertiesWithElvInfo = toProperties(ImmutableMap.of(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, ELV_TYPE));
        final String customerId = UUID.randomUUID().toString();
        propertiesForRecurring = ImmutableMap.of(AdyenPaymentPluginApi.PROPERTY_CUSTOMER_ID, customerId,
                                                 AdyenPaymentPluginApi.PROPERTY_EMAIL, customerId + "0@example.com");
    }

    @Test(groups = "slow")
    public void testAuthorizeAndMultipleCaptures() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfo, context);
        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);
        final PaymentTransaction captureTransaction1 = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, DEFAULT_CURRENCY);
        final PaymentTransaction captureTransaction2 = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, DEFAULT_CURRENCY);

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
        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);
        final PaymentTransaction voidTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.VOID, DEFAULT_CURRENCY);

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

    @Test(groups = "slow", enabled = false, description = "Purchase is not yet supported")
    public void testPurchaseAndRefund() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginCC(), true, propertiesWithCCInfo, context);
        final PaymentTransaction purchaseTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, DEFAULT_CURRENCY);
        final PaymentTransaction refundTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.REFUND, DEFAULT_CURRENCY);

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

    @Test(groups = "slow")
    public void testAuthorizeAndMultipleCapturesSepaDirectDebit() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginSepaDirectDebit(), true, propertiesWithSepaInfo, context);

        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);
        final PaymentTransaction captureTransaction1 = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, DEFAULT_CURRENCY);
        final PaymentTransaction captureTransaction2 = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, DEFAULT_CURRENCY);

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
    public void testAuthorizeAndMultipleCapturesELV() throws Exception {
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginElv(), true, propertiesWithElvInfo, context);

        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);
        final PaymentTransaction captureTransaction1 = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, DEFAULT_CURRENCY);
        final PaymentTransaction captureTransaction2 = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, DEFAULT_CURRENCY);

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                                                                            payment.getId(),
                                                                                                            authorizationTransaction.getId(),
                                                                                                            payment.getPaymentMethodId(),
                                                                                                            authorizationTransaction.getAmount(),
                                                                                                            authorizationTransaction.getCurrency(),
                                                                                                            propertiesWithElvInfo,
                                                                                                            context);
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin, false);

        final PaymentTransactionInfoPlugin captureInfoPlugin1 = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                     payment.getId(),
                                                                                                     captureTransaction1.getId(),
                                                                                                     payment.getPaymentMethodId(),
                                                                                                     captureTransaction1.getAmount(),
                                                                                                     captureTransaction1.getCurrency(),
                                                                                                     propertiesWithElvInfo,
                                                                                                     context);
        verifyPaymentTransactionInfoPlugin(captureTransaction1, captureInfoPlugin1);

        final PaymentTransactionInfoPlugin captureInfoPlugin2 = adyenPaymentPluginApi.capturePayment(payment.getAccountId(),
                                                                                                     payment.getId(),
                                                                                                     captureTransaction2.getId(),
                                                                                                     payment.getPaymentMethodId(),
                                                                                                     captureTransaction2.getAmount(),
                                                                                                     captureTransaction2.getCurrency(),
                                                                                                     propertiesWithElvInfo,
                                                                                                     context);
        verifyPaymentTransactionInfoPlugin(captureTransaction2, captureInfoPlugin2);
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

        final PaymentTransaction authorizationTransaction1 = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);
        final PaymentTransaction authorizationTransaction2 = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);

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

        final PaymentTransaction authorizationTransaction1 = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);
        final PaymentTransaction authorizationTransaction2 = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, DEFAULT_CURRENCY);

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
    public void testHPP() throws Exception {
        final Map<String, String> customFieldsMap = ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_AMOUNT, "10",
                                                                                    AdyenPaymentPluginApi.PROPERTY_SERVER_URL, "http://killbill.io",
                                                                                    AdyenPaymentPluginApi.PROPERTY_CURRENCY, DEFAULT_CURRENCY.name(),
                                                                                    AdyenPaymentPluginApi.PROPERTY_COUNTRY, DEFAULT_COUNTRY);
        final Iterable<PluginProperty> customFields = PluginProperties.buildPluginProperties(customFieldsMap);
        final HostedPaymentPageFormDescriptor descriptor = adyenPaymentPluginApi.buildFormDescriptor(payment.getAccountId(), customFields, ImmutableList.<PluginProperty>of(), context);
        assertEquals(descriptor.getKbAccountId(), payment.getAccountId());
        assertEquals(descriptor.getFormMethod(), "GET");
        assertNotNull(descriptor.getFormUrl());

        // For manual testing
        System.out.println("Redirect to: " + descriptor.getFormUrl());
        System.out.flush();
    }

    private void verifyPaymentTransactionInfoPlugin(final PaymentTransaction paymentTransaction, final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin) {
        verifyPaymentTransactionInfoPlugin(paymentTransaction, paymentTransactionInfoPlugin, true);
    }

    /**
     * Verifies PaymentTransactionInfoPlugin.
     *
     * @param paymentTransaction The PaymentTransaction
     * @param paymentTransactionInfoPlugin The PaymentTransactionInfoPlugin
     * @param authorizedProcessed If {@code true} then the status for Authorize must be Processed, if {@code false} it could be Processed or Pending (e.g. for DirectDebit)
     */
    private void verifyPaymentTransactionInfoPlugin(final PaymentTransaction paymentTransaction, final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin, final boolean authorizedProcessed) {
        assertEquals(paymentTransactionInfoPlugin.getKbPaymentId(), payment.getId());
        assertEquals(paymentTransactionInfoPlugin.getKbTransactionPaymentId(), paymentTransaction.getId());
        if (TransactionType.PURCHASE.equals(paymentTransaction.getTransactionType())) {
            assertEquals(paymentTransactionInfoPlugin.getTransactionType(), TransactionType.CAPTURE);
        } else {
            assertEquals(paymentTransactionInfoPlugin.getTransactionType(), paymentTransaction.getTransactionType());
        }
        if (TransactionType.VOID.equals(paymentTransaction.getTransactionType())) {
            assertNull(paymentTransactionInfoPlugin.getAmount());
            assertNull(paymentTransactionInfoPlugin.getAmount());
        } else {
            assertEquals(paymentTransactionInfoPlugin.getAmount(), paymentTransaction.getAmount());
            assertEquals(paymentTransactionInfoPlugin.getCurrency(), paymentTransaction.getCurrency());
        }
        assertNotNull(paymentTransactionInfoPlugin.getCreatedDate());
        assertNotNull(paymentTransactionInfoPlugin.getEffectiveDate());

        final List<String> expectedGatewayErrors;
        final List<PaymentPluginStatus> expectedPaymentPluginStatus;
        switch (paymentTransaction.getTransactionType()) {
            case AUTHORIZE:
                expectedGatewayErrors = authorizedProcessed
                        ? ImmutableList.of("Authorised")
                        : ImmutableList.of("Authorised", "Received");
                expectedPaymentPluginStatus = authorizedProcessed
                        ? ImmutableList.of(PaymentPluginStatus.PROCESSED)
                        : ImmutableList.of(PaymentPluginStatus.PROCESSED, PaymentPluginStatus.PENDING);
                break;
            case CAPTURE:
            case PURCHASE:
                expectedGatewayErrors = ImmutableList.of("[capture-received]");
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
            case REFUND:
                expectedGatewayErrors = ImmutableList.of("[refund-received]");
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
            case VOID:
                expectedGatewayErrors = ImmutableList.of("[cancel-received]");
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
            default:
                expectedGatewayErrors = ImmutableList.of();
                expectedPaymentPluginStatus = ImmutableList.of(PaymentPluginStatus.PENDING);
                break;
        }
        assertTrue(expectedGatewayErrors.contains(paymentTransactionInfoPlugin.getGatewayError()), paymentTransactionInfoPlugin.getGatewayError());
        assertTrue(expectedPaymentPluginStatus.contains(paymentTransactionInfoPlugin.getStatus()), paymentTransactionInfoPlugin.getStatus().toString());

        assertNull(paymentTransactionInfoPlugin.getGatewayErrorCode());
        assertNotNull(paymentTransactionInfoPlugin.getFirstPaymentReferenceId());
        // NULL for subsequent transactions (modifications)
        //Assert.assertNotNull(paymentTransactionInfoPlugin.getSecondPaymentReferenceId());
        // No additional data for our simple scenarii
        assertTrue(paymentTransactionInfoPlugin.getProperties().isEmpty());
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

    private PaymentMethodPlugin adyenPaymentMethodPluginElv() {
        return adyenPaymentMethodPlugin(payment.getPaymentMethodId().toString(), "{"
                + '"' + PROPERTY_DD_HOLDER_NAME + "\":\"" + ELV_HOLDER_NAME + "\","
                + '"' + PROPERTY_DD_ACCOUNT_NUMBER + "\":\"" + ELV_ACCOUNT_NUMBER + "\","
                + '"' + PROPERTY_DD_BANKLEITZAHL + "\":\"" + ELV_BLZ + '"'
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
