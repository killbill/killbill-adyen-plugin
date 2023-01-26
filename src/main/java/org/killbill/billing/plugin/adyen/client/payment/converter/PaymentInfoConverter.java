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

package org.killbill.billing.plugin.adyen.client.payment.converter;

import org.killbill.adyen.common.Address;
import org.killbill.adyen.common.Installments;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;

import com.google.common.base.Strings;

public class PaymentInfoConverter<T extends PaymentInfo> {

    /**
     * @param paymentInfo to convert
     * @return {@code true} if this converter is capable of handling the payment info
     */
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return true;
    }

    /**
     * Convert a PaymentInfo Object into an Adyen PaymentRequest
     */
    public PaymentRequest convertPaymentInfoToPaymentRequest(final T paymentInfo) {
        return initializePaymentInfo(paymentInfo);
    }

    private PaymentRequest initializePaymentInfo(final PaymentInfo paymentInfo) {
        final PaymentRequest paymentRequest = new PaymentRequest();
        final AnyType2AnyTypeMap map = new AnyType2AnyTypeMap();
        paymentRequest.setAdditionalData(map);

        setCaptureDelayHours(paymentInfo, paymentRequest);
        setInstallments(paymentInfo, paymentRequest);
        setShopperInteraction(paymentInfo, paymentRequest);
        setBillingAddress(paymentInfo, paymentRequest);
        setAcquirer(paymentInfo, paymentRequest);
        setSelectedBrand(paymentInfo, paymentRequest);

        return paymentRequest;
    }

    private void setCaptureDelayHours(final PaymentInfo paymentInfo, final PaymentRequest paymentRequest) {
        paymentRequest.setCaptureDelayHours(paymentInfo.getCaptureDelayHours());
    }

    private void setInstallments(final PaymentInfo paymentInfo, final PaymentRequest paymentRequest) {
        if (paymentInfo.getInstallments() != null) {
            final Installments installments = new Installments();
            installments.setValue(paymentInfo.getInstallments().shortValue());
            paymentRequest.setInstallments(installments);
        }
    }

    private void setShopperInteraction(final PaymentInfo paymentInfo, final PaymentRequest paymentRequest) {
        paymentRequest.setShopperInteraction(paymentInfo.getShopperInteraction());
    }

    private void setBillingAddress(final PaymentInfo paymentInfo, final PaymentRequest paymentRequest) {
        final String street = paymentInfo.getStreet();
        final String houseNumberOrName = paymentInfo.getHouseNumberOrName();
        final String city = paymentInfo.getCity();
        final String postalCode = paymentInfo.getPostalCode();
        final String stateOrProvince = paymentInfo.getStateOrProvince();
        final String country = paymentInfo.getCountry();
        final String adjustedCountry;
        if ("UK".equalsIgnoreCase(country)) {
            // Passing UK will result in: validation 134 Billing address problem (Country UK invalid)
            adjustedCountry = "GB";
        } else if ("QC".equalsIgnoreCase(country)) {
            // Passing QC will result in: validation 134 Billing address problem (Country QC invalid)
            adjustedCountry = "CA";
        } else {
            adjustedCountry = country;
        }

        // Adyen validation docs:
        //    - https://docs.adyen.com/api-reference/payments-api/paymentrequest/
        //    - https://docs.adyen.com/developers/api-reference/common-api/address
        // TL;DR: the billing address per se is optional, but when sending it, the country is always mandatory,
        // while the remaining fields must either be sent all or none (although in our experience the ZIP always has
        // been treated as optional by Adyen: maybe it is mandatory only for US and CA)
        // TODO? Introduce some data validation
        final boolean stateProvinceValid = !("US".equals(adjustedCountry) || "CA".equals(adjustedCountry))
                                           || stateOrProvince != null;

        final boolean addressComplete = street != null && houseNumberOrName != null && city != null
                                        && postalCode != null && stateProvinceValid;

        final boolean addressEmpty = street == null && houseNumberOrName == null && city == null && postalCode == null
                                     && stateOrProvince == null;

        final boolean addressValid = adjustedCountry != null && (addressComplete || addressEmpty);

        // TODO: validate the right format for fields

        if (addressValid) {
            final Address address = new Address();
            address.setStreet(street);
            address.setHouseNumberOrName(houseNumberOrName);
            address.setCity(city);
            address.setPostalCode(postalCode);
            address.setStateOrProvince(stateOrProvince);
            address.setCountry(adjustedCountry);

            paymentRequest.setBillingAddress(address);
        }
    }

    private void setAcquirer(final PaymentInfo paymentInfo, final PaymentRequest paymentRequest) {
        final String acquirerName = paymentInfo.getAcquirer();
        if (acquirerName != null) {
            final AnyType2AnyTypeMap.Entry acquirerCode = new AnyType2AnyTypeMap.Entry();
            acquirerCode.setKey("acquirerCode");
            acquirerCode.setValue(acquirerName);
            paymentRequest.getAdditionalData().getEntry().add(acquirerCode);

            // If the acquirer has an authorisationMid set it to the request too
            final String mid = paymentInfo.getAcquirerMID();
            if (!Strings.isNullOrEmpty(mid)) {
                final AnyType2AnyTypeMap.Entry authorisationMid = new AnyType2AnyTypeMap.Entry();
                authorisationMid.setKey("authorisationMid");
                authorisationMid.setValue(mid);
                paymentRequest.getAdditionalData().getEntry().add(authorisationMid);
            }
        }
    }

    // Set the concrete card brand when using subBrand types (e.g. DineroMail or to force an ELV recurring contract to be handled as SEPA)
    private void setSelectedBrand(final PaymentInfo paymentInfo, final PaymentRequest paymentRequest) {
        if (paymentInfo.getSelectedBrand() != null) {
            paymentRequest.setSelectedBrand(paymentInfo.getSelectedBrand());

            // This option must be set to indicate that the specified brand must be taken, instead of determining the brand based on the card number
            final AnyType2AnyTypeMap.Entry overwriteBrand = new AnyType2AnyTypeMap.Entry();
            overwriteBrand.setKey("overwriteBrand");
            overwriteBrand.setValue("true");
            paymentRequest.getAdditionalData().getEntry().add(overwriteBrand);
        }
    }
}
