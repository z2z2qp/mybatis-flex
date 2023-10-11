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

package com.mybatisflex.coretest;

import com.mybatisflex.core.dialect.DialectFactory;
import com.mybatisflex.core.dialect.IDialect;
import com.mybatisflex.core.logicdelete.LogicDeleteProcessor;
import com.mybatisflex.core.logicdelete.impl.*;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
/**
 * 逻辑删除测试。
 *
 * @author 王帅
 * @since 2023-06-20
 */
@SuppressWarnings("all")
public class LogicDeleteTest {

    private final String logicColumn = "deleted";
    private final IDialect dialect = DialectFactory.getDialect();

    @Test
    public void test() {
        print("DefaultLogicDeleteProcessor", new DefaultLogicDeleteProcessor(), "`deleted` = 1", "`deleted` = 0");
        print("BooleanLogicDeleteProcessor", new BooleanLogicDeleteProcessor(), "`deleted` = true", "`deleted` = false");
        print("IntegerLogicDeleteProcessor", new IntegerLogicDeleteProcessor(), "`deleted` = 1", "`deleted` = 0");
        print("DateTimeLogicDeleteProcessor", new DateTimeLogicDeleteProcessor(), "`deleted` = NOW()", "`deleted` IS NULL");
        print("TimeStampLogicDeleteProcessor", new TimeStampLogicDeleteProcessor(), null, "`deleted` = 0");
        print("PrimaryKeyLogicDeleteProcessor", new PrimaryKeyLogicDeleteProcessor(), "`deleted` = `id`", "`deleted` IS NULL");
    }

    public void print(String type, LogicDeleteProcessor processor, String logicDeletedCondition, String logicNormalCondition) {
        System.out.println("===== " + type + " =====");
        TableInfo tableInfo = TableInfoFactory.ofEntityClass(Account.class);
        String actualLogicDeletedCondition = processor.buildLogicDeletedSet(logicColumn, tableInfo, dialect);
        System.out.println(actualLogicDeletedCondition);
        if (logicDeletedCondition != null) {
            assertEquals(actualLogicDeletedCondition, logicDeletedCondition);
        }
        String actualLogicNormalCondition = processor.buildLogicNormalCondition(logicColumn, tableInfo, dialect);
        System.out.println(actualLogicNormalCondition);
        assertEquals(actualLogicNormalCondition, logicNormalCondition);
    }

}
