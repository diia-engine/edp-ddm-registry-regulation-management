package com.epam.digital.data.platform.management;

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MasterVersionControllerIT extends BaseIT {

  @Test
  @SneakyThrows
  public void getMasterVersionInfo() {
    var changeInfo = new ChangeInfo();
    changeInfo._number = 1;
    changeInfo.owner = new AccountInfo("admin", "admin@epam.com");
    changeInfo.owner.username = "admin";
    changeInfo.topic = "this is description for version candidate";
    changeInfo.subject = "commit message";
    changeInfo.updated = Timestamp.from(
        LocalDateTime.of(2022, 8, 2, 16, 15).toInstant(ZoneOffset.UTC));
    changeInfo.labels = Map.of();

    gerritApiMock.mockGetLastMergedQuery(changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/master")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.id", is("1")),
            jsonPath("$.author", is("admin")),
            jsonPath("$.description", is("this is description for version candidate")),
            jsonPath("$.name", is("commit message")),
            jsonPath("$.published", nullValue()),
            jsonPath("$.inspector", nullValue()),
            jsonPath("$.validations", nullValue()));
  }

  @Test
  @SneakyThrows
  public void getMasterVersionInfo_noLastMergedMR() {
    gerritApiMock.mockGetLastMergedQuery(null);
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/master")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.id", nullValue()),
            jsonPath("$.author", nullValue()),
            jsonPath("$.description", nullValue()),
            jsonPath("$.name", nullValue()),
            jsonPath("$.published", nullValue()),
            jsonPath("$.inspector", nullValue()),
            jsonPath("$.validations", nullValue()));
  }
}
