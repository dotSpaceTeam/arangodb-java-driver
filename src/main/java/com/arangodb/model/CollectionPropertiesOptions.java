/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mark Vollmary
 * @see <a href="https://www.arangodb.com/docs/stable/http/collection-modifying.html#change-properties-of-a-collection">API
 * Documentation</a>
 */
public class CollectionPropertiesOptions {

    private Boolean waitForSync;
    /**
     * @deprecated MMFiles only
     */
    @Deprecated
    private Long journalSize;
    private CollectionSchema schema;
    private List<ComputedValue> computedValues;

    public CollectionPropertiesOptions() {
        super();
    }

    public Boolean getWaitForSync() {
        return waitForSync;
    }

    /**
     * @param waitForSync If true then creating or changing a document will wait until the data has been synchronized to disk.
     * @return options
     */
    public CollectionPropertiesOptions waitForSync(final Boolean waitForSync) {
        this.waitForSync = waitForSync;
        return this;
    }

    /**
     * @deprecated MMFiles only
     */
    @Deprecated
    public Long getJournalSize() {
        return journalSize;
    }

    /**
     * @param journalSize The maximal size of a journal or datafile in bytes. The value must be at least 1048576 (1 MB). Note
     *                    that when changing the journalSize value, it will only have an effect for additional journals or
     *                    datafiles that are created. Already existing journals or datafiles will not be affected.
     * @return options
     * @deprecated MMFiles only
     */
    @Deprecated
    public CollectionPropertiesOptions journalSize(final Long journalSize) {
        this.journalSize = journalSize;
        return this;
    }

    public CollectionSchema getSchema() {
        return schema;
    }

    /**
     * @param schema object that specifies the collection level schema for documents
     * @since ArangoDB 3.7
     * @return options
     */
    public CollectionPropertiesOptions schema(final CollectionSchema schema) {
        this.schema = schema;
        return this;
    }

    /**
     * @param computedValues An optional list of computed values.
     * @return options
     * @since ArangoDB 3.10
     */
    public CollectionPropertiesOptions computedValues(final ComputedValue... computedValues) {
        if(this.computedValues == null) {
            this.computedValues = new ArrayList<>();
        }
        Collections.addAll(this.computedValues, computedValues);
        return this;
    }

}
