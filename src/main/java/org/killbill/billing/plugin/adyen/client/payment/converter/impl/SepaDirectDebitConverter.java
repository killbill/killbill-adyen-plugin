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

package org.killbill.billing.plugin.adyen.client.payment.converter.impl;

import org.killbill.adyen.payment.BankAccount;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.ELVDirectDebit;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

public class SepaDirectDebitConverter extends PaymentInfoConverter<SepaDirectDebit> {

    private static final String SELECTED_BRAND_SEPA = "sepadirectdebit";

    @Override
    public boolean supportsPaymentInfo(final PaymentInfo type) {
        return type instanceof SepaDirectDebit;
    }

    @SuppressWarnings("deprecation")
    @Override
    public PaymentRequest convertPaymentInfoToPaymentRequest(final SepaDirectDebit sepaDirectDebit) {
        final BankAccount bankAccount = new BankAccount();

        final boolean isELV = sepaDirectDebit instanceof ELVDirectDebit;
        if (isELV) {
            final ELVDirectDebit elvDirectDebit = (ELVDirectDebit) sepaDirectDebit;
            bankAccount.setBankAccountNumber(elvDirectDebit.getAccountNumber());
            bankAccount.setBankLocationId(elvDirectDebit.getBlz());
        } else {
            bankAccount.setIban(sepaDirectDebit.getIban());
            bankAccount.setBic(sepaDirectDebit.getBic());
        }

        bankAccount.setOwnerName(sepaDirectDebit.getSepaAccountHolder());
        bankAccount.setCountryCode(sepaDirectDebit.getCountryCode());

        final PaymentRequest paymentRequest = super.convertPaymentInfoToPaymentRequest(sepaDirectDebit);
        paymentRequest.setBankAccount(bankAccount);

        if (!isELV) {
            // From https://docs.adyen.com/developers/api-manual#sepadirectdebit:
            // An SDD payment request requires a selectedBrand field whose value needs to be sepadirectdebit
            paymentRequest.setSelectedBrand(SELECTED_BRAND_SEPA);
        }

        return paymentRequest;
    }
}
