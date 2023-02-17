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

package org.killbill.billing.plugin.adyen.core;

import java.util.Map;
import java.util.Properties;

public class AdyenConfigProperties {

  private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.adyen.";

  public static final String ADYEN_API_KEY = "ADYEN_API_KEY";
  public static final String ADYEN_RETURN_URL = "ADYEN_RETURN_URL";
  public static final String ADYEN_HMAC_KEY = "ADYEN_HMAC_KEY";
  public static final String ADYEN_MERCHANT_ACCOUNT = "ADYEN_MERCHANT_ACCOUNT";
  public static final String ADYEN_ENVIROMENT = "ADYEN_ENVIROMENT";
  public static final String ADYEN_CAPTURE_DELAY_HOURS = "ADYEN_CAPTURE_DELAY_HOURS";
  public static final String ADYEN_KB_USERNAME = "ADYEN_KB_USERNAME";
  public static final String ADYEN_KB_PASSWORD = "ADYEN_KB_PASSWORD";

  private final String region;

  private String apiKey;
  private String merchantAccount;
  private String returnUrl;
  private String hcmaKey;
  private String captureDelayHours;
  private String enviroment;

  private String username;

  private String password;

  public AdyenConfigProperties(final Properties properties, final String region) {
    this.region = region;

    this.apiKey = properties.getProperty(PROPERTY_PREFIX + "apiKey");
    this.merchantAccount = properties.getProperty(PROPERTY_PREFIX + "merchantAccount");
    this.returnUrl = properties.getProperty(PROPERTY_PREFIX + "returnUrl");
    this.hcmaKey = properties.getProperty(PROPERTY_PREFIX + "hcmaKey");
    this.captureDelayHours = properties.getProperty(PROPERTY_PREFIX + "captureDelayHours");
    this.enviroment = properties.getProperty(PROPERTY_PREFIX + "enviroment");
    this.username = properties.getProperty(PROPERTY_PREFIX + "username");
    this.password = properties.getProperty(PROPERTY_PREFIX + "password");
  }

  public String getRegion() {

    return region;
  }

  public String getApiKey() {
    if (apiKey == null || apiKey.isEmpty()) {
      return getClient(ADYEN_API_KEY, null);
    }
    return apiKey;
  }

  public String getHMACKey() {
    if (hcmaKey == null || hcmaKey.isEmpty()) {
      return getClient(ADYEN_HMAC_KEY, null);
    }
    return hcmaKey;
  }

  public String getMerchantAccount() {
    if (merchantAccount == null || merchantAccount.isEmpty()) {
      return getClient(ADYEN_MERCHANT_ACCOUNT, null);
    }

    return merchantAccount;
  }

  public String getReturnUrl() {
    if (returnUrl == null || returnUrl.isEmpty()) {
      return getClient(ADYEN_RETURN_URL, null);
    }

    return returnUrl;
  }

  public String getUsername() {
    if (username == null || username.isEmpty()) {
      return getClient(ADYEN_KB_USERNAME, null);
    }

    return username;
  }

  public String getPassword() {
    if (password == null || password.isEmpty()) {
      return getClient(ADYEN_KB_PASSWORD, null);
    }

    return password;
  }

  public String getCaptureDelayHours() {
    if (captureDelayHours == null || captureDelayHours.isEmpty()) {
      return getClient(ADYEN_CAPTURE_DELAY_HOURS, null);
    }

    return captureDelayHours;
  }

  public String getEnviroment() {
    if (enviroment == null || enviroment.isEmpty()) {
      return getClient(ADYEN_ENVIROMENT, "TEST");
    }

    return enviroment;
  }

  private String getClient(String envKey, String defaultValue) {
    Map<String, String> env = System.getenv();

    String value = env.get(envKey);

    if (value == null || value.isEmpty()) {
      return defaultValue;
    }

    return value;
  }
}
