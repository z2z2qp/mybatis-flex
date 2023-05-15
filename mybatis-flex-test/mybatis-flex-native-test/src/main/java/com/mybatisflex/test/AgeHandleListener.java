package com.mybatisflex.test;

import com.mybatisflex.annotation.InsertListener;

/**
 * 年龄处理监听器
 *
 * @author snow
 * @since 2023/4/28
 */
public class AgeHandleListener implements InsertListener<AgeAware> {

    @Override
    public void onInsert(AgeAware ageAware) {
//        if (entity instanceof AgeAware) {
//            AgeAware ageAware = (AgeAware) entity;
            int age = ageAware.getAge();
            if (age < 0) {
                ageAware.setAge(0);
            }
//        }
    }

    @Override
    public int order() {
        return 10;
    }
}
