/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.api.mapping;

import java.math.BigDecimal;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;

public class AdyenPaymentTransaction implements PaymentTransaction {

    private final String gatewayErrorCode;
    private final String gatewayErrorMsg;
    private final PaymentTransaction delegate;

    // Should we also override processedAmount and processedCurrency?
    public AdyenPaymentTransaction(final String gatewayErrorCode, final String gatewayErrorMsg, final PaymentTransaction delegate) {
        this.gatewayErrorCode = gatewayErrorCode;
        this.gatewayErrorMsg = gatewayErrorMsg;
        this.delegate = delegate;
    }

    @Override
    public UUID getPaymentId() {
        return delegate.getPaymentId();
    }

    @Override
    public String getExternalKey() {
        return delegate.getExternalKey();
    }

    @Override
    public TransactionType getTransactionType() {
        return delegate.getTransactionType();
    }

    @Override
    public DateTime getEffectiveDate() {
        return delegate.getEffectiveDate();
    }

    @Override
    public BigDecimal getAmount() {
        return delegate.getAmount();
    }

    @Override
    public Currency getCurrency() {
        return delegate.getCurrency();
    }

    @Override
    public BigDecimal getProcessedAmount() {
        return delegate.getProcessedAmount();
    }

    @Override
    public Currency getProcessedCurrency() {
        return delegate.getProcessedCurrency();
    }

    @Override
    public String getGatewayErrorCode() {
        return gatewayErrorCode;
    }

    @Override
    public String getGatewayErrorMsg() {
        return gatewayErrorMsg;
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        return delegate.getTransactionStatus();
    }

    @Override
    public PaymentTransactionInfoPlugin getPaymentInfoPlugin() {
        return delegate.getPaymentInfoPlugin();
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }

    @Override
    public DateTime getCreatedDate() {
        return delegate.getCreatedDate();
    }

    @Override
    public DateTime getUpdatedDate() {
        return delegate.getUpdatedDate();
    }
}
