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

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.common.Gender;
import org.killbill.adyen.common.Name;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPaymentRequest3DBuilder extends BaseTestPaymentRequestBuilder {

    private static final String CURRENCY = Currency.EUR.name();

    @Test(groups = "fast")
    public void testWithMerchantAccount() throws Exception {
        final String merchantAccount = "merchantAccount";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withMerchantAccount(merchantAccount)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getMerchantAccount(), merchantAccount, "Wrong MerchantAccount in Request");
    }

    @Test(groups = "fast")
    public void testWithMd() throws Exception {
        final String md = "md";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withMd(md)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getMd(), md, "Wrong MD in Request");
    }

    @Test(groups = "fast")
    public void testWithPaResponse() throws Exception {
        final String paResponse = "paResponse";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withPaResponse(paResponse)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getPaResponse(), paResponse, "Wrong PaResponse in Request");
    }

    @Test(groups = "fast")
    public void testWithReference() throws Exception {
        final String reference = "reference";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withReference(reference)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getReference(), reference, "Wrong Reference in Request");
    }

    @Test(groups = "fast")
    public void testWithSessionId() throws Exception {
        final String sessionId = "sessionId";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withSessionId(sessionId)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getSessionId(), sessionId, "Wrong SessionId in Request");
    }

    @Test(groups = "fast")
    public void testWithShopperEmail() throws Exception {
        final String shopperEmail = "shopperEmail";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withShopperEmail(shopperEmail)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getShopperEmail(), shopperEmail, "Wrong ShopperEmail in Request");
    }

    @Test(groups = "fast")
    public void testWithShopperIP() throws Exception {
        final String shopperIP = "shopperIP";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withShopperIP(shopperIP)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getShopperIP(), shopperIP, "Wrong ShopperIP in Request");
    }

    @Test(groups = "fast")
    public void testWithShopperReference() throws Exception {
        final String shopperReference = "shopperReference";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withShopperReference(shopperReference)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getShopperReference(), shopperReference, "Wrong ShopperReference in Request");
    }

    @Test(groups = "fast")
    public void testWithShopperStatement() throws Exception {
        final String shopperStatement = "shopperStatement";
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withShopperStatement(shopperStatement)
                                                                               .build();

        Assert.assertEquals(paymentRequest3D.getShopperStatement(), shopperStatement, "Wrong ShopperStatement in Request");
    }

    @Test(groups = "fast")
    public void testWithShopperNameByValues() throws Exception {
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withShopperName("First", "Last", "infix", true)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getShopperName(), "No ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getFirstName(), "First", "Wrong First Name in ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getLastName(), "Last", "Wrong Last Name in ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getInfix(), "infix", "Wrong Infix in ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getGender(), Gender.MALE, "Wrong Gender in ShopperName in Request");
    }

    @Test(groups = "fast")
    public void testWithShopperNameByEntity() throws Exception {
        final Name shopperName = new Name();
        shopperName.setFirstName("First");
        shopperName.setLastName("Last");
        shopperName.setInfix("infix");
        shopperName.setGender(Gender.MALE);
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withShopperName(shopperName)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getShopperName(), "No ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getFirstName(), "First", "Wrong First Name in ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getLastName(), "Last", "Wrong Last Name in ShopperName in Request");
        Assert.assertEquals(paymentRequest3D.getShopperName().getInfix(), "infix", "Wrong Infix in ShopperName in Request");
        Assert.assertEquals(Gender.MALE, paymentRequest3D.getShopperName().getGender(), "Wrong Gender in ShopperName in Request");
    }

    @Test(groups = "fast", dataProvider = DP_RECURRING_TYPES)
    public void testWithRecurring(final RecurringType recurringType) throws Exception {
        final PaymentProvider recurringEnabledPaymentProvider = mock(PaymentProvider.class);
        when(recurringEnabledPaymentProvider.isRecurringEnabled()).thenReturn(true);
        when(recurringEnabledPaymentProvider.getRecurringType()).thenReturn(recurringType);

        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withRecurring(recurringEnabledPaymentProvider)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getRecurring(), "No Recurring in Request");
        Assert.assertEquals(paymentRequest3D.getRecurring().getContract(), recurringType.name(), "Wrong Contract in Recurring in Request");
    }

    @Test(groups = "fast")
    public void testWithAmountByValues() throws Exception {
        final Long value = 1L;
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withAmount(CURRENCY, value)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getAmount(), "No Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAmount().getCurrency(), CURRENCY, "Wrong Currency in Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAmount().getValue(), value, "Wrong Value in Amount in Request");
    }

    @Test(groups = "fast")
    public void testWithAmountByEntity() throws Exception {
        final Amount amount = createAmount(CURRENCY, 1L);

        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withAmount(amount)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getAmount(), "No Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAmount().getCurrency(), amount.getCurrency(), "Wrong Currency in Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAmount().getValue(), amount.getValue(), "Wrong Value in Amount in Request");
    }

    @Test(groups = "fast")
    public void testWithAdditionalAmountByValues() throws Exception {
        final Long value = 1L;
        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withAdditionalAmount(CURRENCY, value)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getAdditionalAmount(), "No Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAdditionalAmount().getCurrency(), CURRENCY, "Wrong Currency in Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAdditionalAmount().getValue(), value, "Wrong Value in Amount in Request");
    }

    @Test(groups = "fast")
    public void testWithAdditionalAmountByEntity() throws Exception {
        final Amount amount = createAmount(CURRENCY, 1L);

        final PaymentRequest3D paymentRequest3D = new PaymentRequest3DBuilder().withAdditionalAmount(amount)
                                                                               .build();

        Assert.assertNotNull(paymentRequest3D.getAdditionalAmount(), "No Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAdditionalAmount().getCurrency(), amount.getCurrency(), "Wrong Currency in Amount in Request");
        Assert.assertEquals(paymentRequest3D.getAdditionalAmount().getValue(), amount.getValue(), "Wrong Value in Amount in Request");
    }

    private Amount createAmount(final String currency, final Long value) {
        final Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setValue(value);
        return amount;
    }
}
