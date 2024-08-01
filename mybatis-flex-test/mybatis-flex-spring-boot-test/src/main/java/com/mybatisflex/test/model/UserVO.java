/*
 *  Copyright (c) 2022-2025, Mybatis-Flex (fuhai999@gmail.com).
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

package com.mybatisflex.test.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户 VO 对象。
 *
 * @author 王帅
 * @since 2023-06-07
 */

public class UserVO {

    private String userId;
    private String userName;
    //    private TreeSet<Role> roleList;
//    private Role[] roleList;
    private HashMap<String, Object> roleList;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Map<String, Object> getRoleList() {
        return roleList;
    }

    public void setRoleList(HashMap<String, Object> roleList) {
        this.roleList = roleList;
    }

    @Override
    public String toString() {
        return "UserVO{" +
            "userId='" + userId + '\'' +
            ", userName='" + userName + '\'' +
            ", roleList=" + roleList +
            '}';
    }

}
