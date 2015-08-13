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
     * @param card {@link PaymentInfo} of type {@link Card}.
     * @return Always {@literal null} for Sepa Direct Debit.
     */
    @Override
    public Object convertPaymentInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        return null;
    }

    @Override
    public PaymentType getPaymentType() {
        return SEPA_DIRECT_DEBIT;
    }

    private String holderName(final SepaDirectDebit sepaDirectDebit, final String holderName) {
        if (sepaDirectDebit.getSepaAccountHolder() != null && !sepaDirectDebit.getSepaAccountHolder().isEmpty()) {
            return sepaDirectDebit.getSepaAccountHolder();
        } else {
            return holderName;
        }
    }

}
