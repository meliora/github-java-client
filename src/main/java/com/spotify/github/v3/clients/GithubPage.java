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

import static com.spotify.github.v3.clients.GitHubClient.responseBodyUnchecked;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.spotify.github.async.AsyncPage;
import com.spotify.github.http.ImmutablePagination;
import com.spotify.github.http.Link;
import com.spotify.github.http.Pagination;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Async page implementation for github resources
 *
 * @param <T> resource type
 */
public class GithubPage<T> implements AsyncPage<T> {

  private final GitHubClient github;
  private final String path;
  private final TypeReference<List<T>> typeReference;
  private Map<String, String> extraHeaders;

  /**
   * C'tor.
   *
   * @param github github client
   * @param path resource page path
   * @param typeReference type reference for deserialization
   */
  GithubPage(final GitHubClient github, final String path, final TypeReference<List<T>> typeReference) {
    this.github = github;
    this.path = path;
    this.typeReference = typeReference;
  }

  /**
   * C'tor.
   *
   * @param github github client
   * @param path resource page path
   * @param typeReference type reference for deserialization
   * @param extraHeaders extra headers to use in requests
   */
  GithubPage(final GitHubClient github, final String path, final TypeReference<List<T>> typeReference, final Map<String, String> extraHeaders) {
      this(github, path, typeReference);
      this.extraHeaders = extraHeaders;
  }

    /** {@inheritDoc} */
  @Override
  public CompletableFuture<Pagination> pagination() {
    return linkMapAsync()
        .thenApply(
            linkMap -> {
              final Optional<Integer> maybePreviousPageNumber =
                  Optional.ofNullable(linkMap.get("prev"))
                      .map(prevLink -> pageNumberFromUri(prevLink.url().toString() + 1).orElse(1));

              final Optional<Integer> maybeNextPageNumber =
                  Optional.ofNullable(linkMap.get("next"))
                      .map(
                          prevLink ->
                              pageNumberFromUri(prevLink.url().toString())
                                  .<RuntimeException>orElseThrow(
                                      () ->
                                          new RuntimeException(
                                              "Could not parse page number from Link header with rel=\"next\"")));

              final Integer lastPageNumber =
                  maybePreviousPageNumber
                      .map(pageNumber -> pageNumber + 1)
                      .orElseGet(
                          () ->
                              Optional.ofNullable(linkMap.get("last"))
                                  .map(
                                      lastLink ->
                                          pageNumberFromUri(lastLink.url().toString())
                                              .<RuntimeException>orElseThrow(
                                                  () ->
                                                      new RuntimeException(
                                                          "Could not parse page number from Link "
                                                              + "header with rel=\"last\"")))
                                  .orElse(1));

              final Integer currentPageNumber =
                  maybeNextPageNumber.map(pageNumber -> pageNumber - 1).orElse(lastPageNumber);

              final ImmutablePagination.Builder builder =
                  ImmutablePagination.builder().current(currentPageNumber).last(lastPageNumber);

              maybePreviousPageNumber.ifPresent(builder::previous);
              maybeNextPageNumber.ifPresent(builder::next);

              return builder.build();
            });
  }

  /** {@inheritDoc} */
  @Override
  public CompletableFuture<AsyncPage<T>> nextPage() {
    return linkMapAsync()
        .thenApply(
            linkMap -> {
              final String nextPath =
                  Optional.ofNullable(linkMap.get("next"))
                      .map(nextLink -> nextLink.url().toString().replaceAll(github.urlFor(""), ""))
                      .orElseThrow(() -> new NoSuchElementException("Page iteration exhausted"));
              return create(github, nextPath, typeReference, extraHeaders);
            });
  }

  /** {@inheritDoc} */
  @Override
  public CompletableFuture<Boolean> hasNextPage() {
    return linkMapAsync().thenApply(linkMap -> nonNull(linkMap.get("next")));
  }

  /** {@inheritDoc} */
  @Override
  public AsyncPage<T> clone() {
    return create(github, path, typeReference, extraHeaders);
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<T> iterator() {
    CompletableFuture<Response> future = extraHeaders != null ? github.request(path, extraHeaders) : github.request(path);
    return future
         .thenApply(
            response ->
                github
                    .json()
                    .fromJsonUncheckedNotNull(getPageArrayBody(response), typeReference))
        .join()
        .iterator();
  }


 /**
  * Override point.
  *
  * @return returns the [...] part of the response - defaults to returning whole body from the response
  */
  public String getPageArrayBody(final Response response) {
      return responseBodyUnchecked(response);
  }

 /**
  * Override point.
  *
  * @return new instance of this for parameters
  */
  public GithubPage<T> create(final GitHubClient github, final String path, final TypeReference<List<T>> typeReference, final Map<String, String> extraHeaders) {
      return new GithubPage<>(github, path, typeReference, extraHeaders);
  }

  private CompletableFuture<Map<String, Link>> linkMapAsync() {
    CompletableFuture<Response> future = extraHeaders != null ? github.request(path, extraHeaders) : github.request(path);
    return future
        .thenApply(
            response -> {
                Optional.ofNullable(response.body()).ifPresent(ResponseBody::close);
                return Optional.ofNullable(response.headers().get("Link"))
                    .map(linkHeader -> stream(linkHeader.split(",")))
                    .orElseGet(Stream::empty)
                    .map(linkString -> Link.from(linkString.split(";")))
                    .filter(link -> link.rel().isPresent())
                    .collect(toMap(link -> link.rel().get(), identity()));
            });
  }

  private Optional<Integer> pageNumberFromUri(final String uri) {
    return Optional.ofNullable(uri.replaceAll(".*\\?page=", "").replaceAll("&.*", ""))
        .filter(string -> string.matches("\\d+"))
        .map(Integer::parseInt);
  }
}
