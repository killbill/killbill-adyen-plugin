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

package org.killbill.billing.plugin.adyen.api.mapping;

import org.joda.time.DateTime;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.clock.Clock;

import com.google.common.base.Strings;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ALLOWED_METHODS;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_BLOCKED_METHODS;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_BRAND_CODE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ISSUER_ID;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MERCHANT_RETURN_DATA;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_OFFER_EMAIL;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ORDER_DATA;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_RESULT_URL;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_SERVER_URL;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_SESSION_VALIDITY;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_SHIP_BEFORE_DATE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_SKIN_CODE;

public abstract class WebPaymentFrontendMappingService {

    public static PaymentInfo toPaymentInfo(final AdyenConfigProperties configuration, final Clock clock, final Iterable<PluginProperty> properties) {
        final WebPaymentFrontend paymentInfo = new WebPaymentFrontend();

        final String propertyShipBeforeDate = PluginProperties.findPluginPropertyValue(PROPERTY_SHIP_BEFORE_DATE, properties);
        final DateTime shipBeforeDateTime = propertyShipBeforeDate == null ? clock.getUTCNow().plusHours(1) : new DateTime(propertyShipBeforeDate);
        paymentInfo.setShipBeforeDate(shipBeforeDateTime.toString("yyyy-MM-dd"));

        final String skinCode = PluginProperties.getValue(PROPERTY_SKIN_CODE, configuration.getSkin(paymentInfo.getCountry()), properties);
        paymentInfo.setSkinCode(skinCode);

        final String orderData = PluginProperties.findPluginPropertyValue(PROPERTY_ORDER_DATA, properties);
        paymentInfo.setOrderData(orderData);

        final String sessionValidity = PluginProperties.getValue(PROPERTY_SESSION_VALIDITY, clock.getUTCNow().plusMinutes(15).toString("yyyy-MM-dd'T'HH:mm:ssZZ"), properties);
        paymentInfo.setSessionValidity(sessionValidity);

        final String merchantReturnData = PluginProperties.findPluginPropertyValue(PROPERTY_MERCHANT_RETURN_DATA, properties);
        paymentInfo.setMerchantReturnData(merchantReturnData);

        final String allowedMethods = PluginProperties.findPluginPropertyValue(PROPERTY_ALLOWED_METHODS, properties);
        paymentInfo.setAllowedMethods(allowedMethods);

        final String blockedMethods = PluginProperties.findPluginPropertyValue(PROPERTY_BLOCKED_METHODS, properties);
        paymentInfo.setBlockedMethods(blockedMethods);

        final String brandCode = PluginProperties.getValue(PROPERTY_BRAND_CODE, configuration.getHppVariantOverride(), properties);
        paymentInfo.setBrandCode(brandCode);

        final String issuerId = PluginProperties.findPluginPropertyValue(PROPERTY_ISSUER_ID, properties);
        paymentInfo.setIssuerId(issuerId);

        final String offerEmail = PluginProperties.findPluginPropertyValue(PROPERTY_OFFER_EMAIL, properties);
        paymentInfo.setOfferEmail(offerEmail);

        final String serverUrl = PluginProperties.findPluginPropertyValue(PROPERTY_SERVER_URL, properties);
        final String resultUrl = PluginProperties.findPluginPropertyValue(PROPERTY_RESULT_URL, properties);
        paymentInfo.setResURL(Strings.nullToEmpty(serverUrl) + Strings.nullToEmpty(resultUrl));

        return paymentInfo;
    }
}
