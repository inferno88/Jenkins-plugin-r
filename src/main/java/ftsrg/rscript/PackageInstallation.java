package ftsrg.rscript;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.ListBoxModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class PackageInstallation extends AbstractDescribableImpl<PackageInstallation> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1319802100582835536L;
    private String packages;
    
    @DataBoundConstructor
    public PackageInstallation(String packages) {
	super();
	this.packages = packages;
    }
    
    public String getPackages() {
	return packages;
    }
    
    public void setPackages(String packages) {
	this.packages = packages;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<PackageInstallation> {
	private static String chosenMirror;
	private String chosenPackage = null;
	private List<String> mirrorList;
	
	public DescriptorImpl() {
	    load(); 
	}
	
	public void setChosenPackage(String chosenPackage) {
	    this.chosenPackage = chosenPackage;
	    save();
	}
	
	public String getChosenPackage() {
	    return chosenPackage;
	}
	
	public String getInstalledPackages() {
		StringBuilder installedPackagesBuilder = new StringBuilder();
		for (String entry : PackagesManager.singleton()
			.getInstalledPackagesSet()) {
		    installedPackagesBuilder.append(entry).append("\n");
		}
		installedPackagesBuilder.deleteCharAt(installedPackagesBuilder.length() - 1);
		return installedPackagesBuilder.toString();
	    }
	
        public void setChosenMirror(String chosenMirror) {
	    Hudson.getInstance().getDescriptorByType(ScriptRunner.DescriptorImpl.class).setChosenMirror(chosenMirror);
	}
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            if (json.containsKey("packages")) {	
		JSONArray installsList = json.getJSONArray("packages");
		List<PackageInstallation> packagesToInstall = new ArrayList<PackageInstallation>();
		for (int i = 0; i < installsList.size(); i++) {
		    String currentPackage = installsList.getString(i)
			    .split(":")[1].replace("\"", "").replace("}", "");
		    PackageInstallation newPackage = new PackageInstallation(
			    currentPackage);
		    packagesToInstall.add(newPackage);
		}
		setPackages(packagesToInstall
			.toArray(new PackageInstallation[0]));
            } else {
        	setPackages(null);
            }
            setChosenMirror(json.getString("mirrorList"));
            return true;
        }
	        
        @Override
        public String getDisplayName() {
            return "";
        }
        
        public PackageInstallation[] getPackages() {
            return Hudson.getInstance().getDescriptorByType(ScriptRunner.DescriptorImpl.class).getPackages();
        }

        public void setPackages(PackageInstallation... packages) {
            Hudson.getInstance().getDescriptorByType(ScriptRunner.DescriptorImpl.class).setPackages(packages);
        }
        
        public ListBoxModel doFillInstalledPackagesItems() {
	    ListBoxModel mirrors = new ListBoxModel();
	    Set<String> mirrorsList = PackagesManager.singleton().getInstalledPackagesSet();
	    for (String entry : mirrorsList) {
		mirrors.add(entry);
	    }
	    return mirrors;
	}

	 public ListBoxModel doFillPackagesItems() {
	    return PackagesManager.singleton().getAvailablePackagesAsListBoxModel();
	}
	    
	public static ListBoxModel doFillMirrorListItems() { 
		chosenMirror = Hudson.getInstance().getDescriptorByType(ScriptRunner.DescriptorImpl.class).getChosenMirror();
		ListBoxModel mirrors = new ListBoxModel();
		String[] mirrorsList = MirrorManager.singleton().getMirrors();
		int selected = 34;
		for (int i = 0; i < mirrorsList.length; i++) {
		    String[] splitCurrent = mirrorsList[i].split(" - ");
		    if (chosenMirror != null && chosenMirror.equals(splitCurrent[0])) {
			selected = i;
		    }
		    mirrors.add(splitCurrent[1], splitCurrent[0]);
		}	
		mirrors.get(selected).selected = true;		
	    return mirrors;
	}

	public List<String> getMirrorList() {
	    return mirrorList;
	}

	public void setMirrorList(List<String> mirrorList) {
	    this.mirrorList = mirrorList;
	}	
    }
    
}
