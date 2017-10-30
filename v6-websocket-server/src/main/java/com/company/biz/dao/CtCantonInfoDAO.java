package com.company.biz.dao;

import com.company.biz.model.CtCantonInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public interface CtCantonInfoDAO {

    /**
     * 获取该区域下下级区域
     * @param ctCantonId
     * @return
     */
//    List<CtCantonInfo> selectCantonsByIdDown(@Param("id")  Integer ctCantonId);

    /**
     * 获取该区域下上级区域
     * @param ctCantonId
     * @return
     */
    List<CtCantonInfo> selectCantonsByIdUp(@Param("id")  Integer ctCantonId);

    CtCantonInfo selectCantonsById(@Param("id")  Integer ctCantonId);

    /**
     * 根据区域名称查询区域
     */
    CtCantonInfo getCantonByName(String cantonName);

    CtCantonInfo getCantonByNameAndType(@Param("cantonName") String cantonName , @Param("type") Integer cantonType,
                                        @Param("parent") Integer parentId);

    List<CtCantonInfo> queryCtCantonInfoListByIdList(Map<String ,Object> params);
}
