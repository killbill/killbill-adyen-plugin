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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRequestBuilder {

    @Test(groups = "fast")
    public void testAddAdditionalDataByValues() throws Exception {
        final Object key = "key";
        final Object value = "value";
        final TestRequest testRequest = new RequestBuilderTest().addAdditionalData(key, value)
                                                                .build();

        Assert.assertFalse(testRequest.getAdditionalData().isEmpty(), "No AdditionalData added to Request");
        Assert.assertSame(testRequest.getAdditionalData().get(0).getKey(), key, "Wrong Key in AdditionalData in Request");
        Assert.assertSame(testRequest.getAdditionalData().get(0).getValue(), value, "Wrong Value in AdditionalData in Request");
    }

    @Test(groups = "fast")
    public void testAddAdditionalDataByEntity() throws Exception {
        final AnyType2AnyTypeMap.Entry entry = new AnyType2AnyTypeMap.Entry();
        entry.setKey("key");
        entry.setValue("value");
        final TestRequest testRequest = new RequestBuilderTest().addAdditionalData(entry)
                                                                .build();

        Assert.assertFalse(testRequest.getAdditionalData().isEmpty(), "No AdditionalData added to Request");
        Assert.assertEquals(testRequest.getAdditionalData().get(0).getKey(), entry.getKey(), "Wrong Key in AdditionalData in Request");
        Assert.assertEquals(testRequest.getAdditionalData().get(0).getValue(), entry.getValue(), "Wrong Value in AdditionalData in Request");
    }

    @Test(groups = "fast")
    public void testAddAdditionalDataByMap() throws Exception {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        final Object key = "key";
        final Object value = "value";
        map.put(key, value);
        final TestRequest testRequest = new RequestBuilderTest().addAdditionalData(map)
                                                                .build();

        Assert.assertFalse(testRequest.getAdditionalData().isEmpty(), "No AdditionalData added to Request");
        Assert.assertSame(testRequest.getAdditionalData().get(0).getKey(), key, "Wrong Key in AdditionalData in Request");
        Assert.assertSame(testRequest.getAdditionalData().get(0).getValue(), value, "Wrong Value in AdditionalData in Request");
    }

    @Test(groups = "fast")
    public void testAddAdditionalDataByEntityCollection() throws Exception {
        final AnyType2AnyTypeMap.Entry entry = new AnyType2AnyTypeMap.Entry();
        entry.setKey("key");
        entry.setValue("value");
        final TestRequest testRequest = new RequestBuilderTest().addAdditionalData(Collections.singleton(entry))
                                                                .build();

        Assert.assertFalse(testRequest.getAdditionalData().isEmpty(), "No AdditionalData added to Request");
        Assert.assertEquals(testRequest.getAdditionalData().get(0).getKey(), entry.getKey(), "Wrong Key in AdditionalData in Request");
        Assert.assertEquals(testRequest.getAdditionalData().get(0).getValue(), entry.getValue(), "Wrong Value in AdditionalData in Request");
    }

    private static final class RequestBuilderTest extends RequestBuilder<TestRequest> {

        RequestBuilderTest() {
            super(new TestRequest());
        }

        @Override
        protected List<Entry> getAdditionalData() {
            return request.getAdditionalData();
        }
    }

    private static final class TestRequest {

        private final List<AnyType2AnyTypeMap.Entry> additionalData = new ArrayList<Entry>();

        List<AnyType2AnyTypeMap.Entry> getAdditionalData() {
            return additionalData;
        }
    }
}
