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

package org.killbill.billing.plugin.adyen.api;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.api.PluginProperties;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestAdyenPaymentPluginApiHPP extends TestAdyenPaymentPluginApiBase {

    private static final String PSP_REFERENCE = "4823660019473428";

    @Test(groups = "slow")
    public void testHPPNoPendingPayment() throws Exception {
        triggerBuildFormDescriptor(ImmutableMap.<String, String>of(), null);

        processHPPNotification();

        verifyPayment(TransactionType.AUTHORIZE);
    }

    @Test(groups = "slow")
    public void testHPPWithPendingPurchase() throws Exception {
        // Trigger buildFormDescriptor (and create a PENDING purchase)
        triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true"), TransactionType.PURCHASE);

        processHPPNotification();

        verifyPayment(TransactionType.PURCHASE);
    }

    @Test(groups = "slow")
    public void testHPPWithPendingAuthorization() throws Exception {
        // Trigger buildFormDescriptor (and create a PENDING authorization)
        triggerBuildFormDescriptor(ImmutableMap.<String, String>of(AdyenPaymentPluginApi.PROPERTY_CREATE_PENDING_PAYMENT, "true",
                                                                   AdyenPaymentPluginApi.PROPERTY_AUTH_MODE, "true"),
                                   TransactionType.AUTHORIZE);

        processHPPNotification();

        verifyPayment(TransactionType.AUTHORIZE);
    }

    private void triggerBuildFormDescriptor(final Map<String, String> extraProperties, @Nullable final TransactionType transactionType) throws PaymentPluginApiException, SQLException {
        assertNull(dao.getHppRequest(paymentTransaction.getExternalKey()));
        assertTrue(dao.getResponses(payment.getId(), context.getTenantId()).isEmpty());

        final Builder<String, String> propsBuilder = new Builder<String, String>();
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_AMOUNT, "10");
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_PAYMENT_EXTERNAL_KEY, paymentTransaction.getExternalKey());
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_SERVER_URL, "http://killbill.io");
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_CURRENCY, DEFAULT_CURRENCY.name());
        propsBuilder.put(AdyenPaymentPluginApi.PROPERTY_COUNTRY, DEFAULT_COUNTRY);
        propsBuilder.putAll(extraProperties);
        final Map<String, String> customFieldsMap = propsBuilder.build();
        final Iterable<PluginProperty> customFields = PluginProperties.buildPluginProperties(customFieldsMap);

        final HostedPaymentPageFormDescriptor descriptor = adyenPaymentPluginApi.buildFormDescriptor(payment.getAccountId(), customFields, ImmutableList.<PluginProperty>of(), context);
        assertEquals(descriptor.getKbAccountId(), payment.getAccountId());
        assertEquals(descriptor.getFormMethod(), "GET");
        assertNotNull(descriptor.getFormUrl());
        assertFalse(descriptor.getFormFields().isEmpty());
        assertNotNull(dao.getHppRequest(paymentTransaction.getExternalKey()));

        // For manual testing
        //System.out.println("Redirect to: " + descriptor.getFormUrl());
        //System.out.flush();

        final Boolean withPendingPayment = Boolean.valueOf(customFieldsMap.get(PROPERTY_CREATE_PENDING_PAYMENT));
        if (withPendingPayment) {
            assertEquals(dao.getResponses(payment.getId(), context.getTenantId()).size(), 1);

            final List<PaymentTransactionInfoPlugin> pendingPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(payment.getAccountId(), payment.getId(), ImmutableList.<PluginProperty>of(), context);
            assertEquals(pendingPaymentTransactions.size(), 1);
            assertEquals(pendingPaymentTransactions.get(0).getStatus(), PaymentPluginStatus.PENDING);
            assertEquals(pendingPaymentTransactions.get(0).getTransactionType(), transactionType);
        } else {
            assertTrue(dao.getResponses(payment.getId(), context.getTenantId()).isEmpty());
        }
    }

    private void processHPPNotification() throws PaymentPluginApiException {
        final String notification = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                    "  <soap:Body>\n" +
                                    "    <ns1:sendNotification xmlns:ns1=\"http://notification.services.adyen.com\">\n" +
                                    "      <ns1:notification>\n" +
                                    "        <live xmlns=\"http://notification.services.adyen.com\">true</live>\n" +
                                    "        <notificationItems xmlns=\"http://notification.services.adyen.com\">\n" +
                                    "          <NotificationRequestItem>\n" +
                                    "            <additionalData>\n" +
                                    "              <entry>\n" +
                                    "                <key xsi:type=\"xsd:string\">hmacSignature</key>\n" +
                                    "                <value xsi:type=\"xsd:string\">XlhIGK7wKAFJ1D1aqceFwLkXSL1XXf1DWBVhUo17rqo=</value>\n" +
                                    "              </entry>\n" +
                                    "            </additionalData>\n" +
                                    "            <amount>\n" +
                                    "              <currency xmlns=\"http://common.services.adyen.com\">" + DEFAULT_CURRENCY.name() + "</currency>\n" +
                                    "              <value xmlns=\"http://common.services.adyen.com\">10</value>\n" +
                                    "            </amount>\n" +
                                    "            <eventCode>AUTHORISATION</eventCode>\n" +
                                    "            <eventDate>2013-04-15T06:59:22.278+02:00</eventDate>\n" +
                                    "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                    "            <merchantReference>" + paymentTransaction.getExternalKey() + "</merchantReference>\n" +
                                    "            <operations>\n" +
                                    "              <string>CANCEL</string>\n" +
                                    "              <string>CAPTURE</string>\n" +
                                    "              <string>REFUND</string>\n" +
                                    "            </operations>\n" +
                                    "            <originalReference xsi:nil=\"true\"/>\n" +
                                    "            <paymentMethod>visa</paymentMethod>\n" +
                                    "            <pspReference>" + PSP_REFERENCE + "</pspReference>\n" +
                                    "            <reason>111647:7629:5/2014</reason>\n" +
                                    "            <success>true</success>\n" +
                                    "          </NotificationRequestItem>\n" +
                                    "        </notificationItems>\n" +
                                    "      </ns1:notification>\n" +
                                    "    </ns1:sendNotification>\n" +
                                    "  </soap:Body>\n" +
                                    "</soap:Envelope>";
        final GatewayNotification gatewayNotification = adyenPaymentPluginApi.processNotification(notification, ImmutableList.<PluginProperty>of(), context);
        assertEquals(gatewayNotification.getEntity(), "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><sendNotificationResponse xmlns=\"http://notification.services.adyen.com\" xmlns:ns2=\"http://common.services.adyen.com\"><notificationResponse>[accepted]</notificationResponse></sendNotificationResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>");
    }

    private void verifyPayment(final TransactionType transactionType) throws SQLException, PaymentPluginApiException {
        assertEquals(dao.getResponses(payment.getId(), context.getTenantId()).size(), 1);

        final List<PaymentTransactionInfoPlugin> processedPaymentTransactions = adyenPaymentPluginApi.getPaymentInfo(payment.getAccountId(), payment.getId(), ImmutableList.<PluginProperty>of(), context);
        assertEquals(processedPaymentTransactions.size(), 1);
        assertEquals(processedPaymentTransactions.get(0).getTransactionType(), transactionType);
        assertEquals(processedPaymentTransactions.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        assertEquals(processedPaymentTransactions.get(0).getFirstPaymentReferenceId(), PSP_REFERENCE);
    }
}
