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

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.Test;

import java.util.Arrays;

import static com.mybatisflex.core.query.QueryMethods.select;
import static com.mybatisflex.core.query.QueryMethods.union;
import static com.mybatisflex.coretest.table.AccountTableDef.ACCOUNT;
import static com.mybatisflex.coretest.table.ArticleTableDef.ARTICLE;

public class WithSQLTester {


    @Test
    public void testWithSql1() {
        QueryWrapper query = QueryWrapper.create()
                .with("CTE").asSelect(
                        select().from(ARTICLE).where(ARTICLE.ID.ge(100))
                )
                .select()
                .from(ACCOUNT)
                .where(ACCOUNT.SEX.eq(1));

        System.out.println(query.toSQL());
    }


    @Test
    public void testWithSql2() {
        QueryWrapper query = QueryWrapper.create()
                .withRecursive("CTE").asSelect(
                        select().from(ARTICLE).where(ARTICLE.ID.ge(100))
                )
                .from(ACCOUNT)
                .where(ACCOUNT.SEX.eq(1));

        System.out.println(query.toSQL());
    }

    @Test
    public void testWithSql3() {
        QueryWrapper query = QueryWrapper.create()
                .withRecursive("CTE", "id", "value").asSelect(
                        QueryWrapper.create().from(ARTICLE).where(ARTICLE.ID.ge(100))
                )
                .select()
                .from(ACCOUNT)
                .where(ACCOUNT.SEX.eq(1));

        System.out.println(query.toSQL());
    }


    @Test
    public void testWithSql4() {
        QueryWrapper query = QueryWrapper.create()
                .with("CTE").asSelect(
                        select().from(ARTICLE).where(ARTICLE.ID.ge(100))
                )
                .with("xxx").asSelect(
                        select().from(ARTICLE).where(ARTICLE.ID.ge(200))
                )
                .select()
                .from(ACCOUNT)
                .where(ACCOUNT.SEX.eq(1));

        System.out.println(query.toSQL());
    }

    @Test
    public void testWithSql5() {
        QueryWrapper query = QueryWrapper.create()
            .withRecursive("CTE").asSelect(
                select().from(ARTICLE).where(ARTICLE.ID.ge(100))
            )
            .with("xxx", "id", "name").asValues(
                Arrays.asList("a", "b"),
                union(
                    select().from(ARTICLE).where(ARTICLE.ID.ge(200))
                )

            )
            .from(ACCOUNT)
            .where(ACCOUNT.SEX.eq(1));

        System.out.println(query.toSQL());
    }

}
