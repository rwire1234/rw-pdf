package com.pdf.pdf_generator.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.NodeList;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.xfa.XfaForm;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.transform.Transformer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Service
public class PdfService {

	private static final String TEMPLATE_PDF = "IDS_June2024.pdf";
	private final Map<String, Path> tempXmlMap = new ConcurrentHashMap<>();

	/**
	 * Extract all level-1 subnodes from XFA structure in a XFA PDF.
	 * 
	 * @param src      src the path of source PDF file
	 * @param destPath the dest folder to save subnodes' XML data
	 */
	public static void extractXfa(String src, String destPath) {
		try (PdfReader reader = new PdfReader(src)) {

			reader.setUnethicalReading(true);

			PdfDocument pdfDoc = new PdfDocument(reader);
			PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDoc, false);

			if (acroForm != null && acroForm.getXfaForm() != null) {
				XfaForm xfa = acroForm.getXfaForm();
				Document domDoc = xfa.getDomDocument();
				Element rootElement = domDoc.getDocumentElement();
				if (rootElement != null) {
					System.out.println("Root Element Name: " + rootElement.getNodeName());
					if (rootElement.hasAttributes()) {
						System.out.println("Root Element Attr:");
						for (int i = 0; i < rootElement.getAttributes().getLength(); i++) {
							System.out.println("  " + rootElement.getAttributes().item(i).getNodeName() + ": "
									+ rootElement.getAttributes().item(i).getNodeValue());
						}
					}
					// get all first level children
					NodeList childNodes = rootElement.getChildNodes();
					for (int i = 0; i < childNodes.getLength(); i++) {
						Node childNode = childNodes.item(i);
						if (childNode.getNodeType() == Node.ELEMENT_NODE) {
							Element childElement = (Element) childNode;
							String childName = childElement.getNodeName();
							System.out.println("Child Node: " + childName);
							childName = childName.replace(':', '_');
							String output = destPath + "/" + childName + ".xml";
							try (FileOutputStream fileOutputStream = new FileOutputStream(output)) {
								Transformer transformer = TransformerFactory.newInstance().newTransformer();
								transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
								transformer.setOutputProperty(OutputKeys.INDENT, "yes");
								transformer.transform(new DOMSource(childElement), new StreamResult(fileOutputStream));
							}
						}
					}
				}
			} else {
				System.out.println("iText: The pdf document does not contain an XFA form.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ResponseEntity<?> uploadXml(int id, String number, String xmlContent) {
		Path tempXml = null;
		String key = id + "_" + number;
		try {
			tempXml = Files.createTempFile("uploaded_" + key + "_", ".xml");
			Files.writeString(tempXml, xmlContent);
			tempXmlMap.put(key, tempXml);

			return ResponseEntity.ok("File uploaded successfully! Temp file created.");

		} catch (Exception e) {
			return ResponseEntity.status(500)
					.body("Error uploading file: " + e.getMessage());
		}
	}

	public ResponseEntity<?> downloadPdf(int id, String number) throws IOException {
		String key = id + "_" + number;
		Path tempXml = tempXmlMap.get(key);

		if (tempXml == null || !Files.exists(tempXml)) {
			return ResponseEntity.status(404).body(null);
		}

		Path tempPdf = null;
		Path tempTemplatePdf = null;
		try {
			tempPdf = Files.createTempFile("generated_" + key + "_", ".pdf");

			try (InputStream templateStream = new ClassPathResource(TEMPLATE_PDF).getInputStream()) {
				tempTemplatePdf = Files.createTempFile("template_", ".pdf");
				Files.copy(templateStream, tempTemplatePdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}

			manipulatePdf2(
					tempTemplatePdf.toAbsolutePath().toString(),
					tempXml.toAbsolutePath().toString(),
					tempPdf.toAbsolutePath().toString());

			byte[] pdfBytes = Files.readAllBytes(tempPdf);

			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=Generated_IDS_" + id + "-" + number + ".pdf");

			return ResponseEntity.ok()
					.headers(headers)
					.contentType(MediaType.APPLICATION_PDF)
					.body(pdfBytes);

		} finally {
			if (tempXml != null) {
				Files.deleteIfExists(tempXml);
				tempXmlMap.remove(key);
			}
			if (tempPdf != null) {
				Files.deleteIfExists(tempPdf);
			}
		}
	}

	public void manipulatePdf2(String src, String xml, String dest) throws IOException {

		PdfReader reader = new PdfReader(src);

		PdfDocument pdfDoc = new PdfDocument(
				reader,
				new PdfWriter(dest),
				new StampingProperties().useAppendMode());

		PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);

		XfaForm xfa = form.getXfaForm();

		xfa.fillXfaForm(new FileInputStream(xml));

		xfa.write(pdfDoc);

		pdfDoc.close();
	}

}