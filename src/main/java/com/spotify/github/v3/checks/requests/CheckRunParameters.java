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

package com.spotify.github.v3.checks.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.spotify.github.GithubStyle;
import com.spotify.github.Parameters;
import com.spotify.github.v3.checks.CheckRunStatus;
import org.immutables.value.Value;

import java.util.Optional;

/** Check run retrieval parameters resource */
@Value.Immutable
@GithubStyle
@JsonSerialize(as = ImmutableCheckRunParameters.class)
@JsonDeserialize(as = ImmutableCheckRunParameters.class)
public interface CheckRunParameters extends Parameters {
    @SuppressWarnings("checkstyle:methodname")
    Optional<String> check_name();

    Optional<CheckRunStatus> status();

    Optional<String> filter();

    @SuppressWarnings("checkstyle:methodname")
    Optional<Integer> per_page();

    Optional<Integer> page();

    @SuppressWarnings("checkstyle:methodname")
    Optional<Integer> app_id();
}
