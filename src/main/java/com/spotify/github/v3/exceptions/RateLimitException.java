/*-
 * -\-\-
 * github-api
 * --
 * Copyright (C) 2016 - 2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.github.v3.exceptions;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Thrown if REST API rate limits exceed.
 *
 * @author Marko Kanala, Meliora Ltd (marko.kanala@meliora.fi)
 */
public class RateLimitException extends RequestNotOkException {
    private static final long serialVersionUID = 1L;

    protected Date rateLimitReset;

    public RateLimitException(final String method, final String path, final int statusCode, final String msg, final Map<String, List<String>> headers, final Date rateLimitReset) {
        super(method, path, statusCode, msg, headers);
        this.rateLimitReset = rateLimitReset;
    }

    /**
     * @return The time at which the current rate limit window resets
     */
    public Date getRateLimitReset() {
        return rateLimitReset;
    }
}
