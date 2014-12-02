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
import org.killbill.adyen.common.Gender;
import org.killbill.adyen.common.Name;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.Recurring;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;

public class PaymentRequest3DBuilder extends RequestBuilder<PaymentRequest3D> {

    public PaymentRequest3DBuilder() {
        super(new PaymentRequest3D());
    }

    public PaymentRequest3DBuilder withAmount(final String currency, final Long value) {
        if (value != null) {
            final Amount amount = new Amount();
            amount.setCurrency(currency);
            amount.setValue(value);
            return withAmount(amount);
        }
        return this;
    }

    public PaymentRequest3DBuilder withAmount(final Amount amount) {
        request.setAmount(amount);
        return this;
    }

    public PaymentRequest3DBuilder withAdditionalAmount(final String currency, final Long value) {
        if (value != null) {
            final Amount amount = new Amount();
            amount.setCurrency(currency);
            amount.setValue(value);
            return withAdditionalAmount(amount);
        }
        return this;
    }

    public PaymentRequest3DBuilder withAdditionalAmount(final Amount amount) {
        request.setAdditionalAmount(amount);
        return this;
    }

    public PaymentRequest3DBuilder withBrowserInfo(final BrowserInfo browserInfo) {
        if (browserInfo != null) {
            request.setBrowserInfo(browserInfo);
        }
        return this;
    }

    public PaymentRequest3DBuilder withMerchantAccount(final String value) {
        request.setMerchantAccount(value);
        return this;
    }

    public PaymentRequest3DBuilder withMd(final String value) {
        request.setMd(value);
        return this;
    }

    public PaymentRequest3DBuilder withPaResponse(final String value) {
        request.setPaResponse(value);
        return this;
    }

    public PaymentRequest3DBuilder withReference(final String value) {
        request.setReference(value);
        return this;
    }

    public PaymentRequest3DBuilder withSessionId(final String value) {
        request.setSessionId(value);
        return this;
    }

    public PaymentRequest3DBuilder withShopperEmail(final String value) {
        request.setShopperEmail(value);
        return this;
    }

    public PaymentRequest3DBuilder withShopperIP(final String value) {
        request.setShopperIP(value);
        return this;
    }

    public PaymentRequest3DBuilder withShopperReference(final Number value) {
        return withShopperReference(String.valueOf(value));
    }

    public PaymentRequest3DBuilder withShopperReference(final String value) {
        request.setShopperReference(value);
        return this;
    }

    public PaymentRequest3DBuilder withShopperStatement(final String value) {
        request.setShopperStatement(value);
        return this;
    }

    public PaymentRequest3DBuilder withShopperName(final String firstName, final String lastName, final String infix, final boolean male) {
        final Name name = new Name();
        name.setFirstName(firstName);
        name.setLastName(lastName);
        name.setInfix(infix);
        name.setGender(male ? Gender.MALE : Gender.FEMALE);
        return withShopperName(name);
    }

    public PaymentRequest3DBuilder withShopperName(final Name name) {
        request.setShopperName(name);
        return this;
    }

    public PaymentRequest3DBuilder withRecurring(final PaymentProvider paymentProvider) {
        if (paymentProvider.isRecurringEnabled()) {
            final Recurring recurring = new Recurring();
            recurring.setContract(PaymentRequestBuilder.RECURRING_CONTRACT);
            request.setRecurring(recurring);
        }
        return this;
    }

    public PaymentRequest3DBuilder withSplitSettlementData(final SplitSettlementData splitSettlementData) {
        final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
        addAdditionalData(entries);
        return this;
    }

    @Override
    protected List<AnyType2AnyTypeMap.Entry> getAdditionalData() {
        if (request.getAdditionalData() == null) {
            request.setAdditionalData(new AnyType2AnyTypeMap());
        }
        return request.getAdditionalData().getEntry();
    }
}
