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
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

	String strCallResult = "";
	resp.setContentType("text/plain");
	
	// Obtain an object for accessing the Datastore.
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	// Set the Kind. This is where the book data (as entities) will be stored. 
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
		String strISBNServiceCall = "http://isbndb.com/api/books.xml?access_key=yourKey&index1=isbn&value1=" + strISBN;
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
		
		// Iterate over all of the nodes in the XML to obtain the book's data.
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;				
				
				// Create a new entity for the retrieved book. 
				// The Kind is "Book" because the value of strKind was set to "Book" near the start of this code.
				// The ID of the entity is the ISBN number of the book.
				Entity book = new Entity(strKind, strISBN);
				
				// Assign the new book entity property values obtained from the XML data returned by the ISBNdb web service.
				book.setProperty("Title", getTagValue("TitleLong", eElement));
				book.setProperty("Publisher", getTagValue("PublisherText", eElement));
				book.setProperty("ISBN", strISBN);
				
				// Save the new book entity in the Datastore.
				datastore.put(book);
			}
		}
	} catch (Exception ex) {
		strCallResult = "Warning : " + ex.getMessage() + "<br />";
		resp.getWriter().println(strCallResult);
	}
	// Output the text for the start of the books list.
	resp.getWriter().println("<br />Books list:<br />");
	
	// Create query to return all entities in the Kind "Book" from the Datastore. 
	// The value of strKind was set to "Book" near the start of this code.
	Query q = new Query(strKind);
	PreparedQuery pq = datastore.prepare(q);
	
	// Iterate over all entities returned from the Datastore and 
	// put properties in the response (output) string to be displayed on the web page.
	for (Entity result : pq.asIterable()) {
		resp.getWriter().println("<br />Title : " + (String)result.getProperty("Title"));
		resp.getWriter().println("<br />Publishers Text : " + (String)result.getProperty("Publisher"));
		resp.getWriter().println("<br />ISBN : " + (String)result.getProperty("ISBN"));
		resp.getWriter().println("<br />-------------------------------------------------------------------------------");
	}
}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = (Node) nlList.item(0);
		return nValue.getNodeValue();
	}
}
