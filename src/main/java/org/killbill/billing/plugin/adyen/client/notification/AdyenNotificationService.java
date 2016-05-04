/*
 * Copyright 2014-2016 Groupon, Inc
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMSource;

import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.adyen.notification.ObjectFactory;
import org.killbill.adyen.notification.SendNotification;
import org.killbill.adyen.notification.SendNotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;

public class AdyenNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AdyenNotificationService.class);

    private final List<AdyenNotificationHandler> notificationHandlers;
    private final JAXBContext jaxbContext;

    public AdyenNotificationService(final List<AdyenNotificationHandler> notificationHandlers) throws JAXBException {
        this.notificationHandlers = notificationHandlers;
        this.jaxbContext = JAXBContext.newInstance(SendNotification.class, SendNotificationResponse.class, ObjectFactory.class);
    }

    public String handleNotifications(final String input) {
        final ByteArrayOutputStream outputStream = handleNotifications(new ByteArrayInputStream(input.getBytes(Charsets.UTF_8)));
        return outputStream.toString();
    }

    private ByteArrayOutputStream handleNotifications(final InputStream inputStream) {
        String response = "error";

        SendNotification sendNotification = null;
        try {
            sendNotification = parse(inputStream);
        } catch (final Exception e) {
            logger.warn("Error parsing Adyen notification", e);
        }

        if (sendNotification != null) {
            final List<NotificationRequestItem> listOfNotifications = sendNotification.getNotification()
                                                                                      .getNotificationItems()
                                                                                      .getNotificationRequestItem();

            try {
                for (final NotificationRequestItem item : listOfNotifications) {
                    handleNotification(item);
                }
                response = "[accepted]";
            } catch (final Exception e) {
                logger.warn("Error handling Adyen notification {}", sendNotification, e);
            }
        }

        try {
            return createSendNotificationResponse(response);
        } catch (final Exception e) {
            // Not much we can do. Let Adyen retry.
            throw new RuntimeException(e);
        }
    }

    private void handleNotification(final NotificationRequestItem item) {
        logger.info("Handling notification: eventCode='{}', pspReference='{}', originalReference='{}', success='{}', reason='{}', merchantReference='{}'",
                    item.getEventCode(),
                    item.getPspReference(),
                    item.getOriginalReference(),
                    item.isSuccess(),
                    item.getReason(),
                    item.getMerchantReference());

        final AdyenNotificationHandler adyenNotificationHandler = getAdyenNotificationHandler(item);
        if (adyenNotificationHandler == null) {
            logger.warn("No handler available - ignoring");
            return;
        }

        adyenNotificationHandler.handleNotification(item);
    }

    private AdyenNotificationHandler getAdyenNotificationHandler(final NotificationRequestItem notificationRequestItem) {
        for (final AdyenNotificationHandler handler : notificationHandlers) {
            if (handler.canHandleNotification(notificationRequestItem)) {
                return handler;
            }
        }
        return null;
    }

    private SendNotification parse(final InputStream inputStream) throws ParserConfigurationException, IOException, SAXException, JAXBException {
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        final DocumentBuilder builder = builderFactory.newDocumentBuilder();
        final Document document = builder.parse(inputStream);
        final Node sendNotificationNode = document.getElementsByTagNameNS("http://notification.services.adyen.com", "sendNotification").item(0);

        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        // Use SOAPMessage to handle the envelope
        return (SendNotification) unmarshaller.unmarshal(new DOMSource(sendNotificationNode));
    }

    private ByteArrayOutputStream createSendNotificationResponse(final String value) throws JAXBException, SOAPException, ParserConfigurationException, IOException {
        final SendNotificationResponse response = new SendNotificationResponse();
        response.setNotificationResponse(value);

        // Use SOAPMessage to add the envelope
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(response, document);

        final SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
        soapMessage.getSOAPBody().addDocument(document);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        soapMessage.writeTo(outputStream);

        return outputStream;
    }
}
