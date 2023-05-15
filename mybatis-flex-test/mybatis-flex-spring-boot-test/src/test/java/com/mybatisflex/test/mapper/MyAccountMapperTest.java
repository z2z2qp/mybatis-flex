package com.mybatisflex.test.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.test.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mybatisflex.test.model.table.Tables.ACCOUNT;
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
        for (int i = 0; i < 3_3334; i++) {
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
        assertEquals(33334, i);
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