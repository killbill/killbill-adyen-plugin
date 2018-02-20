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

import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;

import com.google.common.collect.ImmutableList;

import static org.testng.Assert.assertEquals;

public class TestAdyenPaymentPluginApiBase extends TestWithEmbeddedDBBase {

    protected ClockMock clock;
    protected CallContext context;
    protected Account account;
    protected AdyenPaymentPluginApi adyenPaymentPluginApi;
    protected OSGIKillbillAPI killbillApi;
    protected String defaultMerchantAccount;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        clock = new ClockMock();

        context = Mockito.mock(CallContext.class);
        Mockito.when(context.getTenantId()).thenReturn(UUID.randomUUID());

        account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
        killbillApi = TestUtils.buildOSGIKillbillAPI(account);

        TestUtils.buildPaymentMethod(account.getId(), account.getPaymentMethodId(), AdyenActivator.PLUGIN_NAME, killbillApi);

        final OSGIKillbillLogService logService = TestUtils.buildLogService();

        final OSGIConfigPropertiesService configPropertiesService = Mockito.mock(OSGIConfigPropertiesService.class);
        adyenPaymentPluginApi = new AdyenPaymentPluginApi(adyenConfigurationHandler,
                                                          adyenConfigPropertiesConfigurationHandler,
                                                          adyenHostedPaymentPageConfigurationHandler,
                                                          adyenRecurringConfigurationHandler,
                                                          killbillApi,
                                                          configPropertiesService,
                                                          logService,
                                                          clock,
                                                          dao);

        TestUtils.updateOSGIKillbillAPI(killbillApi, adyenPaymentPluginApi);
    }

    @BeforeMethod(groups = "integration")
    public void setUpRemote() throws Exception {
        setUp();
        defaultMerchantAccount = adyenConfigPropertiesConfigurationHandler.getConfigurable(context.getTenantId()).getMerchantAccount(DEFAULT_COUNTRY);
    }

    protected void processNotification(final String eventCode, final boolean success, final String merchantReference, final String pspReference) throws PaymentPluginApiException {
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
                                    "            <eventCode>" + eventCode + "</eventCode>\n" +
                                    "            <eventDate>2013-04-15T06:59:22.278+02:00</eventDate>\n" +
                                    "            <merchantAccountCode>" + defaultMerchantAccount + "</merchantAccountCode>\n" +
                                    "            <merchantReference>" + merchantReference + "</merchantReference>\n" +
                                    "            <operations>\n" +
                                    "              <string>CANCEL</string>\n" +
                                    "              <string>CAPTURE</string>\n" +
                                    "              <string>REFUND</string>\n" +
                                    "            </operations>\n" +
                                    "            <originalReference xsi:nil=\"true\"/>\n" +
                                    "            <paymentMethod>visa</paymentMethod>\n" +
                                    "            <pspReference>" + pspReference + "</pspReference>\n" +
                                    "            <reason>111647:7629:5/2014</reason>\n" +
                                    "            <success>" + success + "</success>\n" +
                                    "          </NotificationRequestItem>\n" +
                                    "        </notificationItems>\n" +
                                    "      </ns1:notification>\n" +
                                    "    </ns1:sendNotification>\n" +
                                    "  </soap:Body>\n" +
                                    "</soap:Envelope>";
        final GatewayNotification gatewayNotification = adyenPaymentPluginApi.processNotification(notification, ImmutableList.<PluginProperty>of(), context);
        assertEquals(gatewayNotification.getEntity(), "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><sendNotificationResponse xmlns=\"http://notification.services.adyen.com\" xmlns:ns2=\"http://common.services.adyen.com\"><notificationResponse>[accepted]</notificationResponse></sendNotificationResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>");
    }
}
