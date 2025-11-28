package com.thumbby.bean;

import lombok.Getter;

@Getter
public class CoderCommonRespondBean {
    
    public enum ResponseCode {
        SUCCESS("success"),
        FAIL("error");
        
        private final String code;
        
        ResponseCode(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }

    private final String code;
    private final String msg;
    private final String data;

    private CoderCommonRespondBean(ResponseCode code, String msg, String data) {
        this.code = code.getCode();
        this.msg = msg;
        this.data = data;
    }

    public static CoderCommonRespondBean success(String msg, String data) {
        return new CoderCommonRespondBean(ResponseCode.SUCCESS, msg, data);
    }

    public static CoderCommonRespondBean fail(String msg, String data) {
        return new CoderCommonRespondBean(ResponseCode.FAIL, msg, data);
    }


}
