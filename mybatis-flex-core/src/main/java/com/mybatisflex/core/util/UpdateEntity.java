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
package com.mybatisflex.core.util;

import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.update.ModifyAttrsRecordProxyFactory;

import java.lang.reflect.Array;

public class UpdateEntity {

    private UpdateEntity() {
    }


    public static <T> T of(Class<T> clazz) {
        clazz = ClassUtil.getUsefulClass(clazz);
        return ModifyAttrsRecordProxyFactory.getInstance().get(clazz);
    }


    public static <T> T of(Class<T> clazz, Object id) {
        clazz = ClassUtil.getUsefulClass(clazz);
        var newEntity = ModifyAttrsRecordProxyFactory.getInstance().get(clazz);
        var tableInfo = TableInfoFactory.ofEntityClass(clazz);
        var primaryKeyList = tableInfo.getPrimaryKeyList();
        var reflector = Reflectors.of(clazz);

        if (primaryKeyList != null && !primaryKeyList.isEmpty()) {
            for (var i = 0; i < primaryKeyList.size(); i++) {
                var idInfo = primaryKeyList.get(i);
                var idValue = getIdValue(id, i);
                var setInvoker = reflector.getSetInvoker(idInfo.getProperty());
                try {
                    setInvoker.invoke(newEntity, new Object[]{ConvertUtil.convert(idValue, idInfo.getPropertyType())});
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        return newEntity;
    }


    private static Object getIdValue(Object id, int index) {
        if (id == null) {
            return null;
        }
        if (ClassUtil.isArray(id.getClass())) {
            if (index >= Array.getLength(id)) {
                return null;
            } else {
                return Array.get(id, index);
            }
        }
        //not array
        return index == 0 ? id : null;
    }


    public static <T> T ofNotNull(T entity) {
        var usefulClass = ClassUtil.getUsefulClass(entity.getClass());

        var newEntity = (T) of(usefulClass);

        var reflector = Reflectors.of(usefulClass);
        var propertyNames = reflector.getGetablePropertyNames();

        for (String propertyName : propertyNames) {
            try {
                Object value = reflector.getGetInvoker(propertyName)
                    .invoke(entity, null);
                if (value != null) {
                    reflector.getSetInvoker(propertyName).invoke(newEntity, new Object[]{value});
                }
            } catch (Exception ignored) {
                // do nothing here.
            }
        }

        return newEntity;
    }


}
