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

package org.killbill.billing.plugin.adyen.client;

import com.google.common.base.Preconditions;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.logging.Slf4jLogger;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.killbill.adyen.payment.Payment;
import org.killbill.adyen.payment.PaymentPortType;
import org.killbill.billing.plugin.adyen.client.jaxws.HttpHeaderInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.IgnoreUnexpectedElementsEventHandler;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingInInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingOutInterceptor;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdyenPaymentPortRegistry implements PaymentPortRegistry {

    private final static String PAYMENT_SERVICE_SUFFIX = "-paymentService";

    private final AdyenConfigProperties config;
    private final LoggingOutInterceptor loggingOutInterceptor;
    private final LoggingInInterceptor loggingInInterceptor;
    private final HttpHeaderInterceptor httpHeaderInterceptor;
    private final Map<String, PaymentPortType> services;

    public AdyenPaymentPortRegistry(final AdyenConfigProperties config,
                                    final LoggingInInterceptor loggingInInterceptor,
                                    final LoggingOutInterceptor loggingOutInterceptor,
                                    final HttpHeaderInterceptor httpHeaderInterceptor) {
        this.loggingInInterceptor = loggingInInterceptor;
        this.loggingOutInterceptor = loggingOutInterceptor;
        this.config = Preconditions.checkNotNull(config, "config");
        this.services = new ConcurrentHashMap<String, PaymentPortType>();
        this.httpHeaderInterceptor = httpHeaderInterceptor;
    }

    @Override
    public void close() throws IOException {
        for (final PaymentPortType service : services.values()) {
            try {
                // See ClientProxy.getClient
                ((ClientProxy) Proxy.getInvocationHandler(service)).close();
            } catch (final RuntimeException ignored) {
            }
        }
    }

    @Override
    public PaymentPortType getPaymentPort(final String countryIsoCode) {
        final String countryCode = AdyenConfigProperties.gbToUK(countryIsoCode);

        if (!this.services.containsKey(countryCode + PAYMENT_SERVICE_SUFFIX)) {
            final PaymentPortType service = createService(Payment.SERVICE,
                    Payment.PaymentHttpPort,
                    config.getPaymentUrl(),
                    config.getUserName(countryCode),
                    config.getPassword(countryCode),
                    config.getPaymentConnectionTimeout(),
                    config.getPaymentReadTimeout());
            this.services.put(countryCode + PAYMENT_SERVICE_SUFFIX, service);
        }
        return this.services.get(countryCode + PAYMENT_SERVICE_SUFFIX);
    }

    private PaymentPortType createService(final QName service,
                                          final QName portName,
                                          final String address,
                                          final String userName,
                                          final String password,
                                          final String connectionTimeout,
                                          final String readTimeout) {
        Preconditions.checkNotNull(service, "service");
        Preconditions.checkNotNull(portName, "portName");
        Preconditions.checkNotNull(address, "address");
        Preconditions.checkNotNull(userName, "username");
        Preconditions.checkNotNull(password, "password");

        // Delegate logging to slf4j (see also https://github.com/killbill/killbill-platform/tree/master/osgi-bundles/libs/slf4j-osgi)
        LogUtils.setLoggerClass(Slf4jLogger.class);

        final Service result = Service.create(null, service);
        result.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);
        final PaymentPortType port = result.getPort(portName, PaymentPortType.class);
        final Client client = ClientProxy.getClient(port);
        client.getEndpoint().put("jaxb-validation-event-handler", new IgnoreUnexpectedElementsEventHandler());
        final HTTPConduit conduit = (HTTPConduit) client.getConduit();
        conduit.getClient().setAllowChunking(config.getAllowChunking());
        if (connectionTimeout != null) {
            conduit.getClient().setConnectionTimeout(Long.valueOf(connectionTimeout));
        }
        if (readTimeout != null) {
            conduit.getClient().setReceiveTimeout(Long.valueOf(readTimeout));
        }
        ((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, userName);
        ((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

        final Endpoint endpoint = client.getEndpoint();
        endpoint.getInInterceptors().add(loggingInInterceptor);
        endpoint.getOutInterceptors().add(loggingOutInterceptor);
        endpoint.getOutInterceptors().add(httpHeaderInterceptor);

        return port;
    }
}
