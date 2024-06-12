/*
 *  Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mybatisflex.codegen.dialect.impl;

import com.mybatisflex.codegen.dialect.AbstractJdbcDialect;
import com.mybatisflex.core.util.StringUtil;

/**
 * @author strignke
 */
public class PostgreSQLJdbcDialect extends AbstractJdbcDialect {
    @Override
    protected String forBuildColumnsSql(String schema, String tableName) {
        return "SELECT * FROM " + (StringUtil.isNotBlank(schema) ? schema + "." : "") + "\"" + tableName + "\"" + " WHERE 1 = 2";
    }
}
