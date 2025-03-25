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

import com.spotify.github.graphql.GraphQLResponse;
import com.spotify.github.graphql.ImmutableGraphQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
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

    public CompletableFuture<GraphQLResponse> executeRepositoryQuery(final String q) {
        String query = String.format("query { repository(owner: \"%s\", name: \"%s\") { %s }", owner, repo, q);
        final String requestBody = github.json().toJsonUnchecked(ImmutableGraphQLQuery.builder().query(query).build());
        log.debug("executeRepositoryQuery: {}", requestBody);
        return github.postGraphql(requestBody, GraphQLResponse.class);
    }

}
