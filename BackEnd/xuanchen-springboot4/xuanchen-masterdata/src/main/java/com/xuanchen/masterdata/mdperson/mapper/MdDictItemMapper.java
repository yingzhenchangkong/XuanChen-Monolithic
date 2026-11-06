package com.xuanchen.masterdata.mdperson.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 人员导入用字典映射（只读）。
 * <p>
 * 主数据模块不依赖 system 模块，这里以只读 SQL 直接读取全局字典表 sys_dict_item，
 * 与迁移前 SysDictItemMapper.selectList(QueryWrapper.in("dict_code", codes)) 取数行为一致
 * （原实体未加逻辑删除注解，因此同样不过滤 del_flag）。
 *
 * @author XuanChen
 */
@Mapper
public interface MdDictItemMapper {

    /**
     * 按字典编码批量取字典项，列以驼峰别名返回，便于调用方按名取值。
     */
    @Select("""
            <script>
            SELECT dict_code AS dictCode,
                   dict_item_text AS dictItemText,
                   dict_item_value AS dictItemValue
            FROM sys_dict_item
            WHERE dict_code IN
            <foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>
            </script>
            """)
    List<Map<String, Object>> selectByCodes(@Param("codes") List<String> codes);
}
