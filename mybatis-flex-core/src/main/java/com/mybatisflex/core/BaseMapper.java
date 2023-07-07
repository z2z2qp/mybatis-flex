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
package com.mybatisflex.core;

import com.mybatisflex.core.constant.FuncName;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.field.FieldQueryBuilder;
import com.mybatisflex.core.mybatis.MappedStatementTypes;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.provider.EntitySqlProvider;
import com.mybatisflex.core.query.*;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.cursor.Cursor;

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
     * @see com.mybatisflex.core.provider.EntitySqlProvider#insert(Map,
     *      ProviderContext)
     */
    @InsertProvider(type = EntitySqlProvider.class, method = "insert")
    int insert(@Param(FlexConsts.ENTITY) T entity, @Param(FlexConsts.IGNORE_NULLS) boolean ignoreNulls);

    default int insertWithPk(T entity) {
        return insertWithPk(entity, true);
    }

    /**
     * 带有主键的插入，此时 entity 不会经过主键生成器生成主键
     *
     * @param entity      带有主键的实体类
     * @param ignoreNulls 是否忽略 null 值
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#insertWithPk(Map,
     *      ProviderContext)
     */
    @InsertProvider(type = EntitySqlProvider.class, method = "insertWithPk")
    int insertWithPk(@Param(FlexConsts.ENTITY) T entity, @Param(FlexConsts.IGNORE_NULLS) boolean ignoreNulls);

    /**
     * 批量插入 entity 数据，只会根据第一条数据来构建插入的字段内容
     *
     * @param entities 插入的数据列表
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#insertBatch(Map,
     *      ProviderContext)
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
            size = 1000;// 默认1000
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
        return insertOrUpdate(entity, false);
    }

    /**
     * 新增 或者 更新，若主键有值，则更新，若没有主键值，则插入
     *
     * @param entity      实体类
     * @param ignoreNulls 是否忽略 null 值
     * @return 返回影响的行数
     */
    default int insertOrUpdate(T entity, boolean ignoreNulls) {
        TableInfo tableInfo = TableInfoFactory.ofEntityClass(entity.getClass());
        Object[] pkArgs = tableInfo.buildPkSqlArgs(entity);
        if (pkArgs.length == 0 || pkArgs[0] == null) {
            return insert(entity, ignoreNulls);
        } else {
            return update(entity, ignoreNulls);
        }
    }

    /**
     * 根据 id 删除数据
     * 如果是多个主键的情况下，需要传入数组 new Object[]{100,101}
     *
     * @param id 主键数据
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteById(Map,
     *      ProviderContext)
     */
    @DeleteProvider(type = EntitySqlProvider.class, method = "deleteById")
    int deleteById(@Param(FlexConsts.PRIMARY_VALUE) Serializable id);

    /**
     * 根据多个 id 批量删除数据
     *
     * @param ids ids 列表
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteBatchByIds(Map,
     *      ProviderContext)
     */
    @DeleteProvider(type = EntitySqlProvider.class, method = "deleteBatchByIds")
    int deleteBatchByIds(@Param(FlexConsts.PRIMARY_VALUE) Collection<? extends Serializable> ids);

    /**
     * 根据多个 id 批量删除数据
     *
     * @param ids  ids 列表
     * @param size 切分大小
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteBatchByIds(Map,
     *      ProviderContext)
     */
    default int deleteBatchByIds(List<? extends Serializable> ids, int size) {
        if (size <= 0) {
            size = 1000;// 默认1000
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
        if (whereConditions == null || whereConditions.isEmpty()) {
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
        return deleteByQuery(QueryWrapper.create().where(condition));
    }

    /**
     * 根据 query 构建的条件来数据吗
     *
     * @param queryWrapper query 条件
     * @return 返回影响的行数
     * @see com.mybatisflex.core.provider.EntitySqlProvider#deleteByQuery(Map,
     *      ProviderContext)
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
     * @see com.mybatisflex.core.provider.EntitySqlProvider#update(Map,
     *      ProviderContext)
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
        if (map == null || map.isEmpty()) {
            throw FlexExceptions.wrap("updateByMap is not allow empty map.");
        }
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
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
     * @see com.mybatisflex.core.provider.EntitySqlProvider#updateByQuery(Map,
     *      ProviderContext)
     */
    @UpdateProvider(type = EntitySqlProvider.class, method = "updateByQuery")
    int updateByQuery(@Param(FlexConsts.ENTITY) T entity, @Param(FlexConsts.IGNORE_NULLS) boolean ignoreNulls,
            @Param(FlexConsts.QUERY) QueryWrapper queryWrapper);

    /**
     * 执行类似 update table set field=field+1 where ... 的场景
     *
     * @param fieldName    字段名
     * @param value        值（ >=0 加，小于 0 减）
     * @param queryWrapper 条件
     * @see EntitySqlProvider#updateNumberAddByQuery(Map, ProviderContext)
     */
    @UpdateProvider(type = EntitySqlProvider.class, method = "updateNumberAddByQuery")
    int updateNumberAddByQuery(@Param(FlexConsts.FIELD_NAME) String fieldName, @Param(FlexConsts.VALUE) Number value,
            @Param(FlexConsts.QUERY) QueryWrapper queryWrapper);

    /**
     * 执行类似 update table set field=field+1 where ... 的场景
     *
     * @param column       字段名
     * @param value        值（ >=0 加，小于 0 减）
     * @param queryWrapper 条件
     * @see EntitySqlProvider#updateNumberAddByQuery(Map, ProviderContext)
     */
    default int updateNumberAddByQuery(QueryColumn column, Number value, QueryWrapper queryWrapper) {
        if (value == null) {
            throw FlexExceptions.wrap("value can not be null.");
        }
        return updateNumberAddByQuery(column.getName(), value, queryWrapper);
    }

    /**
     * 执行类似 update table set field=field+1 where ... 的场景
     *
     * @param fn           字段名
     * @param value        值（ >=0 加，小于 0 减）
     * @param queryWrapper 条件
     * @see EntitySqlProvider#updateNumberAddByQuery(Map, ProviderContext)
     */
    default int updateNumberAddByQuery(LambdaGetter<T> fn, Number value, QueryWrapper queryWrapper) {
        if (value == null) {
            throw FlexExceptions.wrap("value can not be null.");
        }

        TableInfo tableInfo = TableInfoFactory.ofMapperClass(ClassUtil.getUsefulClass(getClass()));
        String column = tableInfo.getColumnByProperty(LambdaUtil.getFieldName(fn));
        return updateNumberAddByQuery(column, value, queryWrapper);
    }

    /**
     * 根据主键来选择数据
     *
     * @param id 多个主键
     * @return entity
     * @see com.mybatisflex.core.provider.EntitySqlProvider#selectOneById(Map,
     *      ProviderContext)
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
        if (whereConditions == null || whereConditions.isEmpty()) {
            throw FlexExceptions.wrap("map can not be null or empty.");
        }
        return selectOneByQuery(QueryWrapper.create().where(whereConditions).limit(1));
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
        return selectOneByQuery(QueryWrapper.create().where(condition).limit(1));
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
    default Optional<T> selectOneByQuery(QueryWrapper queryWrapper) {
        return Optional.ofNullable(MapperUtil.getSelectOneResult(selectListByQuery(queryWrapper)));
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
        return MapperUtil.getSelectOneResult(selectListByQueryAs(queryWrapper, asType));
    }

    /**
     * 根据 map 构建的条件来查询数据
     *
     * @param map where 条件
     * @return entity 数据
     */
    default T selectOneWithRelationsByMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            throw FlexExceptions.wrap("map can not be null or empty.");
        }
        return selectOneWithRelationsByQuery(QueryWrapper.create().where(map).limit(1));
    }

    /**
     * 根据 condition 来查询数据
     *
     * @param condition 条件
     * @return 1 条数据
     */
    default T selectOneWithRelationsByCondition(QueryCondition condition) {
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
        return selectOneWithRelationsByQuery(QueryWrapper.create().where(condition).limit(1));
    }

    /**
     * 根据 queryWrapper 构建的条件来查询 1 条数据
     *
     * @param queryWrapper query 条件
     * @return entity 数据
     */
    default T selectOneWithRelationsByQuery(QueryWrapper queryWrapper) {
        return MapperUtil.queryRelations(this, MapperUtil.getSelectOneResult(selectListByQuery(queryWrapper)));
    }

    /**
     * 根据 queryWrapper 构建的条件来查询 1 条数据
     *
     * @param queryWrapper query 条件
     * @param asType       接收类型
     * @return 数据内容
     */
    default <R> R selectOneWithRelationsByQueryAs(QueryWrapper queryWrapper, Class<R> asType) {
        return MapperUtil.queryRelations(this,
                MapperUtil.getSelectOneResult(selectListByQueryAs(queryWrapper, asType)));
    }

    /**
     * 根据多个主键来查询多条数据
     *
     * @param ids 主键列表
     * @return 数据列表
     * @see com.mybatisflex.core.provider.EntitySqlProvider#selectListByIds(Map,
     *      ProviderContext)
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
     * @param map 条件列表
     * @return 数据列表
     */
    default List<T> selectListByMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            throw FlexExceptions.wrap("map can not be null or empty.");
        }
        return selectListByQuery(QueryWrapper.create().where(map));
    }

    default <R> List<R> selectListByMap(Map<String, Object> whereConditions, Function<T, R> cast) {
        return selectListByQuery(QueryWrapper.create().where(whereConditions), cast);
    }

    /**
     * 根据 map 来构建查询条件，查询多条数据
     *
     * @param map 条件列表
     * @return 数据列表
     */
    default List<T> selectListByMap(Map<String, Object> map, int count) {
        if (map == null || map.isEmpty()) {
            throw FlexExceptions.wrap("map can not be null or empty.");
        }
        return selectListByQuery(QueryWrapper.create().where(map).limit(count));
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
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
     * @see com.mybatisflex.core.provider.EntitySqlProvider#selectListByQuery(Map,
     *      ProviderContext)
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

    default List<T> selectListByQuery(QueryWrapper queryWrapper, Consumer<FieldQueryBuilder<T>>... consumers) {

        List<T> list = selectListByQuery(queryWrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        MapperUtil.queryFields(this, list, consumers);

        return list;
    }

    /**
     * 根据 query 来构建条件查询游标数据 Cursor
     * 该方法必须在事务中才能正常使用，非事务下无法获取数据
     *
     * @param queryWrapper 查询条件
     * @return 游标数据 Cursor
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectListByQuery")
    Cursor<T> selectCursorByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper);

    /**
     * 根据 query 来构建条件查询 Row
     *
     * @param queryWrapper 查询条件
     * @return Row
     */
    @SelectProvider(type = EntitySqlProvider.class, method = "selectListByQuery")
    List<Row> selectRowsByQuery(@Param(FlexConsts.QUERY) QueryWrapper queryWrapper);

    /**
     * 根据 query 来构建条件查询数据列表，要求返回的数据为 asType
     * 这种场景一般用在 left join 时，有多出了 entity 本身的字段内容，可以转换为 dto、vo 等场景时
     *
     * @param queryWrapper 查询条件
     * @param asType       接收数据类型
     * @return 数据列表
     */
    default <R> List<R> selectListByQueryAs(QueryWrapper queryWrapper, Class<R> asType) {
        if (Number.class.isAssignableFrom(asType)
                || String.class == asType) {
            return selectObjectListByQueryAs(queryWrapper, asType);
        }

        if (Map.class.isAssignableFrom(asType)) {
            return (List<R>) selectRowsByQuery(queryWrapper);
        }

        try {
            MappedStatementTypes.setCurrentType(asType);
            return (List<R>) selectListByQuery(queryWrapper);
        } finally {
            MappedStatementTypes.clear();
        }
    }

    /**
     * 根据 query 来构建条件查询数据列表，要求返回的数据为 asType
     *
     * @param queryWrapper 查询条件
     * @param asType       接收的数据类型
     * @param consumers    字段查询
     * @return 数据列表
     */
    default <R> List<R> selectListByQueryAs(QueryWrapper queryWrapper, Class<R> asType,
            Consumer<FieldQueryBuilder<R>>... consumers) {
        List<R> list = selectListByQueryAs(queryWrapper, asType);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        } else {
            MapperUtil.queryFields(this, list, consumers);
            return list;
        }
    }

    /**
     * 查询 entity 及其 relation 注解字段
     *
     * @param queryWrapper 查询条件
     */
    default List<T> selectListWithRelationsByQuery(QueryWrapper queryWrapper) {
        return MapperUtil.queryRelations(this, selectListByQuery(queryWrapper));
    }

    /**
     * 查询 entity 及其 relation 注解字段
     *
     * @param queryWrapper 查询条件
     * @param asType       要求返回的数据类型
     * @return 数据列表
     */
    default <R> List<R> selectListWithRelationsByQueryAs(QueryWrapper queryWrapper, Class<R> asType) {
        if (Number.class.isAssignableFrom(asType)
                || String.class == asType) {
            return selectObjectListByQueryAs(queryWrapper, asType);
        }

        if (Map.class.isAssignableFrom(asType)) {
            return (List<R>) selectRowsByQuery(queryWrapper);
        }

        try {
            MappedStatementTypes.setCurrentType(asType);
            return MapperUtil.queryRelations(this, (List<R>) selectListByQuery(queryWrapper));
        } finally {
            MappedStatementTypes.clear();
        }
    }

    /**
     * 查询 entity 及其 relation 注解字段
     *
     * @param queryWrapper 查询条件
     * @param asType       返回的类型
     * @param consumers    字段查询
     * @return 数据列表
     */
    default <R> List<R> selectListWithRelationsByQueryAs(QueryWrapper queryWrapper, Class<R> asType,
            Consumer<FieldQueryBuilder<R>>... consumers) {
        List<R> list = selectListByQueryAs(queryWrapper, asType);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        } else {
            MapperUtil.queryRelations(this, list);
            MapperUtil.queryFields(this, list, consumers);
            return list;
        }
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
     * 查询全部数据，及其 relation 字段内容
     *
     * @return 数据列表
     */
    default List<T> selectAllWithRelations() {
        return MapperUtil.queryRelations(this, selectListByQuery(QueryWrapper.create()));
    }

    /**
     * 根据 queryWrapper 1 条数据
     * queryWrapper 执行的结果应该只有 1 列，例如
     * QueryWrapper.create().select(ACCOUNT.id).where...
     *
     * @param queryWrapper 查询包装器
     * @return 数据量
     */
    default Object selectObjectByQuery(QueryWrapper queryWrapper) {
        return MapperUtil.getSelectOneResult(selectObjectListByQuery(queryWrapper));
    }

    /**
     * 根据 queryWrapper 1 条数据
     * queryWrapper 执行的结果应该只有 1 列，例如
     * QueryWrapper.create().select(ACCOUNT.id).where...
     *
     * @param queryWrapper 查询包装器
     * @param asType       转换成的数据类型
     * @return 数据量
     */
    default <R> R selectObjectByQueryAs(QueryWrapper queryWrapper, Class<R> asType) {
        return MapperUtil.getSelectOneResult(selectObjectListByQueryAs(queryWrapper, asType));
    }

    /**
     * 根据 queryWrapper 来查询数据列表
     * queryWrapper 执行的结果应该只有 1 列，例如
     * QueryWrapper.create().select(ACCOUNT.id).where...
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
     * queryWrapper 执行的结果应该只有 1 列，例如
     * QueryWrapper.create().select(ACCOUNT.id).where...
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
        try {
            List<Object> objects;
            if (CollectionUtil.isEmpty(selectColumns)) {
                // 未设置 COUNT(...) 列，默认使用 COUNT(*) 查询
                queryWrapper.select(count());
                objects = selectObjectListByQuery(queryWrapper);
            } else if (selectColumns.get(0) instanceof FunctionQueryColumn) {
                // COUNT 函数必须在第一列
                if (!FuncName.COUNT.equalsIgnoreCase(
                        ((FunctionQueryColumn) selectColumns.get(0)).getFnName())) {
                    // 第一个查询列不是 COUNT 函数，使用 COUNT(*) 替换所有的查询列
                    queryWrapper.select(count());
                }
                // 第一个查询列是 COUNT 函数，可以使用 COUNT(1)、COUNT(列名) 代替默认的 COUNT(*)
                objects = selectObjectListByQuery(queryWrapper);
            } else {
                // 查询列中的第一列不是 COUNT 函数
                if (MapperUtil.hasDistinct(selectColumns)) {
                    // 查询列中包含 DISTINCT 去重
                    // 使用子查询 SELECT COUNT(*) FROM (SELECT DISTINCT ...) AS `t`
                    objects = selectObjectListByQuery(MapperUtil.rawCountQueryWrapper(queryWrapper));
                } else {
                    // 使用 COUNT(*) 替换所有的查询列
                    queryWrapper.select(count());
                    objects = selectObjectListByQuery(queryWrapper);
                }
            }
            return MapperUtil.getLongNumber(objects);
        } finally {
            // fixed https://github.com/mybatis-flex/mybatis-flex/issues/49
            CPI.setSelectColumns(queryWrapper, selectColumns);
        }
    }

    /**
     * 根据条件查询数据总量
     *
     * @param condition 条件
     * @return 数据量
     */
    default long selectCountByCondition(QueryCondition condition) {
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
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
    default Page<T> paginateWithRelations(int pageNumber, int pageSize, QueryWrapper queryWrapper) {
        Page<T> page = new Page<>(pageNumber, pageSize);
        return paginateWithRelations(page, queryWrapper);
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

    default Page<T> paginateWithRelations(int pageNumber, int pageSize, QueryCondition condition) {
        Page<T> page = new Page<>(pageNumber, pageSize);
        return paginateWithRelations(page, QueryWrapper.create().where(condition));
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

    default <R> Page<R> paginate(int pageNumber, int pageSize, int totalRow, QueryWrapper queryWrapper,
            Function<T, R> cast) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, queryWrapper, cast);
    }
    default Page<T> paginateWithRelations(int pageNumber, int pageSize, int totalRow, QueryWrapper queryWrapper) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginateWithRelations(page, queryWrapper);
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
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, QueryWrapper.create().where(condition));
    }

    default <R> Page<R> paginate(int pageNumber, int pageSize, int totalRow, QueryCondition condition,
            Function<T, R> cast) {
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginate(page, QueryWrapper.create().where(condition), cast);
    }

    default Page<T> paginateWithRelations(int pageNumber, int pageSize, int totalRow, QueryCondition condition) {
        if (condition == null) {
            throw FlexExceptions.wrap("condition can not be null.");
        }
        Page<T> page = new Page<>(pageNumber, pageSize, totalRow);
        return paginateWithRelations(page, QueryWrapper.create().where(condition));
    }


    /**
     * 分页查询
     *
     * @param page         page，其包含了页码、每页的数据量，可能包含数据总量
     * @param queryWrapper 查询条件
     * @return page 数据
     */
    @SuppressWarnings("unchecked")
    default Page<T> paginate(Page<T> page, QueryWrapper queryWrapper, Consumer<FieldQueryBuilder<T>>... consumers) {
        return paginateAs(page, queryWrapper, null, consumers);
    }

    default Page<T> paginateWithRelations(Page<T> page, QueryWrapper queryWrapper, Consumer<FieldQueryBuilder<T>>... consumers) {
        return doPaginate(page, queryWrapper, null, true, consumers);
    }


    default <R> Page<R> paginateAs(Page<R> page, QueryWrapper queryWrapper, Class<R> asType, Consumer<FieldQueryBuilder<R>>... consumers) {
        return doPaginate(page, queryWrapper, asType, false, consumers);
    }

    default <R> Page<R> paginateWithRelationsAs(Page<R> page, QueryWrapper queryWrapper, Class<R> asType, Consumer<FieldQueryBuilder<R>>... consumers) {
        return doPaginate(page, queryWrapper, asType, true, consumers);
    }


    default <R> Page<R> doPaginate(Page<R> page, QueryWrapper queryWrapper, Class<R> asType, boolean withRelations, Consumer<FieldQueryBuilder<R>>... consumers) {
        try {
            // 只有 totalRow 小于 0 的时候才会去查询总量
            // 这样方便用户做总数缓存，而非每次都要去查询总量
            // 一般的分页场景中，只有第一页的时候有必要去查询总量，第二页以后是不需要的
            if (page.getTotalRow() < 0) {
                QueryWrapper countQueryWrapper;
                if (page.needOptimizeCountQuery()) {
                    countQueryWrapper = MapperUtil.optimizeCountQueryWrapper(queryWrapper);
                } else {
                    countQueryWrapper = MapperUtil.rawCountQueryWrapper(queryWrapper);
                }
                page.setTotalRow(selectCountByQuery(countQueryWrapper));
            }

            if (page.isEmpty()) {
                return page;
            }

            queryWrapper.limit(page.offset(), page.getPageSize());

            List<R> records;
            if (asType != null) {
                records = selectListByQueryAs(queryWrapper, asType);
            } else {
                records = (List<R>) selectListByQuery(queryWrapper);
            }

            if (withRelations) {
                MapperUtil.queryRelations(this, records);
            }

            MapperUtil.queryFields(this, records, consumers);
            page.setRecords(records);

            return page;

        } finally {
            // 将之前设置的 limit 清除掉
            // 保险起见把重置代码放到 finally 代码块中
            CPI.setLimitRows(queryWrapper, null);
            CPI.setLimitOffset(queryWrapper, null);
        }
    }

    default <R> Page<R> paginate(Page<T> page, QueryWrapper queryWrapper, Function<T, R> cast) {
        var r = paginate(page, queryWrapper);
        var records = r.getRecords().stream().map(cast).toList();
        return new Page<>(records, page.getPageNumber(), page.getPageSize(), page.getTotalRow());
    }

}
