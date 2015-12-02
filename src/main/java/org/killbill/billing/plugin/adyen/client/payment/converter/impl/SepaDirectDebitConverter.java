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

import org.killbill.adyen.payment.BankAccount;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.SEPA_DIRECT_DEBIT;


public class SepaDirectDebitConverter implements PaymentInfoConverter<SepaDirectDebit> {

    private static final String SELECTED_BRAND_SEPA = "sepadirectdebit";

    @Override
    public Object convertPaymentInfoToPSPTransferObject(final String holderName, final SepaDirectDebit sepaDirectDebit) {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setIban(sepaDirectDebit.getIban());
        bankAccount.setBic(sepaDirectDebit.getBic());
        bankAccount.setOwnerName(holderName(sepaDirectDebit, holderName));
        bankAccount.setCountryCode(sepaDirectDebit.getCountryCode());
        final PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBankAccount(bankAccount);
        paymentRequest.setSelectedBrand(SELECTED_BRAND_SEPA);
        return paymentRequest;
    }

    /**
     * There is no 3DSecure for Sepa Direct Debit.
     *
     * @param billedAmount Billed amount.
     * @param card         {@link PaymentInfo} of type {@link Card}.
     * @return Always {@literal null} for Sepa Direct Debit.
     */
    @Override
    public Object convertPaymentInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        return null;
    }

    @Override
    public boolean supportsPaymentType(final PaymentType type) {
        return SEPA_DIRECT_DEBIT.equals(type);
    }

    private String holderName(final SepaDirectDebit sepaDirectDebit, final String holderName) {
        if (sepaDirectDebit.getSepaAccountHolder() != null && !sepaDirectDebit.getSepaAccountHolder().isEmpty()) {
            return sepaDirectDebit.getSepaAccountHolder();
        } else {
            return holderName;
        }
    }

}
