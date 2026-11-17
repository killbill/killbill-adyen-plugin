/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
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
package org.killbill.billing.plugin.adyen.client;

import com.adyen.model.Amount;
import java.math.BigDecimal;
import org.killbill.billing.catalog.api.Currency;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestAdyenSDKClientImpl {

  @Test(groups = "fast")
  public void testToAdyenAmountTwoDecimalCurrency() {
    // The scale of the Kill Bill amount must not change the value sent to Adyen
    assertAdyenAmount(Currency.CHF, "10", 1000L);
    assertAdyenAmount(Currency.CHF, "10.0", 1000L);
    assertAdyenAmount(Currency.CHF, "10.00", 1000L);
    assertAdyenAmount(Currency.CHF, "10.000000000", 1000L);
    assertAdyenAmount(Currency.CHF, "10.5", 1050L);
    assertAdyenAmount(Currency.CHF, "0.01", 1L);
    assertAdyenAmount(Currency.USD, "1.000000000", 100L);
  }

  @Test(groups = "fast")
  public void testToAdyenAmountZeroAndThreeDecimalCurrencies() {
    assertAdyenAmount(Currency.JPY, "100", 100L);
    assertAdyenAmount(Currency.JPY, "100.00", 100L);
    assertAdyenAmount(Currency.KWD, "1.5", 1500L);
    assertAdyenAmount(Currency.KWD, "1.234", 1234L);
  }

  @Test(groups = "fast")
  public void testToAdyenAmountRoundsExtraDecimalsHalfUp() {
    assertAdyenAmount(Currency.CHF, "10.005", 1001L);
    assertAdyenAmount(Currency.CHF, "10.004", 1000L);
    assertAdyenAmount(Currency.JPY, "100.5", 101L);
  }

  private static void assertAdyenAmount(
      final Currency currency, final String kbAmount, final long expectedMinorUnits) {
    final Amount amount = AdyenSDKClientImpl.toAdyenAmount(currency, new BigDecimal(kbAmount));
    Assert.assertEquals(amount.getCurrency(), currency.name());
    Assert.assertEquals(amount.getValue(), Long.valueOf(expectedMinorUnits));
  }
}
