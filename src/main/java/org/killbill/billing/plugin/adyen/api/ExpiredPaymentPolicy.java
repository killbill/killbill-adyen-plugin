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
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.clock.Clock;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_FROM_HPP;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_HPP_COMPLETION;

public class ExpiredPaymentPolicy {

    private final Clock clock;

    private final AdyenConfigProperties adyenProperties;

    public ExpiredPaymentPolicy(final Clock clock, final AdyenConfigProperties adyenProperties) {
        this.clock = clock;
        this.adyenProperties = adyenProperties;
    }

    public boolean isExpired(final List<PaymentTransactionInfoPlugin> paymentTransactions) {
        if (!containOnlyAuthsOrPurchases(paymentTransactions)) {
            return false;
        }

        final AdyenPaymentTransactionInfoPlugin transaction = (AdyenPaymentTransactionInfoPlugin) latestTransaction(paymentTransactions);
        if (transaction.getCreatedDate() == null) {
            return false;
        }

        if (transaction.getStatus() == PaymentPluginStatus.PENDING) {
            final DateTime expirationDate = expirationDateForInitialTransactionType(transaction);
            return clock.getNow(expirationDate.getZone()).isAfter(expirationDate);
        }

        return false;
    }

    public PaymentTransactionInfoPlugin latestTransaction(final List<PaymentTransactionInfoPlugin> paymentTransactions) {
        return Iterables.getLast(paymentTransactions);
    }

    private boolean containOnlyAuthsOrPurchases(final List<PaymentTransactionInfoPlugin> transactions) {
        for(final PaymentTransactionInfoPlugin transaction : transactions) {
            if (transaction.getTransactionType() != TransactionType.AUTHORIZE
                && transaction.getTransactionType() != TransactionType.PURCHASE) {
                return false;
            }
        }
        return true;
    }

    private DateTime expirationDateForInitialTransactionType(final AdyenPaymentTransactionInfoPlugin transaction) {
        if (is3ds(transaction)) {
            return transaction.getCreatedDate().plus(adyenProperties.getPending3DsPaymentExpirationPeriod());
        } else if(isHppBuildFormTransaction(transaction)) {
            return transaction.getCreatedDate().plus(adyenProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod());
        }

        final String paymentMethod = getPaymentMethod(transaction);
        return transaction.getCreatedDate().plus(adyenProperties.getPendingPaymentExpirationPeriod(paymentMethod));
    }

    private boolean isHppBuildFormTransaction(final AdyenPaymentTransactionInfoPlugin transaction) {
        if (!transaction.getAdyenResponseRecord().isPresent()) {
            return false;
        }

        final AdyenResponsesRecord adyenResponsesRecord = transaction.getAdyenResponseRecord().get();
        return isHppPayment(adyenResponsesRecord) && !isHppCompletionTransaction(adyenResponsesRecord);
    }

    private boolean is3ds(final AdyenPaymentTransactionInfoPlugin transaction) {
        if (!transaction.getAdyenResponseRecord().isPresent()) {
            return false;
        }

        final AdyenResponsesRecord adyenResponsesRecord = transaction.getAdyenResponseRecord().get();
        //Redirect shopper response can exist for both 3-DS or HPP pending payment.
        return PaymentServiceProviderResult.REDIRECT_SHOPPER.toString().equals(adyenResponsesRecord.getResultCode())
               && !isHppPayment(adyenResponsesRecord);
    }

    private boolean isHppCompletionTransaction(final AdyenResponsesRecord adyenResponsesRecord) {
        return Boolean.valueOf(MoreObjects.firstNonNull(AdyenDao.fromAdditionalData(adyenResponsesRecord.getAdditionalData()).get(PROPERTY_HPP_COMPLETION), false).toString());
    }

    private boolean isHppPayment(final AdyenResponsesRecord adyenResponsesRecord) {
        return Boolean.valueOf(MoreObjects.firstNonNull(AdyenDao.fromAdditionalData(adyenResponsesRecord.getAdditionalData()).get(PROPERTY_FROM_HPP), false).toString());
    }

    // paymentMethod comes from the notification, brandCode from the HPP request
    private String getPaymentMethod(final PaymentTransactionInfoPlugin transaction) {
        final String paymentMethod = PluginProperties.findPluginPropertyValue("paymentMethod", transaction.getProperties());
        return paymentMethod != null ? paymentMethod : PluginProperties.findPluginPropertyValue("brandCode", transaction.getProperties());
    }
}
