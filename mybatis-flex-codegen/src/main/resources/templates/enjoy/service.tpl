package #(packageConfig.servicePackage);

import #(serviceConfig.buildSuperClassImport());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());
import #(packageConfig.mapperPackage).#(table.buildMapperClassName());
import org.springframework.stereotype.Service;

/**
 * #(table.getComment()) 服务层。
 *
#if(javadocConfig.getAuthor())
 * @author #(javadocConfig.getAuthor())
#end
#if(javadocConfig.getSince())
 * @since #(javadocConfig.getSince())
#end
 */
@Service
public class #(table.buildServiceClassName()) extends #(serviceConfig.buildSuperClassName())<#(table.buildMapperClassName()), #(table.buildEntityClassName())> {

}
