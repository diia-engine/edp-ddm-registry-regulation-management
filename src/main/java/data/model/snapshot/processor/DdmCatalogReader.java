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
package data.model.snapshot.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import schemacrawler.schema.Catalog;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DdmCatalogReader implements DdmNamedObjectReader<Catalog> {

  private final DdmTableReader ddmTableReader;

  @Override
  public void readNamedObject(Catalog catalog) {
    catalog.getTables().forEach(ddmTableReader::readNamedObject);
  }
}
