package ftsrg.rscript;

import hudson.util.ListBoxModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class PackagesManager {
	private static final PackagesManager singleton = new PackagesManager();
	private static final String[] PACKAGEGETTERCOMMAND = new String[]{"Rscript","-e","installed.packages()"};
	private static final String CRANURL = "http://cran.r-project.org/web/packages/available_packages_by_name.html";
	private static ListBoxModel availablePackagesCache = null;
	
	private PackagesManager() {
		
	}
	
	public static PackagesManager singleton() {
		return singleton;
	}
	
	public Set<String> getInstalledPackagesSet() {
        	Set<String> installedPackages = new HashSet<String>();
        	try {
        	    Runtime r = Runtime.getRuntime();
        	    Process p = r.exec(PACKAGEGETTERCOMMAND);
        	    BufferedReader b = new BufferedReader(new InputStreamReader(
        		    p.getInputStream()));
        	    String line = b.readLine();
        	    while ((line = b.readLine()) != null && !line.contains("Version")) {
        		if (line != "" && line != "\n" && !line.startsWith(" ")) {
        		    installedPackages.add(line.split(" ")[0]);
        		}
        	    }
        
        	} catch (IOException e) {
        	    e.printStackTrace();
        	}
        	return installedPackages;
        }
	
	public String createFullScript(PackageInstallation[] chosenPackages, String chosenMirror, String command) {
	    StringBuilder fullScriptSB = new StringBuilder("");
	    Set<String> installedPackages = getInstalledPackagesSet();
	    String packageScript = getInstallPackagesScript(installedPackages, chosenPackages, chosenMirror);
	    if (!packageScript.contains("null")) {
		fullScriptSB.append(packageScript).append("\n");		    
	    }
	    if (command != null) {
		fullScriptSB.append(command);		    
	    }
	    return fullScriptSB.toString();
	}
	
	public ListBoxModel getAvailablePackages() {
	    ListBoxModel packages = new ListBoxModel();
	    URL myUrl;
	    try {
		myUrl = new URL(CRANURL);   
        	BufferedReader in = new BufferedReader(new InputStreamReader(myUrl.openStream()));
        	String line;
        	while ((line = in.readLine()) != null) {
        		if (line.startsWith("<td><a href=")) {
        		    String currentPackage = line.split("</a></td><td>")[0]
        			    .split("\">")[1];
        		    packages.add(currentPackage);
        		}
		line = "";
        	}
	    in.close();
	    } catch (MalformedURLException e) {
		e.printStackTrace();
		return null;
	    } catch (IOException e) {
		e.printStackTrace();
		return null;
	    }
	    return packages;
	}
	
	public ListBoxModel getAvailablePackagesAsListBoxModel() {
	    if (availablePackagesCache == null) {
		availablePackagesCache = getAvailablePackages();
	    }
	    return availablePackagesCache;
	}
		
	protected String getInstallPackagesScript(Set<String> installedPackages,PackageInstallation[] chosenPackages, String chosenMirror) {
		StringBuilder sb = new StringBuilder("");
		if (chosenPackages.length > 0) {
		    sb.append("options(repos=structure(c(CRAN=\"").append(chosenMirror).append("\")))\n");		    
		}
		for (int i = 0; i < chosenPackages.length; i++) {
		    	String name = chosenPackages[i].getPackages();
			if (!installedPackages.contains(name)) {
        		    sb.append("install.packages('");
        		    sb.append(chosenPackages[i].getPackages());
        		    sb.append("', dependencies = TRUE)\n");
			}
		}
		return sb.toString();		
	}
}
