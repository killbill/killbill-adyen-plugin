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

package org.killbill.billing.plugin.adyen.client.model;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

public class HppCompletedResult {

    private final String pspReference;
    private final PaymentServiceProviderResult result;
    private final String reason;

    public HppCompletedResult(final String pspReference,
                              final PaymentServiceProviderResult result,
                              @Nullable final String reason) {
        this.pspReference = pspReference;
        this.result = Preconditions.checkNotNull(result);
        this.reason = reason;
    }

    public String getPspReference() {
        return pspReference;
    }

    public PaymentServiceProviderResult getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HppCompletedResult{");
        sb.append("pspReference='").append(pspReference).append('\'');
        sb.append(", result=").append(result);
        sb.append(", reason='").append(reason).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HppCompletedResult that = (HppCompletedResult) o;

        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) {
            return false;
        }
        if (result != that.result) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = pspReference != null ? pspReference.hashCode() : 0;
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (reason != null ? reason.hashCode() : 0);
        return result1;
    }
}
