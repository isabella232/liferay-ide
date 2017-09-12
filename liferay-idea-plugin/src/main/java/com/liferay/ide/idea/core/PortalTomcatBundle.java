/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.idea.core;

import com.liferay.ide.idea.util.CoreUtil;
import com.liferay.ide.idea.util.FileListing;
import com.liferay.ide.idea.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Gregory Amerson
 * @author Simon Jiang
 * @author Terry Jia
 */
public class PortalTomcatBundle extends AbstractPortalBundle {

	public PortalTomcatBundle(Path path) {
		super(path);
	}

	@Override
	public Path getAppServerDeployDir() {
		return Paths.get(getAppServerDir().toString(), "webapps");
	}

	@Override
	public Path getAppServerLibGlobalDir() {
		return Paths.get(getAppServerDir().toString(), "lib", "ext");
	}

	@Override
	public Path getAppServerPortalDir() {
		if (bundlePath == null) {
			return null;
		}

		return Paths.get(getAppServerDir().toString(), "webapps", "ROOT");
	}

	@Override
	public String getDisplayName() {
		return "Tomcat";
	}

	@Override
	public String getHttpPort() {
		File serverXmlFile = Paths.get(getAppServerDir().toString(), "conf", "server.xml").toFile();

		String portValue = getHttpPortValue(serverXmlFile, "Connector", "protocol", "HTTP/1.1", "port");

		if (CoreUtil.isNullOrEmpty(portValue)) {
			return "8080";
		}

		return portValue;
	}

	@Override
	public String getMainClass() {
		return "org.apache.catalina.startup.Bootstrap";
	}

	@Override
	public Path[] getRuntimeClasspath() {
		List<Path> paths = new ArrayList<>();

		Path binPath = Paths.get(bundlePath.toString(), "bin");

		if (binPath.toFile().exists()) {
			paths.add(Paths.get(binPath.toString(), "bootstrap.jar"));

			Path juli = Paths.get(binPath.toString(), "tomcat-juli.jar");

			if (juli.toFile().exists()) {
				paths.add(juli);
			}
		}

		return paths.toArray(new Path[0]);
	}

	@Override
	public String[] getRuntimeStartProgArgs() {
		String[] retval = new String[1];
		retval[0] = "start";

		return retval;
	}

	@Override
	public String[] getRuntimeStartVMArgs() {
		return _getRuntimeVMArgs();
	}

	@Override
	public String[] getRuntimeStopProgArgs() {
		String[] retval = new String[1];
		retval[0] = "stop";

		return retval;
	}

	@Override
	public String[] getRuntimeStopVMArgs() {
		return _getRuntimeVMArgs();
	}

	@Override
	public String getType() {
		return "tomcat";
	}

	@Override
	public Path[] getUserLibs() {
		List<Path> libs = new ArrayList<>();

		try {
			List<File> portallibFiles = FileListing.getFileListing(
				Paths.get(getAppServerPortalDir().toString(), "WEB-INF", "lib").toFile());

			for (File lib : portallibFiles) {
				if (lib.exists() && lib.getName().endsWith(".jar")) {
					libs.add(lib.toPath());
				}
			}

			List<File> libFiles = FileListing.getFileListing(getAppServerLibDir().toFile());

			for (File lib : libFiles) {
				if (lib.exists() && lib.getName().endsWith(".jar")) {
					libs.add(lib.toPath());
				}
			}

			List<File> extlibFiles = FileListing.getFileListing(getAppServerLibGlobalDir().toFile());

			for (File lib : extlibFiles) {
				if (lib.exists() && lib.getName().endsWith(".jar")) {
					libs.add(lib.toPath());
				}
			}
		}
		catch (FileNotFoundException fnfe) {
		}

		return libs.toArray(new Path[libs.size()]);
	}

	@Override
	public void setHttpPort(String port) {
		File serviceXmlFile = Paths.get(getAppServerDir().toString(), "conf", "server.xml").toFile();

		_setHttpPortValue(serviceXmlFile, "Connector", "protocol", "HTTP/1.1", "port", port);
	}

	@Override
	protected Path getAppServerLibDir() {
		return Paths.get(getAppServerDir().toString(), "lib");
	}

	@Override
	protected int getDefaultJMXRemotePort() {
		int retval = 8099;

		Path setenv = Paths.get(getAppServerDir().toString(), "bin", "setenv."+ _getShellExtension());

		String contents = FileUtil.readContents(setenv.toFile(), true);

		String port = null;

		if (contents != null) {
			Matcher matcher = Pattern.compile(
				".*-Dcom.sun.management.jmxremote.port(\\s*)=(\\s*)([0-9]+).*").matcher(contents);

			if (matcher.matches()) {
				port = matcher.group(3);
			}
		}

		if (port != null) {
			retval = Integer.parseInt(port);
		}

		return retval;
	}

	private String[] _getRuntimeVMArgs() {
		List<String> args = new ArrayList<>();

		args.add("-Dcatalina.base=" + "\"" + bundlePath.toString() + "\"");
		args.add("-Dcatalina.home=" + "\"" + bundlePath.toString() + "\"");

		args.add("-Dcom.sun.management.jmxremote");
		args.add("-Dcom.sun.management.jmxremote.authenticate=false");
		args.add("-Dcom.sun.management.jmxremote.port=" + getJmxRemotePort());
		args.add("-Dcom.sun.management.jmxremote.ssl=false");
		args.add("-Dfile.encoding=UTF8");

		args.add("-Djava.endorsed.dirs=" + "\"" + Paths.get(bundlePath.toString(), "endorsed").toString() + "\"");
		args.add("-Djava.io.tmpdir=" + "\"" + Paths.get(bundlePath.toString(), "temp").toString() + "\"");
		args.add("-Djava.net.preferIPv4Stack=true");

		String loggingPath = Paths.get(bundlePath.toString(), "conf", "logging.properties").toString();

		args.add("-Djava.util.logging.config.file=" + "\"" + loggingPath + "\"");
		args.add("-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager");
		args.add("-Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false");
		args.add("-Duser.timezone=GMT");

		return args.toArray(new String[0]);
	}

	private String _getShellExtension() {
		if (System.getProperty("os.name").startsWith("Win")) {
			return "bat";
		}

		return "sh";
	}

	private void _setHttpPortValue(
		File xmlFile, String tagName, String attriName, String attriValue, String targetName, String value) {

		DocumentBuilder db = null;

		DocumentBuilderFactory dbf = null;

		try {
			dbf = DocumentBuilderFactory.newInstance();

			db = dbf.newDocumentBuilder();

			Document document = db.parse(xmlFile);

			NodeList connectorNodes = document.getElementsByTagName(tagName);

			for (int i = 0; i < connectorNodes.getLength(); i++) {
				Node node = connectorNodes.item(i);

				NamedNodeMap attributes = node.getAttributes();

				Node protocolNode = attributes.getNamedItem(attriName);

				if (protocolNode != null) {
					if (protocolNode.getNodeValue().equals(attriValue)) {
						Node portNode = attributes.getNamedItem(targetName);

						portNode.setNodeValue(value);

						break;
					}
				}
			}

			TransformerFactory factory = TransformerFactory.newInstance();

			Transformer transformer = factory.newTransformer();

			DOMSource domSource = new DOMSource(document);

			StreamResult result = new StreamResult(xmlFile);

			transformer.transform(domSource, result);
		}
		catch (Exception e) {
		}
	}

}