package com.fbadsautomation.ai;

import com.fbadsautomation.model.AdContent;
import java.util.List;

public interface AIProvider {
    /**
     * Sinh nội dung quảng cáo từ prompt
     * @param prompt Mô tả quảng cáo
     * @param numberOfVariations Số lượng mẫu quảng cáo cần sinh
     * @param language Ngôn ngữ đầu ra (ví dụ: "en", "vi")
     * @return Danh sách các mẫu quảng cáo
     */
    List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language);
    
    /**
     * Sinh hình ảnh cho quảng cáo
     * @param prompt Mô tả hình ảnh
     * @return URL của hình ảnh được sinh ra
     */
    String generateImage(String prompt);
    
    /**
     * Lấy tên của nhà cung cấp AI
     * @return Tên nhà cung cấp
     */
    String getProviderName();
    
    /**
     * Kiểm tra xem nhà cung cấp có hỗ trợ sinh hình ảnh không
     * @return true nếu hỗ trợ, false nếu không
     */
    boolean supportsImageGeneration();
    
    /**
     * Kiểm tra xem nhà cung cấp có hỗ trợ sinh video không
     * @return true nếu hỗ trợ, false nếu không
     */
    default boolean supportsVideoGeneration() {
        return false;
    }
    
    /**
     * Lấy danh sách các khả năng của nhà cung cấp AI
     * @return Danh sách các khả năng (TEXT, IMAGE, VIDEO)
     */
    default List<String> getCapabilities() {
        java.util.ArrayList<String> capabilities = new java.util.ArrayList<>();
        capabilities.add("TEXT"); // Tất cả các provider đều hỗ trợ text
        
        if (supportsImageGeneration()) {
            capabilities.add("IMAGE");
        }
        
        if (supportsVideoGeneration()) {
            capabilities.add("VIDEO");
        }
        
        return capabilities;
    }
}
