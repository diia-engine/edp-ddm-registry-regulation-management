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

package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.BusinessProcessChangesInfo;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormChangesInfo;
import com.epam.digital.data.platform.management.model.dto.VersionChanges;
import com.epam.digital.data.platform.management.service.impl.GlobalSettingServiceImpl;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest(CandidateVersionController.class)
class CandidateVersionControllerTest {

  static final String BASE_URL = "/versions/candidates";
  public static final String VERSION_CANDIDATE_NAME = "JohnDoe's version candidate";
  public static final String VERSION_CANDIDATE_DESCRIPTION = "Version candidate to change form";
  public static final String VERSION_CANDIDATE_AUTHOR = "JohnDoe@epam.com";

  @MockBean
  private VersionManagementServiceImpl versionManagementService;
  @MockBean
  private GlobalSettingServiceImpl globalSettingService;
  @RegisterExtension
  final RestDocumentationExtension restDocumentation = new RestDocumentationExtension();
  MockMvc mockMvc;
  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation)).build();
  }

  @Test
  @SneakyThrows
  void getVersionListTest() {
    var changeInfo = ChangeInfoDetailedDto.builder()
        .number(1)
        .subject(VERSION_CANDIDATE_NAME)
        .description(VERSION_CANDIDATE_DESCRIPTION)
        .build();
    Mockito.when(versionManagementService.getVersionsList()).thenReturn(List.of(changeInfo));
    mockMvc.perform(
            get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.[0].id", is("1")),
            jsonPath("$.[0].name", is(VERSION_CANDIDATE_NAME)),
            jsonPath("$.[0].description", is(VERSION_CANDIDATE_DESCRIPTION)))
        .andDo(document("versions/candidates/GET"));
    Mockito.verify(versionManagementService).getVersionsList();
  }

  @Test
  @SneakyThrows
  void createNewVersionTest() {
    var request = new CreateVersionRequest();
    request.setName(VERSION_CANDIDATE_NAME);
    request.setDescription(VERSION_CANDIDATE_DESCRIPTION);
    Mockito.when(versionManagementService.createNewVersion(request)).thenReturn("1");

    var expectedVersionDetails = ChangeInfoDetailedDto.builder()
        .number(1)
        .subject(VERSION_CANDIDATE_NAME)
        .description(VERSION_CANDIDATE_DESCRIPTION)
        .owner(VERSION_CANDIDATE_AUTHOR)
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .build();
    Mockito.when(versionManagementService.getVersionDetails("1")).thenReturn(expectedVersionDetails);

    mockMvc.perform(post(BASE_URL)
            .content(objectMapper.writeValueAsString(request))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isCreated(),
            header().string(HttpHeaders.LOCATION, "/versions/candidates/1"),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")),
            jsonPath("$.name", is(VERSION_CANDIDATE_NAME)),
            jsonPath("$.description", is(VERSION_CANDIDATE_DESCRIPTION)),
            jsonPath("$.author", is(VERSION_CANDIDATE_AUTHOR)),
            jsonPath("$.creationDate", is("2022-08-10T11:30:00.000Z")),
            jsonPath("$.latestUpdate", is("2022-08-10T11:40:00.000Z")),
            jsonPath("$.hasConflicts", is(false)),
            jsonPath("$.inspections", nullValue()),
            jsonPath("$.validations", nullValue()))
        .andDo(document("versions/candidates/POST"));
    Mockito.verify(versionManagementService).getVersionDetails("1");
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest() {
    var expectedVersionDetails = ChangeInfoDetailedDto.builder()
        .number(1)
        .subject(VERSION_CANDIDATE_NAME)
        .description(VERSION_CANDIDATE_DESCRIPTION)
        .owner(VERSION_CANDIDATE_AUTHOR)
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .build();
    Mockito.when(versionManagementService.getVersionDetails("1")).thenReturn(expectedVersionDetails);

    mockMvc.perform(get(String.format("%s/%s", BASE_URL, "1")))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")),
            jsonPath("$.name", is(VERSION_CANDIDATE_NAME)),
            jsonPath("$.description", is(VERSION_CANDIDATE_DESCRIPTION)),
            jsonPath("$.author", is(VERSION_CANDIDATE_AUTHOR)),
            jsonPath("$.creationDate", is("2022-08-10T11:30:00.000Z")),
            jsonPath("$.latestUpdate", is("2022-08-10T11:40:00.000Z")),
            jsonPath("$.hasConflicts", is(false)),
            jsonPath("$.inspections", nullValue()),
            jsonPath("$.validations", nullValue()))
        .andDo(document("versions/candidates/{versionCandidateId}/GET"));
    Mockito.verify(versionManagementService).getVersionDetails("1");
  }

  @Test
  @SneakyThrows
  void declineTest() {
    Mockito.doNothing().when(versionManagementService).decline("1");
    mockMvc.perform(post(String.format("%s/%s/%s", BASE_URL, "1", "decline")))
        .andExpect(status().isOk())
        .andDo(document("versions/candidates/{versionCandidateId}/decline/POST"));
    Mockito.verify(versionManagementService).decline("1");
  }

  @Test
  @SneakyThrows
  void submitTest() {
    Mockito.doNothing().when(versionManagementService).submit("1");
    mockMvc.perform(post(String.format("%s/%s/%s", BASE_URL, "1", "submit")))
        .andExpect(status().isOk())
        .andDo(document("versions/candidates/{versionCandidateId}/submit/POST"));
    Mockito.verify(versionManagementService).submit("1");
  }

  @Test
  @SneakyThrows
  void rebaseTest() {
    Mockito.doNothing().when(versionManagementService).rebase("1");
    mockMvc.perform(get(String.format("%s/%s/%s", BASE_URL, "1", "rebase")))
        .andExpect(status().isOk())
        .andDo(document("versions/candidates/{versionCandidateId}/rebase/GET"));
    Mockito.verify(versionManagementService).rebase("1");
  }

  @Test
  @SneakyThrows
  void getChangesTest() {
    List<FormChangesInfo> changedForms = List.of(FormChangesInfo.builder()
        .name("formToBeUpdated")
        .title("JohnDoe's form")
        .status(FileStatus.CHANGED)
        .build());
    List<BusinessProcessChangesInfo> changedProcesses = List.of(BusinessProcessChangesInfo.builder()
        .name("newProcess")
        .title("JohnDoe's process")
        .status(FileStatus.NEW)
        .build());
    VersionChanges expected = VersionChanges.builder()
        .changedForms(changedForms)
        .changedBusinessProcesses(changedProcesses)
        .build();

    Mockito.when(versionManagementService.getVersionChanges("1")).thenReturn(expected);

    mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/1/changes")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.changedForms", hasSize(1)),
            jsonPath("changedBusinessProcesses", hasSize(1)))
        .andDo(document("versions/candidates/{versionCandidateId}/changes/GET"));
    Mockito.verify(versionManagementService).getVersionChanges("1");
  }
}
