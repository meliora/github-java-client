/*-
 * -\-\-
 * github-api
 * --
 * Copyright (C) 2016 - 2024 Spotify AB
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

package com.spotify.github.v3.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.Response;

import java.util.List;
import java.util.Map;

/**
 * A paginating GitHubPage capable of parsing it's array body from a sub element in json.
 *
 * @param <T> resource type
 *
 * @author Marko Kanala, Meliora Ltd (marko.kanala@meliora.fi)
 */
public class WrappedGithubPage<T> extends GithubPage<T> {
    protected String attribute;

    /**
     * C'tor.
     *
     * @param github github client
     * @param path resource page path
     * @param typeReference type reference for deserialization
     * @param attribute attribute to parse the array body, for example "repositories"
     */
    WrappedGithubPage(final GitHubClient github, final String path, final TypeReference<List<T>> typeReference, final String attribute) {
        super(github, path, typeReference);
        this.attribute = attribute;
    }

    /**
     * C'tor.
     *
     * @param github github client
     * @param path resource page path
     * @param typeReference type reference for deserialization
     * @param extraHeaders extra headers to use in requests
     * @param attribute attribute to parse the array body, for example "repositories"
     */
    WrappedGithubPage(final GitHubClient github, final String path, final TypeReference<List<T>> typeReference, final Map<String, String> extraHeaders, final String attribute) {
        super(github, path, typeReference, extraHeaders);
        this.attribute = attribute;
    }

    @Override
    public String getPageArrayBody(final Response response) {
        String content = super.getPageArrayBody(response);
        if (attribute != null) {
/*
{
  "total_count": 1,
  "repositories": [
    {
    }, ...
  ]
}
 */
            // not mecessarily the most fool-proof impl
            int[] startEndPair = findArrayBody(content, "\"" + attribute + "\":[");
            if (startEndPair == null) {
                startEndPair = findArrayBody(content, "\"" + attribute + "\": [");
            }
            if (startEndPair != null) {
                content = content.substring(startEndPair[0], startEndPair[1] + 1);
            }
        }
        return content;
    }

    protected int[] findArrayBody(final String content, final String find) {
        int index = content.indexOf(find);
        if (index > -1) {
            int start = index + find.length() - 1;
            int end = content.lastIndexOf(']');
            if (end > -1) {
                return new int[] {start, end};
            }
        }
        return null;
    }

    @Override
    public GithubPage<T> create(final GitHubClient github, final String path, final TypeReference<List<T>> typeReference, final Map<String, String> extraHeaders) {
        return new WrappedGithubPage<>(github, path, typeReference, extraHeaders, attribute);
    }
}
