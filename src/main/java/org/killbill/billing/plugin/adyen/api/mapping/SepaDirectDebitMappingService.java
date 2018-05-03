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

import javax.annotation.Nullable;

import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.ELVDirectDebit;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.PluginProperties;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_ACCOUNT_NUMBER;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_BANK_IDENTIFIER_CODE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_HOLDER_NAME;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ELV_BLZ;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_SEPA_COUNTRY_CODE;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_COUNTRY;

public abstract class SepaDirectDebitMappingService {

    public static SepaDirectDebit toPaymentInfo(@Nullable final AccountData account, final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        final SepaDirectDebit sepaDirectDebit;

        final String ddAccountNumber = PluginProperties.getValue(PROPERTY_DD_ACCOUNT_NUMBER, paymentMethodsRecord.getCcNumber(), properties);

        final String elvBlz = PluginProperties.findPluginPropertyValue(PROPERTY_ELV_BLZ, properties);
        if (elvBlz != null) {
            @SuppressWarnings("deprecation") final ELVDirectDebit elvDirectDebit = new ELVDirectDebit();
            elvDirectDebit.setBlz(elvBlz);
            elvDirectDebit.setAccountNumber(ddAccountNumber);
            sepaDirectDebit = elvDirectDebit;
        } else {
            sepaDirectDebit = new SepaDirectDebit();
            sepaDirectDebit.setIban(ddAccountNumber);

            final String ddBic = PluginProperties.findPluginPropertyValue(PROPERTY_DD_BANK_IDENTIFIER_CODE, properties);
            sepaDirectDebit.setBic(ddBic);
        }

        final String paymentMethodHolderName = holderName(paymentMethodsRecord.getCcFirstName(), paymentMethodsRecord.getCcLastName());
        final String ddHolderName = PluginProperties.getValue(PROPERTY_DD_HOLDER_NAME, paymentMethodHolderName, properties);
        sepaDirectDebit.setSepaAccountHolder(ddHolderName);

        String countryCode = PluginProperties.findPluginPropertyValue(PROPERTY_SEPA_COUNTRY_CODE, properties);
        if(countryCode == null) {
            countryCode = PluginProperties.getValue(PROPERTY_COUNTRY, paymentMethodsRecord.getCountry(), properties);
            if (countryCode == null && account != null) {
                countryCode = account.getCountry();
            }
        }
        sepaDirectDebit.setCountryCode(countryCode);

        return sepaDirectDebit;
    }

    private static String holderName(final String firstName, final String lastName) {
        return String.format("%s%s", firstName == null ? "" : firstName + " ", lastName);
    }
}
