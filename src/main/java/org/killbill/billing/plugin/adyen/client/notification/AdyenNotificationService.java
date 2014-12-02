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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.adyen.notification.ObjectFactory;
import org.killbill.adyen.notification.SendNotification;
import org.killbill.adyen.notification.SendNotificationResponse;
import org.killbill.billing.plugin.adyen.client.AdyenEventCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;

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

    public ByteArrayOutputStream handleNotifications(final InputStream inputStream) {
        String response;
        try {
            final SendNotification sendNotification = parse(inputStream);
            final List<NotificationRequestItem> listOfNotifications = sendNotification.getNotification()
                                                                                      .getNotificationItems()
                                                                                      .getNotificationRequestItem();

            for (final NotificationRequestItem item : listOfNotifications) {
                handleNotification(item);
            }

            response = "[accepted]";
        } catch (final Exception e) {
            logger.error("Error dealing with Adyen notification", e);
            /**
             * The Adyen server only knows the return string [accepted]. Therefore
             * this method cannot return any other string to signal an error.
             */
            response = "error";
        }

        try {
            return createSendNotificationResponse(response);
        } catch (final Exception e) {
            // Not much we can do. Let Adyen retry.
            throw new RuntimeException(e);
        }
    }

    private void handleNotification(final NotificationRequestItem item) {
        logNotification(item);

        final AdyenEventCode eventCode = AdyenEventCode.fromString(item.getEventCode());
        final AdyenNotificationHandler adyenNotificationHandler = getAdyenNotificationHandler(item);
        if (adyenNotificationHandler == null) {
            logger.warn("No handler available - ignoring");
            return;
        }

        switch (eventCode) {
            case AUTHORISATION:
                if (item.isSuccess()) {
                    adyenNotificationHandler.authorisationSuccess(item);
                } else {
                    adyenNotificationHandler.authorisationFailure(item);
                }
                break;
            case CAPTURE:
                if (item.isSuccess()) {
                    adyenNotificationHandler.captureSuccess(item);
                } else {
                    adyenNotificationHandler.captureFailure(item);
                }
                break;
            case CAPTURE_FAILED:
                adyenNotificationHandler.captureFailed(item);
                break;
            case CANCELLATION:
                if (item.isSuccess()) {
                    adyenNotificationHandler.cancellationSuccess(item);
                } else {
                    adyenNotificationHandler.cancellationFailure(item);
                }
                break;
            case CHARGEBACK:
                adyenNotificationHandler.chargeback(item);
                break;
            case CHARGEBACK_REVERSED:
                adyenNotificationHandler.chargebackReversed(item);
                break;
            case DISPUTE:
                adyenNotificationHandler.dispute(item);
                break;
            case CANCEL_OR_REFUND:
                if (item.isSuccess()) {
                    adyenNotificationHandler.cancelOrRefundSuccess(item);
                } else {
                    adyenNotificationHandler.cancelOrRefundFailure(item);
                }
                break;
            case REFUND:
                if (item.isSuccess()) {
                    adyenNotificationHandler.refundSuccess(item);
                } else {
                    adyenNotificationHandler.refundFailure(item);
                }
                break;
            case REFUNDED_REVERSED:
                adyenNotificationHandler.refundedReversed(item);
                break;
            case REFUND_FAILED:
                adyenNotificationHandler.refundFailed(item);
                break;
            case NOTIFICATION_OF_FRAUD:
                adyenNotificationHandler.notificationOfFraud(item);
                break;
            case REQUEST_FOR_INFORMATION:
                adyenNotificationHandler.requestForInformation(item);
                break;
            case NOTIFICATION_OF_CHARGEBACK:
                adyenNotificationHandler.notificationOfChargeback(item);
                break;
            case REPORT_AVAILABLE:
                adyenNotificationHandler.reportAvailable(item);
                break;
            case NOTIFICATIONTEST:
                adyenNotificationHandler.notificationtest(item);
                break;
            case RECURRING_RECEIVED:
                adyenNotificationHandler.recurringReceived(item);
                break;
            case CANCEL_RECEIVED:
                adyenNotificationHandler.cancelReceived(item);
                break;
            case RECURRING_DETAIL_DISABLED:
                adyenNotificationHandler.recurringDetailDisabled(item);
                break;
            case RECURRING_FOR_USER_DISABLED:
                adyenNotificationHandler.recurringForUserDisabled(item);
                break;
            default:
                logger.warn("Could not handle notification: Event code " + eventCode + " not implemented yet: " + item);
                break;
        }
    }

    private void logNotification(final NotificationRequestItem item) {
        final String itemAsString = Objects.toStringHelper(item)
                                           .add("eventCode", item.getEventCode())
                                           .add("pspReference", item.getPspReference())
                                           .add("originalReference", item.getOriginalReference())
                                           .add("success", item.isSuccess())
                                           .add("reason", item.getReason())
                                           .add("merchantReference", item.getMerchantReference())
                                           .toString();
        logger.info("Handling notification: " + itemAsString);
    }

    private AdyenNotificationHandler getAdyenNotificationHandler(final NotificationRequestItem notificationRequestItem) {
        for (final AdyenNotificationHandler handler : notificationHandlers) {
            if (handler.canHandleNotification(notificationRequestItem)) {
                return handler;
            }
        }
        return null;
    }

    private SendNotification parse(final InputStream inputStream) throws JAXBException, SOAPException, IOException {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        // Use SOAPMessage to handle the envelope
        final SOAPMessage message = MessageFactory.newInstance().createMessage(null, inputStream);
        return (SendNotification) unmarshaller.unmarshal(message.getSOAPBody().extractContentAsDocument());
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
