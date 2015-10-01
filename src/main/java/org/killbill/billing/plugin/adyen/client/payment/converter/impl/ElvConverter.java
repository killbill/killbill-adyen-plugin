/*
 * Copyright 2015 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.client.payment.converter.impl;

import org.killbill.adyen.payment.ELV;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Elv;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

public class ElvConverter implements PaymentInfoConverter<Elv> {

    @Override
    public Object convertPaymentInfoToPSPTransferObject(final String holderName, final Elv paymentInfo) {
        final ELV elv = new ELV();
        elv.setBankAccountNumber(paymentInfo.getElvKontoNummer());
        elv.setBankLocationId(paymentInfo.getElvBlz());
        elv.setAccountHolderName(holderName(paymentInfo, holderName));
        final PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setElv(elv);
        return paymentRequest;
    }

    /**
     * There is no 3DSecure for ELV.
     *
     * @param billedAmount Billed amount.
     * @param card         {@link PaymentInfo} of type {@link Card}.
     * @return Always {@literal null} for ELV.
     */
    @Override
    public Object convertPaymentInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        return null;
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.ELV;
    }

    private String holderName(final Elv elv, final String holderName) {
        if (elv.getElvAccountHolder() != null && !elv.getElvAccountHolder().isEmpty()) {
            return elv.getElvAccountHolder();
        } else {
            return holderName;
        }
    }

}
