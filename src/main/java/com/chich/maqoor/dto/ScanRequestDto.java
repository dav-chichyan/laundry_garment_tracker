package com.chich.maqoor.dto;

import com.chich.maqoor.entity.constant.Departments;
import lombok.Data;

@Data
public class ScanRequestDto {
    private int garmentId;
    private int userId;
    private Departments department;
}


