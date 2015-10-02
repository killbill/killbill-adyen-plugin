/*
 * Copyright 2014-2015 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.transport.http.HTTPException;
import org.killbill.adyen.payment.DirectDebitRequest;
import org.killbill.adyen.payment.DirectDebitResponse;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentPortType;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.adyen.payment.ServiceException;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctc.wstx.exc.WstxEOFException;
import com.google.common.base.Throwables;

import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.REQUEST_NOT_SEND;
import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.RESPONSE_ABOUT_INVALID_REQUEST;
import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.RESPONSE_INVALID;
import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.RESPONSE_NOT_RECEIVED;
import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.UNKNOWN_FAILURE;

public class AdyenPaymentRequestSender implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger("adyen.sender");

    private final PaymentPortRegistry adyenPaymentPortRegistry;

    public AdyenPaymentRequestSender(final PaymentPortRegistry adyenPaymentPortRegistry) {
        this.adyenPaymentPortRegistry = adyenPaymentPortRegistry;
    }

    @SuppressWarnings("unused")
    public AdyenCallResult<DirectDebitResponse> directdebit(final String countryIsoCode, final DirectDebitRequest request) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, DirectDebitResponse>() {
            @Override
            public DirectDebitResponse apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.directdebit(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> authorise(final String countryIsoCode, final PaymentRequest request) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> authorise3D(final String countryIsoCode, final PaymentRequest3D request) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise3D(request);
            }
        });
    }

    public AdyenCallResult<ModificationResult> refund(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.refund(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> cancel(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.cancel(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> cancelOrRefund(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.cancelOrRefund(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> capture(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.capture(modificationRequest);
            }
        });
    }

    private <T> AdyenCallResult<T> callAdyen(final String countryIsoCode, final AdyenCall<PaymentPortType, T> adyenCall) {
        try {
            final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
            final T result = adyenCall.apply(paymentPort);
            return new SuccessfulAdyenCall<T>(result);
        } catch (final Exception e) {
            logger.info("exception during adyen request", e);
            return mapExceptionToCallResult(e);
        }
    }

    /**
     * Educated guess approach to transform CXF exceptions into error status codes.
     * In the future if we encounter further different cases it makes sense to change this if/else structure to a map with lookup.
     */
    private <T> AdyenCallResult<T> mapExceptionToCallResult(final Exception e) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(e);
        final String errorMessage = rootCause.getMessage();
        if (rootCause instanceof ConnectException) {
            return new UnSuccessfulAdyenCall<T>(REQUEST_NOT_SEND, errorMessage);
        } else if (rootCause instanceof SocketTimeoutException) {
            // read timeout
            if (rootCause.getMessage().contains("Read timed out")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_NOT_RECEIVED, errorMessage);
            } else if (rootCause.getMessage().contains("Unexpected end of file from server")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause.getMessage());
            }
        } else if (rootCause instanceof SocketException) {
            if (rootCause.getMessage().contains("Unexpected end of file from server")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, errorMessage);
            }
        } else if (rootCause instanceof UnknownHostException) {
            return new UnSuccessfulAdyenCall<T>(REQUEST_NOT_SEND, errorMessage);
        } else if (rootCause instanceof HTTPException) {
            // e.g. different response code or strange response
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause.getMessage());
        } else if (rootCause instanceof SOAPFaultException) {
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, errorMessage);
        } else if (rootCause instanceof IOException) {
            if (rootCause.getMessage().contains("Invalid Http response")) {
                // unparsable data as response
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, errorMessage);
            } else if (rootCause.getMessage().contains("Bogus chunk size")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, errorMessage);
            }
        } else if (rootCause instanceof WstxEOFException) {
            // happens for example when 301 with empty body is returned...
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, errorMessage);
        } else if (rootCause instanceof SoapFault) {
            return new UnSuccessfulAdyenCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, errorMessage);
        }

        logger.info("unknown exception will be mapped to UNKNOWN_FAILURE", e);
        return new UnSuccessfulAdyenCall<T>(UNKNOWN_FAILURE, errorMessage);
    }

    @Override
    public void close() throws IOException {
        adyenPaymentPortRegistry.close();
    }

    private interface AdyenCall<T, R> {

        R apply(T t) throws ServiceException;
    }
}
