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

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.adyen.api.AdyenCallContext;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.Clock;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;

import com.google.common.base.Objects;

public class KillbillAdyenNotificationHandler implements AdyenNotificationHandler {

    private final OSGIKillbillAPI osgiKillbillAPI;
    private final AdyenDao dao;
    private final Clock clock;

    public KillbillAdyenNotificationHandler(final OSGIKillbillAPI osgiKillbillAPI, final AdyenDao dao, final Clock clock) {
        this.osgiKillbillAPI = osgiKillbillAPI;
        this.dao = dao;
        this.clock = clock;
    }

    @Override
    public boolean canHandleNotification(final NotificationRequestItem item) {
        // Handle them all
        return true;
    }

    @Override
    public void authorisationSuccess(final NotificationRequestItem item) {
        handleNotification(TransactionType.AUTHORIZE, item);
    }

    @Override
    public void authorisationFailure(final NotificationRequestItem item) {
        handleNotification(TransactionType.AUTHORIZE, item);
    }

    @Override
    public void captureSuccess(final NotificationRequestItem item) {
        handleNotification(TransactionType.CAPTURE, item);
    }

    @Override
    public void captureFailure(final NotificationRequestItem item) {
        handleNotification(TransactionType.CAPTURE, item);
    }

    @Override
    public void captureFailed(final NotificationRequestItem item) {
        handleNotification(TransactionType.CAPTURE, item);
    }

    @Override
    public void cancellationSuccess(final NotificationRequestItem item) {
        handleNotification(TransactionType.VOID, item);
    }

    @Override
    public void cancellationFailure(final NotificationRequestItem item) {
        handleNotification(TransactionType.VOID, item);
    }

    @Override
    public void chargeback(final NotificationRequestItem item) {
        // TODO
        handleNotification(TransactionType.CHARGEBACK, item);
    }

    @Override
    public void chargebackReversed(final NotificationRequestItem item) {
        // TODO
        handleNotification(TransactionType.CHARGEBACK, item);
    }

    @Override
    public void refundSuccess(final NotificationRequestItem item) {
        handleNotification(TransactionType.REFUND, item);
    }

    @Override
    public void refundFailure(final NotificationRequestItem item) {
        handleNotification(TransactionType.REFUND, item);
    }

    @Override
    public void refundedReversed(final NotificationRequestItem item) {
        handleNotification(TransactionType.REFUND, item);
    }

    @Override
    public void refundFailed(final NotificationRequestItem item) {
        handleNotification(TransactionType.REFUND, item);
    }

    @Override
    public void notificationOfChargeback(final NotificationRequestItem item) {
        // TODO
        handleNotification(TransactionType.CHARGEBACK, item);
    }

    @Override
    public void cancelOrRefundSuccess(final NotificationRequestItem item) {
        // TODO
        handleNotification(item);
    }

    @Override
    public void notificationOfFraud(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void requestForInformation(final NotificationRequestItem item) {
        // TODO New chargeback?
        handleNotification(item);
    }

    @Override
    public void cancelOrRefundFailure(final NotificationRequestItem item) {
        // TODO
        handleNotification(item);
    }

    @Override
    public void dispute(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void reportAvailable(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void notificationtest(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void recurringReceived(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void cancelReceived(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void recurringDetailDisabled(final NotificationRequestItem item) {
        handleNotification(item);
    }

    @Override
    public void recurringForUserDisabled(final NotificationRequestItem item) {
        handleNotification(item);
    }

    private void handleNotification(final NotificationRequestItem item) {
        handleNotification(null, item);
    }

    private void handleNotification(@Nullable final TransactionType transactionType, final NotificationRequestItem item) {
        final AdyenResponsesRecord record;
        try {
            record = dao.getResponse(item.getPspReference());
        } catch (final SQLException e) {
            // Have Adyen retry
            throw new RuntimeException("Unable to retrieve response for pspReference " + item.getPspReference(), e);
        }

        final UUID kbAccountId = record == null ? null : UUID.fromString(record.getKbAccountId());
        final UUID kbPaymentId = record == null ? null : UUID.fromString(record.getKbPaymentId());
        final UUID kbPaymentTransactionId = record == null ? null : UUID.fromString(record.getKbPaymentTransactionId());
        final NotificationItem notification = new NotificationItem(item);
        final DateTime utcNow = clock.getUTCNow();
        final UUID kbTenantId = record == null ? null : UUID.fromString(record.getKbTenantId());

        recordNotification(kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, notification, utcNow, kbTenantId);

        if (kbAccountId != null && kbPaymentTransactionId != null && kbTenantId != null) {
            notifyKillBill(kbAccountId, kbPaymentTransactionId, notification.getSuccess(), utcNow, kbTenantId);
        }
    }

    private void recordNotification(@Nullable final UUID kbAccountId,
                                    @Nullable final UUID kbPaymentId,
                                    @Nullable final UUID kbPaymentTransactionId,
                                    @Nullable final TransactionType transactionType,
                                    final NotificationItem notification,
                                    final DateTime utcNow,
                                    final UUID kbTenantId) {
        try {
            dao.addNotification(kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, notification, utcNow, kbTenantId);
        } catch (final SQLException e) {
            // Have Adyen retry
            throw new RuntimeException("Unable to record notification " + notification, e);
        }
    }

    private Payment notifyKillBill(final UUID kbAccountId,
                                   final UUID kbPaymentTransactionId,
                                   final Boolean isSuccess,
                                   final DateTime utcNow,
                                   final UUID kbTenantId) {
        final CallContext context = new AdyenCallContext(utcNow, kbTenantId);
        final Account account;
        try {
            account = osgiKillbillAPI.getAccountUserApi().getAccountById(kbAccountId, context);
        } catch (final AccountApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to retrieve account " + kbAccountId, e);
        }

        try {
            return osgiKillbillAPI.getPaymentApi().notifyPendingTransactionOfStateChanged(account, kbPaymentTransactionId, Objects.firstNonNull(isSuccess, false), context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to notify Kill Bill for kbPaymentTransactionId " + kbPaymentTransactionId, e);
        }
    }
}
