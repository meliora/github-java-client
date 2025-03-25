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

package com.spotify.github.graphql;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.spotify.github.GithubStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Basic GraphQL response.
 *
 * @author Marko Kanala, Meliora Ltd (marko.kanala@meliora.fi)
 */
@Value.Immutable
@GithubStyle
@JsonSerialize(as = ImmutableGraphQLResponse.class)
@JsonDeserialize(as = ImmutableGraphQLResponse.class)
public interface GraphQLResponse {
    // {"data":{"repository":{"pullRequest":{"id":"PR_kwDOOGLqn86QCX0t","closingIssuesReferences":{"edges":[{"node":{"id":"I_kwDOOGLqn86vnkny","body":"feature description","number":2,"title":"feature xyz"}}]}}}}}
    // {"data":{"repository":null},"errors":[{"type":"NOT_FOUND","path":["repository"],"locations":[{"line":1,"column":9}],"message":"Could not resolve to a Repository with the name 'meliora/test-project'."}]}
    // {"message":"Problems parsing JSON","documentation_url":"https://docs.github.com/graphql","status":"400"}

    @Nullable
    Map<String, Object> data();

    @Nullable
    String message();

    @Nullable
    String status();

    /**
     * Return content from the response with a dot notation path.
     *
     * @param path dot notation path such as repository.pullRequest.closingIssuesReferences.edges[0].node.number
     * @return Object
     */
    default Object getData(String path) {
        String[] parts = path.split("\\.");
        return getDataRecurse(data(), parts, 0);
    }

    private Object getDataRecurse(Object current, String[] parts, int index) {
        if (index >= parts.length || current == null) {
            return current;
        }

        String part = parts[index];
        if (part.contains("[")) {
            String key = part.substring(0, part.indexOf('['));
            int listIndex = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            }
            if (current instanceof List) {
                current = ((List<?>) current).get(listIndex);
            }
        } else {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            }
        }

        return getDataRecurse(current, parts, index + 1);
    }

}
