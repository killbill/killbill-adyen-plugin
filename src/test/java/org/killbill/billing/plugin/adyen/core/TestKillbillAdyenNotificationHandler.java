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

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.api.OSGIMetrics;
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

public class TestKillbillAdyenNotificationHandler {

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
        final OSGIMetrics killbillMetrics = Mockito.mock(OSGIMetrics.class);

        return new KillbillAdyenNotificationHandler(killbillApi, adyenDao, clock, killbillMetrics);
    }

    private NotificationRequestItem getNotificationRequestItem(final boolean success) {
        final String pspReference = UUID.randomUUID().toString();

        final NotificationRequestItem item = new NotificationRequestItem();
        item.setPspReference(pspReference);
        item.setSuccess(success);

        return item;
    }

    private OSGIKillbillAPI getOSGIKillbillAPI(final Account account, final Payment payment, final NotificationRequestItem item, final AtomicBoolean isKillBillNotified) throws AccountApiException, PaymentApiException {
        final OSGIKillbillAPI killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, null);
        Mockito.when(killbillApi.getPaymentApi().notifyPendingTransactionOfStateChanged(Mockito.eq(account),
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

    private AdyenDao getAdyenDao(final Payment payment, final PaymentTransaction paymentTransaction, final TransactionType transactionType, final NotificationRequestItem item, final AtomicBoolean isNotificationRecorded) throws SQLException {
        final AdyenDao adyenDao = Mockito.mock(AdyenDao.class);

        if (transactionType != null) {
            final AdyenResponsesRecord record = new AdyenResponsesRecord();
            record.setKbAccountId(payment.getAccountId().toString());
            record.setKbPaymentId(payment.getId().toString());
            record.setKbPaymentTransactionId(paymentTransaction.getId().toString());
            record.setKbTenantId(UUID.randomUUID().toString());

            Mockito.when(adyenDao.getResponse(item.getPspReference())).thenReturn(record);
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
}
