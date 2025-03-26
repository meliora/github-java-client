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

import com.spotify.github.graphql.GraphQLPageInfo;
import com.spotify.github.graphql.GraphQLResponse;
import com.spotify.github.jackson.Json;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.spotify.github.v3.clients.MockHelper.createMockResponse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphQLClientTest {

    private GitHubClient github;
    private GraphQLClient graphQLClient;

    @BeforeEach
    public void setUp() {
        github = mock(GitHubClient.class);
        when(github.json()).thenReturn(Json.create());
        when(github.urlFor("")).thenReturn("https://github.com/api/v3");
        graphQLClient = new GraphQLClient(github, "someowner", "somerepo");
    }

    @Test
    public void testGraphQLRequest() throws IOException {
        final Response response = createMockResponse("",
"{\"data\":{\"repository\":{\"pullRequest\":{\"id\":\"PR_kwDOOGLqn86QCX0t\",\"closingIssuesReferences\":{\"edges\":[{\"node\":{\"id\":\"I_kwDOOGLqn86vnkny\",\"body\":\"feature description\",\"number\":2,\"title\":\"feature xyz\"}}]}}}}}"
        );

        when(github.postGraphql(anyString(), eq(GraphQLResponse.class))).thenCallRealMethod();
        when(github.postGraphql(anyString())).thenReturn(completedFuture(response));

        GraphQLResponse r = graphQLClient.executeRepositoryQuery(
                "pullRequest(number:3) { id, closingIssuesReferences (first:50) { edges { node { id body number title } } } }"
        ).join();

        assertThat(r.getData("repository.pullRequest.closingIssuesReferences.edges[0].node.number"), is(2));
    }

    @Test
    public void testGraphQLRequestPageInfo() throws IOException {
        final Response response = createMockResponse("",
                "{\"data\":{\"repository\":{\"pullRequest\":{\"id\":\"PR_kwDOOGLqn86QCX0t\",\"closingIssuesReferences\":{\"edges\":[{\"node\":{\"id\":\"I_kwDOOGLqn86vnkny\",\"body\":\"feature description\",\"number\":2,\"title\":\"feature xyz\"}}],\"pageInfo\":{\"hasNextPage\":true,\"endCursor\":\"Y3Vyc29yOnYyOpHOF4rHRA==\"}}}}}}"
        );

        when(github.postGraphql(anyString(), eq(GraphQLResponse.class))).thenCallRealMethod();
        when(github.postGraphql(anyString())).thenReturn(completedFuture(response));

        GraphQLResponse r = graphQLClient.executeRepositoryQuery(
                "pullRequest(number:3) { id, closingIssuesReferences (first:50) { edges { node { id body number title } } } }"
        ).join();

        assertThat(r.getData("repository.pullRequest.closingIssuesReferences.edges[0].node.number"), is(2));
        GraphQLPageInfo pageInfo = r.getPageInfo("repository.pullRequest.closingIssuesReferences");
        assertNotNull(pageInfo);
        assertTrue(pageInfo.hasNextPage());
        assertEquals("Y3Vyc29yOnYyOpHOF4rHRA==", pageInfo.endCursor());
    }

}
