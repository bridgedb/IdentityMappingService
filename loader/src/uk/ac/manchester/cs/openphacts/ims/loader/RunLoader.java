/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.cs.openphacts.ims.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.bridgedb.sql.SQLUriMapper;
import org.bridgedb.utils.BridgeDBException;
import org.bridgedb.utils.Reporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.manchester.cs.openphacts.ims.loader.transative.TransativeFinderIMS;
import uk.ac.manchester.cs.openphacts.valdator.rdftools.RdfReader;
import uk.ac.manchester.cs.openphacts.valdator.rdftools.VoidValidatorException;
import uk.ac.manchester.cs.openphacts.valdator.utils.UrlReader;

/**
 *
 * @author Christian
 */
public class RunLoader {
    
    private static final String CLEAR_ALL = "clearAll";
    private static final String DIRECTORY = "directory";
    private static final String LINKSET = "linkset";
    private static final String VOID = "void";
    private static final String DO_TRANSITIVE = "doTransitive";      
    
    private final Loader loader;
    private final RdfReader reader;
    private int originalCount = 0;

    public RunLoader(boolean clear) throws BridgeDBException, VoidValidatorException {
        reader = RdfFactoryIMS.getReader();
        if (clear){
            SQLUriMapper.createNew();
            reader.clear();
        }
        loader = new Loader();
    }
    
    public static void clean(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int n = childNodes.getLength() - 1; n >= 0; n--) {
            Node child = childNodes.item(n);
            short nodeType = child.getNodeType();
            if (nodeType == Node.ELEMENT_NODE){
                clean(child);
            } else if (nodeType == Node.TEXT_NODE) {
                String trimmedNodeVal = child.getNodeValue().trim();
                if (trimmedNodeVal.length() == 0) {
                    node.removeChild(child);
                } else {
                    child.setNodeValue(trimmedNodeVal);
                }
            } else if (nodeType == Node.COMMENT_NODE) {
                node.removeChild(child);
            }
        }
    }

    private void loadLinkset(String uri) throws BridgeDBException, VoidValidatorException{
        Reporter.println("Loading linkset " + uri);
            originalCount++;
        //Validator validator = new ValidatorImpl();
        //String result = validator.validateUri(uri, null, "opsVoid", Boolean.TRUE);
        //System.out.println(result);
        File file = UriFileMapper.toFile(uri);
        if (file != null){
            Reporter.println("\tUsing File: " + file.getAbsolutePath());
            loader.load(file);
        } else {
            loader.load(uri, null);
        }
    }
       
    private void loadVoid(String uri) throws BridgeDBException, VoidValidatorException{
        Reporter.println("Loading void " + uri);
        File file = UriFileMapper.toFile(uri);
        if (file != null){
            Reporter.println("\tUsing File: " + file.getAbsolutePath());
            reader.loadFile(file);
        } else {
            reader.loadURI(uri);
        }
        reader.commit();
        reader.close();
    }
    
    public void loadDirectory(String address) throws BridgeDBException, MalformedURLException, IOException, VoidValidatorException {  
        //String address = "http://openphacts.cs.man.ac.uk/ims/linkset/version1.3.alpha2/";
        Reporter.println("Loading directory " + address);
        UrlReader urlReader = new UrlReader(address);
        InputStream stream = urlReader.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String[] headerLinksArray = {"Name", "Last modified", "Size", "Description", "Parent Directory"};
        List headerLinks = Arrays.asList(headerLinksArray);
        String line;
        while ((line = reader.readLine()) != null) {
            //System.out.println(line);
            String[] parts = line.split("<");
            for (String part:parts){
                if (part.startsWith("a ")){
                    String link = part.substring(part.indexOf(">")+1);
                    if (!headerLinks.contains(link)){
                        if (link.equals("drosophila_melanogaster_core_71_546_ensembl_EPDLinkSets.ttl")){
                            System.out.println("HACK skipping drosophila_melanogaster_core_71_546_ensembl_EPDLinkSets.ttl");
                        } else if (link.equals("rattus_norvegicus_core_71_5_ensembl_MGI_transcript_nameLinkSets.ttl")){
                            System.out.println("HACK skipping rattus_norvegicus_core_71_5_ensembl_MGI_transcript_nameLinkSets.ttl");
                        } else {
                            loadLinkset(address + URLEncoder.encode(link, "UTF-8"));
                        }
                    }
                }
            }
        }
        reader.close();
    }
       
    public static void main(String argv[]) throws BridgeDBException {   
        UriFileMapper.init();
        System.out.println("init done");
        RunLoader runLoader = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            URL url;
            if (argv.length == 0){
                //url = new URL("file:///c:/Dropbox/linksets/version1.3.alpha2/load.xml");
                url = new URL("http://openphacts.cs.man.ac.uk/ims/linkset/version1.3.alpha2/load.xml");
            } else {
                url = new URL(argv[0]);
            }    
            InputStream stream = url.openStream();
            doc = dBuilder.parse(stream);
            Element root = doc.getDocumentElement();
            clean(root);
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            root.normalize();
 
            System.out.println("Load read");

            NodeList nList = root.getChildNodes();
 
        	for (int temp = 0; temp < nList.getLength(); temp++) {
         		Node nNode = nList.item(temp);
                if (nNode instanceof Element){
                    String name = nNode.getNodeName();
                    String uri = nNode.getTextContent();
                    if (name.equals(CLEAR_ALL)){
                        runLoader = new RunLoader(true);
                    } else {
                        if (runLoader == null){
                            runLoader = new RunLoader(false);
                        }
                        if (name.equals(LINKSET)){
                            runLoader.loadLinkset(uri);
                        } else if (name.equals(DIRECTORY)){
                            runLoader.loadDirectory(uri);
                        } else if (name.equals(VOID)){
                            runLoader.loadVoid(uri);
                        } else if (name.equals(DO_TRANSITIVE)){
                            TransativeFinderIMS transativeFinder = new TransativeFinderIMS();
                            transativeFinder.UpdateTransative();
                        } else {
                            Reporter.error("Unexpected element " + name);
                        }
                    }
                } else {
                    Reporter.error("Unexpected node " + nNode + " type " + nNode.getClass());            
                }
            }
        } catch (Exception e) {
            throw new BridgeDBException("Error loading ", e);
        }
        System.out.println("Load " + runLoader.originalCount + " linksets plus their transdatives");
    }
 
}