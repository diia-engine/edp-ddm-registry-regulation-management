/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.gerritintegration.service;

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class GerritServiceReviewTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void reviewTest() {
    var reviewResult = new ReviewResult();
    reviewResult.ready = true;
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(reviewResult);
    var changeId = RandomString.make();
    Boolean review = gerritService.review(changeId);

    Mockito.verify(revisionApi, Mockito.times(1)).review(any());
    Assertions.assertThatCode(() ->gerritService.review(changeId))
        .doesNotThrowAnyException();
    Assertions.assertThat(review).isNotNull();
  }

  @Test
  @SneakyThrows
  void notFoundExceptionTest() {

    Mockito.when(changes.id(any())).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));
    Assertions.assertThatCode(() -> gerritService.review("changeId"))
        .isInstanceOf(GerritChangeNotFoundException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(any());
  }

  @Test
  @SneakyThrows
  void httpExceptionTest() {

    Mockito.when(changes.id(any())).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.review("changeId"))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(any());
  }

  @Test
  @SneakyThrows
  void restApiExceptionTest() {

    Mockito.when(changes.id(any())).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.review("changeId"))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(any());
  }
}
