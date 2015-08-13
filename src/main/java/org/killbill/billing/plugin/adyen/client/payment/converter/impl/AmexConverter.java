package org.killbill.billing.plugin.adyen.client.payment.converter.impl;

import org.killbill.billing.plugin.adyen.client.model.PaymentType;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.AMEX;

public class AmexConverter extends CreditCardConverter {

    @Override
    public PaymentType getPaymentType() {
        return AMEX;
    }

}
