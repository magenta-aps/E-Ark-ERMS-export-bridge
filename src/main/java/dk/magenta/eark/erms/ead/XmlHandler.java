package dk.magenta.eark.erms.ead;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.InputStream;

public interface XmlHandler {
	
	/**
	 * Generates JDOM document from an XML input stream. No validation is performed.
	 * @param in
	 * @return
	 */
	Document readXml(InputStream in);
	
	/**
	 * Generates JDOM document from an XML input stream. Validation is performed according 
	 * to the provided XML schemas. A JDOMException is thrown if the XML is not valid.
	 * @param in
	 * @param schemas The schemas to validate against (NOTE: the must be provided in the correct order!). The 
	 * schemas must be located in the src/main/resources folder
	 * @return
	 * @throws JDOMException
	 */
	Document readAndValidateXml(InputStream in, String... schemas) throws JDOMException;

	/**
	 * Gets the latest JDOM error message (e.g. a validation error message)
	 * @return
	 */
	String getErrorMessage();
	
	/**
	 * Write an XML element to System.out (for debugging)
	 * @param e
	 */
	static void writeXml(Element e) {
		XMLOutputter outputter = new XMLOutputter();
		try {
			outputter.output(e, System.out);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	
}
