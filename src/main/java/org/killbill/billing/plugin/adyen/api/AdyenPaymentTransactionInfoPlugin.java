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

package org.killbill.billing.plugin.adyen.api;

import java.math.BigDecimal;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public class AdyenPaymentTransactionInfoPlugin extends PluginPaymentTransactionInfoPlugin {

    public AdyenPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                             final UUID kbTransactionPaymentPaymentId,
                                             final TransactionType transactionType,
                                             final BigDecimal amount,
                                             final Currency currency,
                                             final DateTime utcNow,
                                             final PurchaseResult purchaseResult) {
        super(kbPaymentId,
              kbTransactionPaymentPaymentId,
              transactionType,
              amount,
              currency,
              getPaymentPluginStatus(purchaseResult.getAdyenCallErrorStatus(), purchaseResult.getResult()),
              getGatewayError(purchaseResult),
              getGatewayErrorCode(purchaseResult),
              purchaseResult.getPspReference(),
              purchaseResult.getAuthCode(),
              utcNow,
              utcNow,
              PluginProperties.buildPluginProperties(purchaseResult.getFormParameter()));
    }

    public AdyenPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                             final UUID kbTransactionPaymentPaymentId,
                                             final TransactionType transactionType,
                                             final BigDecimal amount,
                                             @Nullable final Currency currency,
                                             final Optional<PaymentServiceProviderResult> pspResult,
                                             final DateTime utcNow,
                                             final PaymentModificationResponse paymentModificationResponse) {
        super(kbPaymentId,
              kbTransactionPaymentPaymentId,
              transactionType,
              amount,
              currency,
              getPaymentPluginStatus(paymentModificationResponse.getAdyenCallErrorStatus(), pspResult),
              getGatewayError(paymentModificationResponse),
              getGatewayErrorCode(paymentModificationResponse),
              paymentModificationResponse.getPspReference(),
              null,
              utcNow,
              utcNow,
              PluginProperties.buildPluginProperties(paymentModificationResponse.getAdditionalData()));
    }

    public AdyenPaymentTransactionInfoPlugin(final AdyenResponsesRecord record) {
        super(UUID.fromString(record.getKbPaymentId()),
              UUID.fromString(record.getKbPaymentTransactionId()),
              TransactionType.valueOf(record.getTransactionType()),
              record.getAmount(),
              Strings.isNullOrEmpty(record.getCurrency()) ? null : Currency.valueOf(record.getCurrency()),
              getPaymentPluginStatus(record),
              getGatewayError(record),
              getGatewayErrorCode(record),
              record.getPspReference(),
              record.getAuthCode(),
              new DateTime(record.getCreatedDate(), DateTimeZone.UTC),
              new DateTime(record.getCreatedDate(), DateTimeZone.UTC),
              AdyenModelPluginBase.buildPluginProperties(record.getAdditionalData()));
    }

    @Override
    public PaymentPluginStatus getStatus() {
        final String hppTransactionStatus = PluginProperties.findPluginPropertyValue(AdyenPaymentPluginApi.PROPERTY_FROM_HPP_TRANSACTION_STATUS, getProperties());
        if (hppTransactionStatus != null) {
            return PaymentPluginStatus.valueOf(hppTransactionStatus);
        } else {
            return super.getStatus();
        }
    }

    private static String getGatewayError(final PurchaseResult purchaseResult) {
        return purchaseResult.getResultCode() != null ? purchaseResult.getResultCode() : purchaseResult.getAdditionalData().get(PurchaseResult.EXCEPTION_MESSAGE);
    }

    private static String getGatewayError(final PaymentModificationResponse paymentModificationResponse) {
        return paymentModificationResponse.getResponse() != null ? paymentModificationResponse.getResponse() : toString(paymentModificationResponse.getAdditionalData().get(PurchaseResult.EXCEPTION_MESSAGE));
    }

    private static String getGatewayError(final AdyenResponsesRecord record) {
        return record.getResultCode() != null ? record.getResultCode() : toString(AdyenDao.fromAdditionalData(record.getAdditionalData()).get(PurchaseResult.EXCEPTION_MESSAGE));
    }

    private static String getGatewayErrorCode(final PurchaseResult purchaseResult) {
        return purchaseResult.getReason() != null ? purchaseResult.getReason() : purchaseResult.getAdditionalData().get(PurchaseResult.EXCEPTION_CLASS);
    }

    private static String getGatewayErrorCode(final PaymentModificationResponse paymentModificationResponse) {
        return toString(paymentModificationResponse.getAdditionalData().get(PurchaseResult.EXCEPTION_CLASS));
    }

    private static String getGatewayErrorCode(final AdyenResponsesRecord record) {
        return record.getRefusalReason() != null ? record.getRefusalReason() : toString(AdyenDao.fromAdditionalData(record.getAdditionalData()).get(PurchaseResult.EXCEPTION_CLASS));
    }

    /**
     * Transforms adyenCallErrorStatus (where there any technical errors?) and pspResult (was the call successful from a business perspective) into the PaymentPluginStatus.
     * Therefor only one of the given params should be present (if there was a technical error we don't have a psp result and the other way around).
     */
    private static PaymentPluginStatus getPaymentPluginStatus(final Optional<AdyenCallErrorStatus> adyenCallErrorStatus, final Optional<PaymentServiceProviderResult> pspResult) {
        checkArgument(adyenCallErrorStatus.isPresent() ^ pspResult.isPresent());
        return (pspResult.isPresent()) ? pspResultToPaymentPluginStatus(pspResult.get()) : adyenCallErrorStatusToPaymentPluginStatus(adyenCallErrorStatus.get());
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final Optional<PaymentServiceProviderResult> pspResult) {
        return (pspResult.isPresent()) ? pspResultToPaymentPluginStatus(pspResult.get()) : adyenCallErrorStatusToPaymentPluginStatus(AdyenCallErrorStatus.UNKNOWN_FAILURE);
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final AdyenResponsesRecord record) {
        if (Strings.isNullOrEmpty(record.getPspResult())) {
            final String adyenCallErrorStatusString = toString(AdyenDao.fromAdditionalData(record.getAdditionalData()).get(PurchaseResult.ADYEN_CALL_ERROR_STATUS));
            final AdyenCallErrorStatus adyenCallErrorStatus;
            if (Strings.emptyToNull(adyenCallErrorStatusString) == null) {
                adyenCallErrorStatus = AdyenCallErrorStatus.UNKNOWN_FAILURE;
            } else {
                adyenCallErrorStatus = AdyenCallErrorStatus.valueOf(adyenCallErrorStatusString);
            }
            return adyenCallErrorStatusToPaymentPluginStatus(adyenCallErrorStatus);
        } else {
            final Optional<PaymentServiceProviderResult> pspResult = Optional.of(PaymentServiceProviderResult.getPaymentResultForId(record.getPspResult()));
            return getPaymentPluginStatus(pspResult);
        }
    }

    private static PaymentPluginStatus adyenCallErrorStatusToPaymentPluginStatus(final AdyenCallErrorStatus adyenCallErrorStatus) {
        switch (adyenCallErrorStatus) {
            case REQUEST_NOT_SEND:
                return PaymentPluginStatus.CANCELED;
            case RESPONSE_ABOUT_INVALID_REQUEST:
                return PaymentPluginStatus.CANCELED;
            case RESPONSE_NOT_RECEIVED:
                return PaymentPluginStatus.UNDEFINED;
            case RESPONSE_INVALID:
                return PaymentPluginStatus.UNDEFINED;
            case UNKNOWN_FAILURE:
                return PaymentPluginStatus.UNDEFINED;
            default:
                return PaymentPluginStatus.UNDEFINED;
        }
    }

    private static PaymentPluginStatus pspResultToPaymentPluginStatus(final PaymentServiceProviderResult pspResult) {
        switch (pspResult) {
            case INITIALISED:
            case REDIRECT_SHOPPER:
            case RECEIVED:
            case PENDING:
                return PaymentPluginStatus.PENDING;
            case AUTHORISED:
                return PaymentPluginStatus.PROCESSED;
            case REFUSED:
            case ERROR:
            case CANCELLED:
                return PaymentPluginStatus.ERROR;
            default:
                return PaymentPluginStatus.UNDEFINED;
        }
    }

    private static String toString(final Object obj) {
        return obj == null ? null : obj.toString();
    }
}
