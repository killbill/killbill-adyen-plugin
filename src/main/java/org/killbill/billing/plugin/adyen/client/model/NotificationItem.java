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

package org.killbill.billing.plugin.adyen.client.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.notification.NotificationRequestItem;

public class NotificationItem {

    private final Map additionalData;
    private final BigDecimal amount;
    private final String currency;
    private final String eventCode;
    private final DateTime eventDate;
    private final String merchantAccountCode;
    private final String merchantReference;
    private final List<String> operations;
    private final String originalReference;
    private final String paymentMethod;
    private final String pspReference;
    private final String reason;
    private final Boolean success;

    public NotificationItem(final NotificationRequestItem notificationRequestItem) {
        this.additionalData = null;
        final Amount itemAmount = notificationRequestItem.getAmount();
        this.currency = (itemAmount == null ? null : itemAmount.getCurrency());
        if (this.currency != null && itemAmount.getValue() != null) {
            // The amount is in minor units
            final CurrencyUnit currencyUnit = CurrencyUnit.of(this.currency);
            this.amount = Money.ofMinor(currencyUnit, itemAmount.getValue()).getAmount();
        } else {
            this.amount = null;
        }
        this.eventCode = notificationRequestItem.getEventCode();
        this.eventDate = notificationRequestItem.getEventDate() == null ? null : new DateTime(notificationRequestItem.getEventDate().toGregorianCalendar().getTime());
        this.merchantAccountCode = notificationRequestItem.getMerchantAccountCode();
        this.merchantReference = notificationRequestItem.getMerchantReference();
        this.operations = notificationRequestItem.getOperations() == null ? null : notificationRequestItem.getOperations().getString();
        this.originalReference = notificationRequestItem.getOriginalReference();
        this.paymentMethod = notificationRequestItem.getPaymentMethod();
        this.pspReference = notificationRequestItem.getPspReference();
        this.reason = notificationRequestItem.getReason();
        this.success = notificationRequestItem.isSuccess();
    }

    public NotificationItem(final Map additionalData,
                            final BigDecimal amount,
                            final String currency,
                            final String eventCode,
                            final DateTime eventDate,
                            final String merchantAccountCode,
                            final String merchantReference,
                            final List<String> operations,
                            final String originalReference,
                            final String paymentMethod,
                            final String pspReference,
                            final String reason,
                            final Boolean success) {
        this.additionalData = additionalData;
        this.amount = amount;
        this.currency = currency;
        this.eventCode = eventCode;
        this.eventDate = eventDate;
        this.merchantAccountCode = merchantAccountCode;
        this.merchantReference = merchantReference;
        this.operations = operations;
        this.originalReference = originalReference;
        this.paymentMethod = paymentMethod;
        this.pspReference = pspReference;
        this.reason = reason;
        this.success = success;
    }

    public Map getAdditionalData() {
        return additionalData;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getEventCode() {
        return eventCode;
    }

    public DateTime getEventDate() {
        return eventDate;
    }

    public String getMerchantAccountCode() {
        return merchantAccountCode;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public List<String> getOperations() {
        return operations;
    }

    public String getOriginalReference() {
        return originalReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPspReference() {
        return pspReference;
    }

    public String getReason() {
        return reason;
    }

    public Boolean getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NotificationItem{");
        sb.append("additionalData=").append(additionalData);
        sb.append(", amount=").append(amount);
        sb.append(", currency='").append(currency).append('\'');
        sb.append(", eventCode='").append(eventCode).append('\'');
        sb.append(", eventDate=").append(eventDate);
        sb.append(", merchantAccountCode='").append(merchantAccountCode).append('\'');
        sb.append(", merchantReference='").append(merchantReference).append('\'');
        sb.append(", operations=").append(operations);
        sb.append(", originalReference='").append(originalReference).append('\'');
        sb.append(", paymentMethod='").append(paymentMethod).append('\'');
        sb.append(", pspReference='").append(pspReference).append('\'');
        sb.append(", reason='").append(reason).append('\'');
        sb.append(", success=").append(success);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NotificationItem that = (NotificationItem) o;

        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
            return false;
        }
        if (amount != null ? !amount.equals(that.amount) : that.amount != null) {
            return false;
        }
        if (currency != null ? !currency.equals(that.currency) : that.currency != null) {
            return false;
        }
        if (eventCode != null ? !eventCode.equals(that.eventCode) : that.eventCode != null) {
            return false;
        }
        if (eventDate != null ? !eventDate.equals(that.eventDate) : that.eventDate != null) {
            return false;
        }
        if (merchantAccountCode != null ? !merchantAccountCode.equals(that.merchantAccountCode) : that.merchantAccountCode != null) {
            return false;
        }
        if (merchantReference != null ? !merchantReference.equals(that.merchantReference) : that.merchantReference != null) {
            return false;
        }
        if (operations != null ? !operations.equals(that.operations) : that.operations != null) {
            return false;
        }
        if (originalReference != null ? !originalReference.equals(that.originalReference) : that.originalReference != null) {
            return false;
        }
        if (paymentMethod != null ? !paymentMethod.equals(that.paymentMethod) : that.paymentMethod != null) {
            return false;
        }
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) {
            return false;
        }
        if (success != null ? !success.equals(that.success) : that.success != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = additionalData != null ? additionalData.hashCode() : 0;
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (eventCode != null ? eventCode.hashCode() : 0);
        result = 31 * result + (eventDate != null ? eventDate.hashCode() : 0);
        result = 31 * result + (merchantAccountCode != null ? merchantAccountCode.hashCode() : 0);
        result = 31 * result + (merchantReference != null ? merchantReference.hashCode() : 0);
        result = 31 * result + (operations != null ? operations.hashCode() : 0);
        result = 31 * result + (originalReference != null ? originalReference.hashCode() : 0);
        result = 31 * result + (paymentMethod != null ? paymentMethod.hashCode() : 0);
        result = 31 * result + (pspReference != null ? pspReference.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (success != null ? success.hashCode() : 0);
        return result;
    }
}
