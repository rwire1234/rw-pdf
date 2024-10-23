package com.pdf.pdf_generator;

import java.io.File;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.itextpdf.licensing.base.LicenseKey;

@SpringBootApplication
public class PdfGeneratorApplication {

	public static void main(String[] args) {
		LicenseKey.loadLicenseFile(new File("src/main/resources/license.json"));
		SpringApplication.run(PdfGeneratorApplication.class, args);
	}

}
