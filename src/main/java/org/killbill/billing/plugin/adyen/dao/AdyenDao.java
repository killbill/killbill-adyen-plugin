/*
 * Copyright 2021 Wovenware, Inc
 *
 * Wovenware licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.dao;

import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenNotifications.ADYEN_NOTIFICATIONS;
import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods.ADYEN_PAYMENT_METHODS;
import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses.ADYEN_RESPONSES;

import com.adyen.model.notification.NotificationRequestItem;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.plugin.adyen.api.ProcessorOutputDTO;
import org.killbill.billing.plugin.adyen.client.exceptions.FormaterException;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenNotificationsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;

public class AdyenDao
    extends PluginPaymentDao<
        AdyenResponsesRecord, AdyenResponses, AdyenPaymentMethodsRecord, AdyenPaymentMethods> {

  public AdyenDao(final DataSource dataSource) throws SQLException {
    super(ADYEN_RESPONSES, ADYEN_PAYMENT_METHODS, dataSource);
    // Save space in the database
    objectMapper.setSerializationInclusion(Include.NON_EMPTY);
  }

  // Payment methods
  public void addPaymentMethod(
      final UUID kbAccountId,
      final UUID kbPaymentMethodId,
      final Map<String, String> additionalDataMap,
      final boolean isRecurring,
      final UUID kbTenantId,
      final boolean setDefault)
      throws SQLException {
    final Map<String, String> clonedProperties = new HashMap<>(additionalDataMap);

    execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenResponsesRecord>() {

          @Override
          public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
            DSL.using(conn, dialect, settings)
                .insertInto(
                    ADYEN_PAYMENT_METHODS,
                    ADYEN_PAYMENT_METHODS.KB_ACCOUNT_ID,
                    ADYEN_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID,
                    ADYEN_PAYMENT_METHODS.IS_DELETED,
                    ADYEN_PAYMENT_METHODS.ADDITIONAL_DATA,
                    ADYEN_PAYMENT_METHODS.IS_RECURRING,
                    ADYEN_PAYMENT_METHODS.CREATED_DATE,
                    ADYEN_PAYMENT_METHODS.UPDATED_DATE,
                    ADYEN_PAYMENT_METHODS.IS_DEFAULT,
                    ADYEN_PAYMENT_METHODS.KB_TENANT_ID)
                .values(
                    kbAccountId.toString(),
                    kbPaymentMethodId.toString(),
                    (short) FALSE,
                    asString(clonedProperties),
                    (short) fromBoolean(isRecurring),
                    toLocalDateTime(new DateTime()),
                    toLocalDateTime(new DateTime()),
                    (short) fromBoolean(setDefault),
                    kbTenantId.toString())
                .execute();

            return null;
          }
        });
  }

  public void updateIsDeletePaymentMethod(final UUID kbPaymentMethodId, final UUID kbTenantId)
      throws SQLException {
    execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenPaymentMethodsRecord>() {
          @Override
          public AdyenPaymentMethodsRecord withConnection(final Connection conn)
              throws SQLException {

            DSL.using(conn, dialect, settings)
                .update(ADYEN_PAYMENT_METHODS)
                .set(ADYEN_PAYMENT_METHODS.IS_DELETED, (short) TRUE)
                .where(
                    ADYEN_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId.toString()))
                .and(ADYEN_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                .and(ADYEN_PAYMENT_METHODS.IS_DELETED.equal((short) FALSE))
                .execute();
            return null;
          }
        });
  }

  public void updateRecurringDetailsPaymentMethod(
      final UUID kbPaymentMethodId, final UUID kbTenantId, final String recurringData)
      throws SQLException {
    execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenPaymentMethodsRecord>() {
          @Override
          public AdyenPaymentMethodsRecord withConnection(final Connection conn)
              throws SQLException {

            DSL.using(conn, dialect, settings)
                .update(ADYEN_PAYMENT_METHODS)
                .set(ADYEN_PAYMENT_METHODS.RECURRING_DETAIL_REFERENCE, recurringData)
                .where(
                    ADYEN_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId.toString()))
                .and(ADYEN_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                .execute();
            return null;
          }
        });
  }

  public void updateResponse(UUID kbPaymentId, ProcessorOutputDTO outputDTO, UUID tenantId)
      throws SQLException {
    execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenResponsesRecord>() {
          @Override
          public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {

            DSL.using(conn, dialect, settings)
                .update(ADYEN_RESPONSES)
                .set(ADYEN_RESPONSES.PSP_REFERENCE, outputDTO.getPspReferenceCode())
                .set(ADYEN_RESPONSES.TRANSACTION_STATUS, outputDTO.getStatus().name())
                .where(ADYEN_RESPONSES.KB_PAYMENT_ID.equal(kbPaymentId.toString()))
                .and(ADYEN_RESPONSES.KB_TENANT_ID.equal(tenantId.toString()))
                .execute();
            return null;
          }
        });
  }

  public AdyenPaymentMethodsRecord getPaymentMethodsByMethodId(final UUID paymentMethodId)
      throws SQLException {
    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenPaymentMethodsRecord>() {
          @Override
          public AdyenPaymentMethodsRecord withConnection(final Connection conn)
              throws SQLException {
            final List<AdyenPaymentMethodsRecord> response =
                DSL.using(conn, dialect, settings)
                    .selectFrom(ADYEN_PAYMENT_METHODS)
                    .where(
                        ADYEN_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(
                            paymentMethodId.toString()))
                    .and(ADYEN_PAYMENT_METHODS.IS_DELETED.equal((short) FALSE))
                    .orderBy(ADYEN_PAYMENT_METHODS.RECORD_ID.desc())
                    .fetch();

            if (response.isEmpty()) {
              throw new SQLException();
            }
            return response.get(0);
          }
        });
  }

  // Responses
  public AdyenResponsesRecord addResponse(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbPaymentTransactionId,
      final TransactionType transactionType,
      final BigDecimal amount,
      final Currency currency,
      final Map<String, String> additionalData,
      final UUID kbTenantId)
      throws SQLException {
    String tempCurrency = (currency != null) ? currency.name() : null;

    final BigDecimal dbAmount = (amount != null) ? new BigDecimal(amount.toString()) : null;
    final String dbCurrency = tempCurrency;

    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenResponsesRecord>() {
          @Override
          public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
            return DSL.using(conn, dialect, settings)
                .insertInto(
                    ADYEN_RESPONSES,
                    ADYEN_RESPONSES.KB_ACCOUNT_ID,
                    ADYEN_RESPONSES.KB_PAYMENT_ID,
                    ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                    ADYEN_RESPONSES.TRANSACTION_TYPE,
                    ADYEN_RESPONSES.AMOUNT,
                    ADYEN_RESPONSES.CURRENCY,
                    ADYEN_RESPONSES.ADDITIONAL_DATA,
                    ADYEN_RESPONSES.CREATED_DATE,
                    ADYEN_RESPONSES.KB_TENANT_ID)
                .values(
                    kbAccountId.toString(),
                    kbPaymentId.toString(),
                    kbPaymentTransactionId.toString(),
                    transactionType.toString(),
                    dbAmount,
                    dbCurrency,
                    asString(additionalData),
                    toLocalDateTime(DateTime.now()),
                    kbTenantId.toString())
                .returning()
                .fetchOne();
          }
        });
  }

  public AdyenResponsesRecord addResponse(
      UUID kbAccountId,
      UUID kbPaymentId,
      UUID kbTransactionId,
      TransactionType transactionType,
      BigDecimal amount,
      Currency currency,
      PaymentPluginStatus status,
      String sessionId,
      ProcessorOutputDTO outputDTO,
      UUID tenantId)
      throws SQLException {
    String tempCurrency = (currency != null) ? currency.name() : null;

    final BigDecimal dbAmount = (amount != null) ? new BigDecimal(amount.toString()) : null;
    final String dbCurrency = tempCurrency;

    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenResponsesRecord>() {
          @Override
          public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
            return DSL.using(conn, dialect, settings)
                .insertInto(
                    ADYEN_RESPONSES,
                    ADYEN_RESPONSES.KB_ACCOUNT_ID,
                    ADYEN_RESPONSES.KB_PAYMENT_ID,
                    ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                    ADYEN_RESPONSES.TRANSACTION_TYPE,
                    ADYEN_RESPONSES.TRANSACTION_STATUS,
                    ADYEN_RESPONSES.SESSION_ID,
                    ADYEN_RESPONSES.REFERENCE,
                    ADYEN_RESPONSES.AMOUNT,
                    ADYEN_RESPONSES.CURRENCY,
                    ADYEN_RESPONSES.ADDITIONAL_DATA,
                    ADYEN_RESPONSES.CREATED_DATE,
                    ADYEN_RESPONSES.KB_TENANT_ID)
                .values(
                    kbAccountId.toString(),
                    kbPaymentId.toString(),
                    kbTransactionId.toString(),
                    transactionType.toString(),
                    status.toString(),
                    sessionId,
                    outputDTO.getSecondPaymentReferenceId(),
                    dbAmount,
                    dbCurrency,
                    outputDTO.getAdditionalData() != null
                        ? (asString(outputDTO.getAdditionalData()))
                        : null,
                    toLocalDateTime(DateTime.now()),
                    tenantId.toString())
                .returning()
                .fetchOne();
          }
        });
  }

  public AdyenNotificationsRecord addNotification(
      UUID kbAccountId,
      UUID kbPaymentId,
      UUID kbTransactionId,
      NotificationRequestItem item,
      UUID tenantId)
      throws SQLException {
    String tempCurrency =
        (item.getAmount().getCurrency() != null)
            ? Currency.fromCode(item.getAmount().getCurrency()).toString()
            : null;

    final BigDecimal dbAmount =
        (item.getAmount().getDecimalValue() != null)
            ? new BigDecimal(item.getAmount().getDecimalValue().toString())
            : null;
    final String dbCurrency = tempCurrency;
    Short success = (short) (item.isSuccess() ? 1 : 0);
    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenNotificationsRecord>() {
          @Override
          public AdyenNotificationsRecord withConnection(final Connection conn)
              throws SQLException {
            return DSL.using(conn, dialect, settings)
                .insertInto(
                    ADYEN_NOTIFICATIONS,
                    ADYEN_NOTIFICATIONS.KB_ACCOUNT_ID,
                    ADYEN_NOTIFICATIONS.KB_PAYMENT_ID,
                    ADYEN_NOTIFICATIONS.KB_PAYMENT_TRANSACTION_ID,
                    ADYEN_NOTIFICATIONS.SUCCESS,
                    ADYEN_NOTIFICATIONS.EVENT_CODE,
                    ADYEN_NOTIFICATIONS.MERCHANT_ACCOUNT_CODE,
                    ADYEN_NOTIFICATIONS.MERCHANT_REFERENCE,
                    ADYEN_NOTIFICATIONS.ORIGINAL_REFERENCE,
                    ADYEN_NOTIFICATIONS.REASON,
                    ADYEN_NOTIFICATIONS.PSP_REFERENCE,
                    ADYEN_NOTIFICATIONS.AMOUNT,
                    ADYEN_NOTIFICATIONS.CURRENCY,
                    ADYEN_NOTIFICATIONS.CREATED_DATE,
                    ADYEN_NOTIFICATIONS.ADDITIONAL_DATA,
                    ADYEN_NOTIFICATIONS.KB_TENANT_ID)
                .values(
                    kbAccountId.toString(),
                    kbPaymentId.toString(),
                    kbTransactionId.toString(),
                    success,
                    item.getEventCode(),
                    item.getMerchantAccountCode(),
                    item.getMerchantReference(),
                    item.getOriginalReference(),
                    item.getReason(),
                    item.getPspReference(),
                    dbAmount,
                    dbCurrency,
                    toLocalDateTime(DateTime.now()),
                    item.getAdditionalData() != null ? (asString(item.getAdditionalData())) : null,
                    tenantId.toString())
                .returning()
                .fetchOne();
          }
        });
  }

  public AdyenResponsesRecord getSuccessfulPurchaseResponse(
      final UUID kbPaymentId, final UUID kbTenantId) throws SQLException {
    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenResponsesRecord>() {
          @Override
          public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
            return DSL.using(conn, dialect, settings)
                .selectFrom(ADYEN_RESPONSES)
                .where(DSL.field(ADYEN_RESPONSES.KB_PAYMENT_ID).equal(kbPaymentId.toString()))
                .and(DSL.field(ADYEN_RESPONSES.KB_TENANT_ID).equal(kbTenantId.toString()))
                //                .and(DSL.field(ADYEN_RESPONSES.TRANSACTION_TYPE).equal(PURCHASE))
                .orderBy(ADYEN_RESPONSES.RECORD_ID)
                .fetchOne();
          }
        });
  }

  public AdyenResponsesRecord getResponseFromMerchantReference(final String merchantReference)
      throws SQLException {
    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenResponsesRecord>() {
          @Override
          public AdyenResponsesRecord withConnection(final Connection conn) throws SQLException {
            return DSL.using(conn, dialect, settings)
                .selectFrom(ADYEN_RESPONSES)
                .where(
                    DSL.field(ADYEN_RESPONSES.KB_PAYMENT_TRANSACTION_ID).equal(merchantReference))
                .orderBy(ADYEN_RESPONSES.RECORD_ID)
                .fetchOne();
          }
        });
  }

  public AdyenPaymentMethodsRecord getPaymentMethod(final String kbPaymentMethodId)
      throws SQLException {
    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<AdyenPaymentMethodsRecord>() {
          @Override
          public AdyenPaymentMethodsRecord withConnection(final Connection conn)
              throws SQLException {
            return DSL.using(conn, dialect, settings)
                .selectFrom(ADYEN_PAYMENT_METHODS)
                .where(
                    DSL.field(ADYEN_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID).equal(kbPaymentMethodId))
                .fetchOne();
          }
        });
  }

  public List<AdyenResponsesRecord> getSuccessfulPurchaseResponseList(
      final UUID kbPaymentId, final UUID kbTenantId) throws SQLException {
    return execute(
        dataSource.getConnection(),
        new WithConnectionCallback<List<AdyenResponsesRecord>>() {
          @Override
          public List<AdyenResponsesRecord> withConnection(final Connection conn)
              throws SQLException {
            return DSL.using(conn, dialect, settings)
                .selectFrom(ADYEN_RESPONSES)
                .where(DSL.field(ADYEN_RESPONSES.KB_PAYMENT_ID).equal(kbPaymentId.toString()))
                .and(DSL.field(ADYEN_RESPONSES.KB_TENANT_ID).equal(kbTenantId.toString()))
                .orderBy(ADYEN_RESPONSES.RECORD_ID.desc())
                .fetch();
          }
        });
  }

  @SuppressWarnings("rawtypes")
  public static Map mapFromAdditionalDataString(@Nullable final String additionalData) {
    if (additionalData == null) {
      return ImmutableMap.of();
    }

    try {
      return objectMapper.readValue(additionalData, Map.class);
    } catch (final IOException e) {
      throw new FormaterException(e);
    }
  }
}
