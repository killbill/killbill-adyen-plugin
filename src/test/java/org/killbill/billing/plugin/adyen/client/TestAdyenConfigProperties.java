/*
 * Copyright 2016 Groupon, Inc
 * Copyright 2016 The Billing Project, LLC
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

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestAdyenConfigProperties {

    @Test(groups = "fast")
    public void testConfigurationDefault() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "DefaultAccount");
        properties.put("org.killbill.billing.plugin.adyen.username", "DefaultUsername");
        properties.put("org.killbill.billing.plugin.adyen.password", "DefaultPassword");
        properties.put("org.killbill.billing.plugin.adyen.skin", "DefaultSkin");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "DefaultSecret");
        properties.put("org.killbill.billing.plugin.adyen.pendingPaymentExpirationPeriod", "P2D");
        properties.put("org.killbill.billing.plugin.adyen.paymentUrl", "http://paymentUrl.com");
        properties.put("org.killbill.billing.plugin.adyen.recurringUrl", "http://recurringUrl.com");
        properties.put("org.killbill.billing.plugin.adyen.directoryUrl", "http://directoryUrl.com");
        properties.put("org.killbill.billing.plugin.adyen.sensitiveProperties", "ip|username|email|paymentBillingRecord");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);

        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("UK"), "DefaultAccount");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("DE"), "DefaultAccount");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("US"), "DefaultAccount");

        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccount"), "DefaultUsername");

        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsername"), "DefaultPassword");

        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccount"), "DefaultSkin");

        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkin"), "DefaultSecret");

        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkin"), "HmacSHA256");

        Assert.assertEquals(adyenConfigProperties.getPending3DsPaymentExpirationPeriod().toString(), "PT3H");
        Assert.assertEquals(adyenConfigProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod().toString(), "PT3H");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod(null).toString(), "P2D");
        // Don't use per-payment method default since user specified a global setting
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod("paypal").toString(), "P2D");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod("boletobancario_santander").toString(), "P2D");

        Assert.assertEquals(adyenConfigProperties.getPaymentUrl(), "http://paymentUrl.com");
        Assert.assertEquals(adyenConfigProperties.getRecurringUrl(), "http://recurringUrl.com");
        Assert.assertEquals(adyenConfigProperties.getDirectoryUrl(), "http://directoryUrl.com");

        Assert.assertEquals(adyenConfigProperties.getSensitivePropertyKeys(), ImmutableList.of("ip", "username", "email", "paymentBillingRecord"));
    }

    @Test(groups = "fast")
    public void testConfigurationNoOverrides() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "UK#DefaultAccountUK|DE#DefaultAccountDE|US#DefaultAccountUS");
        properties.put("org.killbill.billing.plugin.adyen.username", "UK#DefaultUsernameUK|DE#DefaultUsernameDE|US#DefaultUsernameUS");
        properties.put("org.killbill.billing.plugin.adyen.password", "UK#DefaultPasswordUK|DE#DefaultPasswordDE|DefaultUsernameUS#Default#PasswordUS");
        properties.put("org.killbill.billing.plugin.adyen.skin", "UK#DefaultSkinUK|US#DefaultSkinUS|DE#DefaultSkinDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "UK#DefaultSecretUK|US#DefaultSecretUS|DE#DefaultSecretDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.algorithm", "UK#DefaultAlgorithmUK|US#DefaultAlgorithmUS|DE#DefaultAlgorithmDE");
        properties.put("org.killbill.billing.plugin.adyen.pendingPaymentExpirationPeriod", "paypal#P4D");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);

        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("UK"), "DefaultAccountUK");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("DE"), "DefaultAccountDE");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("US"), "DefaultAccountUS");

        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountUK"), "DefaultUsernameUK");
        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountDE"), "DefaultUsernameDE");
        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountUS"), "DefaultUsernameUS");

        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUK"), "DefaultPasswordUK");
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameDE"), "DefaultPasswordDE");
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUS"), "Default#PasswordUS");

        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUK"), "DefaultSkinUK");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountDE"), "DefaultSkinDE");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUS"), "DefaultSkinUS");

        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUK"), "DefaultSecretUK");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinDE"), "DefaultSecretDE");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUS"), "DefaultSecretUS");

        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUK"), "DefaultAlgorithmUK");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinDE"), "DefaultAlgorithmDE");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUS"), "DefaultAlgorithmUS");

        Assert.assertEquals(adyenConfigProperties.getPending3DsPaymentExpirationPeriod().toString(), "PT3H");
        Assert.assertEquals(adyenConfigProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod().toString(), "PT3H");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod(null).toString(), "P3D");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod("paypal").toString(), "P4D");
        // Use per-payment method default since user did only override paypal
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod("boletobancario_santander").toString(), "P7D");
    }

    @Test(groups = "fast")
    public void testConfigurationWithOverrides() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "UK#DefaultAccountUK|DE#DefaultAccountDE|US#DefaultAccountUS");
        properties.put("org.killbill.billing.plugin.adyen.username", "UK#DefaultUsernameUK|DE#DefaultUsernameDE|OverrideAccountUK#OverrideUsernameUK|US#DefaultUsernameUS");
        properties.put("org.killbill.billing.plugin.adyen.password", "UK#DefaultPasswordUK|OverrideUsernameUK#OverridePasswordUK|DE#DefaultPasswordDE|DefaultUsernameUS#Default#PasswordUS");
        properties.put("org.killbill.billing.plugin.adyen.skin", "UK#DefaultSkinUK|OverrideAccountUK#OverrideSkinUK|US#DefaultSkinUS|DE#DefaultSkinDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "UK#DefaultSecretUK|OverrideSkinUK#OverrideSecretUK|US#Default#SecretUS|DE#DefaultSecretDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.algorithm", "UK#DefaultAlgorithmUK|OverrideSkinUK#OverrideAlgorithmUK|US#DefaultAlgorithmUS|DE#DefaultAlgorithmDE");
        properties.put("org.killbill.billing.plugin.adyen.pendingPaymentExpirationPeriod", "paypal#P4D|boletobancario_santander#P12D");
        properties.put("org.killbill.billing.plugin.adyen.pendingHppPaymentWithoutCompletionExpirationPeriod", "P12D");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);

        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("UK"), "DefaultAccountUK");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("DE"), "DefaultAccountDE");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("US"), "DefaultAccountUS");

        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountUK"), "DefaultUsernameUK");
        Assert.assertEquals(adyenConfigProperties.getUserName("OverrideAccountUK"), "OverrideUsernameUK");
        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountDE"), "DefaultUsernameDE");
        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountUS"), "DefaultUsernameUS");

        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUK"), "DefaultPasswordUK");
        Assert.assertEquals(adyenConfigProperties.getPassword("OverrideUsernameUK"), "OverridePasswordUK");
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameDE"), "DefaultPasswordDE");
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUS"), "Default#PasswordUS");

        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUK"), "DefaultSkinUK");
        Assert.assertEquals(adyenConfigProperties.getSkin("OverrideAccountUK"), "OverrideSkinUK");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountDE"), "DefaultSkinDE");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUS"), "DefaultSkinUS");

        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUK"), "DefaultSecretUK");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("OverrideSkinUK"), "OverrideSecretUK");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinDE"), "DefaultSecretDE");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUS"), "Default#SecretUS");

        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUK"), "DefaultAlgorithmUK");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("OverrideSkinUK"), "OverrideAlgorithmUK");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinDE"), "DefaultAlgorithmDE");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUS"), "DefaultAlgorithmUS");

        Assert.assertEquals(adyenConfigProperties.getPending3DsPaymentExpirationPeriod().toString(), "PT3H");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod(null).toString(), "P3D");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod("paypal").toString(), "P4D");
        Assert.assertEquals(adyenConfigProperties.getPendingPaymentExpirationPeriod("boletobancario_santander").toString(), "P12D");
        Assert.assertEquals(adyenConfigProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod().toString(), "P12D");
    }

    @Test(groups = "fast")
    public void testConfigurationWithFallbacks() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "UK#DefaultAccountUK|FALLBACK#FALLBACKAccountDE");
        properties.put("org.killbill.billing.plugin.adyen.username", "UK#DefaultUsernameUK|FALLBACKAccountDE#DefaultUsernameDE");
        properties.put("org.killbill.billing.plugin.adyen.password", "UK#DefaultPasswordUK|FALLBACKAccountDE#DefaultPasswordDE");
        properties.put("org.killbill.billing.plugin.adyen.skin", "UK#DefaultSkinUK|FALLBACKAccountDE#FALLBACKSkinDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "UK#DefaultSecretUK|FALLBACKAccountDE#FALLBACKHmacDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.algorithm", "UK#DefaultAlgorithmUK|FALLBACKAccountDE#FALLBACKAlgorithmUK");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);

        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("United States"), "FALLBACKAccountDE");
        Assert.assertEquals(adyenConfigProperties.getUserName("FALLBACKAccountDE"), "DefaultUsernameDE");
        Assert.assertEquals(adyenConfigProperties.getPassword("FALLBACKAccountDE"), "DefaultPasswordDE");
        Assert.assertEquals(adyenConfigProperties.getSkin("FALLBACKAccountDE"), "FALLBACKSkinDE");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("FALLBACKAccountDE"), "FALLBACKHmacDE");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("FALLBACKAccountDE"), "FALLBACKAlgorithmUK");
    }

    @Test(groups = "fast",
          expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Failed to find merchant account for countryCode='United States'")
    public void testConfigurationWithoutFallbackWithoutCountryCodeMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "UK#DefaultAccountUK");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);
        adyenConfigProperties.getMerchantAccount("United States");
    }

    @Test(groups = "fast")
    public void testConfigurationWithMultiRegions() throws Exception {
        final Properties properties = new Properties();
        properties.put("us-east-1.org.killbill.billing.plugin.adyen.paymentUrl", "http://paymentUrl1.com");
        properties.put("eu-west-1.org.killbill.billing.plugin.adyen.paymentUrl", "http://paymentUrl2.com");
        properties.put("us-east-1.org.killbill.billing.plugin.adyen.recurringUrl", "http://recurringUrl1.com");
        properties.put("eu-west-1.org.killbill.billing.plugin.adyen.recurringUrl", "http://recurringUrl2.com");
        properties.put("us-east-1.org.killbill.billing.plugin.adyen.directoryUrl", "http://directoryUrl1.com");
        properties.put("eu-west-1.org.killbill.billing.plugin.adyen.directoryUrl", "http://directoryUrl2.com");

        final AdyenConfigProperties adyenConfigPropertiesEast = new AdyenConfigProperties(properties, "us-east-1");
        final AdyenConfigProperties adyenConfigPropertiesWest = new AdyenConfigProperties(properties, "eu-west-1");
        final AdyenConfigProperties adyenConfigPropertiesOther = new AdyenConfigProperties(properties, "local");

        Assert.assertEquals(adyenConfigPropertiesEast.getPaymentUrl(), "http://paymentUrl1.com");
        Assert.assertEquals(adyenConfigPropertiesEast.getRecurringUrl(), "http://recurringUrl1.com");
        Assert.assertEquals(adyenConfigPropertiesEast.getDirectoryUrl(), "http://directoryUrl1.com");

        Assert.assertEquals(adyenConfigPropertiesWest.getPaymentUrl(), "http://paymentUrl2.com");
        Assert.assertEquals(adyenConfigPropertiesWest.getRecurringUrl(), "http://recurringUrl2.com");
        Assert.assertEquals(adyenConfigPropertiesWest.getDirectoryUrl(), "http://directoryUrl2.com");

        Assert.assertNull(adyenConfigPropertiesOther.getPaymentUrl());
        Assert.assertNull(adyenConfigPropertiesOther.getRecurringUrl());
        Assert.assertNull(adyenConfigPropertiesOther.getDirectoryUrl());
    }
}
