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

package org.killbill.billing.plugin.adyen.client.notification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestAdyenNotificationService {

    private static final String AUTHORISATION_NOTIFICATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
                                                             "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                             "              <value xmlns=\"http://common.services.adyen.com\">2995</value>\n" +
                                                             "            </amount>\n" +
                                                             "            <eventCode>AUTHORISATION</eventCode>\n" +
                                                             "            <eventDate>2013-04-15T06:59:22.278+02:00</eventDate>\n" +
                                                             "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                             "            <merchantReference>325147059</merchantReference>\n" +
                                                             "            <operations>\n" +
                                                             "              <string>CANCEL</string>\n" +
                                                             "              <string>CAPTURE</string>\n" +
                                                             "              <string>REFUND</string>\n" +
                                                             "            </operations>\n" +
                                                             "            <originalReference xsi:nil=\"true\"/>\n" +
                                                             "            <paymentMethod>visa</paymentMethod>\n" +
                                                             "            <pspReference>4823660019473428</pspReference>\n" +
                                                             "            <reason>111647:7629:5/2014</reason>\n" +
                                                             "            <success>true</success>\n" +
                                                             "          </NotificationRequestItem>\n" +
                                                             "        </notificationItems>\n" +
                                                             "      </ns1:notification>\n" +
                                                             "    </ns1:sendNotification>\n" +
                                                             "  </soap:Body>\n" +
                                                             "</soap:Envelope>";

    private static final String REFUND_NOTIFICATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                      "  <soap:Body>\n" +
                                                      "    <ns1:sendNotification xmlns:ns1=\"http://notification.services.adyen.com\">\n" +
                                                      "      <ns1:notification>\n" +
                                                      "        <live xmlns=\"http://notification.services.adyen.com\">true</live>\n" +
                                                      "        <notificationItems xmlns=\"http://notification.services.adyen.com\">\n" +
                                                      "          <NotificationRequestItem>\n" +
                                                      "            <additionalData xsi:nil=\"true\"/>\n" +
                                                      "            <amount>\n" +
                                                      "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                      "              <value xmlns=\"http://common.services.adyen.com\">100</value>\n" +
                                                      "            </amount>\n" +
                                                      "            <eventCode>REFUND</eventCode>\n" +
                                                      "            <eventDate>2013-04-15T08:30:12.734+02:00</eventDate>\n" +
                                                      "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                      "            <merchantReference>152919709</merchantReference>\n" +
                                                      "            <operations xsi:nil=\"true\"/>\n" +
                                                      "            <originalReference>1758659397913918</originalReference>\n" +
                                                      "            <paymentMethod>elv</paymentMethod>\n" +
                                                      "            <pspReference>4296660074126515</pspReference>\n" +
                                                      "            <reason xsi:nil=\"true\"/>\n" +
                                                      "            <success>true</success>\n" +
                                                      "          </NotificationRequestItem>\n" +
                                                      "          </notificationItems>\n" +
                                                      "      </ns1:notification>\n" +
                                                      "    </ns1:sendNotification>\n" +
                                                      "  </soap:Body>\n" +
                                                      "</soap:Envelope>";

    private static final String NOTIFICATION_OF_CHARGEBACK_NOTIFICATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                          "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                                          "  <soap:Body>\n" +
                                                                          "    <ns1:sendNotification xmlns:ns1=\"http://notification.services.adyen.com\">\n" +
                                                                          "      <ns1:notification>\n" +
                                                                          "        <live xmlns=\"http://notification.services.adyen.com\">true</live>\n" +
                                                                          "        <notificationItems xmlns=\"http://notification.services.adyen.com\">\n" +
                                                                          "          <NotificationRequestItem>\n" +
                                                                          "            <additionalData xsi:nil=\"true\"/>\n" +
                                                                          "            <amount>\n" +
                                                                          "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                                          "              <value xmlns=\"http://common.services.adyen.com\">304</value>\n" +
                                                                          "            </amount>\n" +
                                                                          "            <eventCode>NOTIFICATION_OF_CHARGEBACK</eventCode>\n" +
                                                                          "            <eventDate>2013-04-15T09:12:02.423+02:00</eventDate>\n" +
                                                                          "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                                          "            <merchantReference>1-6GNEZHGTGT</merchantReference>\n" +
                                                                          "            <operations xsi:nil=\"true\"/>\n" +
                                                                          "            <originalReference>1805647532335033</originalReference>\n" +
                                                                          "            <paymentMethod>directdebit_NL</paymentMethod>\n" +
                                                                          "            <pspReference>4679660098811789</pspReference>\n" +
                                                                          "            <reason>2013.04.12/08.09.20/821</reason>\n" +
                                                                          "            <success>true</success>\n" +
                                                                          "          </NotificationRequestItem>\n" +
                                                                          "          <NotificationRequestItem>\n" +
                                                                          "            <additionalData xsi:nil=\"true\"/>\n" +
                                                                          "            <amount>\n" +
                                                                          "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                                          "              <value xmlns=\"http://common.services.adyen.com\">475</value>\n" +
                                                                          "            </amount>\n" +
                                                                          "            <eventCode>NOTIFICATION_OF_CHARGEBACK</eventCode>\n" +
                                                                          "            <eventDate>2013-04-15T09:12:02.551+02:00</eventDate>\n" +
                                                                          "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                                          "            <merchantReference>1-AYA5VKUZDP</merchantReference>\n" +
                                                                          "            <operations xsi:nil=\"true\"/>\n" +
                                                                          "            <originalReference>1329636226549589</originalReference>\n" +
                                                                          "            <paymentMethod>directdebit_NL</paymentMethod>\n" +
                                                                          "            <pspReference>4894660098821804</pspReference>\n" +
                                                                          "            <reason>2013.04.12/08.18.50/821</reason>\n" +
                                                                          "            <success>true</success>\n" +
                                                                          "          </NotificationRequestItem>\n" +
                                                                          "          <NotificationRequestItem>\n" +
                                                                          "            <additionalData xsi:nil=\"true\"/>\n" +
                                                                          "            <amount>\n" +
                                                                          "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                                          "              <value xmlns=\"http://common.services.adyen.com\">1390</value>\n" +
                                                                          "            </amount>\n" +
                                                                          "            <eventCode>NOTIFICATION_OF_CHARGEBACK</eventCode>\n" +
                                                                          "            <eventDate>2013-04-15T09:12:03.057+02:00</eventDate>\n" +
                                                                          "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                                          "            <merchantReference>1-J6X6UXKG4A</merchantReference>\n" +
                                                                          "            <operations xsi:nil=\"true\"/>\n" +
                                                                          "            <originalReference>4328641331002023</originalReference>\n" +
                                                                          "            <paymentMethod>directdebit_NL</paymentMethod>\n" +
                                                                          "            <pspReference>5299660098821820</pspReference>\n" +
                                                                          "            <reason>2013.04.12/08.26.54/672</reason>\n" +
                                                                          "            <success>true</success>\n" +
                                                                          "          </NotificationRequestItem>\n" +
                                                                          "        </notificationItems>\n" +
                                                                          "      </ns1:notification>\n" +
                                                                          "    </ns1:sendNotification>\n" +
                                                                          "  </soap:Body>\n" +
                                                                          "</soap:Envelope>";

    private static final String CHARGEBACK_REVERSED_NOTIFICATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                   "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                                   "  <soap:Body>\n" +
                                                                   "    <ns1:sendNotification xmlns:ns1=\"http://notification.services.adyen.com\">\n" +
                                                                   "      <ns1:notification>\n" +
                                                                   "        <live xmlns=\"http://notification.services.adyen.com\">true</live>\n" +
                                                                   "        <notificationItems xmlns=\"http://notification.services.adyen.com\">\n" +
                                                                   "          <NotificationRequestItem>\n" +
                                                                   "            <additionalData xsi:nil=\"true\"/>\n" +
                                                                   "            <amount>\n" +
                                                                   "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                                   "              <value xmlns=\"http://common.services.adyen.com\">409</value>\n" +
                                                                   "            </amount>\n" +
                                                                   "            <eventCode>CHARGEBACK_REVERSED</eventCode>\n" +
                                                                   "            <eventDate>2013-01-27T05:11:58.634+01:00</eventDate>\n" +
                                                                   "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                                   "            <merchantReference>1-AWSSVULBWX</merchantReference>\n" +
                                                                   "            <operations xsi:nil=\"true\"/>\n" +
                                                                   "            <originalReference xsi:nil=\"true\"/>\n" +
                                                                   "            <paymentMethod>directdebit_NL</paymentMethod>\n" +
                                                                   "            <pspReference>4815580881292917</pspReference>\n" +
                                                                   "            <reason>Chargeback reversed</reason>\n" +
                                                                   "            <success>true</success>\n" +
                                                                   "          </NotificationRequestItem>\n" +
                                                                   "          </notificationItems>\n" +
                                                                   "      </ns1:notification>\n" +
                                                                   "    </ns1:sendNotification>\n" +
                                                                   "  </soap:Body>\n" +
                                                                   "</soap:Envelope>";

    private static final String REPORT_AVAILABLE_NOTIFICATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                                "  <soap:Body>\n" +
                                                                "    <ns1:sendNotification xmlns:ns1=\"http://notification.services.adyen.com\">\n" +
                                                                "      <ns1:notification>\n" +
                                                                "        <live xmlns=\"http://notification.services.adyen.com\">true</live>\n" +
                                                                "        <notificationItems xmlns=\"http://notification.services.adyen.com\">\n" +
                                                                "          <NotificationRequestItem>\n" +
                                                                "            <additionalData xsi:nil=\"true\"/>\n" +
                                                                "            <amount>\n" +
                                                                "              <currency xmlns=\"http://common.services.adyen.com\">EUR</currency>\n" +
                                                                "              <value xmlns=\"http://common.services.adyen.com\">0</value>\n" +
                                                                "            </amount>\n" +
                                                                "            <eventCode>REPORT_AVAILABLE</eventCode>\n" +
                                                                "            <eventDate>2013-04-15T03:29:21.335+02:00</eventDate>\n" +
                                                                "            <merchantAccountCode>TestMerchant</merchantAccountCode>\n" +
                                                                "            <merchantReference/>\n" +
                                                                "            <operations xsi:nil=\"true\"/>\n" +
                                                                "            <originalReference xsi:nil=\"true\"/>\n" +
                                                                "            <paymentMethod xsi:nil=\"true\"/>\n" +
                                                                "            <pspReference>received_payments_report_2013_04_14.csv</pspReference>\n" +
                                                                "            <reason>https://ca-live.adyen.com/reports/download/MerchantAccount/TestMerchant/received_payments_report_2013_04_14.csv</reason>\n" +
                                                                "            <success>true</success>\n" +
                                                                "          </NotificationRequestItem>\n" +
                                                                "        </notificationItems>\n" +
                                                                "      </ns1:notification>\n" +
                                                                "    </ns1:sendNotification>\n" +
                                                                "  </soap:Body>\n" +
                                                                "</soap:Envelope>";

    private AdyenNotificationHandlerTest handler;
    private AdyenNotificationService notificationService;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        handler = new AdyenNotificationHandlerTest();
        final ImmutableList<AdyenNotificationHandler> handlers = ImmutableList.<AdyenNotificationHandler>of(handler);
        notificationService = new AdyenNotificationService(handlers);
    }

    @Test(groups = "fast")
    public void testHandleAuthorizationNotification() throws Exception {
        handleAndVerifyHandler(AUTHORISATION_NOTIFICATION,
                               ImmutableMap.<Short, BigDecimal>of((short) 0, new BigDecimal("29.95")),
                               ImmutableMap.<Short, Currency>of((short) 0, Currency.EUR));
    }

    @Test(groups = "fast")
    public void testHandleRefundNotification() throws Exception {
        handleAndVerifyHandler(REFUND_NOTIFICATION,
                               ImmutableMap.<Short, BigDecimal>of((short) 0, new BigDecimal("1.00")),
                               ImmutableMap.<Short, Currency>of((short) 0, Currency.EUR));
    }

    @Test(groups = "fast")
    public void testHandleChargebackNotification() throws Exception {
        handleAndVerifyHandler(NOTIFICATION_OF_CHARGEBACK_NOTIFICATION,
                               ImmutableMap.<Short, BigDecimal>of((short) 0, new BigDecimal("3.04"),
                                                                  (short) 1, new BigDecimal("4.75"),
                                                                  (short) 2, new BigDecimal("13.90")),
                               ImmutableMap.<Short, Currency>of((short) 0, Currency.EUR,
                                                                (short) 1, Currency.EUR,
                                                                (short) 2, Currency.EUR));
    }

    @Test(groups = "fast")
    public void testHandleChargebackReversedNotification() throws Exception {
        handleAndVerifyHandler(CHARGEBACK_REVERSED_NOTIFICATION,
                               ImmutableMap.<Short, BigDecimal>of((short) 0, new BigDecimal("4.09")),
                               ImmutableMap.<Short, Currency>of((short) 0, Currency.EUR));
    }

    @Test(groups = "fast")
    public void testHandleReportAvailableNotification() throws Exception {
        handleAndVerifyHandler(REPORT_AVAILABLE_NOTIFICATION,
                               ImmutableMap.<Short, BigDecimal>of((short) 0, BigDecimal.ZERO),
                               ImmutableMap.<Short, Currency>of((short) 0, Currency.EUR));
    }

    private void handleAndVerifyHandler(final String notification, final Map<Short, BigDecimal> amounts, final Map<Short, Currency> currencies) {
        final String response = notificationService.handleNotifications(notification);
        Assert.assertEquals(response, "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><sendNotificationResponse xmlns=\"http://notification.services.adyen.com\" xmlns:ns2=\"http://common.services.adyen.com\"><notificationResponse>[accepted]</notificationResponse></sendNotificationResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>");

        Assert.assertEquals(handler.getItems().size(), NOTIFICATION_OF_CHARGEBACK_NOTIFICATION.equals(notification) ? 3 : 1);

        final List<NotificationRequestItem> items = handler.getItems();
        for (short i = 0; i < items.size(); i++) {
            final NotificationRequestItem notificationRequestItem = items.get(i);
            final NotificationItem notificationItem = new NotificationItem(notificationRequestItem);
            Assert.assertEquals(notificationItem.getAmount().compareTo(amounts.get(i)), 0);
            Assert.assertEquals(notificationItem.getCurrency(), currencies.get(i).name());
        }
    }
}
