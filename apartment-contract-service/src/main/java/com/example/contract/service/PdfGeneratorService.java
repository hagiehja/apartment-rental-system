package com.example.contract.service;

import com.example.contract.entity.Contract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PDF生成服务
 * 注意: 这是一个简化版本,使用文本格式生成合同
 * 生产环境建议使用 iText 7 或其他PDF库生成正式的PDF文件
 */
@Service
@Slf4j
public class PdfGeneratorService {

    @Value("${contract.file.base-path}")
    private String fileBasePath;

    /**
     * 生成合同PDF文件
     * 
     * @param contract 合同信息
     * @return PDF文件路径
     */
    public String generateContractPdf(Contract contract) throws Exception {
        log.info("开始生成合同PDF: contractNo={}", contract.getContractNo());

        // 1. 确保目录存在
        File dir = new File(fileBasePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. 生成文件名
        String fileName = contract.getContractNo() + ".txt"; // 简化版使用txt
        String filePath = Paths.get(fileBasePath, fileName).toString();

        // 3. 生成合同内容
        String content = generateContractContent(contract);

        // 4. 写入文件
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }

        log.info("合同PDF生成成功: filePath={}", filePath);

        return filePath;
    }

    /**
     * 生成合同内容
     */
    private String generateContractContent(Contract contract) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("              房屋租赁合同\n");
        sb.append("═══════════════════════════════════════════\n\n");

        sb.append("合同编号: ").append(contract.getContractNo()).append("\n");
        sb.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n\n");

        sb.append("甲方（出租方/房东）\n");
        sb.append("  用户ID: ").append(contract.getLandlordId()).append("\n\n");

        sb.append("乙方（承租方/租客）\n");
        sb.append("  用户ID: ").append(contract.getTenantId()).append("\n\n");

        sb.append("───────────────────────────────────────────\n");
        sb.append("一、租赁房屋信息\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append("房源名称: ").append(contract.getHouseName()).append("\n");
        if (contract.getHouseAddress() != null) {
            sb.append("房源地址: ").append(contract.getHouseAddress()).append("\n");
        }
        sb.append("房源ID: ").append(contract.getHouseId()).append("\n\n");

        sb.append("───────────────────────────────────────────\n");
        sb.append("二、租赁期限\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append("起始日期: ").append(contract.getStartDate().format(dateFormatter)).append("\n");
        sb.append("终止日期: ").append(contract.getEndDate().format(dateFormatter)).append("\n");
        long months = java.time.temporal.ChronoUnit.MONTHS.between(contract.getStartDate(), contract.getEndDate());
        sb.append("租赁期限: ").append(months).append("个月\n\n");

        sb.append("───────────────────────────────────────────\n");
        sb.append("三、租金及押金\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append("月租金: ¥").append(contract.getRentalAmount()).append("元\n");
        if (contract.getDepositAmount() != null
                && contract.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("押金: ¥").append(contract.getDepositAmount()).append("元\n");
        }
        sb.append("\n");

        sb.append("───────────────────────────────────────────\n");
        sb.append("四、双方权利义务\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append("1. 甲方应确保房屋符合居住条件,并提供必要的设施。\n");
        sb.append("2. 乙方应按时支付租金,爱护房屋及其设施。\n");
        sb.append("3. 未经双方协商一致,任何一方不得擅自解除合同。\n");
        sb.append("4. 租赁期满,乙方应将房屋完好归还甲方。\n\n");

        sb.append("───────────────────────────────────────────\n");
        sb.append("五、签署信息\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append("订单ID: ").append(contract.getOrderId()).append("\n");
        sb.append("合同状态: ").append(getStatusText(contract.getStatus())).append("\n");

        if (contract.getLandlordSignedAt() != null) {
            sb.append("甲方签署时间: ").append(contract.getLandlordSignedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }

        if (contract.getTenantSignedAt() != null) {
            sb.append("乙方签署时间: ").append(contract.getTenantSignedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }

        sb.append("\n═══════════════════════════════════════════\n");
        sb.append("本合同一式两份,甲乙双方各执一份,具有同等法律效力。\n");
        sb.append("═══════════════════════════════════════════\n");

        return sb.toString();
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "待签署";
            case "LANDLORD_SIGNED":
                return "房东已签署";
            case "TENANT_SIGNED":
                return "租客已签署";
            case "COMPLETED":
                return "已完成";
            case "ARCHIVED":
                return "已归档";
            case "CANCELLED":
                return "已作废";
            default:
                return status;
        }
    }
}
