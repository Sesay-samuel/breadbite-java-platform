
package com.adminportal.controller;

import com.adminportal.domain.Bread;
import com.adminportal.service.BreadService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/home/bread")
public class BreadController {

    private static final Path IMAGE_DIRECTORY = Paths.get(
            "src", "main", "resources", "static", "image", "bread").toAbsolutePath().normalize();

    private final BreadService breadService;

    @Autowired
    public BreadController(BreadService breadService) {
        this.breadService = breadService;
    }

    // -------------------------------------------------
    // Display Add Bread Form
    // -------------------------------------------------

    @GetMapping("/add")
    public String addBread(Model model) {

        model.addAttribute("bread", new Bread());

        return "addBread";
    }

    // -------------------------------------------------
    // Process Add Bread Form
    // -------------------------------------------------

    @PostMapping("/add")
    public String addBreadPost(
            @ModelAttribute("bread") Bread bread) {

        MultipartFile breadImage = bread.getBreadImage();

        validateImage(breadImage);

        Bread savedBread = breadService.save(bread);

        saveBreadImage(savedBread.getId(), breadImage);

        return "redirect:/home/bread/breadList";
    }

    // -------------------------------------------------
    // Display Bread List
    // -------------------------------------------------

    @GetMapping("/breadList")
    public String breadList(Model model) {

        List<Bread> breadList = breadService.findAll();

        model.addAttribute("breadList", breadList);

        return "breadList";
    }

    // -------------------------------------------------
    // Display Bread Information
    // -------------------------------------------------

    @GetMapping("/breadInfo")
    public String breadInfo(
            @RequestParam("id") Long id,
            Model model) {

        Optional<Bread> breadOptional = breadService.findOne(id);

        if (breadOptional.isEmpty()) {
            return "redirect:/home/bread/breadList";
        }

        model.addAttribute("bread", breadOptional.get());

        return "breadInfo";
    }

    // -------------------------------------------------
    // Display Update Bread Form
    // -------------------------------------------------

    @GetMapping("/updateBread")
    public String updateBread(
            @RequestParam("id") Long id,
            Model model) {

        Optional<Bread> breadOptional = breadService.findOne(id);

        if (breadOptional.isEmpty()) {
            return "redirect:/home/bread/breadList";
        }

        model.addAttribute("bread", breadOptional.get());

        return "updateBread";
    }

    // -------------------------------------------------
    // Process Update Bread Form
    // -------------------------------------------------

    @PostMapping("/updateBread")
    public String updateBreadPost(
            @ModelAttribute("bread") Bread submittedBread) {

        Long breadId = submittedBread.getId();

        if (breadId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bread ID is required");
        }

        Bread existingBread = breadService.findOne(breadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bread not found"));

        MultipartFile breadImage = submittedBread.getBreadImage();

        validateImage(breadImage);

        // Update only the fields present in the edit form.
        // Preserve baker, type, ingredients, SKU, etc.

        existingBread.setTitle(submittedBread.getTitle());
        existingBread.setCategory(submittedBread.getCategory());
        existingBread.setListPrice(submittedBread.getListPrice());
        existingBread.setOurPrice(submittedBread.getOurPrice());
        existingBread.setInStockNumber(
                submittedBread.getInStockNumber());
        existingBread.setActive(submittedBread.isActive());
        existingBread.setDescription(
                submittedBread.getDescription());

        Bread savedBread = breadService.save(existingBread);

        saveBreadImage(savedBread.getId(), breadImage);

        return "redirect:/home/bread/breadInfo?id="
                + savedBread.getId();
    }

    // -------------------------------------------------
    // Validate Uploaded Bread Image
    // -------------------------------------------------

    private void validateImage(MultipartFile breadImage) {

        if (breadImage == null || breadImage.isEmpty()) {
            return;
        }

        // Limit uploaded images to 5 MB.
        if (breadImage.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bread image must not exceed 5 MB");
        }

        // Existing application stores images as PNG.
        if (!"image/png".equalsIgnoreCase(
                breadImage.getContentType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PNG images are supported");
        }

        // Verify PNG signature rather than trusting the
        // browser-provided Content-Type alone.
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        try (InputStream input = breadImage.getInputStream()) {

            byte[] actualSignature = input.readNBytes(8);

            if (!java.util.Arrays.equals(
                    actualSignature, pngSignature)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid PNG image");
            }

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded image",
                    exception);
        }
    }

    // -------------------------------------------------
    // Save Uploaded Bread Image
    // -------------------------------------------------

    private void saveBreadImage(
            Long breadId,
            MultipartFile breadImage) {

        // Image uploads are optional.
        if (breadImage == null || breadImage.isEmpty()) {
            return;
        }

        // Never create a file named null.png.
        if (breadId == null) {
            return;
        }

        try {

            Files.createDirectories(IMAGE_DIRECTORY);

            Path imagePath = IMAGE_DIRECTORY.resolve(
                    breadId + ".png").normalize();

            if (!imagePath.startsWith(IMAGE_DIRECTORY)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid image path");
            }

            try (InputStream input = breadImage.getInputStream()) {

                Files.copy(
                        input,
                        imagePath,
                        StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to save bread image",
                    exception);
        }
    }
}
