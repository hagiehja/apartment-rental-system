package com.example.house;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class ForceUpdateImagesTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void forceUpdateImages() {
        System.out.println("Starting FORCE update of house images...");

        // 1. 精装两室一厅
        updateImage("精装两室一厅",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?q=80&w=800&auto=format&fit=crop");

        // 2. 温馨一居室
        updateImage("温馨一居室",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?q=80&w=800&auto=format&fit=crop");

        // 3. 豪华三室两厅
        updateImage("豪华三室两厅",
                "https://images.unsplash.com/photo-1502005229766-071c7a2e98a1?q=80&w=800&auto=format&fit=crop");

        // 4. 市中心两室
        updateImage("市中心两室",
                "https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?q=80&w=800&auto=format&fit=crop");

        // 5. 舒适两室
        updateImage("舒适两室",
                "https://images.unsplash.com/photo-1484154218962-a1c002085d2f?q=80&w=800&auto=format&fit=crop");

        // 6. 经济实惠一居室
        updateImage("经济实惠一居室",
                "https://images.unsplash.com/photo-1556020685-ae79c95edfbc?q=80&w=800&auto=format&fit=crop");

        System.out.println("FORCE update complete!");
    }

    private void updateImage(String titleKeyword, String imageUrl) {
        // 先找到 house_id
        String sql = "UPDATE house_image SET image_url = ? WHERE is_cover = 1 AND house_id IN (SELECT house_id FROM house WHERE title LIKE ?)";
        int rows = jdbcTemplate.update(sql, imageUrl, "%" + titleKeyword + "%");
        System.out.println("Updated " + rows + " rows for house with title like: " + titleKeyword);

        if (rows == 0) {
            System.err.println("WARNING: No rows updated for " + titleKeyword);
        }
    }
}
