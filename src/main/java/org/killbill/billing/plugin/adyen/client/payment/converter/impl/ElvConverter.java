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
     * @param card {@link PaymentInfo} of type {@link Card}.
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
