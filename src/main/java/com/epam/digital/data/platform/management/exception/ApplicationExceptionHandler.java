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

package com.epam.digital.data.platform.management.exception;

import com.epam.digital.data.platform.starter.localization.MessageResolver;
import com.epam.digital.data.platform.management.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.management.model.exception.DetailedErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

import static com.epam.digital.data.platform.management.util.Header.TRACE_ID;

@Slf4j
@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  public static final String FILE_ENCODING_EXCEPTION = "FILE_ENCODING_EXCEPTION";
  public static final String FILE_EXTENSION_ERROR = "FILE_EXTENSION_ERROR";
  public static final String FILE_SIZE_ERROR = "FILE_SIZE_ERROR";
  public static final String JWT_PARSING_ERROR = "JWT_PARSING_ERROR";
  private static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  private static final String IMPORT_CEPH_ERROR = "IMPORT_CEPH_ERROR";
  private static final String GET_CEPH_ERROR = "GET_CEPH_ERROR";
  private static final String RUNTIME_ERROR = "RUNTIME_ERROR";

  private final MessageResolver messageResolver;

  public ApplicationExceptionHandler(MessageResolver messageResolver) {
    this.messageResolver = messageResolver;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(FileLoadProcessingException.class)
  public ResponseEntity<DetailedErrorResponse> handleFileLoadProcessingException(
          FileLoadProcessingException exception) {
    log.error("Error during upload to Ceph", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(IMPORT_CEPH_ERROR, exception));
  }

  @ExceptionHandler(GetProcessingException.class)
  public ResponseEntity<DetailedErrorResponse> handleGetProcessingException(
          GetProcessingException exception) {
    log.error("Error during getting from Ceph", exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(newDetailedResponse(GET_CEPH_ERROR, exception));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(FORBIDDEN_OPERATION, exception));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<DetailedErrorResponse> handleMaxUploadSizeExceededException(
          MaxUploadSizeExceededException exception) {
    log.error("File is to large", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(newDetailedResponse(FILE_SIZE_ERROR, exception));
  }

  @ExceptionHandler(CephInvocationException.class)
  public ResponseEntity<DetailedErrorResponse> handleCephInvocationException(
          CephInvocationException exception) {
    log.error("Ceph invocation exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(OpenShiftInvocationException.class)
  public ResponseEntity<DetailedErrorResponse> handleOpenShiftInvocationException(
          OpenShiftInvocationException exception) {
    log.error("Open-shift invocation exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(JwtParsingException.class)
  public ResponseEntity<DetailedErrorResponse> handleJwtParsingException(
          JwtParsingException exception) {
    log.error("Jwt parsing exception", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(newDetailedResponse(JWT_PARSING_ERROR, exception));
  }

  @ExceptionHandler(FileEncodingException.class)
  public ResponseEntity<DetailedErrorResponse> handleFileEncodingException(
          FileEncodingException exception) {
    log.error("File encoding exception", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(newDetailedResponse(FILE_ENCODING_EXCEPTION, exception));
  }

  @ExceptionHandler(FileExtensionException.class)
  public ResponseEntity<DetailedErrorResponse> handleFileExtensionException(
          FileExtensionException exception) {
    log.error("File extension exception", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(newDetailedResponse(FILE_EXTENSION_ERROR, exception));
  }

  private DetailedErrorResponse newDetailedResponse(String code, Exception exception) {
    var response = new DetailedErrorResponse();
    response.setTraceId(MDC.get(TRACE_ID.getHeaderName()));
    response.setCode(code);
    response.setDetails(exception.getMessage());

    var titleKey = FileValidatorErrorMessageTitle.from(code);
    response.setLocalizedMessage(Objects.isNull(titleKey) ? null : messageResolver.getMessage(titleKey));

    return response;
  }
}
