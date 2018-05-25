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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestPaymentRequest3DBuilder extends BaseTestPaymentRequestBuilder {

    @Test(groups = "fast")
    public void testPaymentRequestBuilderWithEmptyFields() {
        final String merchantAccount = UUID.randomUUID().toString();
        final String paymentTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentData paymentData = new PaymentData<Card>(new BigDecimal("20"), Currency.EUR, paymentTransactionExternalKey, new Card());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PaymentRequest3DBuilder builder = new PaymentRequest3DBuilder(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        final PaymentRequest3D paymentRequest = builder.build();

        Assert.assertEquals(paymentRequest.getMerchantAccount(), merchantAccount);
        Assert.assertEquals(paymentRequest.getAmount().getValue(), (Long) 2000L);
        Assert.assertEquals(paymentRequest.getAmount().getCurrency(), "EUR");
        Assert.assertEquals(paymentRequest.getReference(), paymentTransactionExternalKey);
    }

    @Test(groups = "fast")
    public void testPaymentRequestBuilderForCard() {
        final Card paymentInfo = new Card();
        paymentInfo.setMd(UUID.randomUUID().toString());
        paymentInfo.setPaRes(UUID.randomUUID().toString());

        final PaymentRequest3D paymentRequest = verifyPaymentRequestBuilder(paymentInfo);
        Assert.assertEquals(paymentRequest.getMd(), paymentInfo.getMd());
        Assert.assertEquals(paymentRequest.getPaResponse(), paymentInfo.getPaRes());
    }

    @Test(groups = "fast")
    public void testPaymentRequestBuilderForRecurring() {
        final Recurring paymentInfo = new Recurring();
        paymentInfo.setContract(UUID.randomUUID().toString());

        final PaymentRequest3D paymentRequest = verifyPaymentRequestBuilder(paymentInfo);
        Assert.assertEquals(paymentRequest.getRecurring().getContract(), paymentInfo.getContract());
    }

    private PaymentRequest3D verifyPaymentRequestBuilder(final PaymentInfo paymentInfo) {
        paymentInfo.setAcceptHeader(UUID.randomUUID().toString());
        paymentInfo.setUserAgent(UUID.randomUUID().toString());
        paymentInfo.setThreeDThreshold(0L);
        paymentInfo.setTermUrl(UUID.randomUUID().toString());
        paymentInfo.setMpiImplementationType("ACustom3DType");
        paymentInfo.setMpiImplementationTypeValues(ImmutableMap.<String, String>of("ACustom3DType.responseKey1", "abcdefgh01",
                                                                                   "ACustom3DType.responseKey2", "ijklmnop02"));

        final String merchantAccount = UUID.randomUUID().toString();
        final String paymentTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(new BigDecimal("20"), Currency.EUR, paymentTransactionExternalKey, paymentInfo);

        final UserData userData = new UserData();
        userData.setShopperIP(UUID.randomUUID().toString());
        userData.setShopperEmail(UUID.randomUUID().toString());
        userData.setShopperReference(UUID.randomUUID().toString());

        final SplitSettlementData splitSettlementData = new SplitSettlementData(1,
                                                                                "EUR",
                                                                                ImmutableList.<SplitSettlementData.Item>of(new SplitSettlementData.Item(500, "deal1", "voucherId", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal1", "voucherId2", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal2", "travelId", "travel")));
        final Map<String, String> additionalData = null;

        final PaymentRequest3DBuilder builder = new PaymentRequest3DBuilder(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        final PaymentRequest3D paymentRequest = builder.build();

        Assert.assertEquals(paymentRequest.getMerchantAccount(), merchantAccount);
        Assert.assertEquals(paymentRequest.getAmount().getValue(), (Long) 2000L);
        Assert.assertEquals(paymentRequest.getAmount().getCurrency(), "EUR");
        Assert.assertEquals(paymentRequest.getReference(), paymentTransactionExternalKey);
        Assert.assertEquals(paymentRequest.getShopperIP(), userData.getShopperIP());
        Assert.assertEquals(paymentRequest.getShopperEmail(), userData.getShopperEmail());
        Assert.assertEquals(paymentRequest.getShopperReference(), userData.getShopperReference());
        if (paymentInfo.getSelectedBrand() != null) {
            Assert.assertEquals(paymentRequest.getSelectedBrand(), paymentInfo.getSelectedBrand());
        }
        Assert.assertEquals(paymentRequest.getShopperInteraction(), paymentInfo.getShopperInteraction());
        Assert.assertEquals(paymentRequest.getBrowserInfo().getAcceptHeader(), paymentInfo.getAcceptHeader());
        Assert.assertEquals(paymentRequest.getBrowserInfo().getUserAgent(), paymentInfo.getUserAgent());

        final List<Entry> entries = paymentRequest.getAdditionalData().getEntry();
        Assert.assertEquals(findValue(entries, "mpiImplementationType"), "ACustom3DType");
        Assert.assertEquals(findValue(entries, "ACustom3DType.responseKey1"), "abcdefgh01");
        Assert.assertEquals(findValue(entries, "ACustom3DType.responseKey2"), "ijklmnop02");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.api"), "1");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.nrOfItems"), "3");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.totalAmount"), "2000");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.amount"), "500");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.group"), "deal1");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.reference"), "voucherId");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.type"), "voucher");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.amount"), "750");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.group"), "deal1");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.reference"), "voucherId2");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.type"), "voucher");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.amount"), "750");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.group"), "deal2");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.reference"), "travelId");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.type"), "travel");

        return paymentRequest;
    }
}
