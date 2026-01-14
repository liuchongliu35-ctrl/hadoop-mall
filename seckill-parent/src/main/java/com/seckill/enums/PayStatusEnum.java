package com.seckill.enums;

public enum PayStatusEnum {
    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消");

    private final Integer code;
    private final String desc;

    PayStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PayStatusEnum getByCode(Integer code) {
        for (PayStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}