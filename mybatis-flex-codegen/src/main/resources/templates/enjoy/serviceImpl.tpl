package #(globalConfig.serviceImplPackage);

import #(table.buildServiceImplImport());
import #(globalConfig.entityPackage).#(table.buildEntityClassName());
import #(globalConfig.mapperPackage).#(table.buildMapperClassName());
import org.springframework.stereotype.Service;

@Service
public class #(table.buildServiceClassName()) extends #(table.buildServiceImplName())<#(table.buildMapperClassName()), #(table.buildEntityClassName())> {

}