package ${package.Mapper};

import ${superMapperClassPackage};
import ${package.Entity}.${entity};
import org.springframework.stereotype.Repository;

/**
* Mapper --> ${table.comment!}
*
* @author ${author}
* @date ${date}
*/
@Repository
<#if kotlin>
interface ${table.mapperName} : ${superMapperClass}<${entity}> {
<#else>
public interface ${table.mapperName} extends ${superMapperClass}<${entity}> {
</#if>

<#list mapperMethodList as m>
    /**
    * generate by ${m.indexName}
    *
    <#list m.tableFieldList as f>
        * @param ${f.propertyName} ${f.comment}
    </#list>
    */
    ${m.method}
</#list>
}
