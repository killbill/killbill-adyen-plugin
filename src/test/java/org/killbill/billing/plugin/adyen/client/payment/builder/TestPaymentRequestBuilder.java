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

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.common.Gender;
import org.killbill.adyen.common.Name;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.adyen.payment.BankAccount;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData.Item;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Amex;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestPaymentRequestBuilder extends BaseTestPaymentRequestBuilder {

    private static final String ANY_HOLDER_NAME = "anyHolderName";

    private final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(new Properties());
    private final PaymentInfo anyPaymentInfo = new SepaDirectDebit(new PaymentProvider(adyenConfigProperties));

    private PaymentInfoConverterManagement<SepaDirectDebit> paymentInfoConverterManagement;
    private PaymentInfoConverter<SepaDirectDebit> paymentInfoConverter;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        paymentInfoConverterManagement = Mockito.mock(PaymentInfoConverterManagement.class);
        paymentInfoConverter = Mockito.mock(PaymentInfoConverter.class);
    }

    @Test(groups = "fast")
    public void shouldUseTheBankAccountParamsOfTheRequestTheConverterCreated() {
        final String accountHolderName = "accountHolderName";
        final String bankName = "bankName";
        final String bankAccountNumber = "bankAccountNumber";
        final String bankLocationId = "bankLocationId";

        final BankAccount expectedBankAccount = createExpectedBankAccountForRequest(accountHolderName, bankName, bankAccountNumber, bankLocationId);
        final PaymentRequest converterRequest = new PaymentRequest();
        converterRequest.setBankAccount(expectedBankAccount);
        final SepaDirectDebit sepa = new SepaDirectDebit(new PaymentProvider(adyenConfigProperties));

        Mockito.when(paymentInfoConverterManagement.getConverterForPaymentInfo(sepa)).thenReturn(paymentInfoConverter);
        Mockito.when(paymentInfoConverter.convertPaymentInfoToPSPTransferObject(ANY_HOLDER_NAME, sepa)).thenReturn(converterRequest);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(sepa, paymentInfoConverterManagement, ANY_HOLDER_NAME).build();

        final BankAccount actualBankAccount = paymentRequest.getBankAccount();
        Assert.assertSame(actualBankAccount, expectedBankAccount);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheMerchantAccount() {
        final String merchantAccount = "merchantAccount";

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withMerchantAccount(merchantAccount)
                                                                                                                                        .build();
        Assert.assertEquals(paymentRequest.getMerchantAccount(), merchantAccount);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheAmount() {
        final Amount amount = new Amount();
        amount.setCurrency("currency");
        amount.setValue(12L);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withAmount(amount)
                                                                                                                                        .build();

        Assert.assertSame(paymentRequest.getAmount(), amount);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheShopperReference() {
        final String shopperReference = "shopperReference";

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withShopperReference(shopperReference)
                                                                                                                                        .build();

        Assert.assertEquals(paymentRequest.getShopperReference(), shopperReference);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheShopperEmail() {
        final String shopperEmail = "shopperEmail";

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withShopperEmail(shopperEmail)
                                                                                                                                        .build();

        Assert.assertEquals(paymentRequest.getShopperEmail(), shopperEmail);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheShopperIP() {
        final String shopperIp = "shopperIp";

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withShopperIp(shopperIp)
                                                                                                                                        .build();

        Assert.assertEquals(paymentRequest.getShopperIP(), shopperIp);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheReference() {
        final String reference = "reference";

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withReference(reference)
                                                                                                                                        .build();

        Assert.assertEquals(paymentRequest.getReference(), reference);
    }

    @Test(groups = "fast")
    public void testShouldAlwaysSetTheShopperName() {
        final Name shopperName = new Name();
        shopperName.setFirstName("firstName");
        shopperName.setGender(Gender.MALE);
        shopperName.setInfix("infix");
        shopperName.setLastName("lastName");

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(anyPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withShopperName(shopperName)
                                                                                                                                        .build();

        Assert.assertSame(paymentRequest.getShopperName(), shopperName);
    }

    @Test(groups = "fast", dataProvider = DP_RECURRING_TYPES)
    public void shouldContainTheRecurringContractIfUserAndPaymentProviderIsEnabledForRecurring(final RecurringType recurringType) {
        final PaymentProvider paymentProvider = createPaymentProviderWithRecurringType(recurringType);
        final SepaDirectDebit paymentInfoForPaymentProviderWithRecurring = new SepaDirectDebit(paymentProvider);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(paymentInfoForPaymentProviderWithRecurring, paymentInfoConverterManagement, ANY_HOLDER_NAME).withRecurringContractForUser()
                                                                                                                                                                    .build();

        Assert.assertNotNull(paymentRequest.getRecurring());
        Assert.assertSame(paymentRequest.getRecurring().getContract(), recurringType.name());
    }

    @Test(groups = "fast")
    public void shouldContainNoRecurringInformationIfPaymentProviderIsDisabledForRecurring() {
        final PaymentProvider paymentProviderWithoutRecurring = createPaymentProviderWithRecurringType(null);
        final SepaDirectDebit paymentInfoForPaymentProviderWithRecurring = new SepaDirectDebit(paymentProviderWithoutRecurring);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(paymentInfoForPaymentProviderWithRecurring, paymentInfoConverterManagement, ANY_HOLDER_NAME).withRecurringContractForUser()
                                                                                                                                                                    .build();

        Assert.assertNull(paymentRequest.getRecurring());
    }

    @Test(groups = "fast")
    public void shouldSetTheBrowserInfoIfThePaymentInfoIsACard() {
        final long amount = 12L;
        final BrowserInfo expectedBrowserInfo = createBrowserInfo();

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        final Amex amex = new Amex(paymentProvider);
        Mockito.when(paymentInfoConverterManagement.getBrowserInfoFor3DSecureAuth(amount, amex)).thenReturn(expectedBrowserInfo);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withBrowserInfo(amount)
                                                                                                                              .build();

        Assert.assertSame(paymentRequest.getBrowserInfo(), expectedBrowserInfo);
    }

    @Test(groups = "fast")
    public void shouldNotSetTheBrowserInfoIfThePaymentInfoIsNotACard() {
        final long amount = 11L;
        final BrowserInfo notExpectedBrowserInfo = createBrowserInfo();

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        final SepaDirectDebit notACardPaymentInfo = createDefaultSepaWithPaymentProvider(paymentProvider);
        Mockito.when(paymentInfoConverterManagement.getBrowserInfoFor3DSecureAuth(Mockito.anyLong(), Mockito.any(Card.class))).thenReturn(notExpectedBrowserInfo);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(notACardPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withBrowserInfo(amount)
                                                                                                                                             .build();

        Assert.assertNull(paymentRequest.getBrowserInfo());
    }

    @Test(groups = "fast")
    public void shouldContainTheReturnUrlIfPaymentInfoIsACardAndTermurlIsRequiredForProvider() {
        final String returnUrl = "returnUrl";

        final PaymentProvider paymentProvider = Mockito.mock(PaymentProvider.class);
        Mockito.when(paymentProvider.send3DSTermUrl()).thenReturn(true);
        final Amex amex = new Amex(paymentProvider);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withReturnUrl(returnUrl)
                                                                                                                              .build();

        assertAdditionalDataDoesContain(paymentRequest, "returnUrl", returnUrl);
    }

    @Test(groups = "fast")
    public void shouldNotContainTheReturnUrlIfPaymentInfoIsNotACard() {
        final String returnUrl = "returnUrl";

        final PaymentProvider paymentProvider = Mockito.mock(PaymentProvider.class);
        Mockito.when(paymentProvider.send3DSTermUrl()).thenReturn(true);
        final SepaDirectDebit notACardPaymentInfo = createDefaultSepaWithPaymentProvider(paymentProvider);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(notACardPaymentInfo, paymentInfoConverterManagement, ANY_HOLDER_NAME).withReturnUrl(returnUrl)
                                                                                                                                             .build();

        assertAdditionalDataDoesNotContain(paymentRequest, "returnUrl", returnUrl);
    }

    @Test(groups = "fast")
    public void shouldNotContainTheReturnurlIfPaymentInfoIsACardButTermUrlIsNotRequired() {
        final String returnUrl = "returnUrl";

        final PaymentProvider paymentProvider = Mockito.mock(PaymentProvider.class);
        Mockito.when(paymentProvider.send3DSTermUrl()).thenReturn(false);
        final Amex card = new Amex(paymentProvider);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(card, paymentInfoConverterManagement, ANY_HOLDER_NAME).withReturnUrl(returnUrl)
                                                                                                                              .build();

        assertAdditionalDataDoesNotContain(paymentRequest, "returnUrl", returnUrl);
    }

    @Test(groups = "fast")
    public void shouldContainTheSelectedRecurringDetailIdIfThePaymentInfoContainsARecurringId() {
        final String myRecurringDetailId = "myRecurringDetailId";

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setRecurringType(RecurringType.RECURRING);
        final Amex amex = new Amex(paymentProvider);
        amex.setRecurringDetailId(myRecurringDetailId);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withSelectedRecurringDetailReference()
                                                                                                                              .build();

        Assert.assertSame(paymentRequest.getSelectedRecurringDetailReference(), myRecurringDetailId);
    }

    @Test(groups = "fast")
    public void shouldNotContainTheSelectedRecurringDetailIdIfThePaymentInfoRecurringIdIsNull() {
        final String myRecurringDetailId = null;

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        final Amex amex = new Amex(paymentProvider);
        amex.setRecurringDetailId(myRecurringDetailId);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withSelectedRecurringDetailReference()
                                                                                                                              .build();

        Assert.assertNull(paymentRequest.getSelectedRecurringDetailReference());
    }

    @Test(groups = "fast")
    public void shouldNotContainTheSelectedRecurringDetailIdIfThePaymentInfoRecurringIdIsEmpty() {
        final String myRecurringDetailId = "";

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        final Amex amex = new Amex(paymentProvider);
        amex.setRecurringDetailId(myRecurringDetailId);

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withSelectedRecurringDetailReference()
                                                                                                                              .build();

        Assert.assertNull(paymentRequest.getSelectedRecurringDetailReference());
    }

    @Test(groups = "fast")
    public void shouldUseTheRecurringTypeOfThePaymentInfoWhenRecurringDetailReferenceIsSet() {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setRecurringType(RecurringType.ONECLICK);
        final Amex amex = new Amex(paymentProvider);
        amex.setRecurringDetailId("anyRecurringDetailId");

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withRecurringContractForUser()
                                                                                                                              .withSelectedRecurringDetailReference()
                                                                                                                              .build();

        Assert.assertEquals(paymentRequest.getRecurring().getContract(), RecurringType.ONECLICK.name());
    }

    @Test(groups = "fast")
    public void shouldAlwaysContainTheSplitSettlementData() {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        final Amex amex = new Amex(paymentProvider);

        final SplitSettlementData splitSettlementData = new SplitSettlementData(1, "EUR", ImmutableList.<Item>of(new SplitSettlementData.Item(500, "deal1", "voucherId", "voucher"),
                                                                                                                 new SplitSettlementData.Item(750, "deal1", "voucherId2", "voucher"),
                                                                                                                 new SplitSettlementData.Item(750, "deal2", "travelId", "travel")));

        final PaymentRequest paymentRequest = new PaymentRequestBuilder(amex, paymentInfoConverterManagement, ANY_HOLDER_NAME).withSplitSettlementData(splitSettlementData)
                                                                                                                              .build();

        final List<AnyType2AnyTypeMap.Entry> entries = paymentRequest.getAdditionalData().getEntry();

        Assert.assertEquals("1", findValue(entries, "splitsettlementdata.api"));
        Assert.assertEquals("3", findValue(entries, "splitsettlementdata.nrOfItems"));
        Assert.assertEquals("2000", findValue(entries, "splitsettlementdata.totalAmount"));
        Assert.assertEquals("EUR", findValue(entries, "splitsettlementdata.currencyCode"));
        Assert.assertEquals("500", findValue(entries, "splitsettlementdata.item1.amount"));
        Assert.assertEquals("EUR", findValue(entries, "splitsettlementdata.item1.currencyCode"));
        Assert.assertEquals("deal1", findValue(entries, "splitsettlementdata.item1.group"));
        Assert.assertEquals("voucherId", findValue(entries, "splitsettlementdata.item1.reference"));
        Assert.assertEquals("voucher", findValue(entries, "splitsettlementdata.item1.type"));
        Assert.assertEquals("750", findValue(entries, "splitsettlementdata.item2.amount"));
        Assert.assertEquals("EUR", findValue(entries, "splitsettlementdata.item2.currencyCode"));
        Assert.assertEquals("deal1", findValue(entries, "splitsettlementdata.item2.group"));
        Assert.assertEquals("voucherId2", findValue(entries, "splitsettlementdata.item2.reference"));
        Assert.assertEquals("voucher", findValue(entries, "splitsettlementdata.item2.type"));
        Assert.assertEquals("750", findValue(entries, "splitsettlementdata.item3.amount"));
        Assert.assertEquals("EUR", findValue(entries, "splitsettlementdata.item3.currencyCode"));
        Assert.assertEquals("deal2", findValue(entries, "splitsettlementdata.item3.group"));
        Assert.assertEquals("travelId", findValue(entries, "splitsettlementdata.item3.reference"));
        Assert.assertEquals("travel", findValue(entries, "splitsettlementdata.item3.type"));
    }

    private Object findValue(final Collection<Entry> entries, final Object key) {
        for (final AnyType2AnyTypeMap.Entry entry : entries) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private SepaDirectDebit createDefaultSepaWithPaymentProvider(final PaymentProvider paymentProvider) {
        return createSepa("accountHolderName", "recurringDetailId", "iban", "bic", paymentProvider);
    }

    private void assertAdditionalDataDoesNotContain(final PaymentRequest paymentRequest, final String key, final String value) {
        Assert.assertFalse(containsMatchingValue(paymentRequest.getAdditionalData(), key, value));
    }

    private BrowserInfo createBrowserInfo() {
        final BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAcceptHeader("acceptHeader");
        browserInfo.setUserAgent("userAgent");
        return browserInfo;
    }

    private void assertAdditionalDataDoesContain(final PaymentRequest paymentRequest, final String key, final String value) {
        Assert.assertTrue(containsMatchingValue(paymentRequest.getAdditionalData(), key, value));
    }

    private boolean containsMatchingValue(final AnyType2AnyTypeMap values, final String key, final String value) {
        final List<Entry> entries = values.getEntry();
        for (final AnyType2AnyTypeMap.Entry entry : entries) {
            if (entry.getKey().equals(key) && entry.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private PaymentProvider createPaymentProviderWithRecurringType(final RecurringType recurringType) {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setRecurringType(recurringType);
        return paymentProvider;
    }

    private SepaDirectDebit createSepa(final String accountHolderName,
                                       final String recurringDetailId,
                                       final String iban,
                                       final String bic,
                                       final PaymentProvider paymentProvider) {
        final SepaDirectDebit sepaPaymentInfo = new SepaDirectDebit(paymentProvider);
        sepaPaymentInfo.setSepaAccountHolder(accountHolderName);
        sepaPaymentInfo.setRecurringDetailId(recurringDetailId);
        sepaPaymentInfo.setIban(iban);
        sepaPaymentInfo.setBic(bic);
        return sepaPaymentInfo;
    }

    private BankAccount createExpectedBankAccountForRequest(final String accountHolderName,
                                                            final String bankName,
                                                            final String bankAccountNumber,
                                                            final String bankLocationId) {
        final BankAccount expectedElv = new BankAccount();
        expectedElv.setOwnerName(accountHolderName);
        expectedElv.setBankAccountNumber(bankAccountNumber);
        expectedElv.setBankLocationId(bankLocationId);
        expectedElv.setBankName(bankName);
        return expectedElv;
    }
}
