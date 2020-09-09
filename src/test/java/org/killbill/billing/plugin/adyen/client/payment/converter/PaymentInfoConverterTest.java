/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.converter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import static org.testng.Assert.*;

public class PaymentInfoConverterTest {

    @DataProvider
    public static Object[][] paymentInfoProvider() {
        // Testing only address values, by now
        // Map initialization in Java is even more verbose...
        return new String[][]{
                {"street:address1", "houseNumberOrName:address2", "city:city", "postalCode:00000", "stateOrProvince:SOP", "country:CO", "Y"}, // All fields present
                {"country:CO", "Y"}, // Only Country

                {"street:address1", "houseNumberOrName:address2", "city:city", "postalCode:00000", "stateOrProvince:SOP", "N"}, // Missing Country

                {"houseNumberOrName:address2",                    "city:city", "postalCode:00000", "stateOrProvince:SOP", "country:CO", "N"}, // Missing address field
                {"street:address1",                               "city:city", "postalCode:00000", "stateOrProvince:SOP", "country:CO", "N"},
                {"street:address1", "houseNumberOrName:address2",              "postalCode:00000", "stateOrProvince:SOP", "country:CO", "N"},
                {"street:address1", "houseNumberOrName:address2", "city:city",                     "stateOrProvince:SOP", "country:CO", "N"},

                {"street:address1", "houseNumberOrName:address2", "city:city", "postalCode:00000", "country:CO", "Y"}, // No State for Country other than US/CA
                {"street:address1", "houseNumberOrName:address2", "city:city", "postalCode:00000", "country:US", "N"}, // Missing State for US/CA
                {"street:address1", "houseNumberOrName:address2", "city:city", "postalCode:00000", "country:CA", "N"},

                // TODO? New test cases if/when data validation is introduced
                };
    }

    @Test(dataProvider = "paymentInfoProvider")
    public void testConvertPaymentInfoToPaymentRequest(final String[] paramsValue)
            throws NoSuchFieldException, IllegalAccessException {

        final Card pi = buildPaymentInfo(paramsValue);
        final boolean expectedAddrPresent = "Y".equals(paramsValue[paramsValue.length - 1]);

        final PaymentRequest pr = new PaymentInfoConverter<Card>().convertPaymentInfoToPaymentRequest(pi);
        final boolean addrPresent = pr.getBillingAddress() != null;

        assertEquals(addrPresent, expectedAddrPresent);

    }

    private static Card buildPaymentInfo(final String[] values)
            throws IllegalAccessException, NoSuchFieldException {

        final Card pi = new Card();

        for (int i = 0; i < values.length - 1; i++) {
            final String[] pair = values[i].split(":", 2);

            final Field field = PaymentInfo.class.getDeclaredField(pair[0]);
            field.setAccessible(true);
            field.set(pi, "null".equals(pair[1]) ? null : pair[1]);
        }

        return pi;
    }
}
