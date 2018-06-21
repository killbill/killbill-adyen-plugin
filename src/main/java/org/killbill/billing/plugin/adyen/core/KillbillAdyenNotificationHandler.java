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

package org.killbill.billing.plugin.adyen.core;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.api.AdyenCallContext;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.api.mapping.AdyenPaymentTransaction;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenHppRequestsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.core.PaymentApiWrapper;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_AUTH_MODE;
import static org.killbill.billing.plugin.api.core.PaymentApiWrapper.filterForTransaction;

public class KillbillAdyenNotificationHandler implements AdyenNotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(KillbillAdyenNotificationHandler.class);

    // Note that AUTHORISATION maps to either AUTHORIZE or PURCHASE
    private static final Map<String, TransactionType> EVENT_CODES_TO_TRANSACTION_TYPE = ImmutableMap.<String, TransactionType>builder().put("CANCELLATION", TransactionType.VOID)
                                                                                                                                       .put("REFUND", TransactionType.REFUND)
                                                                                                                                       .put("CAPTURE", TransactionType.CAPTURE)
                                                                                                                                       .put("REFUND_WITH_DATA", TransactionType.CREDIT)
                                                                                                                                       .put("CHARGEBACK", TransactionType.CHARGEBACK)
                                                                                                                                       .put("CHARGEBACK_REVERSED", TransactionType.CHARGEBACK)
                                                                                                                                       .build();

    private final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler;
    private final OSGIKillbillAPI osgiKillbillAPI;
    private final AdyenDao dao;
    private final Clock clock;

    public KillbillAdyenNotificationHandler(final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler,
                                            final OSGIKillbillAPI osgiKillbillAPI,
                                            final AdyenDao dao,
                                            final Clock clock) {
        this.adyenConfigPropertiesConfigurationHandler = adyenConfigPropertiesConfigurationHandler;
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
    public void handleNotification(final NotificationRequestItem item) {
        final NotificationItem notification = new NotificationItem(item);
        final DateTime utcNow = clock.getUTCNow();

        UUID kbAccountId = null;
        UUID kbPaymentId = null;
        UUID kbPaymentTransactionId = null;
        UUID kbTenantId = null;
        TransactionType transactionType = null;
        boolean authMode = true;
        boolean isHpp = false;
        try {
            // Check if we have a record for that pspReference (PENDING auth, capture, refund, etc.)
            final AdyenResponsesRecord record = getResponseRecord(notification.getPspReference());
            if (record != null) {
                kbAccountId = UUID.fromString(record.getKbAccountId());
                kbTenantId = UUID.fromString(record.getKbTenantId());
                kbPaymentId = UUID.fromString(record.getKbPaymentId());
                kbPaymentTransactionId = UUID.fromString(record.getKbPaymentTransactionId());
            } else {
                // Check if we have a record for the original pspReference (e.g. chargeback notification)
                final AdyenResponsesRecord originalRecord = getResponseRecord(notification.getOriginalReference());
                if (originalRecord != null) {
                    kbAccountId = UUID.fromString(originalRecord.getKbAccountId());
                    kbTenantId = UUID.fromString(originalRecord.getKbTenantId());
                    kbPaymentId = UUID.fromString(originalRecord.getKbPaymentId());
                    kbPaymentTransactionId = null;
                } else {
                    // Check if the notification is associated with a HPP request
                    final AdyenHppRequestsRecord hppRequest = getHppRequest(notification.getMerchantReference());
                    if (hppRequest != null) {
                        kbAccountId = UUID.fromString(hppRequest.getKbAccountId());
                        kbTenantId = UUID.fromString(hppRequest.getKbTenantId());
                        // The HPP may or may not be associated with a pending payment
                        if (hppRequest.getKbPaymentId() != null) {
                            kbPaymentId = UUID.fromString(hppRequest.getKbPaymentId());
                        }
                        if (hppRequest.getKbPaymentTransactionId() != null) {
                            kbPaymentTransactionId = UUID.fromString(hppRequest.getKbPaymentTransactionId());
                        }

                        final Map additionalData = AdyenDao.fromAdditionalData(hppRequest.getAdditionalData());
                        final Object authModeProperty = MoreObjects.firstNonNull(additionalData.get(PROPERTY_AUTH_MODE), true);
                        authMode = Boolean.valueOf(authModeProperty.toString());

                        isHpp = true;
                    }
                    // Otherwise, it could be a generic notification (like REPORT_AVAILABLE)
                }
            }

            final CallContext context = new AdyenCallContext(clock.getUTCNow(), kbAccountId, kbTenantId);
            final AdyenConfigProperties tenantConfiguration = adyenConfigPropertiesConfigurationHandler.getConfigurable(context.getTenantId());
            final NotificationItem transformedNotification = transformNotificationItemIfNeeded(notification, tenantConfiguration);
            if (transformedNotification != null) {
                // Find the original transaction
                final AdyenResponsesRecord linkedOriginalRecord = getResponseRecord(transformedNotification.getPspReference());
                Preconditions.checkNotNull(linkedOriginalRecord, "Unable to retrieve record for pspReference='%s'", transformedNotification.getPspReference());
                kbPaymentTransactionId = UUID.fromString(linkedOriginalRecord.getKbPaymentTransactionId());
            }
            final PaymentTransaction updatedPaymentTransaction = updateKillbill(MoreObjects.firstNonNull(transformedNotification, notification), kbAccountId, kbPaymentId, kbPaymentTransactionId, isHpp, authMode, tenantConfiguration, context);
            if (updatedPaymentTransaction != null) {
                kbPaymentId = updatedPaymentTransaction.getPaymentId();
                kbPaymentTransactionId = updatedPaymentTransaction.getId();
                transactionType = updatedPaymentTransaction.getTransactionType();
            }
        } finally {
            // Record this notification, for debugging purposes
            recordNotification(notification, kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, utcNow, kbTenantId);
        }
    }

    private NotificationItem transformNotificationItemIfNeeded(final NotificationItem notification, final AdyenConfigProperties tenantConfiguration) {
        if ("CHARGEBACK".equals(notification.getEventCode())) {
            if (tenantConfiguration.getChargebackAsFailurePaymentMethods().contains(notification.getPaymentMethod())) {
                // A SEPA payment failure can be communicated via a CHARGEBACK notification for instance:
                // transform the event into a failed AUTHORISATION instead, for Kill Bill to fix the transaction state
                return new NotificationItem(notification.getAdditionalData(),
                                            notification.getAmount(),
                                            notification.getCurrency(),
                                            "AUTHORISATION",
                                            notification.getEventDate(),
                                            notification.getMerchantAccountCode(),
                                            notification.getMerchantReference(),
                                            notification.getOperations(),
                                            null,
                                            notification.getPaymentMethod(),
                                            // The original reference is the PSP reference
                                            notification.getOriginalReference(),
                                            notification.getReason(),
                                            false);
            }
        }

        return null;
    }

    private PaymentTransaction updateKillbill(final NotificationItem notification,
                                              @Nullable final UUID kbAccountId,
                                              @Nullable final UUID kbPaymentId,
                                              @Nullable final UUID kbPaymentTransactionId,
                                              final boolean isHPP,
                                              final boolean authMode,
                                              final AdyenConfigProperties tenantConfiguration,
                                              final CallContext context) {
        final TransactionType transactionType = EVENT_CODES_TO_TRANSACTION_TYPE.get(notification.getEventCode());

        final PaymentPluginStatus paymentPluginStatus;
        if (ImmutableList.<String>of("AUTHORISATION",
                                     "CANCELLATION",
                                     "REFUND",
                                     "CANCEL_OR_REFUND",
                                     "CAPTURE",
                                     "REFUND_WITH_DATA").contains(notification.getEventCode())) {
            paymentPluginStatus = notification.getSuccess() != null && notification.getSuccess() ? PaymentPluginStatus.PROCESSED : PaymentPluginStatus.ERROR;
        } else if (ImmutableList.<String>of("CAPTURE_FAILED",
                                            "REFUND_FAILED",
                                            "REFUNDED_REVERSED",
                                            "OFFER_CLOSED",
                                            "EXPIRE").contains(notification.getEventCode())) {
            // Whenever a capture or refund actually failed after it was processed by the third party, CAPTURE_FAILED
            // or REFUND_FAILED will be returned. It basically means that there was a technical issue which will need to
            // be further investigated. Most of the times it will for example need to be retried to make it work.
            // REFUNDED_REVERSED means we received back the funds from the bank. This can happen if the card is closed.
            paymentPluginStatus = PaymentPluginStatus.ERROR;
        } else if ("CHARGEBACK".equals(notification.getEventCode())) {
            // Whenever the funds are really deducted we send out the chargeback notification.
            paymentPluginStatus = PaymentPluginStatus.PROCESSED;
        } else if ("CHARGEBACK_REVERSED".equals(notification.getEventCode())) {
            // When you win the case and the funds are returned to your account we send out the chargeback_reversed notification.
            paymentPluginStatus = PaymentPluginStatus.ERROR;
        } else {
            paymentPluginStatus = PaymentPluginStatus.UNDEFINED;
        }

        return updateKillbill(notification, kbAccountId, kbPaymentId, kbPaymentTransactionId, isHPP, authMode, paymentPluginStatus, transactionType, tenantConfiguration, context);
    }

    private PaymentTransaction updateKillbill(final NotificationItem notification,
                                              @Nullable final UUID kbAccountId,
                                              @Nullable final UUID kbPaymentId,
                                              @Nullable final UUID kbPaymentTransactionId,
                                              final boolean isHPP,
                                              final boolean authMode,
                                              final PaymentPluginStatus paymentPluginStatus,
                                              @Nullable final TransactionType expectedTransactionType,
                                              final AdyenConfigProperties tenantConfiguration,
                                              final CallContext context) {
        if (kbPaymentId != null) {
            Preconditions.checkNotNull(context.getTenantId(), String.format("kbTenantId null for kbPaymentId='%s'", kbPaymentId));
            final Payment payment = getPayment(kbPaymentId, context);

            Preconditions.checkArgument(payment.getAccountId().equals(kbAccountId), String.format("kbAccountId='%s' doesn't match payment#accountId='%s'", kbAccountId, payment.getAccountId()));
            final Account account = getAccount(kbAccountId, context);

            AdyenResponsesRecord adyenResponsesRecord = null;
            PaymentTransaction paymentTransaction = null;
            if (kbPaymentTransactionId != null) {
                paymentTransaction = filterForTransaction(payment, kbPaymentTransactionId);
                Preconditions.checkNotNull(paymentTransaction, String.format("kbPaymentTransactionId='%s' not found for kbPaymentId='%s'", kbPaymentTransactionId, kbPaymentId));
                if (isHPP && expectedTransactionType != null && paymentTransaction.getTransactionType() != expectedTransactionType) {
                    // Follow-on transaction
                    paymentTransaction = null;
                } else if (expectedTransactionType != null && paymentTransaction.getTransactionType() != expectedTransactionType) {
                    // Adyen most likely confused us by re-using the PSP reference...
                    paymentTransaction = null;
                } else {
                    // Update the plugin tables
                    adyenResponsesRecord = updateResponse(notification, kbPaymentTransactionId, isHPP, paymentPluginStatus, context.getTenantId());
                }
            }

            // Update Kill Bill
            if (PaymentPluginStatus.UNDEFINED.equals(paymentPluginStatus)) {
                // We cannot do anything
                return paymentTransaction;
            } else if (paymentTransaction != null && TransactionStatus.PENDING.equals(paymentTransaction.getTransactionStatus())) {
                return transitionPendingTransaction(account, kbPaymentId, kbPaymentTransactionId, paymentPluginStatus, tenantConfiguration, context);
            } else if (paymentTransaction != null && paymentTransaction.getPaymentInfoPlugin().getStatus() != paymentPluginStatus) {
                return fixPaymentTransactionState(payment, paymentTransaction, paymentPluginStatus, adyenResponsesRecord, tenantConfiguration, context);
            } else if (paymentTransaction == null && expectedTransactionType == TransactionType.CHARGEBACK && PaymentPluginStatus.PROCESSED.equals(paymentPluginStatus)) {
                return createChargeback(account, kbPaymentId, notification, tenantConfiguration, context);
            } else if (paymentTransaction == null && expectedTransactionType == TransactionType.CHARGEBACK && PaymentPluginStatus.ERROR.equals(paymentPluginStatus)) {
                // There should only be one chargeback in Kill Bill, see https://github.com/killbill/killbill/issues/477
                final PaymentTransaction chargeback = filterForTransaction(payment, TransactionType.CHARGEBACK);
                return createChargebackReversal(account, kbPaymentId, chargeback, tenantConfiguration, context);
            } else if (paymentTransaction == null) {
                // HPP not associated with a pending payment
                return createPayment(account, payment, notification, authMode, expectedTransactionType, paymentPluginStatus, tenantConfiguration, context);
            } else {
                // Payment in Kill Bill has the latest state, nothing to do (we simply updated our plugin tables in case Adyen had extra information for us)
                return paymentTransaction;
            }
        } else if (isHPP) {
            Preconditions.checkNotNull(context.getTenantId(), "kbTenantId null for HPP request");

            Preconditions.checkNotNull(kbAccountId, "kbAccountId null for HPP request");
            final Account account = getAccount(kbAccountId, context);

            // HPP not associated with a pending payment
            return createPayment(account, null, notification, authMode, expectedTransactionType, paymentPluginStatus, tenantConfiguration, context);
        } else {
            // API payment unknown to Kill Bill, does it belong to a different system?
            // Note that we could decide to record a new payment here, this would be useful to migrate data for instance
            return null;
        }
    }

    // Kill Bill APIs

    private Account getAccount(final UUID kbAccountId, final TenantContext context) {
        try {
            return osgiKillbillAPI.getAccountUserApi().getAccountById(kbAccountId, context);
        } catch (final AccountApiException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Failed to retrieve kbAccountId='%s'", kbAccountId), e);
        }
    }

    private UUID getAdyenKbPaymentMethodId(final UUID kbAccountId, final TenantContext context) {
        try {
            return Iterables.<PaymentMethod>find(osgiKillbillAPI.getPaymentApi().getAccountPaymentMethods(kbAccountId, false, false, ImmutableList.<PluginProperty>of(), context),
                                                 new Predicate<PaymentMethod>() {
                                                     @Override
                                                     public boolean apply(final PaymentMethod paymentMethod) {
                                                         return AdyenActivator.PLUGIN_NAME.equals(paymentMethod.getPluginName());
                                                     }
                                                 }).getId();
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Failed to retrieve Adyen payment method for kbAccountId='%s'", kbAccountId), e);
        }
    }

    private Payment getPayment(final UUID kbPaymentId, final TenantContext context) {
        try {
            return osgiKillbillAPI.getPaymentApi().getPayment(kbPaymentId, true, false, ImmutableList.<PluginProperty>of(), context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Failed to retrieve kbPaymentId='%s'", kbPaymentId), e);
        }
    }

    private PaymentTransaction transitionPendingTransaction(final Account account, final UUID kbPaymentId, final UUID kbPaymentTransactionId, final PaymentPluginStatus paymentPluginStatus, final AdyenConfigProperties tenantConfiguration, final CallContext context) {
        final PaymentApiWrapper paymentApiWrapper = getPaymentApiWrapper(tenantConfiguration);
        try {
            return paymentApiWrapper.transitionPendingTransaction(account, kbPaymentId, kbPaymentTransactionId, paymentPluginStatus, context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Failed to transition pending transaction kbPaymentTransactionId='%s'", kbPaymentTransactionId), e);
        }
    }

    private PaymentTransaction fixPaymentTransactionState(final Payment payment, final PaymentTransaction paymentTransaction, final PaymentPluginStatus paymentPluginStatus, @Nullable final AdyenResponsesRecord adyenResponsesRecord, final AdyenConfigProperties tenantConfiguration, final CallContext context) {
        final PaymentTransaction updatedPaymentTransaction;
        if (adyenResponsesRecord == null) {
            updatedPaymentTransaction = paymentTransaction;
        } else {
            final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin = new AdyenPaymentTransactionInfoPlugin(adyenResponsesRecord);
            updatedPaymentTransaction = new AdyenPaymentTransaction(paymentTransactionInfoPlugin.getGatewayErrorCode(), paymentTransactionInfoPlugin.getGatewayError(), paymentTransaction);
        }

        final PaymentApiWrapper paymentApiWrapper = getPaymentApiWrapper(tenantConfiguration);
        try {
            return paymentApiWrapper.fixPaymentTransactionState(payment, paymentPluginStatus, updatedPaymentTransaction, context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Failed to fix transaction kbPaymentTransactionId='%s'", updatedPaymentTransaction.getId()), e);
        }
    }

    private PaymentTransaction createPayment(final Account account, @Nullable final Payment payment, final NotificationItem notification, final boolean authMode, final TransactionType expectedTransactionType, final PaymentPluginStatus paymentPluginStatus, final AdyenConfigProperties tenantConfiguration, final CallContext context) {
        final UUID kbPaymentMethodId = payment != null ? payment.getPaymentMethodId() : getAdyenKbPaymentMethodId(account.getId(), context);
        final UUID kbPaymentId = payment != null ? payment.getId() : null;
        final BigDecimal amount = notification.getAmount();
        final Currency currency = notification.getCurrency() != null ? Currency.valueOf(notification.getCurrency()) : null;
        final String paymentExternalKey = payment != null ? payment.getExternalKey() : Strings.emptyToNull(notification.getMerchantReference());
        final Iterable<PluginProperty> pluginProperties = toPluginProperties(notification, true, paymentPluginStatus);

        TransactionType transactionType = MoreObjects.firstNonNull(expectedTransactionType, TransactionType.AUTHORIZE);
        if (transactionType == TransactionType.AUTHORIZE && !authMode) {
            // Auto-capture mode configured in Adyen
            transactionType = TransactionType.PURCHASE;
        }

        final String paymentTransactionExternalKey;
        switch (transactionType) {
            case AUTHORIZE:
            case CREDIT:
            case PURCHASE:
                paymentTransactionExternalKey = Strings.emptyToNull(notification.getMerchantReference());
                break;
            case CAPTURE:
            case CHARGEBACK:
            case REFUND:
            case VOID:
            default:
                // We cannot use the merchant reference nor the PSP reference (not necessarily unique)
                paymentTransactionExternalKey = null;
                break;
        }

        final PaymentApiWrapper paymentApiWrapper = getPaymentApiWrapper(tenantConfiguration);
        try {
            return paymentApiWrapper.createPayment(transactionType, account, kbPaymentMethodId, kbPaymentId, amount, currency, paymentExternalKey, paymentTransactionExternalKey, pluginProperties, context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to record payment", e);
        }
    }

    private PaymentTransaction createChargeback(final Account account, final UUID kbPaymentId, final NotificationItem notification, final AdyenConfigProperties tenantConfiguration, final CallContext context) {
        final BigDecimal amount = notification.getAmount();
        final Currency currency = Currency.valueOf(notification.getCurrency());
        // We cannot use the merchant reference here, because it's the one associated with the auth.
        // We cannot use the PSP reference either as it may be the one from the capture.
        final String paymentTransactionExternalKey = null;

        final PaymentApiWrapper paymentApiWrapper = getPaymentApiWrapper(tenantConfiguration);
        try {
            return paymentApiWrapper.createChargeback(account, kbPaymentId, amount, currency, paymentTransactionExternalKey, context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to record chargeback", e);
        }
    }

    private PaymentApiWrapper getPaymentApiWrapper(final AdyenConfigProperties tenantConfiguration) {
        final Boolean invoicePaymentEnabled = tenantConfiguration != null && tenantConfiguration.getInvoicePaymentEnabled();
        return new PaymentApiWrapper(osgiKillbillAPI, invoicePaymentEnabled);
    }

    private PaymentTransaction createChargebackReversal(final Account account, final UUID kbPaymentId, final PaymentTransaction chargeback, final AdyenConfigProperties tenantConfiguration, final CallContext context) {
        final PaymentApiWrapper paymentApiWrapper = getPaymentApiWrapper(tenantConfiguration);
        try {
            return paymentApiWrapper.createChargebackReversal(account, kbPaymentId, chargeback, context);
        } catch (final PaymentApiException e) {
            // Have Adyen retry
            throw new RuntimeException("Failed to record chargeback reversal", e);
        }
    }

    private Iterable<PluginProperty> toPluginProperties(final NotificationItem notification, final boolean isHPP, final PaymentPluginStatus paymentPluginStatus) {
        final ImmutableMap.Builder<String, Object> pluginPropertiesMapBuilder = new ImmutableMap.Builder<String, Object>();
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_FROM_HPP, isHPP);
        pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_FROM_HPP_TRANSACTION_STATUS, paymentPluginStatus.toString());
        if (notification.getMerchantReference() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_REFERENCE, notification.getMerchantReference());
        }
        if (notification.getPspReference() != null) {
            pluginPropertiesMapBuilder.put(AdyenPaymentPluginApi.PROPERTY_PSP_REFERENCE, notification.getPspReference());
        }
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
        return PluginProperties.buildPluginProperties(purchasePropertiesMap);
    }

    // DAO

    private AdyenResponsesRecord getResponseRecord(final String pspReference) {
        try {
            return dao.getResponse(pspReference);
        } catch (final SQLException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Unable to retrieve response for pspReference='%s'", pspReference), e);
        }
    }

    private AdyenHppRequestsRecord getHppRequest(final String merchantReference) {
        try {
            return dao.getHppRequest(merchantReference);
        } catch (final SQLException e) {
            throw new RuntimeException(String.format("Unable to retrieve HPP request for merchantReference='%s'", merchantReference), e);
        }
    }

    private void recordNotification(final NotificationItem notification,
                                    @Nullable final UUID kbAccountId,
                                    @Nullable final UUID kbPaymentId,
                                    @Nullable final UUID kbPaymentTransactionId,
                                    @Nullable final TransactionType transactionType,
                                    final DateTime utcNow,
                                    final UUID kbTenantId) {
        try {
            dao.addNotification(kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, notification, utcNow, kbTenantId);
        } catch (final SQLException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Unable to record notification %s", notification), e);
        }
    }

    private AdyenResponsesRecord updateResponse(final NotificationItem notification, final UUID kbTransactionId, final boolean isHPP, final PaymentPluginStatus paymentPluginStatus, final UUID kbTenantId) {
        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForPluginStatus(paymentPluginStatus);
        final Iterable<PluginProperty> pluginProperties = toPluginProperties(notification, isHPP, paymentPluginStatus);
        try {
            return dao.updateResponse(kbTransactionId, paymentServiceProviderResult, pluginProperties, kbTenantId);
        } catch (final SQLException e) {
            // Have Adyen retry
            throw new RuntimeException(String.format("Unable to update response for kbTransactionId='%s'", kbTransactionId), e);
        }
    }
}
