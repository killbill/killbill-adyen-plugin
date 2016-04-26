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

package org.killbill.billing.plugin.adyen.client.payment.converter.impl;

import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.MISTER_CASH;

public class MisterCashConverter extends CreditCardConverter {

    private static final String SELECTED_BRAND_MISTER_CASH = "bcmc";

    @Override
    public Object convertPaymentInfoToPSPTransferObject(final String holderName, final Card paymentInfo) {
        final PaymentRequest result = (PaymentRequest) super.convertPaymentInfoToPSPTransferObject(holderName, paymentInfo);
        // From https://docs.adyen.com/developers/api-manual#paymentrequests:
        // For the MisterCash payment method, it can be set to bcmc, to be processed like a MisterCash card
        result.setSelectedBrand(SELECTED_BRAND_MISTER_CASH);
        return result;
    }

    @Override
    public boolean supportsPaymentType(final PaymentType type) {
        return MISTER_CASH.equals(type);
    }
}
