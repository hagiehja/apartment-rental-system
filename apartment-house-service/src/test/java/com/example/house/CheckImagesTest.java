package com.example.house;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class CheckImagesTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkImages() throws Exception {
        System.out.println("Checking house_image table...");
        List<Map<String, Object>> images = jdbcTemplate.queryForList("SELECT * FROM house_image");

        StringBuilder sb = new StringBuilder();
        sb.append("Check Time: ").append(java.time.LocalDateTime.now()).append("\n");
        for (Map<String, Object> image : images) {
            sb.append("Image ID: ").append(image.get("image_id"))
                    .append(", House ID: ").append(image.get("house_id"))
                    .append(", URL: ").append(image.get("image_url"))
                    .append("\n");
        }
        sb.append("Total images: ").append(images.size());

        java.nio.file.Files.write(
                java.nio.file.Paths.get("check_result.txt"),
                sb.toString().getBytes());
    }
}
