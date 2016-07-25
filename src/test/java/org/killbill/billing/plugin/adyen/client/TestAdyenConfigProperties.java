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

public class TestAdyenConfigProperties {

    @Test(groups = "fast")
    public void testConfigurationDefault() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "DefaultAccount");
        properties.put("org.killbill.billing.plugin.adyen.username", "DefaultUsername");
        properties.put("org.killbill.billing.plugin.adyen.password", "DefaultPassword");
        properties.put("org.killbill.billing.plugin.adyen.skin", "DefaultSkin");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "DefaultSecret");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);

        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("UK"), "DefaultAccount");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("DE"), "DefaultAccount");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("US"), "DefaultAccount");

        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccount"), "DefaultUsername");

        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsername"), "DefaultPassword");

        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccount"), "DefaultSkin");

        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkin"), "DefaultSecret");

        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkin"), "HmacSHA256");
    }

    @Test(groups = "fast")
    public void testConfigurationNoOverrides() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "UK#DefaultAccountUK|DE#DefaultAccountDE|US#DefaultAccountUS");
        properties.put("org.killbill.billing.plugin.adyen.username", "UK#DefaultUsernameUK|DE#DefaultUsernameDE|US#DefaultUsernameUS");
        properties.put("org.killbill.billing.plugin.adyen.password", "UK#DefaultPasswordUK|DE#DefaultPasswordDE|DefaultUsernameUS#DefaultPasswordUS");
        properties.put("org.killbill.billing.plugin.adyen.skin", "UK#DefaultSkinUK|US#DefaultSkinUS|DE#DefaultSkinDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "UK#DefaultSecretUK|US#DefaultSecretUS|DE#DefaultSecretDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.algorithm", "UK#DefaultAlgorithmUK|US#DefaultAlgorithmUS|DE#DefaultAlgorithmDE");
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(properties);

        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("UK"), "DefaultAccountUK");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("DE"), "DefaultAccountDE");
        Assert.assertEquals(adyenConfigProperties.getMerchantAccount("US"), "DefaultAccountUS");

        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountUK"), "DefaultUsernameUK");
        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountDE"), "DefaultUsernameDE");
        Assert.assertEquals(adyenConfigProperties.getUserName("DefaultAccountUS"), "DefaultUsernameUS");

        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUK"), "DefaultPasswordUK");
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameDE"), "DefaultPasswordDE");
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUS"), "DefaultPasswordUS");

        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUK"), "DefaultSkinUK");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountDE"), "DefaultSkinDE");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUS"), "DefaultSkinUS");

        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUK"), "DefaultSecretUK");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinDE"), "DefaultSecretDE");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUS"), "DefaultSecretUS");

        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUK"), "DefaultAlgorithmUK");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinDE"), "DefaultAlgorithmDE");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUS"), "DefaultAlgorithmUS");
    }

    @Test(groups = "fast")
    public void testConfigurationWithOverrides() throws Exception {
        final Properties properties = new Properties();
        properties.put("org.killbill.billing.plugin.adyen.merchantAccount", "UK#DefaultAccountUK|DE#DefaultAccountDE|US#DefaultAccountUS");
        properties.put("org.killbill.billing.plugin.adyen.username", "UK#DefaultUsernameUK|DE#DefaultUsernameDE|OverrideAccountUK#OverrideUsernameUK|US#DefaultUsernameUS");
        properties.put("org.killbill.billing.plugin.adyen.password", "UK#DefaultPasswordUK|OverrideUsernameUK#OverridePasswordUK|DE#DefaultPasswordDE|DefaultUsernameUS#DefaultPasswordUS");
        properties.put("org.killbill.billing.plugin.adyen.skin", "UK#DefaultSkinUK|OverrideAccountUK#OverrideSkinUK|US#DefaultSkinUS|DE#DefaultSkinDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.secret", "UK#DefaultSecretUK|OverrideSkinUK#OverrideSecretUK|US#DefaultSecretUS|DE#DefaultSecretDE");
        properties.put("org.killbill.billing.plugin.adyen.hmac.algorithm", "UK#DefaultAlgorithmUK|OverrideSkinUK#OverrideAlgorithmUK|US#DefaultAlgorithmUS|DE#DefaultAlgorithmDE");
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
        Assert.assertEquals(adyenConfigProperties.getPassword("DefaultUsernameUS"), "DefaultPasswordUS");

        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUK"), "DefaultSkinUK");
        Assert.assertEquals(adyenConfigProperties.getSkin("OverrideAccountUK"), "OverrideSkinUK");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountDE"), "DefaultSkinDE");
        Assert.assertEquals(adyenConfigProperties.getSkin("DefaultAccountUS"), "DefaultSkinUS");

        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUK"), "DefaultSecretUK");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("OverrideSkinUK"), "OverrideSecretUK");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinDE"), "DefaultSecretDE");
        Assert.assertEquals(adyenConfigProperties.getHmacSecret("DefaultSkinUS"), "DefaultSecretUS");

        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUK"), "DefaultAlgorithmUK");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("OverrideSkinUK"), "OverrideAlgorithmUK");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinDE"), "DefaultAlgorithmDE");
        Assert.assertEquals(adyenConfigProperties.getHmacAlgorithm("DefaultSkinUS"), "DefaultAlgorithmUS");
    }
}
