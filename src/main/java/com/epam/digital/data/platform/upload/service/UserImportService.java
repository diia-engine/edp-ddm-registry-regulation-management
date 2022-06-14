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

package com.epam.digital.data.platform.upload.service;

import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.upload.exception.CephInvocationException;
import com.epam.digital.data.platform.upload.exception.GetProcessingException;
import com.epam.digital.data.platform.upload.exception.ImportProcessingException;
import com.epam.digital.data.platform.upload.model.SecurityContext;
import com.epam.digital.data.platform.upload.model.dto.CephEntityImportDto;
import com.epam.digital.data.platform.upload.model.dto.CephEntityReadDto;
import com.epam.digital.data.platform.upload.model.dto.CephFileDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserImportService {
  private static final String CEPH_OBJECT_CONTENT_TYPE = "application/octet-stream";
  private static final String USERNAME = "username";
  private static final String NAME = "name";
  private static final String ID = "id";

  private final CephService userImportCephService;
  private final UserInfoService userInfoService;
  private final String userImportFileBucket;

  public UserImportService(
          CephService userImportCephService,
          @Value("${user-import-ceph.bucket}") String userImportFileBucket,
          UserInfoService userInfoService) {
    this.userImportCephService = userImportCephService;
    this.userImportFileBucket = userImportFileBucket;
    this.userInfoService = userInfoService;
  }

  public CephEntityImportDto storeFile(MultipartFile file, SecurityContext securityContext) {
    if (Objects.isNull(file) || file.isEmpty()) {
      throw new ImportProcessingException("File cannot be saved to Ceph - file is null or empty");
    }

    var originalFilename = file.getOriginalFilename();

    if (StringUtils.isBlank(originalFilename)) {
      throw new ImportProcessingException("File cannot be saved to Ceph - file name is missed");
    }

    String encodedFileName;
    try {
      encodedFileName = new String(Base64.getEncoder().encode(originalFilename.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new ImportProcessingException("Cannot read file name", e);
    }

    String username = userInfoService.createUsername(securityContext.getAccessToken());

    var cephKey = UUID.randomUUID();
    saveFileToCeph(cephKey.toString(), file, encodedFileName, username);

    return new CephEntityImportDto(cephKey);
  }

  public CephEntityReadDto getFileInfo(SecurityContext securityContext) {
    try {
      Set<String> keys = userImportCephService.getKeys(userImportFileBucket, StringUtils.EMPTY);
      if (keys.isEmpty()) {
        return new CephEntityReadDto();
      }

      String username = userInfoService.createUsername(securityContext.getAccessToken());

      return userImportCephService.getMetadata(userImportFileBucket, keys)
              .stream()
              .filter(cephObjectMetadata -> StringUtils.equals(cephObjectMetadata.getUserMetadata().get(USERNAME), username))
              .findFirst()
              .map(cephObjectMetadata -> mapToDto(cephObjectMetadata.getUserMetadata()))
              .orElse(new CephEntityReadDto());
    } catch (Exception e) {
      throw new CephInvocationException("Failed retrieve files info", e);
    }
  }

  private CephEntityReadDto mapToDto(Map<String, String> userMetadata) {
    return CephEntityReadDto
            .builder()
            .id(userMetadata.getOrDefault(ID, StringUtils.EMPTY))
            .name(new String(Base64.getDecoder().decode(userMetadata.getOrDefault(NAME, StringUtils.EMPTY))))
            .build();
  }

  public void delete(String cephKey) {
    try {
      userImportCephService.delete(userImportFileBucket, Set.of(cephKey));
    } catch (Exception e) {
      throw new CephInvocationException("Failed delete file to ceph, cephKey: " + cephKey, e);
    }
  }

  public CephFileDto downloadFile(String cephKey) {
    Optional<CephObject> cephObjectOptional;

    try {
      cephObjectOptional = userImportCephService.get(userImportFileBucket, cephKey);
    } catch (Exception e) {
      throw new CephInvocationException("Failed download file from ceph, cephKey: " + cephKey, e);
    }

    var cephObject = cephObjectOptional.orElseThrow(() -> new GetProcessingException("File not found in Ceph: " + cephKey));

    var fileName = Optional.ofNullable(cephObject.getMetadata().getUserMetadata().get(NAME))
            .orElseThrow(() -> new GetProcessingException("Failed download file from ceph - missed file name, cephKey: " + cephKey));

    var decodedFileName = new String(Base64.getDecoder().decode(fileName), StandardCharsets.UTF_8);

    return new CephFileDto(decodedFileName, cephObject.getContent(), cephObject.getMetadata().getContentLength());
  }

  private void saveFileToCeph(String cephKey, MultipartFile file, String originalFilename, String username) {
    log.info("Storing file to Ceph. Key: {}, Name: {}", cephKey, originalFilename);
    try {
      userImportCephService.put(
              userImportFileBucket,
              cephKey,
              CEPH_OBJECT_CONTENT_TYPE,
              Map.of(NAME, originalFilename, ID, cephKey, USERNAME, username),
              new ByteArrayInputStream(file.getBytes()));
    } catch (Exception e) {
      throw new CephInvocationException("Failed saving file to ceph", e);
    }
  }

}
