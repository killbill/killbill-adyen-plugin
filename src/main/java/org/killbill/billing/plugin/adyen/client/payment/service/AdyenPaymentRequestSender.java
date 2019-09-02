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
import org.killbill.adyen.payment.*;
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

    private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentRequestSender.class);

    private final PaymentPortRegistry adyenPaymentPortRegistry;

    public AdyenPaymentRequestSender(final PaymentPortRegistry adyenPaymentPortRegistry) {
        this.adyenPaymentPortRegistry = adyenPaymentPortRegistry;
    }

    public AdyenCallResult<PaymentResult> authorise(final String merchantAccount, final PaymentRequest request) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> authorise3D(final String merchantAccount, final PaymentRequest3D request) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise3D(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> authorise3Ds2(final String merchantAccount, final PaymentRequest3Ds2 request) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise3Ds2(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> refundWithData(final String merchantAccount, final PaymentRequest request) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.refundWithData(request);
            }
        });
    }

    public AdyenCallResult<ModificationResult> refund(final String merchantAccount, final ModificationRequest modificationRequest) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.refund(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> cancel(final String merchantAccount, final ModificationRequest modificationRequest) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.cancel(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> cancelOrRefund(final String merchantAccount, final ModificationRequest modificationRequest) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.cancelOrRefund(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> capture(final String merchantAccount, final ModificationRequest modificationRequest) {
        return callAdyen(merchantAccount, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(final PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.capture(modificationRequest);
            }
        });
    }

    private <T> AdyenCallResult<T> callAdyen(final String merchantAccount, final AdyenCall<PaymentPortType, T> adyenCall) {
        final long startTime = System.currentTimeMillis();
        try {
            final PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(merchantAccount);
            final T result = adyenCall.apply(paymentPort);

            final long duration = System.currentTimeMillis() - startTime;
            return new SuccessfulAdyenCall<T>(result, duration);
        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.warn("Exception during Adyen request", e);

            final UnSuccessfulAdyenCall<T> unsuccessfulResult = mapExceptionToCallResult(e);
            unsuccessfulResult.setDuration(duration);
            return unsuccessfulResult;
        }
    }

    /**
     * Educated guess approach to transform CXF exceptions into error status codes.
     * In the future if we encounter further different cases it makes sense to change this if/else structure to a map with lookup.
     */
    private <T> UnSuccessfulAdyenCall<T> mapExceptionToCallResult(final Exception e) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(e);
        final String errorMessage = rootCause.getMessage();
        if (rootCause instanceof ConnectException) {
            return new UnSuccessfulAdyenCall<T>(REQUEST_NOT_SEND, rootCause);
        } else if (rootCause instanceof SocketTimeoutException) {
            // read timeout
            if (errorMessage.contains("Read timed out")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_NOT_RECEIVED, rootCause);
            } else if (errorMessage.contains("Unexpected end of file from server")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof SocketException) {
            if (errorMessage.contains("Unexpected end of file from server")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof UnknownHostException) {
            return new UnSuccessfulAdyenCall<T>(REQUEST_NOT_SEND, rootCause);
        } else if (rootCause instanceof HTTPException) {
            if (((HTTPException) rootCause).getResponseCode() == 401) {
                return new UnSuccessfulAdyenCall<T>(REQUEST_NOT_SEND, rootCause);
            } else {
                // e.g. different response code or strange response
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof SOAPFaultException) {
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
        } else if (rootCause instanceof IOException) {
            if (errorMessage.contains("Invalid Http response")) {
                // unparsable data as response
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
            } else if (errorMessage.contains("Bogus chunk size")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof WstxEOFException) {
            // happens for example when 301 with empty body is returned...
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause);
        } else if (rootCause instanceof SoapFault) {
            return new UnSuccessfulAdyenCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause);
        }

        return new UnSuccessfulAdyenCall<T>(UNKNOWN_FAILURE, rootCause);
    }

    @Override
    public void close() throws IOException {
        adyenPaymentPortRegistry.close();
    }

    private interface AdyenCall<T, R> {

        R apply(T t) throws ServiceException;
    }
}
