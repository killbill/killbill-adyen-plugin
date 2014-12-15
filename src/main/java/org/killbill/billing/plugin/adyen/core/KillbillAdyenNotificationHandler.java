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

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.adyen.api.AdyenCallContext;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenHppRequestsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

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
        final NotificationItem notification = new NotificationItem(item);
        final DateTime utcNow = clock.getUTCNow();

        UUID kbAccountId = null;
        UUID kbPaymentId = null;
        UUID kbPaymentTransactionId = null;
        UUID kbTenantId = null;
        try {
            // First, determine if the notification is for an API call or HPP request
            final AdyenResponsesRecord record = getResponseRecord(item);
            if (record != null) {
                // API
                kbAccountId = UUID.fromString(record.getKbAccountId());
                kbTenantId = UUID.fromString(record.getKbTenantId());
                kbPaymentId = UUID.fromString(record.getKbPaymentId());
                kbPaymentTransactionId = UUID.fromString(record.getKbPaymentTransactionId());
            } else {
                // HPP
                final AdyenHppRequestsRecord hppRequest = getHppRequest(notification);
                if (hppRequest != null) {
                    kbAccountId = UUID.fromString(hppRequest.getKbAccountId());
                    kbTenantId = UUID.fromString(hppRequest.getKbTenantId());
                }
                // Otherwise, maybe REPORT_AVAILABLE notification?
            }

            if (kbAccountId != null && kbTenantId != null) {
                // Retrieve the account
                final CallContext context = new AdyenCallContext(utcNow, kbTenantId);
                final Account account = getAccount(kbAccountId, context);

                // Update Kill Bill
                if (record != null) {
                    notifyKillBill(account, kbPaymentTransactionId, notification, context);
                } else {
                    final Payment payment = recordPayment(account, notification, context);
                    kbPaymentId = payment.getId();
                    kbPaymentTransactionId = payment.getTransactions().iterator().next().getPaymentId();
                }
            }
        } finally {
            recordNotification(kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, notification, utcNow, kbTenantId);
        }
    }

    private AdyenResponsesRecord getResponseRecord(final NotificationRequestItem item) {
        try {
            return dao.getResponse(item.getPspReference());
        } catch (final SQLException e) {
            // Have Adyen retry
            throw new RuntimeException("Unable to retrieve response for pspReference " + item.getPspReference(), e);
        }
    }

    private AdyenHppRequestsRecord getHppRequest(final NotificationItem notification) {
        try {
            return dao.getHppRequest(notification.getMerchantReference());
        } catch (final SQLException e) {
            throw new RuntimeException("Unable to retrieve HPP request for merchantReference " + notification.getMerchantReference(), e);
        }
    }

    private Account getAccount(final UUID kbAccountId, final CallContext context) {
        try {
            return osgiKillbillAPI.getAccountUserApi().getAccountById(kbAccountId, context);
        } catch (final AccountApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to retrieve account " + kbAccountId, e);
        }
    }

    private Payment notifyKillBill(final Account account, final UUID kbPaymentTransactionId, final NotificationItem notification, final CallContext context) {
        final Boolean isSuccess = Objects.firstNonNull(notification.getSuccess(), false);
        try {
            return osgiKillbillAPI.getPaymentApi().notifyPendingTransactionOfStateChanged(account, kbPaymentTransactionId, isSuccess, context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to notify Kill Bill for kbPaymentTransactionId " + kbPaymentTransactionId, e);
        }
    }

    private Payment recordPayment(final Account account, final NotificationItem notification, final CallContext context) {
        final UUID kbPaymentMethodId = getAdyenKbPaymentMethodId(account.getId(), context);
        final UUID kbPaymentId = null;
        final BigDecimal amount = notification.getAmount();
        final Currency currency = Currency.valueOf(notification.getCurrency());
        final String paymentExternalKey = notification.getMerchantReference();
        final String paymentTransactionExternalKey = notification.getMerchantReference();

        final ImmutableMap.Builder<String, Object> pluginPropertiesMapBuilder = new ImmutableMap.Builder<String, Object>();
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_FROM_HPP, true);
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_REFERENCE, notification.getMerchantReference());
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_PSP_REFERENCE, notification.getPspReference());
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_AMOUNT, amount);
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_CURRENCY, currency);

        if (notification.getAdditionalData() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_ADDITIONAL_DATA, notification.getAdditionalData());
        }
        if (notification.getEventCode() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_EVENT_CODE, notification.getEventCode());
        }
        if (notification.getEventDate() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_EVENT_DATE, notification.getEventDate());
        }
        if (notification.getMerchantAccountCode() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, notification.getMerchantAccountCode());
        }
        if (notification.getOperations() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_OPERATIONS, notification.getOperations());
        }
        if (notification.getOriginalReference() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_ORIGINAL_REFERENCE, notification.getOriginalReference());
        }
        if (notification.getPaymentMethod() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_PAYMENT_METHOD, notification.getPaymentMethod());
        }
        if (notification.getReason() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_REASON, notification.getReason());
        }
        if (notification.getSuccess() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_SUCCESS, notification.getSuccess());
        }
        final ImmutableMap<String, Object> purchasePropertiesMap = pluginPropertiesMapBuilder.build();
        final Iterable<PluginProperty> purchaseProperties = PluginProperties.buildPluginProperties(purchasePropertiesMap);

        try {
            return osgiKillbillAPI.getPaymentApi().createPurchase(account,
                                                                  kbPaymentMethodId,
                                                                  kbPaymentId,
                                                                  amount,
                                                                  currency,
                                                                  paymentExternalKey,
                                                                  paymentTransactionExternalKey,
                                                                  purchaseProperties,
                                                                  context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to record purchase", e);
        }
    }

    private UUID getAdyenKbPaymentMethodId(final UUID kbAccountId, final TenantContext context) {
        try {
            return Iterables.<PaymentMethod>find(osgiKillbillAPI.getPaymentApi().getAccountPaymentMethods(kbAccountId, false, ImmutableList.<PluginProperty>of(), context),
                                                 new Predicate<PaymentMethod>() {
                                                     @Override
                                                     public boolean apply(final PaymentMethod paymentMethod) {
                                                         return AdyenActivator.PLUGIN_NAME.equals(paymentMethod.getPluginName());
                                                     }
                                                 }).getId();
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to locate Adyen payment method for account " + kbAccountId, e);
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
}
