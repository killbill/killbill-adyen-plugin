/*
 * Copyright 2014 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.List;

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.common.Name;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.Recurring;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.converter.impl.NullObjectConverter;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

public class PaymentRequestBuilder extends RequestBuilder<PaymentRequest> {

    private static final String CONTINOUS_AUTHENTICATION_ADYEN = "ContAuth";

    private static final PaymentInfoConverter NULL_CONVERTER = new NullObjectConverter();

    private final PaymentInfo paymentInfo;
    private final PaymentInfoConverterManagement paymentInfoConverterManagement;

    PaymentRequestBuilder(final PaymentInfo paymentInfo,
                          final PaymentInfoConverterManagement paymentInfoConverterManagement,
                          final String holderName) {
        this.paymentInfo = paymentInfo;
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;

        request = createInitialRequestForPaymentInfo(holderName);
    }

    private PaymentRequest createInitialRequestForPaymentInfo(final String holderName) {
        final PaymentRequest initialRequest = createRequestForPaymentInfo(holderName);
        if (initialRequest.getAdditionalData() == null) {
            initialRequest.setAdditionalData(new AnyType2AnyTypeMap());
        }
        if (paymentInfo.isContinuousAuthenticationEnabled()) {
            initialRequest.setShopperInteraction(CONTINOUS_AUTHENTICATION_ADYEN);
        }
        return initialRequest;
    }

    private PaymentRequest createRequestForPaymentInfo(final String holderName) {
        final PaymentInfoConverter<PaymentInfo> converter = MoreObjects.<PaymentInfoConverter>firstNonNull(paymentInfoConverterManagement.getConverterForPaymentInfo(paymentInfo), NULL_CONVERTER);
        final Object convertedObject = converter.convertPaymentInfoToPSPTransferObject(holderName, paymentInfo);
        if (convertedObject instanceof PaymentRequest) {
            return (PaymentRequest) convertedObject;
        }
        throw new IllegalStateException("converted object must be of type PaymentRequest: " + convertedObject);
    }

    @Override
    protected List<AnyType2AnyTypeMap.Entry> getAdditionalData() {
        return request.getAdditionalData().getEntry();
    }

    public PaymentRequestBuilder withMerchantAccount(final String merchantAccount) {
        request.setMerchantAccount(merchantAccount);
        return this;
    }

    @Override
    public PaymentRequest build() {
        if (paymentRequestWithRecurringPaymentData()) {
            final Recurring recurring = (request.getRecurring() == null) ? new Recurring() : request.getRecurring();
            recurring.setContract(paymentInfo.getPaymentProvider().getRecurringType().name());
        }
        return request;
    }

    private boolean paymentRequestWithRecurringPaymentData() {
        return !Strings.isNullOrEmpty(request.getSelectedRecurringDetailReference());
    }

    public PaymentRequestBuilder withAmount(final String currency, final Long value) {
        if (value != null) {
            final Amount amount = new Amount();
            amount.setCurrency(currency);
            amount.setValue(value);
            return withAmount(amount);
        }
        return this;
    }

    public PaymentRequestBuilder withAmount(final Amount amount) {
        request.setAmount(amount);
        return this;
    }

    public PaymentRequestBuilder withShopperReference(final String shopperReference) {
        request.setShopperReference(shopperReference);
        return this;
    }

    public PaymentRequestBuilder withShopperEmail(final String shopperEmail) {
        request.setShopperEmail(shopperEmail);
        return this;
    }

    public PaymentRequestBuilder withShopperIp(final String shopperIp) {
        request.setShopperIP(shopperIp);
        return this;
    }

    public PaymentRequestBuilder withReference(final String reference) {
        request.setReference(reference);
        return this;
    }

    public PaymentRequestBuilder withShopperName(final String firstName, final String lastName) {
        final Name name = new Name();
        name.setFirstName(firstName);
        name.setLastName(lastName);
        return withShopperName(name);
    }

    public PaymentRequestBuilder withShopperName(final Name shopperName) {
        request.setShopperName(shopperName);
        return this;
    }

    public PaymentRequestBuilder withRecurringContractForUser() {
        if (paymentInfo.getPaymentProvider().isRecurringEnabled()) {
            final Recurring recurring = createRecurring(paymentInfo.getRecurringType());
            request.setRecurring(recurring);
        }
        return this;
    }

    private Recurring createRecurring(final RecurringType recurringContract) {
        final Recurring recurring = new Recurring();
        recurring.setContract(recurringContract.name());
        return recurring;
    }

    public PaymentRequestBuilder withBrowserInfo(final Long amount) {
        if (isCard()) {
            final BrowserInfo browserInfo = (BrowserInfo) paymentInfoConverterManagement.getBrowserInfoFor3DSecureAuth(amount, (Card) paymentInfo);
            request.setBrowserInfo(browserInfo);
        }
        return this;
    }

    public PaymentRequestBuilder withReturnUrl(final String returnUrl) {
        if (isCard() && paymentInfo.getPaymentProvider().send3DSTermUrl()) {
            addAdditionalData("returnUrl", returnUrl);
        }
        return this;
    }

    private boolean isCard() {
        return paymentInfo instanceof Card;
    }

    public PaymentRequestBuilder withSelectedRecurringDetailReference() {
        if (isRecurringInformationAvailable()) {
            request.setSelectedRecurringDetailReference(paymentInfo.getRecurringDetailId());
        }
        return this;
    }

    private boolean isRecurringInformationAvailable() {
        return !Strings.isNullOrEmpty(paymentInfo.getRecurringDetailId());
    }

    public PaymentRequestBuilder withSplitSettlementData(final SplitSettlementData splitSettlementData) {
        final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
        addAdditionalData(entries);
        return this;
    }
}
