package ${package.Entity};

<#list importEntityPackages as pkg>
import ${pkg};
</#list>
<#-- 类型标记：按字段类型补齐 MP 默认模板不生成的 import -->
<#assign hasLocalDateTime=false hasLocalDate=false hasLocalTime=false hasTableLogic=false/>
<#list table.fields as f>
    <#if f.propertyType == "LocalDateTime"><#assign hasLocalDateTime=true/></#if>
    <#if f.propertyType == "LocalDate"><#assign hasLocalDate=true/></#if>
    <#if f.propertyType == "LocalTime"><#assign hasLocalTime=true/></#if>
    <#if f.name == "del_flag"><#assign hasTableLogic=true/></#if>
</#list>
<#if hasLocalDateTime || hasLocalDate || hasLocalTime>
import com.fasterxml.jackson.annotation.JsonFormat;
</#if>
<#if hasTableLogic>
import com.baomidou.mybatisplus.annotation.TableLogic;
</#if>
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;

/**
* 实体类 --> ${table.comment!}
*
* @author ${author}
* @date ${date}
*/
<#list entityClassAnnotations as an>
    ${an.displayName}
</#list>
<#if superEntityClass??>
    public class ${entity} extends ${superEntityClass}<#if activeRecord><${entity}></#if> {
<#elseif activeRecord>
    public class ${entity} extends Model<${entity}> {
<#elseif entitySerialVersionUID>
    public class ${entity} implements Serializable {
<#else>
    public class ${entity} {
</#if>
<#if entitySerialVersionUID>

    private static final long serialVersionUID = 1L;
</#if>
<#-- 审计字段不进 Excel 导入导出 -->
<#assign auditFields = ["create_by", "create_time", "update_by", "update_time", "del_flag"]/>
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>
    <#if field.keyFlag>
        <#assign keyPropertyName="${field.propertyName}"/>
    </#if>

    <#if field.comment!?length gt 0>
        /**
        * ${field.comment}
        */
    </#if>
    <#list field.annotationAttributesList as an>
        ${an.displayName}
    </#list>
    <#-- Excel 注解：主键与审计字段忽略，其余以字段注释作为表头 -->
    <#if field.keyFlag || auditFields?seq_contains(field.name)>
    @ExcelIgnore
    <#else>
        <#assign excelHead = (field.comment!?length gt 0)?then(field.comment, field.name)?replace("\"", "'")/>
    @ExcelProperty(value = "${excelHead}")
    </#if>
    <#if field.name == "del_flag">
    @TableLogic
    </#if>
    <#if field.propertyType == "LocalDateTime">
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private ${field.propertyType} ${field.propertyName};
    <#elseif field.propertyType == "LocalDate">
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private ${field.propertyType} ${field.propertyName};
    <#elseif field.propertyType == "LocalTime">
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
        private ${field.propertyType} ${field.propertyName};
    <#else>
        private ${field.propertyType} ${field.propertyName};
    </#if>
</#list>
<#------------  END 字段循环遍历  ---------->
<#if !entityLombokModel>
    <#list table.fields as field>
        <#if field.propertyType == "boolean">
            <#assign getprefix="is"/>
        <#else>
            <#assign getprefix="get"/>
        </#if>

        public ${field.propertyType} ${getprefix}${field.capitalName}() {
        return ${field.propertyName};
        }

        <#if chainModel>
            public ${entity} set${field.capitalName}(${field.propertyType} ${field.propertyName}) {
        <#else>
            public void set${field.capitalName}(${field.propertyType} ${field.propertyName}) {
        </#if>
        this.${field.propertyName} = ${field.propertyName};
        <#if chainModel>
            return this;
        </#if>
        }
    </#list>
</#if>
<#if entityColumnConstant>
    <#list table.fields as field>

        public static final String ${field.name?upper_case} = "${field.name}";
    </#list>
</#if>
<#if activeRecord>

    @Override
    public Serializable pkVal() {
    <#if keyPropertyName??>
        return this.${keyPropertyName};
    <#else>
        return null;
    </#if>
    }
</#if>
<#if !entityLombokModel && entityToString>

    @Override
    public String toString() {
    return "${entity}{" +
    <#list table.fields as field>
        <#if field_index==0>
            "${field.propertyName} = " + ${field.propertyName} +
        <#else>
            ", ${field.propertyName} = " + ${field.propertyName} +
        </#if>
    </#list>
    "}";
    }
</#if>

}
