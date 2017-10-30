package com.company.biz.model;

import java.io.Serializable;
import java.util.Date;

/**
 *  天马系统的行政区域表
 */
public class CtCantonInfo implements Serializable{
    private static final long serialVersionUID = -7670351242139832576L;
    private Integer id;

    private String cantonCode;

    private String cantonName;

    private Integer parent;

    private Integer type;

    private String province;

    private String city;

    private String county;

    private String township;

    private String village;

    private Integer lockVersion;

    private Date createdTime;

    private Date updatedTime;

    private Integer creatorId;

    private String creatorName;

    private Integer updaterId;

    private String updaterName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCantonCode() {
        return cantonCode;
    }

    public void setCantonCode(String cantonCode) {
        this.cantonCode = cantonCode == null ? null : cantonCode.trim();
    }

    public String getCantonName() {
        return cantonName;
    }

    public void setCantonName(String cantonName) {
        this.cantonName = cantonName == null ? null : cantonName.trim();
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province == null ? null : province.trim();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city == null ? null : city.trim();
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county == null ? null : county.trim();
    }

    public String getTownship() {
        return township;
    }

    public void setTownship(String township) {
        this.township = township == null ? null : township.trim();
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village == null ? null : village.trim();
    }

    public Integer getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Integer lockVersion) {
        this.lockVersion = lockVersion;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName == null ? null : creatorName.trim();
    }

    public Integer getUpdaterId() {
        return updaterId;
    }

    public void setUpdaterId(Integer updaterId) {
        this.updaterId = updaterId;
    }

    public String getUpdaterName() {
        return updaterName;
    }

    public void setUpdaterName(String updaterName) {
        this.updaterName = updaterName == null ? null : updaterName.trim();
    }
}