package ftsrg.rscript;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Messages;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ScriptRunner extends Builder {
    private static final String fileExtension = ".R";
    private static final String backUpMirror = "http://cran.rapporter.net/";
    private String chosenMirror;
    private String command;
    @DataBoundConstructor
    public ScriptRunner(String command) {
	this.command = command;
    }

    public final String getCommand() {
	return command;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
	    BuildListener listener) throws InterruptedException {
	return perform(build, launcher, (TaskListener) listener);
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws InterruptedException {
	String chosenMirror = getDescriptor().getChosenMirror() != null ? getDescriptor().getChosenMirror() : backUpMirror ;
	PackageInstallation[] packagesToInstall = getDescriptor().getPackages();
	
	FilePath workSpace = build.getWorkspace();
	FilePath rScript = null;
	boolean successfullyRan = false;
	
	if (workSpace == null) {
	    try {
		throw new NoSuchFileException("Workspace could not be set!");
	    } catch (NoSuchFileException e) {
		e.printStackTrace();
	    }
	}
	try {
	    try {
		String fullScript;
		if (command.contains("options(repos=structure(")) {
		    fullScript = PackagesManager.singleton().createFullScript(packagesToInstall, "", command);
		} else {
		    fullScript = PackagesManager.singleton().createFullScript(packagesToInstall, chosenMirror, command);
		}
		rScript = workSpace.createTextTempFile("RScriptTemp",
			getFileExtension(), fullScript, false);
	    } catch (IOException e) {
		Util.displayIOException(e, listener);
		e.printStackTrace(listener.fatalError(Messages
			.CommandInterpreter_UnableToProduceScript()));
		return false;
	    }

	  
	    try {
		EnvVars envVars = build.getEnvironment(listener);
		for (Map.Entry<String, String> e : build.getBuildVariables()
			.entrySet()) {
		    envVars.put(e.getKey(), e.getValue());
		}

		if (launcher.launch().cmds(buildCommandLine(rScript))
			.envs(envVars).stdout(listener).pwd(workSpace).join() == 0) {
		    successfullyRan = true;
		}
	    } catch (IOException e) {
		Util.displayIOException(e, listener);
		e.printStackTrace(listener.fatalError(Messages
			.CommandInterpreter_CommandFailed()));
	    }
	} finally {
	    try {
		if (rScript != null) {
		    rScript.delete();
		}
	    } catch (IOException e) {
		Util.displayIOException(e, listener);
		e.printStackTrace(listener.fatalError(Messages
			.CommandInterpreter_UnableToDelete(rScript)));
	    } catch (Exception e) {
		e.printStackTrace(listener.fatalError(Messages
			.CommandInterpreter_UnableToDelete(rScript)));
	    }
	}
	return successfullyRan;
    }

    public String[] buildCommandLine(FilePath script) {
	return new String[] { "Rscript", script.getRemote() };
    }

    protected String getFileExtension() {
	return fileExtension;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public String getChosenMirror() {
	return chosenMirror;
    }

    public void setChosenMirror(String chosenMirror) {
	this.chosenMirror = chosenMirror;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
	
	@CopyOnWrite
	private volatile PackageInstallation[] packages = new PackageInstallation[0];
	private transient int queried = -2;
	private String chosenMirror;
	private String command;
	
	public DescriptorImpl() {
	    queried = -2;
	    load();
	}
	
	protected DescriptorImpl(Class<? extends ScriptRunner> clazz) {
            super(clazz);
        }

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
	    req.bindJSON(this, formData);
	    save();
	    return super.configure(req,formData);
	}
	
	@Override
	public String getDisplayName() {
	    return "Execute R script";
	}

	@Override
	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
	    return true;
	}
	
	public void setPackages(PackageInstallation... packages) { 
            this.packages = packages;
            save();
        }

	public void increaseQueried() {
	    queried++;
	}
	public PackageInstallation[] getPackages() {
	    return packages;
	}
	
	public String getCommand() {
	    return command;
	}

	public void setCommand(String command) {
	    this.command = command;
	    save();
	}

	public Set<String> getInstalledPackages() {
	    return PackagesManager.singleton().getInstalledPackagesSet();
	}

	public String getChosenMirror() {
	    return chosenMirror;
	}

	public void setChosenMirror(String chosenMirror) {
	    this.chosenMirror = chosenMirror;
	    save();
	}

	public int getQueried() {
	    return queried;
	}
	
	@Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(ScriptRunner.class, formData);
        }
    }
}
