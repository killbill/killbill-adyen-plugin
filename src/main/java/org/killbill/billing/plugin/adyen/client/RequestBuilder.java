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

import org.killbill.adyen.payment.AnyType2AnyTypeMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class RequestBuilder<R> {
    protected R request;

    protected RequestBuilder(final R request) {
        this.request = request;
    }

    public R build() {
        return request;
    }

    protected RequestBuilder() {
        super();
    }

    public RequestBuilder<R> addAdditionalData(final Object key, final Object value) {
        return addAdditionalData(createAdditionalDataEntry(key, value));
    }

    public RequestBuilder<R> addAdditionalData(final AnyType2AnyTypeMap.Entry entry) {
        getAdditionalData().add(entry);
        return this;
    }

    public RequestBuilder<R> addAdditionalData(final Map<Object, Object> entries) {
        final List<AnyType2AnyTypeMap.Entry> result = new ArrayList<AnyType2AnyTypeMap.Entry>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.add(createAdditionalDataEntry(entry.getKey(), entry.getValue()));
        }
        return addAdditionalData(result);
    }

    public RequestBuilder<R> addAdditionalData(final Collection<AnyType2AnyTypeMap.Entry> entries) {
        getAdditionalData().addAll(entries);
        return this;
    }

    protected abstract List<AnyType2AnyTypeMap.Entry> getAdditionalData();

    protected AnyType2AnyTypeMap.Entry createAdditionalDataEntry(final Object key, final Object value) {
        final AnyType2AnyTypeMap.Entry entry = new AnyType2AnyTypeMap.Entry();
        entry.setKey(key);
        entry.setValue(value);
        return entry;
    }

}
