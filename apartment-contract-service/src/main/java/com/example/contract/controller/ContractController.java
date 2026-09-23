package com.example.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.common.api.Result;
import com.example.contract.dto.SignContractDTO;
import com.example.contract.entity.Contract;
import com.example.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 合同服务API接口
 */
@RestController
@RequestMapping("/contract")
@Slf4j
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * 根据订单生成合同(内部服务调用)
     */
    @PostMapping("/generate")
    public Result<Long> generateContract(@RequestBody Contract contract) {
        try {
            Long contractId = contractService.generateContract(contract);
            return Result.success(contractId);
        } catch (Exception e) {
            log.error("生成合同失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询合同详情
     */
    @GetMapping("/{contractId}")
    public Result<Contract> getContractDetail(@PathVariable Long contractId) {
        try {
            Contract contract = contractService.getContractById(contractId);
            return Result.success(contract);
        } catch (Exception e) {
            log.error("查询合同详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据订单ID查询合同
     */
    @GetMapping("/order/{orderId}")
    public Result<Contract> getContractByOrderId(@PathVariable Long orderId) {
        try {
            Contract contract = contractService.getContractByOrderId(orderId);
            return Result.success(contract);
        } catch (Exception e) {
            log.error("查询合同失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 签署合同
     */
    @PostMapping("/sign/{contractId}")
    public Result<Void> signContract(
            @PathVariable Long contractId,
            @RequestBody SignContractDTO dto) {
        try {
            contractService.signContract(contractId, dto);
            return Result.success();
        } catch (Exception e) {
            log.error("签署合同失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询用户的合同列表
     */
    @GetMapping("/list")
    public Result<IPage<Contract>> getContractList(
            @RequestParam Long userId,
            @RequestParam String userType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<Contract> result = contractService.getContractList(
                    userId, userType, status, page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询合同列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载合同文件
     */
    @GetMapping("/download/{contractId}")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long contractId) {
        try {
            Contract contract = contractService.getContractById(contractId);

            if (contract.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }

            File file = new File(contract.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + contract.getContractNo() + ".txt\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            log.error("下载合同失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 取消合同(内部服务调用)
     */
    @PostMapping("/cancel/order/{orderId}")
    public Result<Void> cancelContract(@PathVariable Long orderId) {
        try {
            contractService.cancelContract(orderId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消合同失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "apartment-contract-service");
        data.put("status", "UP");
        data.put("port", 8092);
        return Result.success(data);
    }

    /**
     * 申请退租
     */
    @PostMapping("/{contractId}/terminate")
    public Result<java.math.BigDecimal> terminateContract(
            @PathVariable Long contractId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, String> requestBody) {
        try {
            String reason = requestBody.get("reason");
            java.math.BigDecimal refundAmount = contractService.terminateContract(contractId, userId, reason);
            return Result.success(refundAmount);
        } catch (Exception e) {
            log.error("申请退租失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 计算退款金额（预览）
     */
    @GetMapping("/{contractId}/calculate-refund")
    public Result<java.math.BigDecimal> calculateRefund(@PathVariable Long contractId) {
        try {
            java.math.BigDecimal refundAmount = contractService.calculateRefund(contractId);
            return Result.success(refundAmount);
        } catch (Exception e) {
            log.error("计算退款金额失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 记录房东收入（内部服务调用）
     */
    @PostMapping("/landlord-income")
    public Result<Void> recordLandlordIncome(
            @RequestParam String orderNo,
            @RequestParam java.math.BigDecimal amount) {
        try {
            contractService.recordLandlordIncome(orderNo, amount);
            return Result.success(null);
        } catch (Exception e) {
            log.error("记录房东收入失败", e);
            return Result.error(e.getMessage());
        }
    }
}
