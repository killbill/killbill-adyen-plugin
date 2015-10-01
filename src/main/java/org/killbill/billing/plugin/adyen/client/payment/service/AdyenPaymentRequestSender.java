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

import com.ctc.wstx.exc.WstxEOFException;
import com.google.common.base.Throwables;
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

import javax.xml.ws.soap.SOAPFaultException;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.*;

public class AdyenPaymentRequestSender implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger("adyen.sender");

    private final PaymentPortRegistry adyenPaymentPortRegistry;

    public AdyenPaymentRequestSender(final PaymentPortRegistry adyenPaymentPortRegistry) {
        this.adyenPaymentPortRegistry = adyenPaymentPortRegistry;
    }

    @SuppressWarnings("unused")
    public AdyenCallResult<DirectDebitResponse> directdebit(final String countryIsoCode, final DirectDebitRequest request) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, DirectDebitResponse>() {
            @Override
            public DirectDebitResponse apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.directdebit(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> authorise(final String countryIsoCode, final PaymentRequest request) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise(request);
            }
        });
    }

    public AdyenCallResult<PaymentResult> authorise3D(final String countryIsoCode, final PaymentRequest3D request) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, PaymentResult>() {
            @Override
            public PaymentResult apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.authorise3D(request);
            }
        });
    }

    public AdyenCallResult<ModificationResult> refund(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.refund(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> cancel(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.cancel(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> cancelOrRefund(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.cancelOrRefund(modificationRequest);
            }
        });
    }

    public AdyenCallResult<ModificationResult> capture(final String countryIsoCode, final ModificationRequest modificationRequest) {
        return callAdyen(countryIsoCode, new AdyenCall<PaymentPortType, ModificationResult>() {
            @Override
            public ModificationResult apply(PaymentPortType paymentPort) throws ServiceException {
                return paymentPort.capture(modificationRequest);
            }
        });
    }

    private <T> AdyenCallResult<T> callAdyen(String countryIsoCode, AdyenCall<PaymentPortType, T> adyenCall) {
        try {
            PaymentPortType paymentPort = adyenPaymentPortRegistry.getPaymentPort(countryIsoCode);
            T result = adyenCall.apply(paymentPort);
            return new SuccessfulAdyenCall<T>(result);
        } catch (Exception e) {
            logger.info("exception during adyen reqeust", e);
            return mapExceptionToCallResult(e);
        }
    }

    /**
     * Educated guess approach to transform CXF exceptions into error status codes.
     * In the future if we encounter further different cases it makes sense to change this if/else structure to a map with lookups.
     */
    private <T> AdyenCallResult<T> mapExceptionToCallResult(Exception e) {
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable rootCause = Throwables.getRootCause(e);
        if (rootCause instanceof SocketTimeoutException) {
            // read timeout
            if (rootCause.getMessage().contains("Read timed out")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_NOT_RECEIVED, e.getMessage());
            } else if (rootCause.getMessage().contains("Unexpected end of file from server")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause.getMessage());
            }
        } else if (rootCause instanceof SocketException) {
            if (rootCause.getMessage().contains("Unexpected end of file from server")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, e.getMessage());
            }
        } else if (rootCause instanceof HTTPException) {
            // connection timeout
            if (((HTTPException) rootCause).getResponseCode() == 503) {
                return new UnSuccessfulAdyenCall<T>(REQUEST_NOT_SEND, rootCause.getMessage());
            }
            // e.g. different response code or strange response
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, rootCause.getMessage());
        } else if (rootCause instanceof SOAPFaultException) {
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, e.getMessage());
        } else if (rootCause instanceof IOException) {
            if (rootCause.getMessage().contains("Invalid Http response")) {
                // unparsable data as response
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, e.getMessage());
            } else if (rootCause.getMessage().contains("Bogus chunk size")) {
                return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, e.getMessage());
            }
        } else if (rootCause instanceof WstxEOFException) {
            // happens for example when 301 with empty body is returned...
            return new UnSuccessfulAdyenCall<T>(RESPONSE_INVALID, e.getMessage());
        } else if (rootCause instanceof SoapFault) {
            return new UnSuccessfulAdyenCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, e.getMessage());
        }

        logger.info("unknown exception will be mapped to UNKNOWN_FAILURE", e);
        return new UnSuccessfulAdyenCall<T>(UNKNOWN_FAILURE, e.getMessage());
    }

    @Override
    public void close() throws IOException {
        adyenPaymentPortRegistry.close();
    }


    private interface AdyenCall<T, R> {
        R apply(T t) throws ServiceException;
    }
}
