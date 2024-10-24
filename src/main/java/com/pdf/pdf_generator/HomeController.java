package com.pdf.pdf_generator;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.xfa.XfaForm;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@RestController
public class HomeController {
	public static final String RESOURCE = "src/main/resources/IDS_June2024.pdf";
	public static String RESULT = "src/main/resources/";
	public static final String DESTINATION = "src/main/resources/";
	public static final String XML_DATA = "src/main/resources/dataToFill.xml";
	private static final String UPLOAD_DIR = "src/main/resources/";
	public static String XML_FILE = "src/main/resources/";

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

	public void manipulatePdf2(String src, String xml, String dest)
			throws IOException {
		PdfReader reader = new PdfReader(src);
		PdfDocument pdfDoc = new PdfDocument(reader, new PdfWriter(dest), new StampingProperties().useAppendMode());
		PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
		XfaForm xfa = form.getXfaForm();
		xfa.fillXfaForm(new FileInputStream(xml));
		xfa.write(pdfDoc);
		pdfDoc.close();
	}

	@PostMapping("/api/v2/upload-xml/{id}/{number}")
	public ResponseEntity<String> uploadXml(@PathVariable int id, @PathVariable String number,
			@RequestBody String xmlContent) {
		try {
			Path uploadPath = Paths.get(UPLOAD_DIR);
			String fileName = "uploaded_file_" + id + "-" + number + ".xml";
			XML_FILE += "uploaded_file_" + id + "-" + number + ".xml";
			// Save the XML content to a file
			Path filePath = uploadPath.resolve(fileName);
			Files.write(filePath, xmlContent.getBytes());
			XML_FILE = "src/main/resources/";
			return ResponseEntity.ok("File uploaded successfully! Filename: " + fileName);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
		}
	}

	@GetMapping("/api/v2/download/{id}/{number}")
	@ResponseBody
	public ResponseEntity<byte[]> downloadPdf(@PathVariable int id, @PathVariable String number) throws IOException {

		String filePdfName = "Generated_IDS_" + id + "-" + number + ".pdf";
		XML_FILE += "uploaded_file_" + id + "-" + number + ".xml";
		RESULT += filePdfName;
		// Generate the PDF
		manipulatePdf2(RESOURCE, XML_FILE, RESULT);

		// Read the PDF file into a byte array
		byte[] pdfBytes = Files.readAllBytes(Paths.get(RESULT));

		// Set headers for download
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filePdfName);

		XML_FILE = "src/main/resources/";
		RESULT = "src/main/resources/";

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdfBytes);
	}

	@GetMapping("/api/v2")
	public String index() throws IOException {
		return "PDF Generator";
	}

}
