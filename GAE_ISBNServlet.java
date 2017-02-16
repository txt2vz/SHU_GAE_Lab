package ca.shu;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class GAE_ISBNServlet extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String strCallResult = "";
		resp.setContentType("text/plain");

		// Obtain an object for accessing the Datastore.
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		// Set the Kind. This is where the book data (as entities) will be
		// stored.
		// A Kind is the equivalent of a Table in a relational database.
		String strKind = "Book";

		try {
			// Extract the user entered ISBN number
			String strISBN = req.getParameter("isbn");

			// Do basic validation - could be much more thorough
			if (strISBN == null)
				throw new Exception("ISBN field empty.");

			// Trim
			strISBN = strISBN.trim();
			if (strISBN.length() == 0)
				throw new Exception("ISBN field empty.");

			// Create URL for API call - adding user entered isbn number
			String strISBNServiceCall = "http://isbndb.com/api/books.xml?access_key=your_key&index1=isbn&value1="
					+ strISBN;
			URL url = new URL(strISBNServiceCall);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer response = new StringBuffer();
			String line;

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close();

			strCallResult = response.toString();

			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(strCallResult.toString())));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("BookData");

			if (nList.getLength() < 1)
				throw new Exception("no books returned: Check the ISBN.");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					resp.getWriter().println("<br />Title : " + getTagValue("TitleLong", eElement));
					resp.getWriter().println("<br />Publishers Text : " + getTagValue("PublisherText", eElement));					
				}

				resp.getWriter().println(strCallResult);
			}
		} catch (Exception ex) {
			strCallResult = "Error: " + ex.getMessage();
			resp.getWriter().println(strCallResult);
		}

	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = (Node) nlList.item(0);
		return nValue.getNodeValue();
	}
}
