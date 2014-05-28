package org.apodhrad.eclipse.source_downloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SourceDownloader {

	public static final String DEFAULT_VERSION = "luna";
	public static final String FOLDER_PROPERTY = "source.dir";

	private String url;
	private String targetFolder;
	private String currentLocation;
	private Stack<String> stack;
	
	private String matcher;

	public static void main(String[] args) throws Exception {
		String version = DEFAULT_VERSION;
		if (args.length > 0) {
			version = args[0];
		} 
		String matcher = "";
		if (args.length > 1) {
			matcher = args[1];
		}
		String folder = System.getProperty(FOLDER_PROPERTY, System.getProperty("user.home"));
		String targetFolder = folder + "/source-downloader/" + version;
		String url = "http://download.eclipse.org/releases/" + version;
		new SourceDownloader(url, targetFolder, matcher).downloadSources();
	}

	public SourceDownloader(String url, String targetFolder, String matcher) {
		this.url = url;
		this.targetFolder = targetFolder;
		this.matcher = matcher;

		stack = new Stack<String>();
		stack.push("compositeArtifacts.jar");
	}

	public void downloadSources() throws Exception {
		System.out.println("All sources will be downloaded into '" + targetFolder + "'");
		new File(targetFolder).delete();
		while (!stack.isEmpty()) {
			String currentFile = stack.pop();
			if (!currentFile.endsWith(".jar")) {
				currentLocation = currentFile;
				currentFile += "/artifacts.jar";
			}
			download(url, currentFile);
		}
	}

	public void download(String url, String fileName) throws Exception {
		URL source = new URL(url + "/" + fileName);
		File target = new File(targetFolder, fileName);
		System.out.print("Downloading " + fileName + "... ");
		FileUtils.copyURLToFile(source, target);
		System.out.println("done.");

		if (fileName.toLowerCase().endsWith("artifacts.jar")) {
			parse(target);
		}
	}

	public void parse(File file) throws ZipException, IOException, ParserConfigurationException, SAXException {
		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			if (zipEntry.getName().endsWith(".xml")) {
				InputStream is = zipFile.getInputStream(zipEntry);
				parse(is);
				break;
			}
		}
		zipFile.close();
	}

	public void parse(InputStream is) throws IOException, ParserConfigurationException, SAXException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		MyHandler handler = new MyHandler();
		saxParser.parse(is, handler);

	}

	public class MyHandler extends DefaultHandler {

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equals("child")) {
				String location = attributes.getValue("location");
				if (!location.startsWith("http")) {
					stack.push(location);
				}
			}
			if (qName.equals("artifact")) {
				String classifier = attributes.getValue("classifier");
				String id = attributes.getValue("id");
				String version = attributes.getValue("version");
				if (classifier.equals("osgi.bundle") && id.endsWith(".source") && id.contains(matcher)) {
					stack.push(currentLocation + "/plugins/" + id + "_" + version + ".jar");
				}
			}
		}
	}
}
