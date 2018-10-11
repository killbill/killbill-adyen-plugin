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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.Period;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_IP;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestAdyenPaymentPluginApiHPP extends TestAdyenPaymentPluginApiBase {

    private String paymentTransactionExternalKey;
    private String pspReference;

    @Override
    @BeforeMethod(groups = "integration")
    public void setUpRemote() throws Exception {
        super.setUpRemote();

        paymentTransactionExternalKey = UUID.randomUUID().toString();
        pspReference = UUID.randomUUID().toString();
    }

    @Test(groups = "integration")
    public void testHPPNoPendingPayment() throws Exception {
        triggerBuildFormDescriptor(ImmutableMap.<String, String>of(), null);

        processHPPNotification();

        verifyPayment(TransactionType.AUTHORIZE);
    }

    @Test(groups = "integration")
    public void testHPPWithPendingPurchase() throws Exception {
        // Trigger buildFormDescriptor (and create a PENDING purchase)
        triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true"), TransactionType.PURCHASE);

        processHPPNotification();

        verifyPayment(TransactionType.PURCHASE);
    }

    @Test(groups = "integration")
    public void testHPPWithPendingAuthorization() throws Exception {
        // Trigger buildFormDescriptor (and create a PENDING authorization)
        triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                   AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                   TransactionType.AUTHORIZE);

        processHPPNotification();

        verifyPayment(TransactionType.AUTHORIZE);
    }

    @Test(groups = "integration")
    public void testAuthorizeAndExpireHppWithoutCompletion() throws Exception {
        Payment payment = triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                   AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                   TransactionType.AUTHORIZE);
        Period expirationPeriod = adyenConfigProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod().minusMinutes(1);
        clock.setDeltaFromReality(expirationPeriod.toStandardDuration().getMillis());

        List<PaymentTransactionInfoPlugin> expiredPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(),
                                                                                                                   payment.getId(),
                                                                                                                   ImmutableList.<PluginProperty>of(),
                                                                                                                   context);
        assertEquals(expiredPaymentTransactions.size(), 1);
        final PaymentTransactionInfoPlugin pendingTrx = expiredPaymentTransactions.get(0);
        assertEquals(pendingTrx.getTransactionType(), TransactionType.AUTHORIZE);
        assertEquals(pendingTrx.getStatus(), PaymentPluginStatus.PENDING);

        expirationPeriod = adyenConfigProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod().plusMinutes(1);
        clock.setDeltaFromReality(expirationPeriod.toStandardDuration().getMillis());

        expiredPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(),
                                                                          payment.getId(),
                                                                          ImmutableList.<PluginProperty>of(),
                                                                          context);
        assertEquals(expiredPaymentTransactions.size(), 1);
        final PaymentTransactionInfoPlugin canceledTransaction = expiredPaymentTransactions.get(0);
        assertEquals(canceledTransaction.getTransactionType(), TransactionType.AUTHORIZE);
        assertEquals(canceledTransaction.getStatus(), PaymentPluginStatus.CANCELED);
    }

    @Test(groups = "integration")
    public void testAuthorizeAndExpireHppWithompletition() throws Exception {
        Payment payment = triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                                     AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                                     TransactionType.AUTHORIZE);
        adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                               payment.getId(),
                                               payment.getTransactions().get(0).getId(),
                                               payment.getPaymentMethodId(),
                                               payment.getAuthAmount(),
                                               payment.getCurrency(),
                                               ImmutableList.of(),
                                               context);
        Period expirationPeriod = adyenConfigProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod().plusMinutes(1);
        clock.setDeltaFromReality(expirationPeriod.toStandardDuration().getMillis());

        List<PaymentTransactionInfoPlugin> paymentTransactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(),
                                                                                                             payment.getId(),
                                                                                                             ImmutableList.<PluginProperty>of(),
                                                                                                             context);
        assertEquals(paymentTransactions.size(), 1);
        final PaymentTransactionInfoPlugin pendingTrx = paymentTransactions.get(0);
        assertEquals(pendingTrx.getTransactionType(), TransactionType.AUTHORIZE);
        assertEquals(pendingTrx.getStatus(), PaymentPluginStatus.PENDING);

        expirationPeriod = adyenConfigProperties.getPendingPaymentExpirationPeriod(null).plusMinutes(1);
        clock.setDeltaFromReality(expirationPeriod.toStandardDuration().getMillis());
        List<PaymentTransactionInfoPlugin> expiredPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(),
                                                                                                             payment.getId(),
                                                                                                             ImmutableList.<PluginProperty>of(),
                                                                                                             context);
        assertEquals(expiredPaymentTransactions.size(), 1);
        final PaymentTransactionInfoPlugin canceledTrx = expiredPaymentTransactions.get(0);
        assertEquals(canceledTrx.getTransactionType(), TransactionType.AUTHORIZE);
        assertEquals(canceledTrx.getStatus(), PaymentPluginStatus.CANCELED);
    }

    @Test(groups = "integration")
    public void testCancelExpiredPayment() throws Exception {
        final Payment payment = triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                                           AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                                           TransactionType.AUTHORIZE);
        final Period expirationPeriod = adyenConfigProperties.getPendingPaymentExpirationPeriod(null);
        assertEquals(expirationPeriod.toString(), "P3D");

        final Period preExpirationPeriod = expirationPeriod.minusMinutes(1);
        clock.setDeltaFromReality(preExpirationPeriod.toStandardDuration().getMillis());
        assertEquals(adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context).get(0).getStatus(), PaymentPluginStatus.PENDING);

        final Period postExpirationPeriod = expirationPeriod.plusMinutes(1);
        clock.setDeltaFromReality(postExpirationPeriod.toStandardDuration().getMillis());

        final List<PaymentTransactionInfoPlugin> transactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context);
        final PaymentTransactionInfoPlugin canceledTransaction = transactions.get(0);
        assertEquals(canceledTransaction.getStatus(), PaymentPluginStatus.CANCELED);

        final PluginProperty updateMessage = PluginProperties.findPluginProperties("message", canceledTransaction.getProperties()).iterator().next();
        assertEquals(updateMessage.getValue(), "Payment Expired - Cancelled by Janitor");
    }

    @Test(groups = "integration")
    public void testCancelExpiredPayPalPayment() throws Exception {
        final Payment payment = triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                                           AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                                           TransactionType.AUTHORIZE);
        dao.updateResponse(payment.getTransactions().get(0).getId(), PaymentServiceProviderResult.PENDING, ImmutableList.<PluginProperty>of(new PluginProperty("paymentMethod", "paypal", false)), context.getTenantId());

        final Period expirationPeriod = adyenConfigProperties.getPendingPaymentExpirationPeriod("paypal");
        assertEquals(expirationPeriod.toString(), "P1D");

        final Period preExpirationPeriod = expirationPeriod.minusMinutes(1);
        clock.setDeltaFromReality(preExpirationPeriod.toStandardDuration().getMillis());
        assertEquals(adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context).get(0).getStatus(), PaymentPluginStatus.PENDING);

        final Period postExpirationPeriod = expirationPeriod.plusMinutes(1);
        clock.setDeltaFromReality(postExpirationPeriod.toStandardDuration().getMillis());

        final List<PaymentTransactionInfoPlugin> transactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context);
        final PaymentTransactionInfoPlugin canceledTransaction = transactions.get(0);
        assertEquals(canceledTransaction.getStatus(), PaymentPluginStatus.CANCELED);

        final PluginProperty updateMessage = PluginProperties.findPluginProperties("message", canceledTransaction.getProperties()).iterator().next();
        assertEquals(updateMessage.getValue(), "Payment Expired - Cancelled by Janitor");
    }

    @Test(groups = "integration")
    public void testCancelExpiredPayPalPaymentNoNotification() throws Exception {
        final Payment payment = triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                                           AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true",
                                                                                           AdyenPaymentPluginApi.PROPERTY_BRAND_CODE, "paypal"),
                                                           TransactionType.AUTHORIZE);

        final Period expirationPeriod = adyenConfigProperties.getPendingPaymentExpirationPeriod("paypal");
        assertEquals(expirationPeriod.toString(), "P1D");

        final Period preExpirationPeriod = expirationPeriod.minusMinutes(1);
        clock.setDeltaFromReality(preExpirationPeriod.toStandardDuration().getMillis());
        assertEquals(adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context).get(0).getStatus(), PaymentPluginStatus.PENDING);

        final Period postExpirationPeriod = expirationPeriod.plusMinutes(1);
        clock.setDeltaFromReality(postExpirationPeriod.toStandardDuration().getMillis());

        final List<PaymentTransactionInfoPlugin> transactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context);
        final PaymentTransactionInfoPlugin canceledTransaction = transactions.get(0);
        assertEquals(canceledTransaction.getStatus(), PaymentPluginStatus.CANCELED);

        final PluginProperty updateMessage = PluginProperties.findPluginProperties("message", canceledTransaction.getProperties()).iterator().next();
        assertEquals(updateMessage.getValue(), "Payment Expired - Cancelled by Janitor");
    }

    @Test(groups = "integration")
    public void testCancelExpiredBoletoPayment() throws Exception {
        final Payment payment = triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                                           AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                                           TransactionType.AUTHORIZE);
        dao.updateResponse(payment.getTransactions().get(0).getId(), PaymentServiceProviderResult.PENDING, ImmutableList.<PluginProperty>of(new PluginProperty("paymentMethod", "boletobancario_santander", false)), context.getTenantId());

        final Period expirationPeriod = adyenConfigProperties.getPendingPaymentExpirationPeriod("boletobancario_santander");
        assertEquals(expirationPeriod.toString(), "P7D");

        final Period preExpirationPeriod = expirationPeriod.minusMinutes(1);
        clock.setDeltaFromReality(preExpirationPeriod.toStandardDuration().getMillis());
        assertEquals(adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context).get(0).getStatus(), PaymentPluginStatus.PENDING);

        final Period postExpirationPeriod = expirationPeriod.plusMinutes(1);
        clock.setDeltaFromReality(postExpirationPeriod.toStandardDuration().getMillis());

        final List<PaymentTransactionInfoPlugin> transactions = adyenPaymentPluginApi.getPaymentInfo(account.getId(), payment.getId(), Collections.<PluginProperty>emptyList(), context);
        final PaymentTransactionInfoPlugin canceledTransaction = transactions.get(0);
        assertEquals(canceledTransaction.getStatus(), PaymentPluginStatus.CANCELED);

        final PluginProperty updateMessage = PluginProperties.findPluginProperties("message", canceledTransaction.getProperties()).iterator().next();
        assertEquals(updateMessage.getValue(), "Payment Expired - Cancelled by Janitor");
    }

    private Payment triggerBuildFormDescriptor(final Map<String, String> extraProperties, @Nullable final TransactionType transactionType) throws PaymentPluginApiException, SQLException, PaymentApiException {
        assertNull(dao.getHppRequest(paymentTransactionExternalKey));
        assertTrue(killbillApi.getPaymentApi().getAccountPayments(account.getId(), false, false, ImmutableList.<PluginProperty>of(), context).isEmpty());

        final Builder<String, String> propsBuilder = new Builder<String, String>();
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_AMOUNT, "10");
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_PAYMENT_EXTERNAL_KEY, paymentTransactionExternalKey);
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_SERVER_URL, "http://killbill.io");
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_CURRENCY, DEFAULT_CURRENCY.name());
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_COUNTRY, DEFAULT_COUNTRY);
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_IP, "0.0.0.0");
        propsBuilder.putAll(extraProperties);
        final Map<String, String> customFieldsMap = propsBuilder.build();
        final Iterable<PluginProperty> customFields = PluginProperties.buildPluginProperties(customFieldsMap);

        final HostedPaymentPageFormDescriptor descriptor = adyenPaymentPluginApi.buildFormDescriptor(account.getId(), customFields, ImmutableList.<PluginProperty>of(), context);
        assertEquals(descriptor.getKbAccountId(), account.getId());
        assertEquals(descriptor.getFormMethod(), "GET");
        assertNotNull(descriptor.getFormUrl());
        assertFalse(descriptor.getFormFields().isEmpty());
        assertNotNull(dao.getHppRequest(paymentTransactionExternalKey));
        assertFalse(AdyenDao.fromAdditionalData(dao.getHppRequest(paymentTransactionExternalKey).getAdditionalData()).containsKey(PROPERTY_IP));

        // For manual testing
        //System.out.println("Redirect to: " + descriptor.getFormUrl());
        //System.out.flush();

        final Boolean withPendingPayment = Boolean.valueOf(customFieldsMap.get(PROPERTY_CREATE_PENDING_PAYMENT));
        if (withPendingPayment) {
            final List<Payment> accountPayments = killbillApi.getPaymentApi().getAccountPayments(account.getId(), false, false, ImmutableList.<PluginProperty>of(), context);
            assertEquals(accountPayments.size(), 1);
            final Payment payment = accountPayments.get(0);

            final List<AdyenResponsesRecord> responses = dao.getResponses(payment.getId(), context.getTenantId());
            assertEquals(responses.size(), 1);
            assertEquals(responses.get(0).getPspResult(), PaymentServiceProviderResult.REDIRECT_SHOPPER.getResponses()[0]);

            final List<PaymentTransactionInfoPlugin> pendingPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(payment.getAccountId(), payment.getId(), ImmutableList.<PluginProperty>of(), context);
            assertEquals(pendingPaymentTransactions.size(), 1);
            assertEquals(pendingPaymentTransactions.get(0).getStatus(), PaymentPluginStatus.PENDING);
            assertEquals(pendingPaymentTransactions.get(0).getTransactionType(), transactionType);

            return payment;
        }

        return null;
    }

    private void processHPPNotification() throws PaymentPluginApiException {
        final String eventCode = "AUTHORISATION";
        final boolean success = true;
        processNotification(eventCode, success, paymentTransactionExternalKey, pspReference);
    }

    private void verifyPayment(final TransactionType transactionType) throws SQLException, PaymentPluginApiException, PaymentApiException {
        final List<Payment> accountPayments = killbillApi.getPaymentApi().getAccountPayments(account.getId(), false, false, ImmutableList.<PluginProperty>of(), context);
        assertEquals(accountPayments.size(), 1);
        final Payment payment = accountPayments.get(0);

        final List<AdyenResponsesRecord> responses = dao.getResponses(payment.getId(), context.getTenantId());
        // Unlike 3D-S redirects which have two rows (2 calls to Adyen), we only have one row per HPP call
        assertEquals(responses.size(), 1);
        // After the redirect or processing the notification, we updated the psp_result
        assertEquals(responses.get(0).getPspResult(), PaymentServiceProviderResult.AUTHORISED.getResponses()[0]);

        final List<PaymentTransactionInfoPlugin> processedPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(payment.getAccountId(), payment.getId(), ImmutableList.<PluginProperty>of(), context);
        assertEquals(processedPaymentTransactions.size(), 1);
        assertEquals(processedPaymentTransactions.get(0).getTransactionType(), transactionType);
        assertEquals(processedPaymentTransactions.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        assertEquals(processedPaymentTransactions.get(0).getFirstPaymentReferenceId(), pspReference);
    }
}
