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
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.billing.catalog.api.Currency;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestModificationRequestBuilder {

    private static final String CURRENCY = Currency.EUR.name();

    @Test(groups = "fast")
    public void testWithOriginalReference() throws Exception {
        final String originalReference = "originalReference";
        final ModificationRequest modificationRequest = new ModificationRequestBuilder().withOriginalReference(originalReference)
                                                                                        .build();

        Assert.assertEquals(modificationRequest.getOriginalReference(), originalReference, "Wrong OriginalReference in Request");
    }

    @Test(groups = "fast")
    public void testWithMerchantAccount() throws Exception {
        final String merchantAccount = "merchantAccount";
        final ModificationRequest modificationRequest = new ModificationRequestBuilder().withMerchantAccount(merchantAccount)
                                                                                        .build();

        Assert.assertEquals(modificationRequest.getMerchantAccount(), merchantAccount, "Wrong MerchantAccount in Request");
    }

    @Test(groups = "fast")
    public void testWithAuthorisationCode() throws Exception {
        final String authorisationCode = "authorisationCode";
        final ModificationRequest modificationRequest = new ModificationRequestBuilder().withAuthorisationCode(authorisationCode)
                                                                                        .build();

        Assert.assertEquals(modificationRequest.getAuthorisationCode(), authorisationCode, "Wrong AuthorisationCode in Request");
    }

    @Test(groups = "fast")
    public void testWithAmountByValues() throws Exception {
        final Long value = 1L;
        final ModificationRequest modificationRequest = new ModificationRequestBuilder().withAmount(CURRENCY, value)
                                                                                        .build();

        Assert.assertNotNull(modificationRequest.getModificationAmount(), "No Amount in Request");
        Assert.assertEquals(modificationRequest.getModificationAmount().getCurrency(), CURRENCY, "Wrong Currency in Amount in Request");
        Assert.assertEquals(modificationRequest.getModificationAmount().getValue(), value, "Wrong Value in Amount in Request");
    }

    @Test(groups = "fast")
    public void testWithAmountByEntity() throws Exception {
        final Amount amount = createAmount(CURRENCY, 1L);

        final ModificationRequest modificationRequest = new ModificationRequestBuilder().withAmount(amount)
                                                                                        .build();

        Assert.assertNotNull(modificationRequest.getModificationAmount(), "No Amount in Request");
        Assert.assertEquals(modificationRequest.getModificationAmount().getCurrency(), amount.getCurrency(), "Wrong Currency in Amount in Request");
        Assert.assertEquals(modificationRequest.getModificationAmount().getValue(), amount.getValue(), "Wrong Value in Amount in Request");
    }

    private Amount createAmount(final String currency, final Long value) {
        final Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setValue(value);
        return amount;
    }
}
