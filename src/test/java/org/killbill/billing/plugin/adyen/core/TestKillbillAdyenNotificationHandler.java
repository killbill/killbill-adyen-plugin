/*
 * Copyright 2014 Groupon, Inc
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestKillbillAdyenNotificationHandler {

    private final static String NO_PAYMENT = "NoPayment";

    @Test(groups = "fast")
    public void testHandleAuthorizationSuccess() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);
        final AtomicBoolean isKillbillNotified = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItem(success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandler(TransactionType.AUTHORIZE, item, isNotificationRecorded, isKillbillNotified);

        killbillAdyenNotificationHandler.authorisationSuccess(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertTrue(isKillbillNotified.get());
    }

    @Test(groups = "fast")
    public void testHandleCaptureFailure() throws Exception {
        final boolean success = false;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);
        final AtomicBoolean isKillbillNotified = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItem(success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandler(TransactionType.CAPTURE, item, isNotificationRecorded, isKillbillNotified);

        killbillAdyenNotificationHandler.captureFailure(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertTrue(isKillbillNotified.get());
    }

    @Test(groups = "fast")
    public void testHandleChargeback() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);
        final AtomicBoolean isKillbillNotified = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItemForChargeback(success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandlerForChargeback(TransactionType.CHARGEBACK, item, isNotificationRecorded, isKillbillNotified);

        killbillAdyenNotificationHandler.chargeback(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertTrue(isKillbillNotified.get());
    }

    @Test(groups = "fast")
    public void testHandleChargebackForNotificationWithoutChargeback() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);
        final AtomicBoolean isKillbillNotified = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItemForChargeback(success);
        item.setOriginalReference(NO_PAYMENT);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandlerForChargeback(TransactionType.CHARGEBACK, item, isNotificationRecorded, isKillbillNotified);

        killbillAdyenNotificationHandler.chargeback(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertFalse(isKillbillNotified.get());
    }

    @Test(groups = "fast")
    public void testReportAvailable() throws Exception {
        final boolean success = true;

        final AtomicBoolean isNotificationRecorded = new AtomicBoolean(false);
        final AtomicBoolean isKillbillNotified = new AtomicBoolean(false);

        final NotificationRequestItem item = getNotificationRequestItem(success);
        final KillbillAdyenNotificationHandler killbillAdyenNotificationHandler = getKillbillAdyenNotificationHandler(null, item, isNotificationRecorded, isKillbillNotified);

        killbillAdyenNotificationHandler.reportAvailable(item);

        Assert.assertTrue(isNotificationRecorded.get());
        Assert.assertFalse(isKillbillNotified.get());
    }

    private KillbillAdyenNotificationHandler getKillbillAdyenNotificationHandler(@Nullable final TransactionType transactionType, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded, final AtomicBoolean isKillBillNotified) throws AccountApiException, PaymentApiException, SQLException {
        final Account account = TestUtils.buildAccount(Currency.EUR, "DE");
        final Payment payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency());
        final PaymentTransaction paymentTransaction = TestUtils.buildPaymentTransaction(payment, transactionType, account.getCurrency());

        final OSGIKillbillAPI killbillApi = getOSGIKillbillAPI(account, payment, item, isKillBillNotified);
        final AdyenDao adyenDao = getAdyenDao(payment, paymentTransaction, transactionType, item, isNotificationRecorded);
        final DefaultClock clock = new DefaultClock();

        return new KillbillAdyenNotificationHandler(killbillApi, adyenDao, clock, new IdentifierGenerator());
    }

    private KillbillAdyenNotificationHandler getKillbillAdyenNotificationHandlerForChargeback(@Nullable final TransactionType transactionType, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded, final AtomicBoolean isKillBillNotified) throws AccountApiException, PaymentApiException, SQLException {
        final Account account = TestUtils.buildAccount(Currency.EUR, "DE");
        final Payment payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency());
        final PaymentTransaction paymentTransaction = TestUtils.buildPaymentTransaction(payment, transactionType, account.getCurrency());
        String paymentTransactionExternalKey = UUID.randomUUID().toString();
        when(paymentTransaction.getExternalKey()).thenReturn(paymentTransactionExternalKey);

        final OSGIKillbillAPI killbillApi = getOSGIKillbillAPIForChargeback(account, payment, item, isKillBillNotified);
        final AdyenDao adyenDao = getAdyenDaoForChargeback(payment, paymentTransaction, transactionType, item, isNotificationRecorded);
        final DefaultClock clock = new DefaultClock();

        IdentifierGenerator identifierGenerator = mock(IdentifierGenerator.class);
        when(identifierGenerator.getRandomPaymentTransactionExternalKey()).thenReturn(paymentTransactionExternalKey);

        return new KillbillAdyenNotificationHandler(killbillApi, adyenDao, clock, identifierGenerator);
    }

    private NotificationRequestItem getNotificationRequestItem(final boolean success) {
        final String pspReference = UUID.randomUUID().toString();

        final NotificationRequestItem item = new NotificationRequestItem();
        item.setPspReference(pspReference);
        item.setSuccess(success);

        return item;
    }

    private NotificationRequestItem getNotificationRequestItemForChargeback(final boolean success) {
        final String originalReference = UUID.randomUUID().toString();

        final NotificationRequestItem item = new NotificationRequestItem();
        item.setOriginalReference(originalReference);
        item.setSuccess(success);

        Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setValue(10l);

        item.setAmount(amount);

        return item;
    }

    private OSGIKillbillAPI getOSGIKillbillAPI(final Account account, final Payment payment, final NotificationRequestItem item, final AtomicBoolean isKillBillNotified) throws AccountApiException, PaymentApiException {
        final OSGIKillbillAPI killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, null);
        when(killbillApi.getPaymentApi().notifyPendingTransactionOfStateChanged(Mockito.eq(account),
                                                                                        Mockito.<UUID>any(),
                                                                                        Mockito.eq(item.isSuccess()),
                                                                                        Mockito.<CallContext>any()))
               .thenAnswer(new Answer<Payment>() {
                   @Override
                   public Payment answer(final InvocationOnMock invocation) throws Throwable {
                       // Had to use Mockito.<UUID>any() above instead of the actual id
                       isKillBillNotified.set(invocation.getArguments()[1].equals(payment.getTransactions().iterator().next().getId()));
                       return null;
                   }
               });

        return killbillApi;
    }

    private OSGIKillbillAPI getOSGIKillbillAPIForChargeback(final Account account, final Payment payment, final NotificationRequestItem item, final AtomicBoolean isKillBillNotified) throws AccountApiException, PaymentApiException {
        final OSGIKillbillAPI killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, null);
        when(killbillApi.getPaymentApi().createChargeback(Mockito.eq(account),
                                                                  Mockito.<UUID>any(),
                                                                  Mockito.<BigDecimal>any(),
                                                                  Mockito.<Currency>any(),
                                                                  Mockito.<String>any(),
                                                                  Mockito.<CallContext>any()))
               .thenAnswer(new Answer<Payment>() {
                   @Override
                   public Payment answer(final InvocationOnMock invocation) throws Throwable {
                       // Had to use Mockito.<UUID>any() above instead of the actual id
                       isKillBillNotified.set(true);

                       return payment;
                   }
               });

        return killbillApi;
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

    private AdyenDao getAdyenDaoForChargeback(final Payment payment, final PaymentTransaction paymentTransaction, final TransactionType transactionType, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded) throws SQLException {
        final AdyenDao adyenDao = mock(AdyenDao.class);

        if (!item.getOriginalReference().equals(NO_PAYMENT)) {
            final AdyenResponsesRecord record = new AdyenResponsesRecord();
            record.setKbAccountId(payment.getAccountId().toString());
            record.setKbPaymentId(payment.getId().toString());
            record.setKbPaymentTransactionId(paymentTransaction.getId().toString());
            record.setKbTenantId(UUID.randomUUID().toString());

            when(adyenDao.getResponse(item.getOriginalReference())).thenReturn(record);
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) throws Throwable {
                    isNotificationRecorded.set(true);
                    return null;
                }
            }).when(adyenDao)
                   .addNotification                                                    (Mockito.eq(UUID.fromString(record.getKbAccountId())),
                                    Mockito.eq(UUID.fromString(record.getKbPaymentId())),
                                    Mockito.eq(UUID.fromString(record.getKbPaymentTransactionId())),
                                    Mockito.eq(transactionType),
                                    Mockito.<NotificationItem>any(),
                                    Mockito.<DateTime>any(),
                                    Mockito.eq(UUID.fromString(record.getKbTenantId())));
        } else {
            when(adyenDao.getResponse(item.getOriginalReference())).thenReturn(null);
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) throws Throwable {
                    isNotificationRecorded.set(true);
                    return null;
                }
            }).when(adyenDao)
                   .addNotification(Mockito.<UUID>any(), Mockito.<UUID>any(), Mockito.<UUID>any(), Mockito.eq(transactionType), Mockito.<NotificationItem>any(), Mockito.<DateTime>any(), Mockito.<UUID>any());

        }

        return adyenDao;
    }

}
