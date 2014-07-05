package org.apodhrad.eclipse.source_downloader.actions;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class SampleAction implements IWorkbenchWindowActionDelegate {

	public static final int BUFFER = 1024;
	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public SampleAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		CoolDialog coolDialog = new CoolDialog(Display.getCurrent().getActiveShell());
		coolDialog.open();

		final Object[] result = coolDialog.getResult();

		Job job = new Job("Downloading sources") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Downloading", result.length);
				for (int i = 0; i < result.length; i++) {
					monitor.subTask(" " + result[i]);
					try {
						download(result[i].toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
					monitor.worked(1);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	public void download(String url) {
		Location location = Platform.getInstallLocation();
		String path = location.getURL().getPath();
		String[] part = url.split("/");
		String name = "";
		for (int i = 0; i < part.length; i++) {
			if (part[i].endsWith(".jar")) {
				name = part[i];
			}
		}
		try {
			saveUrl(path + "/dropins/" + name, url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveUrl(final String filename, final String urlString)
	        throws MalformedURLException, IOException {
	    BufferedInputStream in = null;
	    FileOutputStream fout = null;
	    try {
	        in = new BufferedInputStream(new URL(urlString).openStream());
	        fout = new FileOutputStream(filename);

	        final byte data[] = new byte[BUFFER];
	        int count;
	        while ((count = in.read(data, 0, BUFFER)) != -1) {
	            fout.write(data, 0, count);
	        }
	    } finally {
	        if (in != null) {
	            in.close();
	        }
	        if (fout != null) {
	            fout.close();
	        }
	    }
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}