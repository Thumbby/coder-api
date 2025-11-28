package com.thumbby.bean;

import lombok.Data;

@Data
public class CoderExecuteQuery {

    private String processId;
    private String prompt;
    private boolean stream = false;

}
