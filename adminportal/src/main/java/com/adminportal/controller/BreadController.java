package com.adminportal.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.adminportal.domain.Bread;
import com.adminportal.service.BreadService;

@Controller
@RequestMapping("/home/bread")
public class BreadController {

    @Autowired
    private BreadService breadService;

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public String addBread(Model model) {
        Bread bread = new Bread();
        model.addAttribute("bread", bread);
        return "addBread";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public String addBreadPost(@ModelAttribute("bread") Bread bread, HttpServletRequest request) {
        breadService.save(bread);

        MultipartFile breadImage = bread.getBreadImage();

        try {
            byte[] bytes = breadImage.getBytes();
            String name = bread.getId() + ".png";
            BufferedOutputStream stream = new BufferedOutputStream(
                    new FileOutputStream(new File("src/main/resources/static/image/bread/" + name)));
            stream.write(bytes);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:breadList";
    }
    
    @RequestMapping("/breadList")
    public String breadList(Model model) {
        List<Bread> breadList = breadService.findAll();
        model.addAttribute("breadList", breadList);
        return "breadList";
    }

    @RequestMapping("/breadInfo")
    public String breadInfo(@RequestParam("id") Long id, Model model) {
        Optional<Bread> breadOptional = breadService.findOne(id);

        if (breadOptional.isPresent()) {
            Bread bread = breadOptional.get();
            model.addAttribute("bread", bread);
            return "breadInfo";
        } else {
            // Handle case when bread is not found
            model.addAttribute("breadNotFound", true);
            return "redirect:/home/bread/breadList"; // Adjust the redirect as needed
        }
    }
    
    @RequestMapping("/updateBread")
    public String updateBread(@RequestParam("id") Long id, Model model) {
        Optional<Bread> breadOptional = breadService.findOne(id);

        if (breadOptional.isPresent()) {
            Bread bread = breadOptional.get();
            model.addAttribute("bread", bread);
            return "updateBread";
        } else {
            // Handle case when bread is not found
            model.addAttribute("breadNotFound", true);
            return "redirect:/home/bread/breadList"; // Adjust the redirect as needed
        }
    }
    
    @RequestMapping(value="/updateBread", method=RequestMethod.POST)
    public String updateBreadPost(@ModelAttribute("bread") Bread bread, HttpServletRequest request) {
        breadService.save(bread);
        
        MultipartFile breadImage = bread.getBreadImage();
        
        if(!breadImage.isEmpty()) {
            try {
                byte[] bytes = breadImage.getBytes();
                String name = bread.getId() + ".png";
                
                Files.delete(Paths.get("src/main/resources/static/image/bread/"+name));
                
                BufferedOutputStream stream = new BufferedOutputStream(
                        new FileOutputStream(new File("src/main/resources/static/image/bread/" + name)));
                stream.write(bytes);
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "redirect:/home/bread/breadInfo?id="+bread.getId();
    }
}
