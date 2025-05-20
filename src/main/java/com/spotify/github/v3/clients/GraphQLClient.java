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

package com.spotify.github.v3.clients;

import com.spotify.github.graphql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

/**
 * Simple client to execute queries against a Repository graph.
 *
 * @author Marko Kanala, Meliora Ltd (marko.kanala@meliora.fi)
 */
public class GraphQLClient {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GitHubClient github;
    private final String owner;
    private final String repo;

    GraphQLClient(final GitHubClient github, final String owner, final String repo) {
        this.github = github;
        this.owner = owner;
        this.repo = repo;
    }

    static GraphQLClient create(final GitHubClient github, final String owner, final String repo) {
        return new GraphQLClient(github, owner, repo);
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

// Not supported in GitHub's API as of 20250520
//
//    /**
//     * Executes a simple graphql query with HTTP GET method (and it's caveats).
//     *
//     * @param q query (will be encapsulated to a repository block), short enough to fit into query string
//     * @return response
//     */
//    public CompletableFuture<GraphQLResponse> executeSimpleRepositoryQuery(final String q) {
//        String query = parseRepositoryQuery(q, null, false);
//        log.debug("executeSimpleRepositoryQuery: {}", query);
//        return github.getGraphql(query, GraphQLResponse.class);
//    }

    /**
     * Executes a simple graphql repository query.
     *
     * @param q query (will be encapsulated to a repository block)
     * @return response
     */
    public CompletableFuture<GraphQLResponse> executeRepositoryQuery(final String q) {
        return executeRepositoryQuery(q, null);
    }

    /**
     * Executes a variabled graphql repository query.
     *
     * @param q query (will be encapsulated to a repository block)
     * @param variables variables to include
     * @return response
     */
    public CompletableFuture<GraphQLResponse> executeRepositoryQuery(final String q, final Map<String, Object> variables) {
        String query = parseRepositoryQuery(q, variables);
        final String requestBody = github.json().toJsonUnchecked(ImmutableGraphQLQuery.builder().query(query).variables(variables).build());
        log.debug("executeRepositoryQuery: {}", requestBody);
        return github.postGraphql(requestBody, GraphQLResponse.class);
    }

    protected String parseRepositoryQuery(final String q, final Map<String, Object> variables) {
        return parseRepositoryQuery(q, variables, true);
    }

    protected String parseRepositoryQuery(final String q, final Map<String, Object> variables, final boolean wrapToQueryBlock) {
        StringBuilder prefix = new StringBuilder(wrapToQueryBlock ? "query" : "");
        if (variables != null && !variables.isEmpty()) {
            prefix.append('(');
            for (Map.Entry<String, Object> v : variables.entrySet()) {
                if (prefix.charAt(prefix.length() - 1) != '(') {
                    prefix.append(", ");
                }
                String type = resolveVariableType(v.getValue());
                // query($pullrequest_number:Int!
                prefix.append('$').append(v.getKey()).append(':').append(type).append('!');
            }
            prefix.append(')');
        }
        if (prefix.length() > 0) {
            return String.format(prefix + " { repository(owner: \"%s\", name: \"%s\") { %s } }", owner, repo, q);
        } else {
            return String.format("{ repository(owner: \"%s\", name: \"%s\") { %s } }", owner, repo, q);
        }
    }

    /**
     * Executes a simple graphql repository query with pagination support.
     *
     * @param q query encapsulated in repository-block
     * @param resultsPerPage how many results should be returned
     * @return reponse
     */
    public CompletableFuture<PaginatedGraphQLResponse> executePaginatedQuery(final String q, final int resultsPerPage) {
        return executePaginatedQuery(q, resultsPerPage, null);
    }

    /**
     * Executes a variabled graphql repository query with pagination support.
     *
     * @param q query encapsulated in repository-block
     * @param resultsPerPage how many results should be returned
     * @param variables variables to include
     * @return reponse
     */
    public CompletableFuture<PaginatedGraphQLResponse> executePaginatedQuery(final String q, final int resultsPerPage, final Map<String, Object> variables) {
        return executePaginatedQuery(q, resultsPerPage, variables, null);
    }

    /**
     * Executes a request to fetch the next page for a paginated result.
     *
     * @param r response
     * @return response
     * @throws NoSuchElementException if no next page is available
     */
    public CompletableFuture<PaginatedGraphQLResponse> getNextPage(final PaginatedGraphQLResponse r) {
        if (!r.hasNextPage()) {
            throw new NoSuchElementException("GraphQL cursor iteration exhausted");
        }
        return executePaginatedQuery(r.originalQuery(), r.resultsPerPage(), r.variables(), r.pageInfo().endCursor());
    }

    private CompletableFuture<PaginatedGraphQLResponse> executePaginatedQuery(final String q, final int resultsPerPage, final Map<String, Object> variables, final String cursor) {
        // pullRequests(#PAGINATE#) => pullRequests(first:n, after:null)
        int index = q.indexOf("#PAGINATE#");
        if (index > -1) {
            // insert pageInfo => find { and it's closing }
            index = q.indexOf('{', index) + 1;
            int find = 1, closing = -1;
            while (index < q.length()) {
                char c = q.charAt(index);
                if (c == '}') {
                    find--;
                    if (find == 0) {
                        closing = index;
                        break;
                    }
                } else if (c == '{') {
                    find++;
                }
                index++;
            }
            if (closing == -1) {
                throw new IllegalArgumentException("cannot find pageInfo insertion position from query.");
            }
            // insert pageInfo block
            String query = q.substring(0, closing - 1) + " pageInfo { endCursor hasNextPage } " + q.substring(closing);
            // ... and pagination vars
            query = query.replace("#PAGINATE#", "first:" + resultsPerPage + ", after:" + (cursor != null ? "\"" + cursor + "\"" : null));
            return executeRepositoryQuery(query, variables).thenApply(r -> {
                ImmutablePaginatedGraphQLResponse.Builder b = ImmutablePaginatedGraphQLResponse.builder().from(r)
                        .originalQuery(q)
                        .resultsPerPage(resultsPerPage)
                        .pageInfo(r.findPageInfo());
                if (variables != null) {
                    b.variables(variables);
                }
                return b.build();
            });
        } else {
            throw new IllegalArgumentException("query missing #PAGINATE# tag.");
        }
    }

    protected static String resolveVariableType(final Object obj) {
        if (obj == null) {
            throw new RuntimeException("Only non-null variables supported in GraphQLQuery.");
        }
        Class<?> clazz = obj.getClass();

        if (clazz == Integer.class || clazz == Long.class) {
            return "Int";
        } else if (clazz == Float.class || clazz == Double.class) {
            return "Float";
        } else if (clazz == String.class) {
            return "String";
        } else if (clazz == Boolean.class) {
            return "Boolean";
        } else if (obj instanceof List<?>) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty()) {
                return "[" + resolveVariableType(list.get(0)) + "]";
            } else {
                throw new RuntimeException("Only non-empty list variables supported in GraphQLQuery.");
            }
        } else {
            throw new RuntimeException("Only Scalar variables supported in GraphQLQuery.");
        }
    }

}
