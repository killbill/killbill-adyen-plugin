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

package org.killbill.billing.plugin.adyen.api;

import java.util.List;

import org.joda.time.DateTime;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.clock.Clock;

public class ExpiredPaymentPolicy {

    private final Clock clock;

    private final AdyenConfigProperties adyenProperties;

    public ExpiredPaymentPolicy(final Clock clock, AdyenConfigProperties adyenProperties) {
        this.clock = clock;
        this.adyenProperties = adyenProperties;
    }

    public boolean isExpired(List<PaymentTransactionInfoPlugin> paymentTransactions) {
        // Expecting only one payment transaction
        if (paymentTransactions.isEmpty() || paymentTransactions.size() > 1) {
            return false;
        }

        final AdyenPaymentTransactionInfoPlugin transaction = (AdyenPaymentTransactionInfoPlugin) paymentTransactions.get(0);
        if (transaction.getCreatedDate() == null) {
            return false;
        }

        if (isAuthOrPurchase(transaction) && transaction.getStatusFromHpp() == PaymentPluginStatus.PENDING) {
            final DateTime expirationDate = expirationDate(transaction);
            return clock.getNow(expirationDate.getZone()).isAfter(expirationDate);
        }

        return false;
    }

    private boolean isAuthOrPurchase(final AdyenPaymentTransactionInfoPlugin transaction) {
        return transaction.getTransactionType() == TransactionType.AUTHORIZE
               || transaction.getTransactionType() == TransactionType.PURCHASE;
    }

    private DateTime expirationDate(AdyenPaymentTransactionInfoPlugin transaction) {
        return transaction.getCreatedDate().plusDays(adyenProperties.getHppExpirationPeriodInDays());
    }
}
