/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.builder.checkout;

import com.adyen.model.checkout.DefaultPaymentMethodDetails;
import com.adyen.model.checkout.LineItem;
import com.adyen.model.checkout.PaymentMethodDetails;
import com.adyen.model.checkout.PaymentsRequest;
import com.google.common.base.Charsets;
import com.adyen.model.*;
import java.util.Base64;

import org.killbill.billing.plugin.adyen.api.mapping.klarna.PropertyMapper;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.adyen.client.payment.builder.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class CheckoutPaymentsBuilder extends RequestBuilder<PaymentsRequest> {
    private final String merchantAccount;
    private final PaymentData paymentData;
    private final UserData userData;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutPaymentsBuilder.class);
    public final static String OPEN_INVOICE_MERCHANT_DATA = "openinvoicedata.merchantData";

    public CheckoutPaymentsBuilder(final String merchantAccount,
                                   final PaymentData paymentData,
                                   final UserData userData) {
        super(new PaymentsRequest());
        this.merchantAccount = merchantAccount;
        this.paymentData = paymentData;
        this.userData = userData;
    }

    @Override
    public PaymentsRequest build() {
        KlarnaPaymentInfo paymentInfo = null;
        if (paymentData.getPaymentInfo() instanceof KlarnaPaymentInfo) {
            paymentInfo = (KlarnaPaymentInfo) paymentData.getPaymentInfo();
        } else {
            logger.error("Invalid paymentInfo object, expected KlarnaPaymentInfo");
            return request;
        }

        if(merchantAccount != null) {
            request.setMerchantAccount(merchantAccount);
        } else {
            logger.error("merchantAccount is null, can not create payment request");
            return request;
        }

        //payment method
        if(paymentInfo.getPaymentMethod() != null) {
            PaymentMethodDetails paymentMethod = new DefaultPaymentMethodDetails();
            paymentMethod.setType(paymentInfo.getPaymentMethod());
            request.setPaymentMethod(paymentMethod);
        }

        //shipping address
        if(paymentInfo.usingShippingAddress() &&
           paymentInfo.getShippingAddress() != null) {
            setDeliveryAddress(paymentInfo.getShippingAddress());
        }

        request.setReference(paymentInfo.getOrderReference());
        request.setCountryCode(paymentInfo.getCountryCode());
        request.setReturnUrl(paymentInfo.getReturnUrl());

        setAmount();
        setShopperData();
        setInvoiceLines(paymentInfo);
        setAdditionalData(paymentInfo);
        return request;
    }

    private void setDeliveryAddress(PropertyMapper.Address shippingAddress){
        Address address = new Address();
        address.setHouseNumberOrName(shippingAddress.getAddress1());
        address.setStreet(shippingAddress.getAddress2());
        address.setStateOrProvince(shippingAddress.getState());
        address.setCity(shippingAddress.getCity());
        address.setCountry(shippingAddress.getCountry());
        address.setPostalCode(shippingAddress.getPostalCode());
        request.setDeliveryAddress(address);
    }

    private void setAdditionalData(KlarnaPaymentInfo paymentInfo) {
        //base64 encoded data (customer+voucher+seller)
        String additionalData = paymentInfo.getAdditionalData();
        if(additionalData != null) {
            String encodedData = Base64.getEncoder().encodeToString(additionalData.getBytes(Charsets.UTF_8));
            request.putAdditionalDataItem(OPEN_INVOICE_MERCHANT_DATA, encodedData);
        } else {
            logger.error("Failed to include merchant data in payment request");
        }
    }

    private void setInvoiceLines(KlarnaPaymentInfo paymentInfo) {
        List<PropertyMapper.LineItem>  items = paymentInfo.getItems();
        List<LineItem> invoiceLines = new ArrayList<LineItem>();
        if(items.size() > 0) {
            for(PropertyMapper.LineItem item: items) {
                LineItem lineItem = new LineItem();
                lineItem.setId(item.getId());
                lineItem.setDescription(item.getDescription());
                lineItem.setAmountIncludingTax(item.getAmountIncludingTax());
                lineItem.setQuantity(item.getQuantity());
                invoiceLines.add(lineItem);
            }
        }

        request.setLineItems(invoiceLines);
    }

    private void setAmount() {
        if (paymentData.getAmount() == null || paymentData.getCurrency() == null) {
            return;
        }

        final String currency = paymentData.getCurrency().name();
        final Amount amount = new Amount();
        amount.setValue(toMinorUnits(paymentData.getAmount(), currency));
        amount.setCurrency(currency);
        request.setAmount(amount);
    }

    private void setShopperData() {
        final Name name = new Name();
        name.setFirstName(userData.getFirstName());
        name.setInfix(userData.getInfix());
        name.setLastName(userData.getLastName());
        if (userData.getGender() != null) {
            name.setGender(Name.GenderEnum.valueOf(userData.getGender().toUpperCase()));
        }
        if (userData.getFirstName() != null ||
            userData.getInfix() != null ||
            userData.getLastName() != null ||
            userData.getGender() != null) {
            request.setShopperName(name);
        }

        request.setShopperLocale(userData.getShopperLocale().toString());
        request.setShopperEmail(userData.getShopperEmail());
        request.setShopperIP(userData.getShopperIP());
        request.setShopperReference(userData.getShopperReference());
    }
}
