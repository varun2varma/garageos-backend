package com.garageos.modules.master.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.master.dto.response.EmployeeRoleResponse;
import com.garageos.modules.master.service.MasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master")
@RequiredArgsConstructor
public class MasterController {

    private final MasterService service;

    @GetMapping("/employee-roles")
    public ResponseEntity<ApiResponse<List<EmployeeRoleResponse>>> getEmployeeRoles() {

        return ApiResponseUtil.success(

                "Employee roles fetched successfully.",

                service.getEmployeeRoles()

        );

    }

}