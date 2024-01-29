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

package com.mybatisflex.test.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.test.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mybatisflex.test.model.table.AccountTableDef.ACCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 庄佳彬
 * @since 2023/4/24 19:37
 */
@SpringBootTest
class MyAccountMapperTest {

    @Autowired
    private MyAccountMapper mapper;

    @Test
    void insertBatch() {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Account account = new Account();
            account.setBirthday(new Date());
            account.setAge(i % 60);
            account.setUserName(String.valueOf(i));
            accounts.add(account);
        }
        //删除初始化数据
        mapper.deleteById(1);
        mapper.deleteById(2);
        try {
            mapper.insertBatch(accounts);
        } catch (Exception e) {
            System.out.println("异常");
        }
        int i = mapper.insertBatch(accounts, 1000);
        assertEquals(10, i);
    }

    @Test
    void selectListByQueryAs() {
        QueryWrapper wrapper = QueryWrapper.create()
                .select(ACCOUNT.ID,ACCOUNT.USER_NAME)
                .from(ACCOUNT)
                .limit(1);
        List<User> list = mapper.selectListByQueryAs(wrapper, User.class);
        System.out.println(list);

    }
    static class User{
        private long id;
        private String userName;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", userName='" + userName + '\'' +
                    '}';
        }
    }

    @Test
    void selectObjectListByQueryAs() {
        QueryWrapper wrapper = QueryWrapper.create()
                .select(ACCOUNT.ID)
                .from(ACCOUNT)
                .limit(1);
        List<Long> list = mapper.selectObjectListByQueryAs(wrapper, Long.class);
        System.out.println(list);

    }

}
