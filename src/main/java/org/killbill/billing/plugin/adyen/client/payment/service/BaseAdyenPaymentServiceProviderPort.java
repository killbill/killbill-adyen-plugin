/*
 * Copyright 2015-2016 Groupon, Inc
 * Copyright 2015-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.service;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.slf4j.Logger;

public abstract class BaseAdyenPaymentServiceProviderPort {

    protected Logger logger;

    protected void logTransaction(final String transactionType, final UserData userData, final String merchantAccount, final PaymentData paymentData, @Nullable final PurchaseResult result, @Nullable final AdyenCallResult<?> adyenCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendMerchantAccount(logBuffer, merchantAccount);
        appendPaymentData(logBuffer, paymentData);
        appendUserData(logBuffer, userData);
        appendPurchaseResult(logBuffer, result);
        if (adyenCall != null) {
            appendDuration(logBuffer, adyenCall.getDuration());
        }
        logBuffer.append(", error=false");

        logger.info(logBuffer.toString());
    }

    protected void logTransaction(final String transactionType, final String pspReference, String merchantAccount, final PaymentData paymentData, final PaymentModificationResponse response, final AdyenCallResult<?> adyenCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendMerchantAccount(logBuffer, merchantAccount);
        appendPaymentData(logBuffer, paymentData);
        appendPspReference(logBuffer, pspReference);
        appendModificationResponse(logBuffer, response);
        appendDuration(logBuffer, adyenCall.getDuration());
        logBuffer.append(", error=false");

        logger.info(logBuffer.toString());
    }

    protected void logTransactionError(final String transactionType, final UserData userData, String merchantAccount, final PaymentData paymentData, final AdyenCallResult<?> adyenCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendMerchantAccount(logBuffer, merchantAccount);
        appendPaymentData(logBuffer, paymentData);
        appendUserData(logBuffer, userData);
        appendAdyenCall(logBuffer, adyenCall);
        logBuffer.append(", error=true");

        logger.info(logBuffer.toString());
    }

    protected void logTransactionError(final String transactionType, final String pspReference, String merchantAccount, final PaymentData paymentData, final AdyenCallResult<?> adyenCall) {
        final StringBuilder logBuffer = new StringBuilder();
        appendTransactionType(logBuffer, transactionType);
        appendMerchantAccount(logBuffer, merchantAccount);
        appendPspReference(logBuffer, pspReference);
        appendPaymentData(logBuffer, paymentData);
        appendPspReference(logBuffer, pspReference);
        appendAdyenCall(logBuffer, adyenCall);
        logBuffer.append(", error=true");

        logger.info(logBuffer.toString());
    }

    private void appendTransactionType(final StringBuilder buffer, final String transactionType) {
        buffer.append("op='").append(transactionType).append("'");
    }

    private void appendMerchantAccount(final StringBuilder buffer, final String merchantAccount) {
        buffer.append(", merchantAccount='").append(merchantAccount).append("'");
    }

    private void appendPaymentData(final StringBuilder buffer, final PaymentData paymentData) {
        if (paymentData == null ) {
            return;
        }

        if (paymentData.getAmount() != null) {
            buffer.append(", amount='")
                  .append(paymentData.getAmount())
                  .append("'");
        }
        if (paymentData.getAmount() != null) {
            buffer.append(", amount='")
                  .append(paymentData.getAmount())
                  .append("'");
        }
        if (paymentData.getPaymentTransactionExternalKey() != null) {
            buffer.append(", paymentTransactionExternalKey='")
                  .append(paymentData.getPaymentTransactionExternalKey())
                  .append("'");
        }
    }

    private void appendUserData(final StringBuilder buffer, final UserData userData) {
        if (userData != null && userData.getShopperReference() != null) {
            buffer.append(", customerId='")
                  .append(userData.getShopperReference())
                  .append("'");
        }
    }


    private void appendPspReference(final StringBuilder buffer, final String pspReference) {
        if (pspReference != null) {
            buffer.append(", pspReference='")
                  .append(pspReference)
                  .append("'");
        }
    }

    private void appendPurchaseResult(final StringBuilder buffer, @Nullable final PurchaseResult result) {
        if (result != null) {
            buffer.append(", ").append(result);
        }
    }

    private void appendModificationResponse(final StringBuilder buffer, final PaymentModificationResponse response) {
        buffer.append(", ").append(response);
    }

    private void appendDuration(final StringBuilder buffer, final long duration) {
        buffer.append(", duration=").append(duration);
    }

    private void appendAdyenCall(final StringBuilder buffer, final AdyenCallResult<?> adyenCall) {
        buffer.append(", ").append(adyenCall);
    }
}
