package #(packageConfig.serviceImplPackage);

import #(serviceImplConfig.buildSuperClassImport());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());
import #(packageConfig.mapperPackage).#(table.buildMapperClassName());
#if(table.getGlobalConfig().isServiceGenerateEnable())
import #(packageConfig.servicePackage).#(table.buildServiceClassName());
#end
import org.springframework.stereotype.Service;

/**
 * #(table.getComment()) 服务层实现。
 *
 * @author #(javadocConfig.getAuthor())
 * @since #(javadocConfig.getSince())
 */
@Service
class #(table.buildServiceImplClassName()) : #(serviceImplConfig.buildSuperClassName())<#(table.buildMapperClassName()), #(table.buildEntityClassName())>()#if(table.getGlobalConfig().isServiceGenerateEnable()), #(table.buildServiceClassName())#end {}
