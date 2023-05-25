/**
 * Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mybatisflex.core;

import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.field.FieldQuery;
import com.mybatisflex.core.field.FieldQueryBuilder;
import com.mybatisflex.core.mybatis.MappedStatementTypes;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.provider.EntitySqlProvider;
import com.mybatisflex.core.query.*;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.util.CollectionUtil;
import com.mybatisflex.core.util.ConvertUtil;
import com.mybatisflex.core.util.ObjectUtil;
import com.mybatisflex.core.util.StringUtil;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;

import static com.mybatisflex.core.query.QueryMethods.count;

public interface BaseMapper<T> {

    /**
     * 插入 entity 数据
     *
     * @param entity 实体类
     * @return 返回影响的行数
     */
    default int insert(T entity) {
        return insert(entity, false);
    }


    /**
     * 插入 entity 数据，但是忽略 null 的数据，只对有值的内容进行插入
     * 这样的好处是数据库已经配置了一些默认值，这些默认值才会生效
     *
     * @param entity 实体类
     * @return 返回影响的行数
     */
    default int insertSelective(T entity) {
        return insert(entity, true);
    }


    /**
     * 插入 entity 数据
     *
     * @param entity 实体类
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#insert(Map, ProviderContext)
     */
    @InsertProvider(type = EntitySqlProvider.class, method = "insert")
    int insert(@Param(FlexConsts.ENTITY) T entity, @Param(FlexConsts.IGNORE_NULLS) boolean ignoreNulls);


    /**
     * 批量插入 entity 数据，只会根据第一条数据来构建插入的字段内容
     *
     * @param entities 插入的数据列表
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#insertBatch(Map, ProviderContext)
     * @see com.mybatisflex.core.FlexConsts#METHOD_INSERT_BATCH
     */
    @InsertProvider(type = EntitySqlProvider.class, method = FlexConsts.METHOD_INSERT_BATCH)
    int insertBatch(@Param(FlexConsts.ENTITIES) List<T> entities);

    /**
     * 批量插入 entity 数据，按 size 切分
     *
     * @param entities 插入的数据列表
     * @param size     切分大小
     * @return 影响行数
     */
    default int insertBatch(List<T> entities, int size) {
        if (size <= 0) {
            size = 1000;//默认1000
        }
        int sum = 0;
        int entitiesSize = entities.size();
        int maxIndex = entitiesSize / size + (entitiesSize % size == 0 ? 0 : 1);
        for (int i = 0; i < maxIndex; i++) {
            List<T> list = entities.subList(i * size, Math.min(i * size + size, entitiesSize));
            sum += insertBatch(list);
        }
        return sum;
    }

    /**
     * 新增 或者 更新，若主键有值，则更新，若没有主键值，则插入
     *
     * @param entity 实体类
     * @return 返回影响的行数
     */
    default int insertOrUpdate(T entity) {
        TableInfo tableInfo = TableInfoFactory.ofEntityClass(entity.getClass());
        Object[] pkArgs = tableInfo.buildPkSqlArgs(entity);
        if (pkArgs.length == 0 || pkArgs[0] == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    /**
     * 根据 id 删除数据
     * 如果是多个主键的情况下，需要传入数组 new Object[]{100,101}
     *
     * @param id 主键数据
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteById(Map, ProviderContext)
     */
    @DeleteProvider(type = EntitySqlProvider.class, method = "deleteById")
    int deleteById(@Param(FlexConsts.PRIMARY_VALUE) Serializable id);


    /**
     * 根据多个 id 批量删除数据
     *
     * @param ids ids 列表
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteBatchByIds(Map, ProviderContext)
     */
    @DeleteProvider(type = EntitySqlProvider.class, method = "deleteBatchByIds")
    int deleteBatchByIds(@Param(FlexConsts.PRIMARY_VALUE) Collection<? extends Serializable> ids);

    /**
     * 根据多个 id 批量删除数据
     *
     * @param ids  ids 列表
     * @param size 切分大小
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteBatchByIds(Map, ProviderContext)
     */
    default int deleteBatchByIds(List<? extends Serializable> ids, int size) {
        if (size <= 0) {
            size = 1000;//默认1000
        }
        int sum = 0;
        int entitiesSize = ids.size();
        int maxIndex = entitiesSize / size + (entitiesSize % size == 0 ? 0 : 1);
        for (int i = 0; i < maxIndex; i++) {
            List<? extends Serializable> list = ids.subList(i * size, Math.min(i * size + size, entitiesSize));
            sum += deleteBatchByIds(list);
        }
        return sum;
    }


    /**
     * 根据 map 构建的条件来删除数据
     *
     * @param whereConditions 条件
     * @return 返回影响的行数
     */
    default int deleteByMap(Map<String, Object> whereConditions) {
        if (ObjectUtil.areNull(whereConditions) || whereConditions.isEmpty()) {
            throw FlexExceptions.wrap("deleteByMap is not allow empty map.");
        }
        return deleteByQuery(QueryWrapper.create().where(whereConditions));
    }

    /**
     * 根据条件来删除数据
     *
     * @param condition 条件
     * @return 返回影响的行数
     */
    default int deleteByCondition(QueryCondition condition) {
        return deleteByQuery(QueryWrapper.create().where(condition));
    }

    /**
     * 根据 query 构建的条件来数据吗
     *
     * @param queryWrapper query 条件
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteByQuery(Map, ProviderContext)
     */
    @DeleteProvider(type = EntitySqlProvider.class, method = "deleteByQuery")
    int deleteByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper);


    /**
     * 根据主键来更新数据，若 entity 属性数据为 null，该属性不会新到数据库
     *
     * @param entity 数据内容，必须包含有主键
     * @return 返回影响的行数
     */
    default int update(T entity) {
        return update(entity, true);
    }

    /**
     * 根据主键来更新数据到数据库
     *
     * @param entity      数据内容
     * @param ignoreNulls 是否忽略空内容字段
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#update(Map, ProviderContext)
     */
    @UpdateProvider(type = EntitySqlProvider.class, method = "update")
    int update(@Param(FlexConsts.ENTITY) T entity, @Param(FlexConsts.IGNORE_NULLS) boolean ignoreNulls);


    /**
     * 根据 map 构建的条件来更新数据
     *
     * @param entity 数据
     * @param map    where 条件内容
     * @return 返回影响的行数
     */
    default int updateByMap(T entity, Map<String, Object> map) {
        return updateByQuery(entity, QueryWrapper.create().where(map));
    }

    /**
     * 根据 condition 来更新数据
     *
     * @param entity    数据
     * @param condition 条件
     * @return 返回影响的行数
     */
    default int updateByCondition(T entity, QueryCondition condition) {
        return updateByQuery(entity, QueryWrapper.create().where(condition));
    }

    /**
     * 根据 condition 来更新数据
     *
     * @param entity      数据
     * @param ignoreNulls 是否忽略 null 数据，默认为 true
     * @param condition   条件
     * @return 返回影响的行数
     */
    default int updateByCondition(T entity, boolean ignoreNulls, QueryCondition condition) {
        return updateByQuery(entity, ignoreNulls, QueryWrapper.create().where(condition));
    }


    /**
     * 根据 query 构建的条件来更新数据
     *
     * @param entity       数据内容
     * @param queryWrapper query 条件
     * @return 返回影响的行数
     */

    default int updateByQuery(T entity, QueryWrapper queryWrapper) {
        return updateByQuery(entity, true, queryWrapper);
    }

    /**
     * 根据 query 构建的条件来更新数据
     *
     * @param entity       数据内容
     * @param ignoreNulls  是否忽略空值
     * @param queryWrapper query 条件
     * @see com.mybatisflex.core.provider.EntitySqlProvider#updateByQuery(Map, ProviderContext)
     */
    @UpdateProvider(type = EntitySqlProvider.class, method = "updateByQuery")
    int updateByQuery(@Param(FlexConsts.ENTITY) T entity, @Param(FlexConsts.IGNORE_NULLS) boolean ignoreNulls, @Param(FlexConsts.QUERY) QueryWrapper queryWrapper);


    /**
     * 根据主键来选择数据
     *
     * @param id 多个主键
     * @return entity
     * @see com.mybatisflex.core.provider.EntitySqlProvider#selectOneById(Map, ProviderContext)
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectOneById")
    Optional<T> selectOneById(@Param(FlexConsts.PRIMARY_VALUE) Serializable id);

    /**
     * 根据主键来选择数据
     *
     * @param id   id 主键
     * @param cast 转换器
     * @param <R>  需要对象泛型
     * @return 需要的对象
     */
    default <R> Optional<R> selectOneById(Serializable id, Function<T, R> cast) {
        Objects.requireNonNull(cast, "转换器不能为null");
        var result = selectOneById(id);
        return result.map(cast);
    }


    /**
     * 根据 map 构建的条件来查询数据
     *
     * @param whereConditions where 条件
     * @return entity 数据
     */
    default Optional<T> selectOneByMap(Map<String, Object> whereConditions) {
        return selectOneByQuery(QueryWrapper.create().where(whereConditions));
    }

    default <R> Optional<R> selectOneByMap(Map<String, Object> whereConditions, Function<T, R> cast) {
        return selectOneByQuery(QueryWrapper.create().where(whereConditions), cast);
    }


    /**
     * 根据 condition 来查询数据
     *
     * @param condition 条件
     * @return 1 条数据
     */
    default Optional<T> selectOneByCondition(QueryCondition condition) {
        return selectOneByQuery(QueryWrapper.create().where(condition));
    }

    default <R> Optional<R> selectOneByCondition(QueryCondition condition, Function<T, R> cast) {
        return selectOneByQuery(QueryWrapper.create().where(condition), cast);
    }


    /**
     * 根据 queryWrapper 构建的条件来查询 1 条数据
     *
     * @param queryWrapper query 条件
     * @return entity 数据
     */
    default Optional<T> selectOneByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper) {
        var entities = selectListByQuery(queryWrapper.limit(1));
        return (entities == null || entities.isEmpty()) ? Optional.empty() : Optional.ofNullable(entities.get(0));
    }

    default <R> Optional<R> selectOneByQuery(QueryWrapper queryWrapper, Function<T, R> cast) {
        var entities = selectListByQuery(queryWrapper.limit(1), cast);
        return (entities == null || entities.isEmpty()) ? Optional.empty() : Optional.ofNullable(entities.get(0));
    }


    /**
     * 根据 queryWrapper 构建的条件来查询 1 条数据
     *
     * @param queryWrapper query 条件
     * @param asType       接收类型
     * @return 数据内容
     */
    default <R> R selectOneByQueryAs(QueryWrapper queryWrapper, Class<R> asType) {
        try {
            MappedStatementTypes.setCurrentType(asType);
            List<R> entities = selectListByQueryAs(queryWrapper.limit(1), asType);
            return (entities == null || entities.isEmpty()) ? null : entities.get(0);
        } finally {
            MappedStatementTypes.clear();
        }
    }

    /**
     * 根据多个主键来查询多条数据
     *
     * @param ids 主键列表
     * @return 数据列表
     * @see com.mybatisflex.core.provider.EntitySqlProvider#selectListByIds(Map, ProviderContext)
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectListByIds")
    List<T> selectListByIds(@Param(FlexConsts.PRIMARY_VALUE) Collection<? extends Serializable> ids);

    /**
     * 根据多个主键来查询多条数据
     *
     * @param ids  主键列表
     * @param cast 转换器
     * @param <R>  需要的泛型
     * @return 数据列表
     */
    default <R> List<R> selectListByIds(Collection<? extends Serializable> ids, Function<T, R> cast) {
        var result = selectListByIds(ids);
        return result.stream().map(cast).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    /**
     * 根据 map 来构建查询条件，查询多条数据
     *
     * @param whereConditions 条件列表
     * @return 数据列表
     */
    default List<T> selectListByMap(Map<String, Object> whereConditions) {
        return selectListByQuery(QueryWrapper.create().where(whereConditions));
    }

    default <R> List<R> selectListByMap(Map<String, Object> whereConditions, Function<T, R> cast) {
        return selectListByQuery(QueryWrapper.create().where(whereConditions), cast);
    }


    /**
     * 根据 map 来构建查询条件，查询多条数据
     *
     * @param whereConditions 条件列表
     * @return 数据列表
     */
    default List<T> selectListByMap(Map<String, Object> whereConditions, int count) {
        return selectListByQuery(QueryWrapper.create().where(whereConditions).limit(count));
    }

    default <R> List<R> selectListByMap(Map<String, Object> whereConditions, int count, Function<T, R> cast) {
        return selectListByQuery(QueryWrapper.create().where(whereConditions).limit(count), cast);
    }


    /**
     * 根据 condition 来查询数据
     *
     * @param condition condition 条件
     * @return 数据列表
     */
    default List<T> selectListByCondition(QueryCondition condition) {
        return selectListByQuery(QueryWrapper.create().where(condition));
    }

    default <R> List<R> selectListByCondition(QueryCondition condition, Function<T, R> cast) {
        return selectListByQuery(QueryWrapper.create().where(condition), cast);
    }


    /**
     * 根据 condition 来查询数据
     *
     * @param condition condition 条件
     * @param count     数据量
     * @return 数据列表
     */
    default List<T> selectListByCondition(QueryCondition condition, int count) {
        return selectListByQuery(QueryWrapper.create().where(condition).limit(count));
    }

    default <R> List<R> selectListByCondition(QueryCondition condition, int count, Function<T, R> cast) {
        return selectListByQuery(QueryWrapper.create().where(condition).limit(count), cast);
    }


    /**
     * 根据 query 来构建条件查询数据列表
     *
     * @param queryWrapper 查询条件
     * @return 数据列表
     * @see com.mybatisflex.core.provider.EntitySqlProvider#selectListByQuery(Map, ProviderContext)
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectListByQuery")
    List<T> selectListByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper);

    /**
     * 根据 query 来构建条件查询数据列表
     *
     * @param queryWrapper 查询条件
     * @param cast         转换器
     * @param <R>          需要的类别
     * @return 数据类别
     */
    default <R> List<R> selectListByQuery(QueryWrapper queryWrapper, Function<T, R> cast) {
        List<T> result = selectListByQuery(queryWrapper);
        return result.stream().map(cast).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    default List<T> selectListByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper
            , Consumer<FieldQueryBuilder<T>>... consumers) {

        List<T> list = selectListByQuery(queryWrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        list.forEach(entity -> {
            for (Consumer<FieldQueryBuilder<T>> consumer : consumers) {
                FieldQueryBuilder<T> fieldQueryBuilder = new FieldQueryBuilder<>(entity);
                consumer.accept(fieldQueryBuilder);
                FieldQuery fieldQuery = fieldQueryBuilder.build();
                QueryWrapper childQuery = fieldQuery.getQueryWrapper();
                MetaObject entityMetaObject = SystemMetaObject.forObject(entity);
                Class<?> setterType = entityMetaObject.getSetterType(fieldQuery.getField());

                Class<?> mappingType = fieldQuery.getMappingType();
                if (mappingType == null) {
                    if (setterType.isAssignableFrom(Collection.class)) {
                        throw new IllegalStateException("Mapping Type can not be null for query Many.");
                    } else if (setterType.isArray()) {
                        mappingType = setterType.getComponentType();
                    } else {
                        mappingType = setterType;
                    }
                }

                Object value;
                try {
                    MappedStatementTypes.setCurrentType(mappingType);
                    if (setterType.isAssignableFrom(List.class)) {
                        value = selectListByQueryAs(childQuery, mappingType);
                    } else if (setterType.isAssignableFrom(Set.class)) {
                        value = selectListByQueryAs(childQuery, mappingType);
                        value = new HashSet<>((Collection<?>) value);
                    } else if (setterType.isArray()) {
                        value = selectListByQueryAs(childQuery, mappingType);
                        value = ((List<?>) value).toArray();
                    } else {
                        value = selectOneByQueryAs(childQuery, mappingType);
                    }
                } finally {
                    MappedStatementTypes.clear();
                }
                entityMetaObject.setValue(fieldQuery.getField(), value);
            }
        });

        return list;
    }


    /**
     * 根据 query 来构建条件查询数据列表，要求返回的数据为 asType
     * 这种场景一般用在 left join 时，有多出了 entity 本身的字段内容，可以转换为 dto、vo 等场景时
     *
     * @param queryWrapper 查询条件
     * @param asType       接收数据类型
     * @return 数据列表
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectListByQuery")
    <R> List<R> selectListByQueryAs(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper, Class<R> asType);


    default <R> List<R> selectListByQueryAs(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper, Class<R> asType
            , Consumer<FieldQueryBuilder<R>>... consumers) {
        List<R> list;
        try {
            MappedStatementTypes.setCurrentType(asType);
            list = selectListByQueryAs(queryWrapper, asType);
        } finally {
            MappedStatementTypes.clear();
        }

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        list.forEach(entity -> {
            for (Consumer<FieldQueryBuilder<R>> consumer : consumers) {
                FieldQueryBuilder<R> fieldQueryBuilder = new FieldQueryBuilder<>(entity);
                consumer.accept(fieldQueryBuilder);
                FieldQuery fieldQuery = fieldQueryBuilder.build();
                QueryWrapper childQuery = fieldQuery.getQueryWrapper();

                MetaObject entityMetaObject = SystemMetaObject.forObject(entity);
                Class<?> setterType = entityMetaObject.getSetterType(fieldQuery.getField());

                Class<?> mappingType = fieldQuery.getMappingType();
                if (mappingType == null) {
                    if (setterType.isAssignableFrom(Collection.class)) {
                        throw new IllegalStateException("Mapping Type can not be null for query Many.");
                    } else if (setterType.isArray()) {
                        mappingType = setterType.getComponentType();
                    } else {
                        mappingType = setterType;
                    }
                }

                Object value;
                try {
                    MappedStatementTypes.setCurrentType(mappingType);
                    if (setterType.isAssignableFrom(List.class)) {
                        value = selectListByQueryAs(childQuery, mappingType);
                    } else if (setterType.isAssignableFrom(Set.class)) {
                        value = selectListByQueryAs(childQuery, mappingType);
                        value = new HashSet<>((Collection<?>) value);
                    } else if (setterType.isArray()) {
                        value = selectListByQueryAs(childQuery, mappingType);
                        value = ((List<?>) value).toArray();
                    } else {
                        value = selectOneByQueryAs(childQuery, mappingType);
                    }
                } finally {
                    MappedStatementTypes.clear();
                }


                entityMetaObject.setValue(fieldQuery.getField(), value);
            }
        });

        return list;
    }


    /**
     * 查询全部数据
     *
     * @return 数据列表
     */
    default List<T> selectAll() {
        return selectListByQuery(QueryWrapper.create());
    }


    /**
     * 根据 queryWrapper 1 条数据
     * queryWrapper 执行的结果应该只有 1 列，例如 QueryWrapper.create().select(ACCOUNT.id).where...
     *
     * @param queryWrapper 查询包装器
     * @return 数据量
     */
    default Object selectObjectByQuery(QueryWrapper queryWrapper) {
        List<Object> objects = selectObjectListByQuery(queryWrapper.limit(1));
        return objects == null || objects.isEmpty() ? null : objects.get(0);
    }


    /**
     * 根据 queryWrapper 来查询数据列表
     * queryWrapper 执行的结果应该只有 1 列，例如 QueryWrapper.create().select(ACCOUNT.id).where...
     *
     * @param queryWrapper 查询包装器
     * @return 数据列表
     * @see EntitySqlProvider#selectObjectByQuery(Map, ProviderContext)
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectObjectByQuery")
    List<Object> selectObjectListByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper);


    /**
     * 对 {@link #selectObjectListByQuery(QueryWrapper)} 进行数据转换
     * 根据 queryWrapper 来查询数据列表
     * queryWrapper 执行的结果应该只有 1 列，例如 QueryWrapper.create().select(ACCOUNT.id).where...
     *
     * @param queryWrapper 查询包装器
     * @param asType       转换成的数据类型
     * @return 数据列表
     */
    default <R> List<R> selectObjectListByQueryAs(QueryWrapper queryWrapper, Class<R> asType) {
        List<Object> queryResults = selectObjectListByQuery(queryWrapper);
        if (queryResults == null || queryResults.isEmpty()) {
            return Collections.emptyList();
        }
        return queryResults.stream().map(it -> ConvertUtil.convert(it, asType)).toList();
    }


    /**
     * 查询数据量
     *
     * @param queryWrapper 查询包装器
     * @return 数据量
     */
    default long selectCountByQuery(QueryWrapper queryWrapper) {
        List<QueryColumn> selectColumns = CPI.getSelectColumns(queryWrapper);
        if (CollectionUtil.isEmpty(selectColumns)) {
            queryWrapper.select(count());
        }
        List<Object> objects = selectObjectListByQuery(queryWrapper);
        Object object = objects == null || objects.isEmpty() ? null : objects.get(0);
        if (object == null) {
            return 0;
        } else if (object instanceof Number) {
            return ((Number) object).longValue();
        } else {
            throw FlexExceptions.wrap("selectCountByQuery error, Can not get number value for queryWrapper: %s", queryWrapper);
        }
    }


    /**
     * 根据条件查询数据总量
     *
     * @param condition 条件
     * @return 数据量
     */
    default long selectCountByCondition(QueryCondition condition) {
        return selectCountByQuery(QueryWrapper.create().where(condition));
    }


    /**
     * 分页查询
     *
     * @param pageNumber   当前页码
     * @param pageSize     每页的数据量
     * @param queryWrapper 查询条件
     * @return 返回 Page 数据
     */
    default Page<T> paginate(int pageNumber, int pageSize, QueryWrapper queryWrapper) {
        Page<T> page = new Page<>(pageNumber, pageSize);
        return paginate(page, queryWrapper);
    }

    default <R> Page<R> paginate(int pageNumber, int pageSize, QueryWrapper queryWrapper, Function<T, R> cast) {
        Page<T> page = new Page<>(pageNumber, pageSize);
        return paginate(page, queryWrapper, cast);
    }


    /**
     * 根据条件分页查询
     *
     * @param pageNumber 当前页面
     * @param pageSize   每页的数据量
     * @param condition  查询条件
     * @return 返回 Page 数据
     */
    default Page<T> paginate(int pageNumber, int pageSize, QueryCondition condition) {
        Page<T> page = new Page<>(pageNumber, pageSize);
        return paginate(page, QueryWrapper.create().where(condition));
    }

    default <R> Page<R> paginate(int pageNumber, int pageSize, QueryCondition condition, Function<T, R> cast) {
        Page<T> page = new Page<>(pageNumber, pageSize);
        return paginate(page, QueryWrapper.create().where(condition), cast);
    }

    /**
     * 分页查询
     *
     * @param pageNumber   当前页码
     * @param pageSize     每页的数据量
     * @param totalRow     数据总量
     * @param queryWrapper 查询条件
     * @return 返回 Page 数据
     */
    default Page<T> paginate(int pageNumber, int pageSize, int totalRow, QueryWrapper queryWrapper) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, queryWrapper);
    }

    default <R> Page<R> paginate(int pageNumber, int pageSize, int totalRow, QueryWrapper queryWrapper, Function<T, R> cast) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, queryWrapper, cast);
    }


    /**
     * 根据条件分页查询
     *
     * @param pageNumber 当前页面
     * @param pageSize   每页的数据量
     * @param totalRow   数据总量
     * @param condition  查询条件
     * @return 返回 Page 数据
     */
    default Page<T> paginate(int pageNumber, int pageSize, int totalRow, QueryCondition condition) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, QueryWrapper.create().where(condition));
    }

    default <R> Page<R> paginate(int pageNumber, int pageSize, int totalRow, QueryCondition condition, Function<T, R> cast) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, QueryWrapper.create().where(condition), cast);
    }


    /**
     * 分页查询
     *
     * @param page         page，其包含了页码、每页的数据量，可能包含数据总量
     * @param queryWrapper 查询条件
     * @return page 数据
     */
    default Page<T> paginate(Page<T> page, QueryWrapper queryWrapper) {
        return paginateAs(page, queryWrapper, null);
    }


    default <R> Page<R> paginateAs(Page<R> page, QueryWrapper queryWrapper, Class<R> asType) {
        List<QueryColumn> selectColumns = CPI.getSelectColumns(queryWrapper);
        List<QueryOrderBy> orderBys = CPI.getOrderBys(queryWrapper);

        List<Join> joins = CPI.getJoins(queryWrapper);
        boolean removedJoins = true;
        // 只有 totalRow 小于 0 的时候才会去查询总量
        // 这样方便用户做总数缓存，而非每次都要去查询总量
        // 一般的分页场景中，只有第一页的时候有必要去查询总量，第二页以后是不需要的
        if (page.getTotalRow() < 0) {

            //移除 select
            CPI.setSelectColumns(queryWrapper, Collections.singletonList(count().as("total")));

            //移除 OrderBy
            if (CollectionUtil.isNotEmpty(orderBys)) {
                CPI.setOrderBys(queryWrapper, null);
            }


            //移除 left join
            if (joins != null && !joins.isEmpty()) {
                for (Join join : joins) {
                    if (!Join.TYPE_LEFT.equals(CPI.getJoinType(join))) {
                        removedJoins = false;
                        break;
                    }
                }
            } else {
                removedJoins = false;
            }

            if (removedJoins) {
                List<String> joinTables = new ArrayList<>();
                joins.forEach(join -> {
                    QueryTable joinQueryTable = CPI.getJoinQueryTable(join);
                    if (joinQueryTable != null && StringUtil.isNotBlank(joinQueryTable.getName())) {
                        joinTables.add(joinQueryTable.getName());
                    }
                });

                QueryCondition where = CPI.getWhereQueryCondition(queryWrapper);
                if (CPI.containsTable(where, CollectionUtil.toArrayString(joinTables))) {
                    removedJoins = false;
                }
            }

            if (removedJoins) {
                CPI.setJoins(queryWrapper, null);
            }


            long count = selectCountByQuery(queryWrapper);
            page.setTotalRow(count);
        }

        if (page.getTotalRow() == 0 || page.getPageNumber() > page.getTotalPage()) {
            return page;
        }

        //重置 selectColumns
        CPI.setSelectColumns(queryWrapper, selectColumns);

        //重置 orderBys
        if (CollectionUtil.isNotEmpty(orderBys)) {
            CPI.setOrderBys(queryWrapper, orderBys);
        }

        //重置 join
        if (removedJoins) {
            CPI.setJoins(queryWrapper, joins);
        }

        int offset = page.getPageSize() * (page.getPageNumber() - 1);
        queryWrapper.limit(offset, page.getPageSize());

        if (asType != null) {
            try {
                // 调用内部方法，不走代理，需要主动设置 MappedStatementType
                // fixed https://gitee.com/mybatis-flex/mybatis-flex/issues/I73BP6
                MappedStatementTypes.setCurrentType(asType);
                List<R> records = selectListByQueryAs(queryWrapper, asType);
                page.setRecords(records);
            } finally {
                MappedStatementTypes.clear();
            }
        } else {
            List<R> records = (List<R>) selectListByQuery(queryWrapper);
            page.setRecords(records);
        }
        return page;
    }

    default <R> Page<R> paginate(Page<T> page, QueryWrapper queryWrapper, Function<T, R> cast) {
        var r = paginate(page, queryWrapper);
        var records = r.getRecords().stream().map(cast).toList();
        return new Page<>(records, page.getPageNumber(), page.getPageSize(), page.getTotalRow());
    }
}
