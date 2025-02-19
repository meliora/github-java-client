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

package com.spotify.github.v3;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.spotify.github.GithubStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

/**
 * A File attached to an Issue or a Comment.
 */
@Value.Immutable
@GithubStyle
@JsonSerialize(as = ImmutableAttachmentFile.class)
@JsonDeserialize(as = ImmutableAttachmentFile.class)
public interface AttachmentFile {
    @Nullable
    String contentType();

    @Nullable
    Charset charset();

    @Nullable
    Long contentLength();

    byte[] bytes();
}
