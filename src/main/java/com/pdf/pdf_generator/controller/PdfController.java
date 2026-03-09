package com.pdf.pdf_generator.controller;

import com.pdf.pdf_generator.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "PDF Generator API", description = "APIs for uploading XML and generating PDF")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Operation(summary = "Upload XML file", description = "Uploads XML data which will be used to generate the PDF")
    @PostMapping("/upload-xml/{id}/{number}")
    public ResponseEntity<?> uploadXml(
            @PathVariable int id,
            @PathVariable String number,
            @RequestBody String xmlContent) {

        try {
            return pdfService.uploadXml(id, number, xmlContent);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error uploading XML file: " + e.getMessage());
        }
    }

    @Operation(summary = "Download generated PDF", description = "Generates and downloads the PDF based on uploaded XML")
    @GetMapping("/download/{id}/{number}")
    public ResponseEntity<?> downloadPdf(
            @PathVariable int id,
            @PathVariable String number) throws IOException {

        try {
            return pdfService.downloadPdf(id, number);
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body("Error generating or downloading PDF: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate PDF from XML string", description = "Returns PDF file", responses = {
            @ApiResponse(responseCode = "200", description = "PDF file", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    })
    @PostMapping("/generate-pdf")
    public ResponseEntity<?> generatePdf(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "XML string used to generate PDF", required = true, content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "<root>\n  <field1>value1</field1>\n  <field2>value2</field2>\n</root>"))) @RequestBody String xmlContent) {

        Path tempXml = null;
        Path tempPdf = null;
        Path tempPdfTemplate = null;

        try {
            tempXml = Files.createTempFile("temp_", ".xml");
            Files.writeString(tempXml, xmlContent);

            tempPdf = Files.createTempFile("temp_", ".pdf");

            InputStream pdfTemplateStream = new ClassPathResource(PdfService.TEMPLATE_PDF).getInputStream();
            tempPdfTemplate = Files.createTempFile("template_", ".pdf");
            Files.copy(pdfTemplateStream, tempPdfTemplate, StandardCopyOption.REPLACE_EXISTING);

            pdfService.manipulatePdf2(
                    tempPdfTemplate.toString(),
                    tempXml.toString(),
                    tempPdf.toString());

            byte[] pdfBytes = Files.readAllBytes(tempPdf);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=Generated_IDS_" + UUID.randomUUID() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());

        } finally {
            try {
                if (tempXml != null)
                    Files.deleteIfExists(tempXml);
                if (tempPdf != null)
                    Files.deleteIfExists(tempPdf);
                if (tempPdfTemplate != null)
                    Files.deleteIfExists(tempPdfTemplate);
            } catch (IOException ignored) {
            }
        }
    }

    @Operation(summary = "Health Check API")
    @GetMapping()
    public String index() {
        return "PDF Generator";
    }
}
