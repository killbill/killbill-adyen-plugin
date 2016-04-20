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

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;

public abstract class BaseAdyenPaymentServiceProviderPort {

    protected Long toMinorUnits(final PaymentData paymentData, final BigDecimal amountBD) {
        if (paymentData == null) {
            return null;
        }
        return toMinorUnits(paymentData.getPaymentInfo(), amountBD);
    }

    protected Long toMinorUnits(final PaymentInfo paymentInfo, final BigDecimal amountBD) {
        if (paymentInfo == null) {
            return null;
        }
        return toMinorUnits(paymentInfo.getPaymentProvider(), amountBD);
    }

    protected Long toMinorUnits(final PaymentProvider paymentProvider, final BigDecimal amountBD) {
        if (paymentProvider == null || paymentProvider.getCurrency() == null) {
            return null;
        }
        return toMinorUnits(paymentProvider.getCurrency().getCurrencyCode(), amountBD);
    }

    protected Long toMinorUnits(final String currencyIsoCode, final BigDecimal amountBD) {
        // The payment amount specified in minor units, without the decimal separator
        final CurrencyUnit currencyUnit = CurrencyUnit.of(currencyIsoCode);
        // HALF_UP consistent with org.killbill.billing.util.currency.KillBillMoney, although this might need to be configurable?
        return Money.of(currencyUnit, amountBD, RoundingMode.HALF_UP).getAmountMinorLong();
    }
}
