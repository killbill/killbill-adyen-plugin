/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.model;

import java.util.Map;

public class FrontendForm {

    private final Map<String, String> formParameter;
    private final String formUrl;

    public FrontendForm(final Map<String, String> formParameter, final String formUrl) {
        this.formParameter = formParameter;
        this.formUrl = formUrl;
    }

    public Map<String, String> getFormParameter() {
        return formParameter;
    }

    public String getFormUrl() {
        return formUrl;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("FrontendForm [formParameter=");
        builder.append(formParameter);
        builder.append(", formUrl=");
        builder.append(formUrl);
        builder.append("]");
        return builder.toString();
    }
}
