package ${package.Service};

import ${package.Entity}.${entity};
import ${superServiceClassPackage};

/**
* Service --> ${table.comment!}
*
* @author ${author}
* @date ${date}
*/
<#if kotlin>
    interface ${table.serviceName} : ${superServiceClass}<${entity}>
<#else>
    public interface ${table.serviceName} extends ${superServiceClass}<${entity}> {

    }
</#if>
