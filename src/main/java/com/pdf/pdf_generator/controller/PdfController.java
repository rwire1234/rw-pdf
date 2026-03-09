package com.pdf.pdf_generator.controller;

import com.pdf.pdf_generator.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/api/v2/upload-xml/{id}/{number}")
    public ResponseEntity<String> uploadXml(
            @PathVariable int id,
            @PathVariable String number,
            @RequestBody String xmlContent) {

        return pdfService.uploadXml(id, number, xmlContent);
    }

    @GetMapping("/api/v2/download/{id}/{number}")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable int id,
            @PathVariable String number) throws IOException {

        return pdfService.downloadPdf(id, number);
    }

    @GetMapping("/api/v2")
    public String index() {
        return "PDF Generator";
    }
}
