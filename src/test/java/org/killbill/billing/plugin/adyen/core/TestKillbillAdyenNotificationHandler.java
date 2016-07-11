/*
 * Copyright 2014-2016 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.core;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenHppRequestsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.DefaultClock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestKillbillAdyenNotificationHandler {

    private Account account;
    private Payment payment;
    private OSGIKillbillAPI killbillApi;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        account = TestUtils.buildAccount(Currency.EUR, "DE");
        killbillApi = TestUtils.buildOSGIKillbillAPI(account);

        TestUtils.buildPaymentMethod(account.getId(), account.getPaymentMethodId(), AdyenActivator.PLUGIN_NAME, killbillApi);
        payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency(), killbillApi);

        Mockito.when(killbillApi.getPaymentApi().createCapture(Mockito.<Account>any(),
                                                               Mockito.<UUID>any(),
                                                               Mockito.<BigDecimal>any(),
                                                               Mockito.<Currency>any(),
                                                               Mockito.<String>any(),
                                                               Mockito.<Iterable<PluginProperty>>any(),
                                                               Mockito.<CallContext>any()))
               .then(new Answer<Payment>() {
                   @Override
                   public Payment answer(final InvocationOnMock invocation) throws Throwable {
                       final BigDecimal amount = (BigDecimal) invocation.getArguments()[2];
                       final Currency currency = (Currency) invocation.getArguments()[3];
                       final String paymentTransactionExternalKey = MoreObjects.firstNonNull((String) invocation.getArguments()[4], UUID.randomUUID().toString());

                       TestUtils.buildPaymentTransaction(payment, paymentTransactionExternalKey, TransactionType.CAPTURE, TransactionStatus.SUCCESS, amount, currency);

                       return payment;
                   }
               });
    }

    @Test(groups = "fast")
    public void testHandleAuthorizationCaptureSuccess() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItem("AUTHORISATION", success, payment.getExternalKey());
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandler(TransactionType.AUTHORIZE, item, isNotificationRecorded);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem captureItem = getNotificationRequestItem("CAPTURE", success, payment.getExternalKey());
        killbillAdyenNotificationHandler.handleNotification(captureItem);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
    }

    @Test(groups = "fast")
    public void testHandleCaptureFailure() throws Exception {
        final boolean success = false;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItem("CAPTURE", success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandler(TransactionType.CAPTURE, item, isNotificationRecorded);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(payment.getTransactions().size() - 1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(payment.getTransactions().size() - 1).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(payment.getTransactions().size() - 1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(payment.getTransactions().size() - 1).getTransactionStatus(), TransactionStatus.PAYMENT_FAILURE);
    }

    @Test(groups = "fast")
    public void testHandleChargeback() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItemForChargeback(success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandlerForChargeback(item, isNotificationRecorded);

        Assert.assertEquals(payment.getTransactions().size(), 1);

        killbillAdyenNotificationHandler.handleNotification(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(payment.getTransactions().size() - 1).getTransactionType(), TransactionType.CHARGEBACK);
    }

    @Test(groups = "fast")
    public void testReportAvailable() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItem("REPORT_AVAILABLE", success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandler(null, item, isNotificationRecorded);

        final List<PaymentTransaction> oldPaymentTransactions = ImmutableList.<PaymentTransaction>copyOf(payment.getTransactions());

        killbillAdyenNotificationHandler.handleNotification(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertEquals(payment.getTransactions(), oldPaymentTransactions);
    }

    private KillbillAdyenNotificationHandler getKillbillAdyenNotificationHandler(@Nullable final TransactionType transactionType, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded) throws AccountApiException, PaymentApiException, SQLException {
        final PaymentTransaction paymentTransaction = TestUtils.buildPaymentTransaction(payment, transactionType, TransactionStatus.PENDING, BigDecimal.TEN, account.getCurrency());

        final AdyenDao adyenDao = getAdyenDao(payment, paymentTransaction, transactionType, item, isNotificationRecorded);
        final DefaultClock clock = new DefaultClock();

        return new KillbillAdyenNotificationHandler(killbillApi, adyenDao, clock);
    }

    private KillbillAdyenNotificationHandler getKillbillAdyenNotificationHandlerForChargeback(final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded) throws AccountApiException, PaymentApiException, SQLException {
        TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, TransactionStatus.SUCCESS, BigDecimal.TEN, account.getCurrency());

        final AdyenDao adyenDao = getAdyenDaoForChargeback(payment, item, isNotificationRecorded);
        final DefaultClock clock = new DefaultClock();

        return new KillbillAdyenNotificationHandler(killbillApi, adyenDao, clock);
    }

    private NotificationRequestItem getNotificationRequestItem(final String eventCode, final boolean success) {
        return getNotificationRequestItem(eventCode, success, null);
    }

    private NotificationRequestItem getNotificationRequestItem(final String eventCode, final boolean success, final String merchantReference) {
        final String pspReference = UUID.randomUUID().toString();

        final NotificationRequestItem item = new NotificationRequestItem();
        item.setEventCode(eventCode);
        item.setPspReference(pspReference);
        item.setMerchantReference(merchantReference);
        item.setSuccess(success);

        return item;
    }

    private NotificationRequestItem getNotificationRequestItemForChargeback(final boolean success) {
        final String originalReference = UUID.randomUUID().toString();

        final NotificationRequestItem item = new NotificationRequestItem();
        item.setEventCode("CHARGEBACK");
        item.setOriginalReference(originalReference);
        item.setSuccess(success);

        Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setValue(10l);

        item.setAmount(amount);

        return item;
    }

    private AdyenDao getAdyenDao(final Payment payment, final PaymentTransaction paymentTransaction, final TransactionType transactionType, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded) throws SQLException {
        final AdyenDao adyenDao = mock(AdyenDao.class);

        if (transactionType != null) {
            final AdyenResponsesRecord record = new AdyenResponsesRecord();
            record.setKbAccountId(payment.getAccountId().toString());
            record.setKbPaymentId(payment.getId().toString());
            record.setKbPaymentTransactionId(paymentTransaction.getId().toString());
            record.setKbTenantId(UUID.randomUUID().toString());

            when(adyenDao.getResponse(item.getPspReference())).thenReturn(record);
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) throws Throwable {
                    isNotificationRecorded.set(true);
                    return null;
                }
            })
                   .when(adyenDao)
                   .addNotification(Mockito.eq(UUID.fromString(record.getKbAccountId())),
                                    Mockito.eq(UUID.fromString(record.getKbPaymentId())),
                                    Mockito.eq(UUID.fromString(record.getKbPaymentTransactionId())),
                                    Mockito.eq(transactionType),
                                    Mockito.<NotificationItem>any(),
                                    Mockito.<DateTime>any(),
                                    Mockito.eq(UUID.fromString(record.getKbTenantId())));

            final AdyenHppRequestsRecord hppRequestsRecord = new AdyenHppRequestsRecord();
            hppRequestsRecord.setKbAccountId(payment.getAccountId().toString());
            hppRequestsRecord.setKbPaymentId(payment.getId().toString());
            hppRequestsRecord.setKbPaymentTransactionId(paymentTransaction.getId().toString());
            hppRequestsRecord.setKbTenantId(UUID.randomUUID().toString());
            when(adyenDao.getHppRequest(item.getMerchantReference())).thenReturn(hppRequestsRecord);
        } else {
            // Special case for reports
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) throws Throwable {
                    isNotificationRecorded.set(true);
                    return null;
                }
            })
                   .when(adyenDao)
                   .addNotification(Mockito.<UUID>eq(null),
                                    Mockito.<UUID>eq(null),
                                    Mockito.<UUID>eq(null),
                                    Mockito.<TransactionType>eq(null),
                                    Mockito.<NotificationItem>any(),
                                    Mockito.<DateTime>any(),
                                    Mockito.<UUID>eq(null));
        }

        return adyenDao;
    }

    private AdyenDao getAdyenDaoForChargeback(final Payment payment, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded) throws SQLException {
        final TransactionType transactionType = TransactionType.CHARGEBACK;

        final AdyenDao adyenDao = mock(AdyenDao.class);

        final AdyenResponsesRecord record = new AdyenResponsesRecord();
        record.setKbAccountId(payment.getAccountId().toString());
        record.setKbPaymentId(payment.getId().toString());
        record.setKbTenantId(UUID.randomUUID().toString());

        when(adyenDao.getResponse(item.getOriginalReference())).thenReturn(record);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                isNotificationRecorded.set(true);
                return null;
            }
        }).when(adyenDao)
               .addNotification(Mockito.eq(UUID.fromString(record.getKbAccountId())),
                                Mockito.eq(UUID.fromString(record.getKbPaymentId())),
                                Mockito.<UUID>any(),
                                Mockito.eq(transactionType),
                                Mockito.<NotificationItem>any(),
                                Mockito.<DateTime>any(),
                                Mockito.eq(UUID.fromString(record.getKbTenantId())));

        return adyenDao;
    }
}
