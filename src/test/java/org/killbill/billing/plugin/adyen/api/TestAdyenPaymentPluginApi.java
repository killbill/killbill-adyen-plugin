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

import javax.annotation.Nullable;

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
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.mockito.Mockito;
import org.testng.Assert;
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

public class TestAdyenPaymentPluginApi extends TestWithEmbeddedDBBase {

    private Payment payment;
    private CallContext context;
    private AdyenPaymentPluginApi adyenPaymentPluginApi;
    private Iterable<PluginProperty> propertiesWithCCInfo;
    private Iterable<PluginProperty> propertiesWithSepaInfo;
    private Iterable<PluginProperty> propertiesWithElvInfo;

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

        propertiesWithCCInfo = toProperties(ImmutableMap.<String, String>builder()
                .put(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, CC_TYPE)
                .put(AdyenPaymentPluginApi.PROPERTY_CC_LAST_NAME, "Dupont")
                .put(AdyenPaymentPluginApi.PROPERTY_CC_NUMBER, CC_NUMBER)
                .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH, String.valueOf(CC_EXPIRATION_MONTH))
                .put(AdyenPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR, String.valueOf(CC_EXPIRATION_YEAR))
                .put(AdyenPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE, CC_VERIFICATION_VALUE).build());

        propertiesWithSepaInfo = toProperties(ImmutableMap.of(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, DD_TYPE));
        propertiesWithElvInfo = toProperties(ImmutableMap.of(AdyenPaymentPluginApi.PROPERTY_CC_TYPE, ELV_TYPE));
    }

    @Test(groups = "slow")
    public void testAuthorizeAndMultipleCaptures() throws Exception {
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
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin, "Received");

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
        adyenPaymentPluginApi.addPaymentMethod(payment.getAccountId(), payment.getPaymentMethodId(), adyenPaymentMethodPluginElv(), true, propertiesWithSepaInfo, context);

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
        verifyPaymentTransactionInfoPlugin(authorizationTransaction, authorizationInfoPlugin, "Received");

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
    public void testHPP() throws Exception {
        final Map<String, String> customFieldsMap = ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_AMOUNT, "10",
                                                                                    AdyenPaymentPluginApi.PROPERTY_SERVER_URL, "http://killbill.io",
                                                                                    AdyenPaymentPluginApi.PROPERTY_CURRENCY, DEFAULT_CURRENCY.name(),
                                                                                    AdyenPaymentPluginApi.PROPERTY_COUNTRY, DEFAULT_COUNTRY);
        final Iterable<PluginProperty> customFields = PluginProperties.buildPluginProperties(customFieldsMap);
        final HostedPaymentPageFormDescriptor descriptor = adyenPaymentPluginApi.buildFormDescriptor(payment.getAccountId(), customFields, ImmutableList.<PluginProperty>of(), context);
        Assert.assertEquals(descriptor.getKbAccountId(), payment.getAccountId());
        Assert.assertEquals(descriptor.getFormMethod(), "GET");
        Assert.assertNotNull(descriptor.getFormUrl());

        // For manual testing
        System.out.println("Redirect to: " + descriptor.getFormUrl());
        System.out.flush();
    }
    private void verifyPaymentTransactionInfoPlugin(final PaymentTransaction paymentTransaction, final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin) {
        verifyPaymentTransactionInfoPlugin(paymentTransaction, paymentTransactionInfoPlugin, null);
    }

    private void verifyPaymentTransactionInfoPlugin(final PaymentTransaction paymentTransaction, final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin, final String alternateResponseForAuthorize) {
        Assert.assertEquals(paymentTransactionInfoPlugin.getKbPaymentId(), payment.getId());
        Assert.assertEquals(paymentTransactionInfoPlugin.getKbTransactionPaymentId(), paymentTransaction.getId());
        if (TransactionType.PURCHASE.equals(paymentTransaction.getTransactionType())) {
            Assert.assertEquals(paymentTransactionInfoPlugin.getTransactionType(), TransactionType.CAPTURE);
        } else {
            Assert.assertEquals(paymentTransactionInfoPlugin.getTransactionType(), paymentTransaction.getTransactionType());
        }
        if (TransactionType.VOID.equals(paymentTransaction.getTransactionType())) {
            Assert.assertNull(paymentTransactionInfoPlugin.getAmount());
            Assert.assertNull(paymentTransactionInfoPlugin.getAmount());
        } else {
            Assert.assertEquals(paymentTransactionInfoPlugin.getAmount(), paymentTransaction.getAmount());
            Assert.assertEquals(paymentTransactionInfoPlugin.getCurrency(), paymentTransaction.getCurrency());
        }
        Assert.assertNotNull(paymentTransactionInfoPlugin.getCreatedDate());
        Assert.assertNotNull(paymentTransactionInfoPlugin.getEffectiveDate());
        // Always pending if successful
        Assert.assertEquals(paymentTransactionInfoPlugin.getStatus(), PaymentPluginStatus.PENDING);

        final List<String> expectedGatewayErrors;
        switch (paymentTransaction.getTransactionType()) {
            case AUTHORIZE:
                expectedGatewayErrors = alternateResponseForAuthorize != null
                        ? ImmutableList.of("Authorised", alternateResponseForAuthorize)
                        : ImmutableList.of("Authorised");
                break;
            case CAPTURE:
            case PURCHASE:
                expectedGatewayErrors = ImmutableList.of("[capture-received]");
                break;
            case REFUND:
                expectedGatewayErrors = ImmutableList.of("[refund-received]");
                break;
            case VOID:
                expectedGatewayErrors = ImmutableList.of("[cancel-received]");
                break;
            default:
                expectedGatewayErrors = ImmutableList.of();
                break;
        }
        Assert.assertTrue(expectedGatewayErrors.contains(paymentTransactionInfoPlugin.getGatewayError()), paymentTransactionInfoPlugin.getGatewayError());

        Assert.assertNull(paymentTransactionInfoPlugin.getGatewayErrorCode());
        Assert.assertNotNull(paymentTransactionInfoPlugin.getFirstPaymentReferenceId());
        // NULL for subsequent transactions (modifications)
        //Assert.assertNotNull(paymentTransactionInfoPlugin.getSecondPaymentReferenceId());
        // No additional data for our simple scenarii
        Assert.assertTrue(paymentTransactionInfoPlugin.getProperties().isEmpty());
    }

    private Iterable<PluginProperty> toProperties(final Map<String, String> propertiesString) {
        return Iterables.transform(propertiesString.keySet(),
                                   new Function<String, PluginProperty>() {
                                       @Override
                                       public PluginProperty apply(@Nullable final String input) {
                                           return new PluginProperty(input, propertiesString.get(input), false);
                                       }
                                   });
    }

    private PaymentMethodPlugin adyenPaymentMethodPluginSepaDirectDebit() {
        final AdyenPaymentMethodsRecord record = new AdyenPaymentMethodsRecord();
        record.setKbPaymentMethodId(payment.getPaymentMethodId().toString());
        record.setIsDefault(AdyenDao.TRUE);
        record.setAdditionalData("{"
                + '"' + PROPERTY_DD_HOLDER_NAME + "\":\"" + DD_HOLDER_NAME + "\","
                + '"' + PROPERTY_DD_ACCOUNT_NUMBER + "\":\"" + DD_IBAN + "\","
                + '"' + PROPERTY_DD_BANK_IDENTIFIER_CODE + "\":\"" + DD_BIC + '"'
                + '}');
        return new AdyenPaymentMethodPlugin(record);
    }

    private PaymentMethodPlugin adyenPaymentMethodPluginElv() {
        final AdyenPaymentMethodsRecord record = new AdyenPaymentMethodsRecord();
        record.setKbPaymentMethodId(payment.getPaymentMethodId().toString());
        record.setIsDefault(AdyenDao.TRUE);
        record.setAdditionalData("{"
                + '"' + PROPERTY_DD_HOLDER_NAME + "\":\"" + ELV_HOLDER_NAME + "\","
                + '"' + PROPERTY_DD_ACCOUNT_NUMBER + "\":\"" + ELV_ACCOUNT_NUMBER + "\","
                + '"' + PROPERTY_DD_BANKLEITZAHL + "\":\"" + ELV_BLZ + '"'
                + '}');
        return new AdyenPaymentMethodPlugin(record);
    }

}
