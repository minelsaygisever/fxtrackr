package com.minelsaygisever.fxtrackr.dto;

import lombok.Data;

@Data
public class FixerError {
    private int code;
    private String type;
    private String info;
}
