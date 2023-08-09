package com.mybatisfle.test.mybatisflexspringbootseata.service;


import com.mybatisfle.test.mybatisflexspringbootseata.entity.AccountTbl;
import com.mybatisfle.test.mybatisflexspringbootseata.entity.table.AccountTblTableDef;
import com.mybatisfle.test.mybatisflexspringbootseata.mapper.AccountTblMapper;
import com.mybatisfle.test.mybatisflexspringbootseata.mapper.OrderTblMapper;
import com.mybatisfle.test.mybatisflexspringbootseata.mapper.StockTblMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TestService {

    @Autowired
    AccountTblMapper accountTblMapper;

    @Autowired
    OrderTblMapper orderTblMapper;

    @Autowired
    StockTblMapper stockTblMapper;

    //    @Transactional
    public boolean buy() {
//        DataSourceKey.use("accountdb");
        QueryWrapper account = new QueryWrapper();
        account.where(AccountTblTableDef.ACCOUNT_TBL.USER_ID.eq("1001"));
        Optional<AccountTbl> accountTbl = accountTblMapper.selectOneByQuery(account);
        accountTbl.ifPresent(it -> {
            it.setMoney(it.getMoney() - 5);
            accountTblMapper.update(it);
        });
//        DataSourceKey.use("stockdb");
//        QueryWrapper stock = new QueryWrapper();
//        stock.where("id=1");
//        StockTbl stockTbl = stockTblMapper.selectOneByQuery(stock);
//        stockTbl.setCount(stockTbl.getCount() - 1);
//        stockTblMapper.update(stockTbl);
//        DataSourceKey.use("orderdb");
//        OrderTbl orderTbl = new OrderTbl();
//        orderTbl.setCount(5);
//        orderTbl.setMoney(5);
//        orderTbl.setUserId(accountTbl.getUserId());
//        orderTbl.setCount(1);
//        orderTbl.setCommodityCode(stockTbl.getCommodityCode());
//        orderTblMapper.insert(orderTbl);
        return true;
    }
}
