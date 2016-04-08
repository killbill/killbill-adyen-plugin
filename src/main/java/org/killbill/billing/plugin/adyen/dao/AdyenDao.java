/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenHppRequestsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenNotificationsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenHppRequests.ADYEN_HPP_REQUESTS;
import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenNotifications.ADYEN_NOTIFICATIONS;
import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods.ADYEN_PAYMENT_METHODS;
import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses.ADYEN_RESPONSES;

public class AdyenDao extends PluginPaymentDao<AdyenResponsesRecord, AdyenResponses, AdyenPaymentMethodsRecord, AdyenPaymentMethods> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Joiner JOINER = Joiner.on(",");

    public AdyenDao(final DataSource dataSource) throws SQLException {
        super(AdyenResponses.ADYEN_RESPONSES, AdyenPaymentMethods.ADYEN_PAYMENT_METHODS, dataSource);
    }

    // Payment methods

    public void setPaymentMethodToken(final String kbPaymentMethodId, final String token, final String kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<AdyenResponsesRecord>() {
                    @Override
                    public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .update(ADYEN_PAYMENT_METHODS)
                           .set(ADYEN_PAYMENT_METHODS.TOKEN, token)
                           .where(ADYEN_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId))
                           .and(ADYEN_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId))
                           .and(ADYEN_PAYMENT_METHODS.IS_DELETED.equal(FALSE))
                           .execute();
                        return null;
                    }
                });
    }

    // HPP requests

    public void addHppRequest(final UUID kbAccountId,
                              @Nullable final UUID kbPaymentId,
                              @Nullable final UUID kbPaymentTransactionId,
                              final String transactionExternalKey,
                              final Map additionalDataMap,
                              final DateTime utcNow,
                              final UUID kbTenantId) throws SQLException {
        final String additionalData = getAdditionalData(additionalDataMap);

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .insertInto(ADYEN_HPP_REQUESTS,
                                       ADYEN_HPP_REQUESTS.KB_ACCOUNT_ID,
                                       ADYEN_HPP_REQUESTS.KB_PAYMENT_ID,
                                       ADYEN_HPP_REQUESTS.KB_PAYMENT_TRANSACTION_ID,
                                       ADYEN_HPP_REQUESTS.TRANSACTION_EXTERNAL_KEY,
                                       ADYEN_HPP_REQUESTS.ADDITIONAL_DATA,
                                       ADYEN_HPP_REQUESTS.CREATED_DATE,
                                       ADYEN_HPP_REQUESTS.KB_TENANT_ID)
                           .values(kbAccountId.toString(),
                                   kbPaymentId != null ? kbPaymentId.toString() : null,
                                   kbPaymentTransactionId != null ? kbPaymentTransactionId.toString() : null,
                                   transactionExternalKey,
                                   additionalData,
                                   toTimestamp(utcNow),
                                   kbTenantId.toString())
                           .execute();
                        return null;
                    }
                });
    }

    public AdyenHppRequestsRecord getHppRequest(final String merchantReference) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<AdyenHppRequestsRecord>() {
                           @Override
                           public AdyenHppRequestsRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(ADYEN_HPP_REQUESTS)
                                         .where(ADYEN_HPP_REQUESTS.TRANSACTION_EXTERNAL_KEY.equal(merchantReference))
                                         .orderBy(ADYEN_HPP_REQUESTS.RECORD_ID.desc())
                                         .fetchOne();
                           }
                       });
    }

    // Responses

    public AdyenResponsesRecord addAdyenResponse(final UUID kbAccountId,
                                                 final UUID kbPaymentId,
                                                 final UUID kbPaymentTransactionId,
                                                 final TransactionType transactionType,
                                                 final BigDecimal amount,
                                                 final Currency currency,
                                                 final Map additionalData,
                                                 final DateTime utcNow,
                                                 final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<AdyenResponsesRecord>() {
                           @Override
                           public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
                               DSL.using(conn, dialect, settings)
                                  .insertInto(ADYEN_RESPONSES,
                                              ADYEN_RESPONSES.KB_ACCOUNT_ID,
                                              ADYEN_RESPONSES.KB_PAYMENT_ID,
                                              ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                              ADYEN_RESPONSES.TRANSACTION_TYPE,
                                              ADYEN_RESPONSES.AMOUNT,
                                              ADYEN_RESPONSES.CURRENCY,
                                              ADYEN_RESPONSES.PSP_REFERENCE,
                                              ADYEN_RESPONSES.ADDITIONAL_DATA,
                                              ADYEN_RESPONSES.CREATED_DATE,
                                              ADYEN_RESPONSES.KB_TENANT_ID)
                                  .values(kbAccountId.toString(),
                                          kbPaymentId.toString(),
                                          kbPaymentTransactionId.toString(),
                                          transactionType.toString(),
                                          amount,
                                          currency == null ? null : currency.name(),
                                          getProperty(AdyenPaymentPluginApi.PROPERTY_PSP_REFERENCE, additionalData),
                                          asString(additionalData),
                                          toTimestamp(utcNow),
                                          kbTenantId.toString())
                                  .execute();

                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(ADYEN_RESPONSES)
                                         .where(ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                         .and(ADYEN_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .orderBy(ADYEN_RESPONSES.RECORD_ID.desc())
                                         .fetchOne();
                           }
                       });
    }

    public void addResponse(final UUID kbAccountId,
                            final UUID kbPaymentId,
                            final UUID kbPaymentTransactionId,
                            final TransactionType transactionType,
                            final BigDecimal amount,
                            final Currency currency,
                            final PurchaseResult result,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {
        final String dccAmountValue = getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result);
        final String additionalData = getAdditionalData(result);

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .insertInto(ADYEN_RESPONSES,
                                       ADYEN_RESPONSES.KB_ACCOUNT_ID,
                                       ADYEN_RESPONSES.KB_PAYMENT_ID,
                                       ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                       ADYEN_RESPONSES.TRANSACTION_TYPE,
                                       ADYEN_RESPONSES.AMOUNT,
                                       ADYEN_RESPONSES.CURRENCY,
                                       ADYEN_RESPONSES.PSP_RESULT,
                                       ADYEN_RESPONSES.PSP_REFERENCE,
                                       ADYEN_RESPONSES.AUTH_CODE,
                                       ADYEN_RESPONSES.RESULT_CODE,
                                       ADYEN_RESPONSES.REFUSAL_REASON,
                                       ADYEN_RESPONSES.REFERENCE,
                                       ADYEN_RESPONSES.PSP_ERROR_CODES,
                                       ADYEN_RESPONSES.PAYMENT_INTERNAL_REF,
                                       ADYEN_RESPONSES.FORM_URL,
                                       ADYEN_RESPONSES.DCC_AMOUNT,
                                       ADYEN_RESPONSES.DCC_CURRENCY,
                                       ADYEN_RESPONSES.DCC_SIGNATURE,
                                       ADYEN_RESPONSES.ISSUER_URL,
                                       ADYEN_RESPONSES.MD,
                                       ADYEN_RESPONSES.PA_REQUEST,
                                       ADYEN_RESPONSES.ADDITIONAL_DATA,
                                       ADYEN_RESPONSES.CREATED_DATE,
                                       ADYEN_RESPONSES.KB_TENANT_ID)
                           .values(kbAccountId.toString(),
                                   kbPaymentId.toString(),
                                   kbPaymentTransactionId.toString(),
                                   transactionType.toString(),
                                   amount,
                                   currency,
                                   result.getResult().isPresent() ? result.getResult().get().toString() : null,
                                   result.getPspReference(),
                                   result.getAuthCode(),
                                   result.getResultCode(),
                                   result.getReason(),
                                   result.getReference(),
                                   null,
                                   result.getPaymentInternalRef(),
                                   result.getFormUrl(),
                                   dccAmountValue == null ? null : new BigDecimal(dccAmountValue),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_MD, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result),
                                   additionalData,
                                   toTimestamp(utcNow),
                                   kbTenantId.toString())
                           .execute();
                        return null;
                    }
                });
    }

    public void addResponse(final UUID kbAccountId,
                            final UUID kbPaymentId,
                            final UUID kbPaymentTransactionId,
                            final TransactionType transactionType,
                            @Nullable final BigDecimal amount,
                            @Nullable final Currency currency,
                            final PaymentModificationResponse result,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {
        final String dccAmountValue = getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result);
        final String additionalData = getAdditionalData(result);

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .insertInto(ADYEN_RESPONSES,
                                       ADYEN_RESPONSES.KB_ACCOUNT_ID,
                                       ADYEN_RESPONSES.KB_PAYMENT_ID,
                                       ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                       ADYEN_RESPONSES.TRANSACTION_TYPE,
                                       ADYEN_RESPONSES.AMOUNT,
                                       ADYEN_RESPONSES.CURRENCY,
                                       ADYEN_RESPONSES.PSP_RESULT,
                                       ADYEN_RESPONSES.PSP_REFERENCE,
                                       ADYEN_RESPONSES.AUTH_CODE,
                                       ADYEN_RESPONSES.RESULT_CODE,
                                       ADYEN_RESPONSES.REFUSAL_REASON,
                                       ADYEN_RESPONSES.REFERENCE,
                                       ADYEN_RESPONSES.PSP_ERROR_CODES,
                                       ADYEN_RESPONSES.PAYMENT_INTERNAL_REF,
                                       ADYEN_RESPONSES.FORM_URL,
                                       ADYEN_RESPONSES.DCC_AMOUNT,
                                       ADYEN_RESPONSES.DCC_CURRENCY,
                                       ADYEN_RESPONSES.DCC_SIGNATURE,
                                       ADYEN_RESPONSES.ISSUER_URL,
                                       ADYEN_RESPONSES.MD,
                                       ADYEN_RESPONSES.PA_REQUEST,
                                       ADYEN_RESPONSES.ADDITIONAL_DATA,
                                       ADYEN_RESPONSES.CREATED_DATE,
                                       ADYEN_RESPONSES.KB_TENANT_ID)
                           .values(kbAccountId.toString(),
                                   kbPaymentId.toString(),
                                   kbPaymentTransactionId.toString(),
                                   transactionType.toString(),
                                   amount,
                                   currency,
                                   result.getResponse(),
                                   result.getPspReference(),
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   dccAmountValue == null ? null : new BigDecimal(dccAmountValue),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_MD, result),
                                   getProperty(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result),
                                   additionalData,
                                   toTimestamp(utcNow),
                                   kbTenantId.toString())
                           .execute();
                        return null;
                    }
                });
    }

    public AdyenResponsesRecord updateResponse(final UUID kbPaymentTransactionId, final Iterable<PluginProperty> additionalPluginProperties, final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalProperties = PluginProperties.toMap(additionalPluginProperties);

        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<AdyenResponsesRecord>() {
                           @Override
                           public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
                               final AdyenResponsesRecord response = DSL.using(conn, dialect, settings)
                                                                        .selectFrom(ADYEN_RESPONSES)
                                                                        .where(ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                                                        .and(ADYEN_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                                                        .orderBy(ADYEN_RESPONSES.RECORD_ID.desc())
                                                                        .fetchOne();

                               if (response == null) {
                                   throw new SQLException("Unable to retrieve response row for kbPaymentTransactionId " + kbPaymentTransactionId);
                               }

                               final Map originalData = new HashMap(fromAdditionalData(response.getAdditionalData()));
                               originalData.putAll(additionalProperties);
                               final String mergedAdditionalData = getAdditionalData(originalData);

                               DSL.using(conn, dialect, settings)
                                  .update(ADYEN_RESPONSES)
                                  .set(ADYEN_RESPONSES.PSP_REFERENCE, getProperty(AdyenPaymentPluginApi.PROPERTY_PSP_REFERENCE, additionalProperties))
                                  .set(ADYEN_RESPONSES.ADDITIONAL_DATA, mergedAdditionalData)
                                  .where(ADYEN_RESPONSES.RECORD_ID.equal(response.getRecordId()))
                                  .execute();

                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(ADYEN_RESPONSES)
                                         .where(ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                         .and(ADYEN_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .orderBy(ADYEN_RESPONSES.RECORD_ID.desc())
                                         .fetchOne();
                           }
                       });
    }

    public AdyenResponsesRecord getResponse(final String pspReference) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<AdyenResponsesRecord>() {
                           @Override
                           public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(ADYEN_RESPONSES)
                                         .where(ADYEN_RESPONSES.PSP_REFERENCE.equal(pspReference))
                                         .orderBy(ADYEN_RESPONSES.RECORD_ID.desc())
                                         .fetchOne();
                           }
                       });
    }

    // Notifications

    public void addNotification(@Nullable final UUID kbAccountId,
                                @Nullable final UUID kbPaymentId,
                                @Nullable final UUID kbPaymentTransactionId,
                                @Nullable final TransactionType transactionType,
                                final NotificationItem notification,
                                final DateTime utcNow,
                                @Nullable final UUID kbTenantId) throws SQLException {
        final String additionalData = getAdditionalData(notification.getAdditionalData());

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .insertInto(ADYEN_NOTIFICATIONS,
                                       ADYEN_NOTIFICATIONS.KB_ACCOUNT_ID,
                                       ADYEN_NOTIFICATIONS.KB_PAYMENT_ID,
                                       ADYEN_NOTIFICATIONS.KB_PAYMENT_TRANSACTION_ID,
                                       ADYEN_NOTIFICATIONS.TRANSACTION_TYPE,
                                       ADYEN_NOTIFICATIONS.AMOUNT,
                                       ADYEN_NOTIFICATIONS.CURRENCY,
                                       ADYEN_NOTIFICATIONS.EVENT_CODE,
                                       ADYEN_NOTIFICATIONS.EVENT_DATE,
                                       ADYEN_NOTIFICATIONS.MERCHANT_ACCOUNT_CODE,
                                       ADYEN_NOTIFICATIONS.MERCHANT_REFERENCE,
                                       ADYEN_NOTIFICATIONS.OPERATIONS,
                                       ADYEN_NOTIFICATIONS.ORIGINAL_REFERENCE,
                                       ADYEN_NOTIFICATIONS.PAYMENT_METHOD,
                                       ADYEN_NOTIFICATIONS.PSP_REFERENCE,
                                       ADYEN_NOTIFICATIONS.REASON,
                                       ADYEN_NOTIFICATIONS.SUCCESS,
                                       ADYEN_NOTIFICATIONS.ADDITIONAL_DATA,
                                       ADYEN_NOTIFICATIONS.CREATED_DATE,
                                       ADYEN_NOTIFICATIONS.KB_TENANT_ID)
                           .values(kbAccountId == null ? null : kbAccountId.toString(),
                                   kbPaymentId == null ? null : kbPaymentId.toString(),
                                   kbPaymentTransactionId == null ? null : kbPaymentTransactionId.toString(),
                                   transactionType == null ? null : transactionType.toString(),
                                   notification.getAmount(),
                                   notification.getCurrency(),
                                   notification.getEventCode(),
                                   toTimestamp(notification.getEventDate()),
                                   notification.getMerchantAccountCode(),
                                   notification.getMerchantReference(),
                                   getString(notification.getOperations()),
                                   notification.getOriginalReference(),
                                   notification.getPaymentMethod(),
                                   notification.getPspReference(),
                                   notification.getReason(),
                                   notification.getSuccess() == null ? FALSE : fromBoolean(notification.getSuccess()),
                                   additionalData,
                                   toTimestamp(utcNow),
                                   kbTenantId == null ? null : kbTenantId.toString())
                           .execute();
                        return null;
                    }
                });
    }

    public AdyenNotificationsRecord getNotification(final String pspReference) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<AdyenNotificationsRecord>() {
                           @Override
                           public AdyenNotificationsRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(ADYEN_NOTIFICATIONS)
                                         .where(ADYEN_NOTIFICATIONS.PSP_REFERENCE.equal(pspReference))
                                         .orderBy(ADYEN_NOTIFICATIONS.RECORD_ID.desc())
                                         .fetchOne();
                           }
                       });
    }

    private String getString(@Nullable final Iterable iterable) {
        if (iterable == null || !iterable.iterator().hasNext()) {
            return null;
        } else {
            return JOINER.join(Iterables.transform(iterable, Functions.toStringFunction()));
        }
    }

    private String getProperty(final String key, final PurchaseResult result) {
        return getProperty(key, result.getFormParameter());
    }

    private String getProperty(final String key, final PaymentModificationResponse response) {
        return getProperty(key, response.getAdditionalData());
    }

    private String getAdditionalData(final PurchaseResult result) throws SQLException {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        if (result.getAdditionalData() != null && !result.getAdditionalData().isEmpty()) {
            additionalDataMap.putAll(result.getAdditionalData());
        }
        if (result.getFormParameter() != null && !result.getFormParameter().isEmpty()) {
            additionalDataMap.putAll(result.getFormParameter());
        }
        if (additionalDataMap.isEmpty()) {
            return null;
        } else {
            return getAdditionalData(additionalDataMap);
        }
    }

    private String getAdditionalData(final PaymentModificationResponse response) throws SQLException {
        return getAdditionalData(response.getAdditionalData());
    }

    private String getAdditionalData(final Map additionalData) throws SQLException {
        if (additionalData == null || additionalData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(additionalData);
        } catch (final JsonProcessingException e) {
            throw new SQLException(e);
        }
    }

    public static Map fromAdditionalData(final String additionalData) {
        if (additionalData == null) {
            return ImmutableMap.of();
        }

        try {
            return objectMapper.readValue(additionalData, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
