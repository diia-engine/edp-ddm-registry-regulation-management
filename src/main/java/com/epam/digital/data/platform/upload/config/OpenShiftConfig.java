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

package com.epam.digital.data.platform.upload.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import jodd.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenShiftConfig {

  @Bean
  public Config config(@Value("${open-shift.base-domain}") String baseDomain,
                       @Value("${open-shift.password}") String password,
                       @Value("${open-shift.username}") String userName,
                       @Value("${open-shift.cluster}") String cluster,
                       @Value("${open-shift.url}") String url,
                       @Value("${open-shift.namespace}") String namespace) {
    return new ConfigBuilder()
            .withUsername(userName)
            .withPassword(Base64.decodeToString(password))
            .withTrustCerts(true)
            .withMasterUrl(String.format(url, cluster, baseDomain))
            .withNamespace(namespace)
            .build();
  }
}