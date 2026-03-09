package com.pdf.pdf_generator.controller;

import com.pdf.pdf_generator.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@RequestMapping("/api/v2")
@Tag(name = "PDF Generator API", description = "APIs for uploading XML and generating PDF")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Operation(summary = "Upload XML file", description = "Uploads XML data which will be used to generate the PDF")
    @PostMapping("/upload-xml/{id}/{number}")
    public ResponseEntity<String> uploadXml(
            @PathVariable int id,
            @PathVariable String number,
            @RequestBody String xmlContent) {

        return pdfService.uploadXml(id, number, xmlContent);
    }

    @Operation(summary = "Download generated PDF", description = "Generates and downloads the PDF based on uploaded XML")
    @GetMapping("/download/{id}/{number}")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable int id,
            @PathVariable String number) throws IOException {

        return pdfService.downloadPdf(id, number);
    }

    @Operation(summary = "Health Check API")
    @GetMapping()
    public String index() {
        return "PDF Generator";
    }
}
