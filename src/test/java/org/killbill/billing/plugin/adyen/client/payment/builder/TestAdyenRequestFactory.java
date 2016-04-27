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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class TestAdyenRequestFactory {

    private static final DateTime ANY_DATE = DateTime.now();
    private static final String ANY_CUSTOMER_ID = "2";
    private static final long ANY_AMOUNT = 12L;

    private Signer signer;
    private PaymentInfoConverterManagement paymentInfoConverterManagement;
    private AdyenConfigProperties adyenConfigProperties;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        signer = Mockito.mock(Signer.class);
        paymentInfoConverterManagement = Mockito.mock(PaymentInfoConverterManagement.class);
        adyenConfigProperties = Mockito.mock(AdyenConfigProperties.class);
        Mockito.when(adyenConfigProperties.getMerchantAccount(Mockito.<String>any())).thenReturn(UUID.randomUUID().toString());
        Mockito.when(adyenConfigProperties.getSkin(Mockito.<String>any())).thenReturn(UUID.randomUUID().toString());
    }

    @Test(groups = "fast")
    public void testCreateMpiAdditionalData() {
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("md", "md_value");
        final String my_type = "My_Type";
        final String some_value = "Some_Value";
        final String some_key = "Some_Key";
        parameters.put(AdyenRequestFactory.MPI_IMPLEMENTATION_TYPE, my_type);
        parameters.put(some_key, some_value);

        final List<Entry> entries = new AdyenRequestFactory(null, null, null).createMpiAdditionalData(parameters);
        Assert.assertEquals(2, entries.size());
        final Map<String, String> map = new HashMap<String, String>();
        map.put((String) entries.get(0).getKey(), (String) entries.get(0).getValue());
        map.put((String) entries.get(1).getKey(), (String) entries.get(1).getValue());

        Assert.assertEquals(some_value, map.get(my_type + "." + some_key));
        Assert.assertEquals(my_type, map.get(AdyenRequestFactory.MPI_IMPLEMENTATION_TYPE));
    }

    @Test(groups = "fast")
    public void splitSettlementDataIsAddedToAdyenModificationRequestsForPaymentExecution() throws Exception {
        final SplitSettlementData settlementData = SplitSettlementData.newInstance(1, "EUR")
                                                                      .withItem(new SplitSettlementData.Item(500, "deal1", "TRADE-id", "TRADE"))
                                                                      .withItem(new SplitSettlementData.Item(1000, "deal2", "TRADE-id", "FOOD"))
                                                                      .build();

        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, null);

        final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(validPaymentProvider(), 0L, "ref", settlementData);

        assertSplitSettlementDataIsValid(settlementData, modificationRequest);
    }

    @Test(groups = "fast")
    public void splitSettlementDataIsAddedToRequestForCancelOrRefundWithoutAmount() throws Exception {
        final SplitSettlementData settlementData = SplitSettlementData.newInstance(1, "EUR")
                                                                      .withItem(new SplitSettlementData.Item(500, "deal1", "TRADE-id", "TRADE"))
                                                                      .withItem(new SplitSettlementData.Item(1000, "deal2", "TRADE-id", "FOOD"))
                                                                      .build();
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, null);

        final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(validPaymentProvider(), "ref", settlementData);

        assertSplitSettlementDataIsValid(settlementData, modificationRequest);
    }

    @Test(groups = "fast")
    public void getHppParamsMapAddsCorrectSplitSettlementData() throws Exception {
        final int api = 1;
        final String signature = "signature";
        final String currencyCode = "EUR";

        final int item1Amount = 500;
        final String item1Group = "deal1";
        final String item1Reference = "TRADE-id";
        final String item1Type = "TRADE";

        final int item2Amount = 1000;
        final String item2Group = "deal2";
        final String item2Reference = "TRADE-id";
        final String item2Type = "FOOD";

        final String reference = "reference";

        final SplitSettlementData settlementData = SplitSettlementData.newInstance(api, currencyCode)
                                                                      .withItem(new SplitSettlementData.Item(item1Amount, item1Group, item1Reference, item1Type))
                                                                      .withItem(new SplitSettlementData.Item(item2Amount, item2Group, item2Reference, item2Type))
                                                                      .build();

        final CreditCard amex = Mockito.mock(CreditCard.class);
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setPaymentType(PaymentType.AMEX);

        Mockito.when(signer.computeSignature(Mockito.anyLong(), Mockito.anyString(), Mockito.<String>any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.<PaymentInfo>any(), Mockito.<String>any())).thenReturn(signature);
        Mockito.when(signer.getBase64EncodedSignature(Mockito.anyString(), Mockito.anyString())).thenReturn(signature);
        Mockito.when(amex.getPaymentProvider()).thenReturn(paymentProvider);

        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);
        final Map<String, String> formParameter = adyenRequestFactory.getHppParamsMap(ANY_AMOUNT, paymentProvider, "anything", "anything", "anything", "anything", reference, ANY_DATE, amex, settlementData, "anything", ANY_CUSTOMER_ID, Locale.getDefault());

        final Map<String, String> expectedSplitSettlementEntries = ImmutableMap.<String, String>builder()
                                                                               .put("splitsettlementdata.api", api + "")
                                                                               .put("splitsettlementdata.sig", signature)
                                                                               .put("splitsettlementdata.nrOfItems", 2 + "")
                                                                               .put("splitsettlementdata.totalAmount", item1Amount + item2Amount + "")
                                                                               .put("splitsettlementdata.currencyCode", currencyCode)
                                                                               .put("splitsettlementdata.item1.amount", item1Amount + "")
                                                                               .put("splitsettlementdata.item1.currencyCode", currencyCode)
                                                                               .put("splitsettlementdata.item1.group", item1Group)
                                                                               .put("splitsettlementdata.item1.reference", item1Reference)
                                                                               .put("splitsettlementdata.item1.type", item1Type)
                                                                               .put("splitsettlementdata.item2.amount", item2Amount + "")
                                                                               .put("splitsettlementdata.item2.currencyCode", currencyCode)
                                                                               .put("splitsettlementdata.item2.group", item2Group)
                                                                               .put("splitsettlementdata.item2.reference", item2Reference)
                                                                               .put("splitsettlementdata.item2.type", item2Type)
                                                                               .build();

        for (final String key : expectedSplitSettlementEntries.keySet()) {
            Assert.assertEquals(formParameter.get(key), expectedSplitSettlementEntries.get(key));
        }
    }

    private void assertSplitSettlementDataIsValid(final SplitSettlementData settlementData,
                                                  final ModificationRequest modificationRequest) {
        final Map<String, String> expectedSplitSettlementEntries = getExpectedSplitSettlementParams(settlementData);
        final AnyType2AnyTypeMap additionalData = modificationRequest.getAdditionalData();
        final Map<String, String> actualMap = anyType2AnyTypeMapToStringMap(additionalData);
        Assert.assertTrue(actualMap.entrySet().containsAll(expectedSplitSettlementEntries.entrySet()));
    }

    private PaymentProvider validPaymentProvider() {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setCurrency(Currency.getInstance("EUR"));
        return paymentProvider;
    }

    private Map<String, String> getExpectedSplitSettlementParams(final SplitSettlementData settlementData) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                                                                         .put("splitsettlementdata.api", String.valueOf(settlementData.getApi()))
                                                                         .put("splitsettlementdata.nrOfItems", String.valueOf(settlementData.getItems().size()))
                                                                         .put("splitsettlementdata.totalAmount", String.valueOf(settlementData.getTotalAmount()))
                                                                         .put("splitsettlementdata.currencyCode", settlementData.getCurrencyCode());

        for (int i = 0; i < settlementData.getItems().size(); i++) {
            final String itemDescription = "item" + (i + 1);
            final SplitSettlementData.Item item = settlementData.getItems().get(i);
            builder.put("splitsettlementdata." + itemDescription + ".amount", String.valueOf(item.getAmount()))
                   .put("splitsettlementdata." + itemDescription + ".currencyCode", settlementData.getCurrencyCode())
                   .put("splitsettlementdata." + itemDescription + ".group", item.getGroup())
                   .put("splitsettlementdata." + itemDescription + ".reference", item.getReference())
                   .put("splitsettlementdata." + itemDescription + ".type", item.getType());
        }
        return builder.build();
    }

    private Map<String, String> anyType2AnyTypeMapToStringMap(final AnyType2AnyTypeMap additionalData) {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        if (additionalData != null) {
            for (final AnyType2AnyTypeMap.Entry e : additionalData.getEntry()) {
                additionalDataMap.put(e.getKey().toString(), e.getValue().toString());
            }
        }
        return additionalDataMap;
    }
}
