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

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.Amount;
import java.math.BigDecimal;
import java.util.Properties;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.core.AdyenConfigProperties;
import org.testng.Assert;
import org.testng.SkipException;
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
  public void testToAdyenAmountCurrenciesWithFewerDecimalsInAdyen() {
    // Kill Bill keeps 2 decimals for IDR and CVE, Adyen uses 0: amounts are rounded HALF_UP
    assertAdyenAmount(Currency.IDR, "1000", 1000L);
    assertAdyenAmount(Currency.IDR, "1000.00", 1000L);
    assertAdyenAmount(Currency.IDR, "1000.50", 1001L);
    assertAdyenAmount(Currency.IDR, "1000.49", 1000L);
    assertAdyenAmount(Currency.CVE, "500.50", 501L);
  }

  @Test(groups = "fast")
  public void testToAdyenAmountRoundsExtraDecimalsHalfUp() {
    assertAdyenAmount(Currency.CHF, "10.005", 1001L);
    assertAdyenAmount(Currency.CHF, "10.004", 1000L);
    assertAdyenAmount(Currency.JPY, "100.5", 101L);
  }

  @Test(groups = "fast")
  public void testBuildClientTest() {
    final Client client = AdyenSDKClientImpl.buildClient(configProperties("TEST", null));
    Assert.assertEquals(client.getConfig().getEnvironment(), Environment.TEST);
    Assert.assertEquals(
        client.getConfig().getCheckoutEndpoint(), "https://checkout-test.adyen.com/checkout");
  }

  @Test(groups = "fast")
  public void testBuildClientLiveWithPrefix() {
    final Client client =
        AdyenSDKClientImpl.buildClient(configProperties("LIVE", " 1797a841fbb37ca7-AdyenDemo "));
    Assert.assertEquals(client.getConfig().getEnvironment(), Environment.LIVE);
    Assert.assertEquals(
        client.getConfig().getCheckoutEndpoint(),
        "https://1797a841fbb37ca7-AdyenDemo-checkout-live.adyenpayments.com/checkout");
  }

  @Test(groups = "fast")
  public void testBuildClientLiveWithoutPrefix() {
    if (System.getenv(AdyenConfigProperties.ADYEN_LIVE_ENDPOINT_URL_PREFIX) != null) {
      throw new SkipException(
          AdyenConfigProperties.ADYEN_LIVE_ENDPOINT_URL_PREFIX + " is set in the environment");
    }
    Assert.expectThrows(
        IllegalStateException.class,
        () -> AdyenSDKClientImpl.buildClient(configProperties("LIVE", null)));
    Assert.expectThrows(
        IllegalStateException.class,
        () -> AdyenSDKClientImpl.buildClient(configProperties("LIVE", "  ")));
  }

  private static AdyenConfigProperties configProperties(
      final String environment, final String liveEndpointUrlPrefix) {
    final Properties properties = new Properties();
    properties.setProperty("org.killbill.billing.plugin.adyen.apiKey", "test_api_key");
    properties.setProperty("org.killbill.billing.plugin.adyen.enviroment", environment);
    if (liveEndpointUrlPrefix != null) {
      properties.setProperty(
          "org.killbill.billing.plugin.adyen.liveEndpointUrlPrefix", liveEndpointUrlPrefix);
    }
    return new AdyenConfigProperties(properties, "CH");
  }

  private static void assertAdyenAmount(
      final Currency currency, final String kbAmount, final long expectedMinorUnits) {
    final Amount amount = AdyenSDKClientImpl.toAdyenAmount(currency, new BigDecimal(kbAmount));
    Assert.assertEquals(amount.getCurrency(), currency.name());
    Assert.assertEquals(amount.getValue(), Long.valueOf(expectedMinorUnits));
  }
}
