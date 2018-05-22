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

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.client.payment.converter.impl.PaymentInfoConverterService;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestPaymentRequestBuilder extends BaseTestPaymentRequestBuilder {

    @Test(groups = "fast")
    public void testPaymentRequestBuilderWithEmptyFields() {
        final String merchantAccount = UUID.randomUUID().toString();
        final String paymentTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentData paymentData = new PaymentData<Card>(new BigDecimal("20"), Currency.EUR, paymentTransactionExternalKey, new Card());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;
        final PaymentInfoConverterService paymentInfoConverterManagement = new PaymentInfoConverterService();

        final PaymentRequestBuilder builder = new PaymentRequestBuilder(merchantAccount, paymentData, userData, splitSettlementData, additionalData, paymentInfoConverterManagement);
        final PaymentRequest paymentRequest = builder.build();

        Assert.assertEquals(paymentRequest.getMerchantAccount(), merchantAccount);
        Assert.assertEquals(paymentRequest.getAmount().getValue(), (Long) 2000L);
        Assert.assertEquals(paymentRequest.getAmount().getCurrency(), "EUR");
        Assert.assertEquals(paymentRequest.getReference(), paymentTransactionExternalKey);
    }

    @Test(groups = "fast")
    public void testPaymentRequestBuilderForCard() {
        final Card paymentInfo = new Card();
        paymentInfo.setExpiryMonth(10);
        paymentInfo.setExpiryYear(2019);
        paymentInfo.setHolderName(UUID.randomUUID().toString());
        paymentInfo.setNumber(UUID.randomUUID().toString());
        paymentInfo.setCvc(UUID.randomUUID().toString());

        final PaymentRequest paymentRequest = verifyPaymentRequestBuilder(paymentInfo);
        Assert.assertEquals(paymentRequest.getCard().getExpiryMonth(), "10");
        Assert.assertEquals(paymentRequest.getCard().getExpiryYear(), "2019");
        Assert.assertEquals(paymentRequest.getCard().getHolderName(), paymentInfo.getHolderName());
        Assert.assertEquals(paymentRequest.getCard().getNumber(), paymentInfo.getNumber());
        Assert.assertEquals(paymentRequest.getCard().getCvc(), paymentInfo.getCvc());
    }

    @Test(groups = "fast")
    public void testPaymentRequestBuilderForSepa() {
        final SepaDirectDebit paymentInfo = new SepaDirectDebit();
        paymentInfo.setIban(UUID.randomUUID().toString());
        paymentInfo.setSepaAccountHolder(UUID.randomUUID().toString());
        paymentInfo.setCountryCode(UUID.randomUUID().toString());

        final PaymentRequest paymentRequest = verifyPaymentRequestBuilder(paymentInfo);
        Assert.assertEquals(paymentRequest.getBankAccount().getIban(), paymentInfo.getIban());
        Assert.assertEquals(paymentRequest.getBankAccount().getOwnerName(), paymentInfo.getSepaAccountHolder());
        Assert.assertEquals(paymentRequest.getBankAccount().getCountryCode(), paymentInfo.getCountryCode());
        Assert.assertEquals(paymentRequest.getSelectedBrand(), "sepadirectdebit");
    }

    @Test(groups = "fast")
    public void testPaymentRequestBuilderForRecurring() {
        final Recurring paymentInfo = new Recurring();
        paymentInfo.setContract(UUID.randomUUID().toString());

        final PaymentRequest paymentRequest = verifyPaymentRequestBuilder(paymentInfo);
        Assert.assertEquals(paymentRequest.getRecurring().getContract(), paymentInfo.getContract());
    }

    private PaymentRequest verifyPaymentRequestBuilder(final PaymentInfo paymentInfo) {
        paymentInfo.setAcceptHeader(UUID.randomUUID().toString());
        paymentInfo.setUserAgent(UUID.randomUUID().toString());
        paymentInfo.setThreeDThreshold(0L);
        paymentInfo.setMpiDataDirectoryResponse(UUID.randomUUID().toString());
        paymentInfo.setMpiDataAuthenticationResponse(UUID.randomUUID().toString());
        paymentInfo.setMpiDataCavv("12345678901234567890");
        paymentInfo.setMpiDataCavvAlgorithm(UUID.randomUUID().toString());
        paymentInfo.setMpiDataXid("09876543210987654321");
        paymentInfo.setMpiDataEci(UUID.randomUUID().toString());
        paymentInfo.setAcquirer(UUID.randomUUID().toString());
        paymentInfo.setAcquirerMID(UUID.randomUUID().toString());

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

        final PaymentInfoConverterService paymentInfoConverterManagement = new PaymentInfoConverterService();

        final PaymentRequestBuilder builder = new PaymentRequestBuilder(merchantAccount, paymentData, userData, splitSettlementData, additionalData, paymentInfoConverterManagement);
        final PaymentRequest paymentRequest = builder.build();

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
        Assert.assertEquals(paymentRequest.getMpiData().getDirectoryResponse(), paymentInfo.getMpiDataDirectoryResponse());
        Assert.assertEquals(paymentRequest.getMpiData().getAuthenticationResponse(), paymentInfo.getMpiDataAuthenticationResponse());
        Assert.assertEquals(paymentRequest.getMpiData().getCavv(), "MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=".getBytes());
        Assert.assertEquals(paymentRequest.getMpiData().getXid(), "MDk4NzY1NDMyMTA5ODc2NTQzMjE=".getBytes());
        Assert.assertEquals(paymentRequest.getMpiData().getEci(), paymentInfo.getMpiDataEci());
        Assert.assertEquals(paymentRequest.getMpiData().getCavvAlgorithm(), paymentInfo.getMpiDataCavvAlgorithm());

        final List<AnyType2AnyTypeMap.Entry> entries = paymentRequest.getAdditionalData().getEntry();
        Assert.assertEquals(findValue(entries, "acquirerCode"), paymentInfo.getAcquirer());
        Assert.assertEquals(findValue(entries, "authorisationMid"), paymentInfo.getAcquirerMID());
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
