package com.pdf.pdf_generator.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static final String RESOURCE = "src/main/resources/IDS_June2024.pdf";
    public static String RESULT = "src/main/resources/pdf-files/";
    public static final String UPLOAD_DIR = "src/main/resources/xml-files/";
    public static String XML_FILE = "src/main/resources/xml-files/";

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

    public ResponseEntity<String> uploadXml(int id, String number, String xmlContent) {

        try {

            Path uploadPath = Paths.get(UPLOAD_DIR);

            String fileName = "uploaded_file_" + id + "-" + number + ".xml";

            Path filePath = uploadPath.resolve(fileName);

            Files.write(filePath, xmlContent.getBytes());

            return ResponseEntity.ok("File uploaded successfully! Filename: " + fileName);

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    public ResponseEntity<byte[]> downloadPdf(int id, String number) throws IOException {

        String filePdfName = "Generated_IDS_" + id + "-" + number + ".pdf";

        String xmlFile = XML_FILE + "uploaded_file_" + id + "-" + number + ".xml";

        String resultFile = RESULT + filePdfName;

        manipulatePdf2(RESOURCE, xmlFile, resultFile);

        byte[] pdfBytes = Files.readAllBytes(Paths.get(resultFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + filePdfName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    public void manipulatePdf2(String src, String xml, String dest) throws IOException {

        PdfReader reader = new PdfReader(src);

        PdfDocument pdfDoc = new PdfDocument(
                reader,
                new PdfWriter(dest),
                new StampingProperties().useAppendMode()
        );

        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);

        XfaForm xfa = form.getXfaForm();

        xfa.fillXfaForm(new FileInputStream(xml));

        xfa.write(pdfDoc);

        pdfDoc.close();
    }

}