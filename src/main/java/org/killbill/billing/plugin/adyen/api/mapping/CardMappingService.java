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

package org.killbill.billing.plugin.adyen.api.mapping;

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.PluginProperties;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_CC_ISSUER_COUNTRY;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_EXPIRATION_MONTH;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_EXPIRATION_YEAR;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_FIRST_NAME;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_LAST_NAME;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_NUMBER;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_VERIFICATION_VALUE;

// By convention, support the same keys as the Ruby plugins (https://github.com/killbill/killbill-plugin-framework-ruby/blob/master/lib/killbill/helpers/active_merchant/payment_plugin.rb)
public abstract class CardMappingService {

    public static Card toPaymentInfo(final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        final Card card = new Card();

        final String ccNumber = PluginProperties.getValue(PROPERTY_CC_NUMBER, paymentMethodsRecord.getCcNumber(), properties);
        card.setNumber(ccNumber);

        final String ccFirstName = PluginProperties.getValue(PROPERTY_CC_FIRST_NAME, paymentMethodsRecord.getCcFirstName(), properties);
        final String ccLastName = PluginProperties.getValue(PROPERTY_CC_LAST_NAME, paymentMethodsRecord.getCcLastName(), properties);
        card.setHolderName(holderName(ccFirstName, ccLastName));

        final String ccExpirationMonth = PluginProperties.getValue(PROPERTY_CC_EXPIRATION_MONTH, paymentMethodsRecord.getCcExpMonth(), properties);
        if (ccExpirationMonth != null) {
            card.setExpiryMonth(Integer.valueOf(ccExpirationMonth));
        }

        final String ccExpirationYear = PluginProperties.getValue(PROPERTY_CC_EXPIRATION_YEAR, paymentMethodsRecord.getCcExpYear(), properties);
        if (ccExpirationYear != null) {
            card.setExpiryYear(Integer.valueOf(ccExpirationYear));
        }

        final String ccVerificationValue = PluginProperties.getValue(PROPERTY_CC_VERIFICATION_VALUE, paymentMethodsRecord.getCcVerificationValue(), properties);
        card.setCvc(ccVerificationValue);

        final String issuerCountry = PluginProperties.findPluginPropertyValue(PROPERTY_CC_ISSUER_COUNTRY, properties);
        card.setIssuerCountry(issuerCountry);

        return card;
    }

    private static String holderName(final String firstName, final String lastName) {
        return String.format("%s%s", firstName == null ? "" : firstName + " ", lastName);
    }
}
