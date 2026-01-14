package com.seckill.enums;

public enum ProductStatusEnum {
    OFFLINE(0, "下架"),
    ONLINE(1, "上架");

    private final Integer code;
    private final String desc;

    ProductStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProductStatusEnum getByCode(Integer code) {
        for (ProductStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}