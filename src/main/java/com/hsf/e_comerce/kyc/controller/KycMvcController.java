package com.hsf.e_comerce.kyc.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.kyc.entity.EKycSession;
import com.hsf.e_comerce.kyc.service.KycOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/kyc")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KycMvcController {

    private final KycOrchestratorService orchestratorService;

    /**
     * Trang chủ KYC - Hiển thị hướng dẫn và bắt đầu
     */
    @GetMapping
    public String kycHome(@CurrentUser User user, Model model) {
        // Check if user already has a session
        EKycSession existingSession = orchestratorService.getLatestSessionByUser(user.getId());
        
        if (existingSession != null) {
            model.addAttribute("kycSession", existingSession);
            return "kyc/dashboard";
        }
        
        return "kyc/home";
    }

    /**
     * Bắt đầu session KYC mới
     */
    @PostMapping("/start")
    public String startKycSession(@CurrentUser User user, RedirectAttributes redirectAttributes) {
        try {
            UUID sessionId = orchestratorService.startSession(user.getId());
            redirectAttributes.addFlashAttribute("success", "Đã tạo phiên KYC mới. Hãy tải lên giấy tờ của bạn.");
            return "redirect:/kyc/session/" + sessionId;
        } catch (Exception e) {
            log.error("Failed to start KYC session", e);
            redirectAttributes.addFlashAttribute("error", "Không thể tạo phiên KYC: " + e.getMessage());
            return "redirect:/kyc";
        }
    }

    /**
     * Trang chi tiết session - Upload documents
     */
    @GetMapping("/session/{sessionId}")
    public String kycSession(
            @PathVariable("sessionId") UUID sessionId,
            @CurrentUser User user,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            EKycSession session = orchestratorService.getSession(sessionId, user.getId());
            model.addAttribute("kycSession", session);
            return "kyc/upload";
        } catch (Exception e) {
            log.error("Failed to get session", e);
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phiên KYC: " + e.getMessage());
            return "redirect:/kyc";
        }
    }

    /**
     * Upload CCCD mặt trước
     */
    @PostMapping("/session/{sessionId}/upload-front")
    public String uploadFront(
            @PathVariable("sessionId") UUID sessionId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file");
                return "redirect:/kyc/session/" + sessionId;
            }

            Map<String, Object> result = orchestratorService.uploadFileAndAttach(
                    sessionId, 
                    user.getId(), 
                    file, 
                    "CCCD mặt trước",
                    "Identity card front"
            );

            if (Boolean.TRUE.equals(result.get("ok"))) {
                redirectAttributes.addFlashAttribute("success", "Đã tải lên CCCD mặt trước thành công!");
            } else {
                String step = (String) result.get("step");
                String reason = (String) result.get("reason");
                redirectAttributes.addFlashAttribute("error", 
                    "Tải lên thất bại ở bước " + step + ": " + reason);
            }
        } catch (Exception e) {
            log.error("Error uploading front file", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải lên: " + e.getMessage());
        }
        return "redirect:/kyc/session/" + sessionId;
    }

    /**
     * Upload CCCD mặt sau
     */
    @PostMapping("/session/{sessionId}/upload-back")
    public String uploadBack(
            @PathVariable("sessionId") UUID sessionId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file");
                return "redirect:/kyc/session/" + sessionId;
            }

            Map<String, Object> result = orchestratorService.uploadFileAndAttach(
                    sessionId,
                    user.getId(),
                    file,
                    "CCCD mặt sau",
                    "Identity card back"
            );

            if (Boolean.TRUE.equals(result.get("ok"))) {
                redirectAttributes.addFlashAttribute("success", "Đã tải lên CCCD mặt sau thành công!");
            } else {
                String step = (String) result.get("step");
                String reason = (String) result.get("reason");
                redirectAttributes.addFlashAttribute("error", 
                    "Tải lên thất bại ở bước " + step + ": " + reason);
            }
        } catch (Exception e) {
            log.error("Error uploading back file", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải lên: " + e.getMessage());
        }
        return "redirect:/kyc/session/" + sessionId;
    }

    /**
     * Upload ảnh selfie
     */
    @PostMapping("/session/{sessionId}/upload-selfie")
    public String uploadSelfie(
            @PathVariable("sessionId") UUID sessionId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file");
                return "redirect:/kyc/session/" + sessionId;
            }

            Map<String, Object> result = orchestratorService.uploadFileAndAttach(
                    sessionId,
                    user.getId(),
                    file,
                    "Ảnh selfie",
                    "Selfie photo"
            );

            if (Boolean.TRUE.equals(result.get("ok"))) {
                redirectAttributes.addFlashAttribute("success", "Đã tải lên ảnh selfie thành công!");
            } else {
                String step = (String) result.get("step");
                String reason = (String) result.get("reason");
                redirectAttributes.addFlashAttribute("error",
                        "Tải lên thất bại ở bước " + step + ": " + reason);
            }
        } catch (Exception e) {
            log.error("Error uploading selfie file", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải lên: " + e.getMessage());
        }
        return "redirect:/kyc/session/" + sessionId;
    }

    /**
     * Xử lý OCR và hiển thị thông tin
     */
    @GetMapping("/session/{sessionId}/review")
    public String reviewInformation(
            @PathVariable UUID sessionId,
            @CurrentUser User user,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            EKycSession session = orchestratorService.getSession(sessionId, user.getId());
            
            // Check if documents are uploaded
            if (session.getFrontHash() == null || session.getSelfieHash() == null) {
                redirectAttributes.addFlashAttribute("error", 
                    "Vui lòng tải lên đầy đủ CCCD mặt trước và ảnh selfie");
                return "redirect:/kyc/session/" + sessionId;
            }

            // Extract information from OCR
            Map<String, Object> ocrFrontData = orchestratorService.extractFrontInfo(sessionId, user.getId());
            Map<String, Object> ocrBackData = null;
            if (session.getBackHash() != null) {
                ocrBackData = orchestratorService.extractBackInfo(sessionId, user.getId());
            }
            
            model.addAttribute("kycSession", session);
            model.addAttribute("frontInfo", ocrFrontData);
            model.addAttribute("backInfo", ocrBackData);
            
            return "kyc/review";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xử lý thông tin: " + e.getMessage());
            return "redirect:/kyc/session/" + sessionId;
        }
    }

    /**
     * Xác nhận và so sánh khuôn mặt
     */
    @PostMapping("/session/{sessionId}/verify")
    public String verifyIdentity(
            @PathVariable UUID sessionId,
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> result = orchestratorService.compareAndVerify(sessionId, user.getId());
            
            Boolean isMatch = (Boolean) result.get("isMatch");
            Double matchScore = (Double) result.get("matchScore");
            
            if (Boolean.TRUE.equals(isMatch) && matchScore >= 95.0) {
                redirectAttributes.addFlashAttribute("success", 
                    "Xác minh danh tính thành công! Điểm khớp: " + matchScore + "%");
                return "redirect:/kyc/success";
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "Xác minh thất bại. Khuôn mặt không khớp với CCCD. Điểm: " + matchScore + "%");
                return "redirect:/kyc/session/" + sessionId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xác minh: " + e.getMessage());
            return "redirect:/kyc/session/" + sessionId;
        }
    }

    /**
     * Trang thành công
     */
    @GetMapping("/success")
    public String kycSuccess(@CurrentUser User user, Model model) {
        model.addAttribute("user", user);
        return "kyc/success";
    }

    /**
     * Lịch sử các phiên KYC
     */
    @GetMapping("/history")
    public String kycHistory(@CurrentUser User user, Model model) {
        // Get all sessions for user
        // (Cần implement method này trong service)
        return "kyc/history";
    }
}
