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

package org.killbill.billing.plugin.adyen.client;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.killbill.adyen.payment.Payment;
import org.killbill.adyen.payment.PaymentPortType;
import org.osgi.service.log.LogService;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.killbill.billing.plugin.adyen.client.AdyenConfigProperties.gbToUK;


public class AdyenPaymentPortRegistry implements PaymentPortRegistry {

    private final AdyenConfigProperties config;
    private final Map<String, Object> services;
    private final LogService logService;

    private final static String PAYMENT_SERVICE_SUFFIX = "-paymentService";


    public AdyenPaymentPortRegistry(AdyenConfigProperties config, LogService logService)
            throws Exception {
        this.logService = logService;
        this.config = checkNotNull(config, "config");
        this.services = new ConcurrentHashMap<String, Object>();
    }


    @Override
    public PaymentPortType getPaymentPort(final String countryIsoCode) {

        final String countryCode = gbToUK(countryIsoCode);

        if (!this.services.containsKey(checkNotNull(countryCode) + PAYMENT_SERVICE_SUFFIX)) {
            Object service = createService(Payment.SERVICE, Payment.PaymentHttpPort, PaymentPortType.class, config.getPaymentUrl(),
                    config.getUserName(countryCode), config.getPassword(countryCode));
            this.services.put(countryCode + PAYMENT_SERVICE_SUFFIX, service);
        }
        return (PaymentPortType) this.services.get(countryCode + PAYMENT_SERVICE_SUFFIX);
    }


    private Object createService(final QName service,
                                 final QName portName,
                                 final Class<?> serviceEndpointInterfaceClass,
                                 final String address,
                                 final String userName,
                                 final String password) {

        return createService(service, portName, serviceEndpointInterfaceClass, address, userName, password, null);
    }

    private Object createService(final QName service,
                                 final QName portName,
                                 final Class<?> serviceEndpointInterfaceClass,
                                 final String address,
                                 final String userName,
                                 final String password,
                                 final String timeout) {
        checkNotNull(service, "service");
        checkNotNull(portName, "portName");
        checkNotNull(serviceEndpointInterfaceClass, "serviceEndpointInterfaceClass");
        checkNotNull(address, "address");
        checkNotNull(userName, "userName");
        checkNotNull(password, "password");
        // we don't use an wsdl as we don't want the hassle with retrieving it online(that may change, be unavailable)
        // or from the right location in all our different deployments

        logService.log(LogService.LOG_INFO, "Initializing Adyen gateway client..." +
                ", service = " + service +
                ", address = " + address +
                ", portName = " + portName +
                ", userName = " + userName
                /* + ", password = " + password */);

        //URL uri = this.getClass().getClassLoader().getResource("cxf/Payment.wsdl");
        //Service result = Service.create(uri, service);
        Service result = Service.create(null, service);

        // configure the endPointAddress
        result.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);
        // configure username/pw
        Object port = result.getPort(portName, serviceEndpointInterfaceClass);
        Client client = ClientProxy.getClient(port);
        client.getEndpoint().put("jaxb-validation-event-handler", new IgnoreUnexpectedElementsEventHandler());
        if (timeout != null) {
            HTTPConduit conduit = (HTTPConduit) client.getConduit();
            conduit.getClient().setReceiveTimeout(Long.valueOf(timeout));
        }
        ((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, userName);
        ((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
        // configure logging interceptors
        // it seems this can be done only using cxf
        Endpoint endpoint = client.getEndpoint();
        endpoint.getInInterceptors().add(new ObfuscatingLoggingInInterceptor());
        endpoint.getOutInterceptors().add(new ObfuscatingLoggingOutInterceptor());
        logService.log(LogService.LOG_INFO, "Done initializing Adyen gateway client...");
        return port;
    }
}
