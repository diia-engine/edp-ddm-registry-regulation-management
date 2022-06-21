/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.upload.validator.generic;

import com.epam.digital.data.platform.upload.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.upload.model.ValidationResult;
import com.epam.digital.data.platform.upload.validator.Validator;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

public class FileExistenceValidator extends Validator {

  @Override
  public void selfValidate(MultipartFile inputFile, ValidationResult validationResult) {
    if (Objects.isNull(inputFile) || inputFile.isEmpty()) {
      throw new FileLoadProcessingException("File cannot be saved to Ceph - file is null or empty");
    }
  }
}