package org.apodhrad.eclipse.source_downloader.actions;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apodhrad.eclipse.source_downloader.Activator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.osgi.framework.Version;

/**
 * 
 * @author apodhrad
 *
 */
public class CoolDialog extends FilteredItemsSelectionDialog {
	
	private static final String DIALOG_SETTINGS = "FilteredResourcesSelectionDialogExampleSettings";

	protected Map<String, String> artifacts;

	public CoolDialog(Shell shell) {
		super(shell, true);
		setTitle("Filtered Resources Selection Dialog Example");
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = Activator.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
			public boolean matchItem(Object item) {
				return matches(item.toString());
			}

			public boolean isConsistentItem(Object item) {
				return true;
			}
		};
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getItemsComparator() {
		return new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		};
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter,
			IProgressMonitor progressMonitor) throws CoreException {
		Map<String, String> artifacts = getArtifacts(progressMonitor);
		progressMonitor.beginTask("Searching", artifacts.size());
		Set<String> plugins = artifacts.keySet();
		for (String plugin : plugins) {
			contentProvider.add(plugin, itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	@Override
	public String getElementName(Object item) {
		return item.toString();
	}

	@Override
	public Object[] getResult() {
		Object[] result = super.getResult();
		if (result == null) {
			return new Object[] {};
		}
		Object[] location = new String[result.length];
		for (int i = 0; i < result.length; i++) {
			location[i] = artifacts.get(result[i]);
		}
		return location;
	}

	protected Map<String, String> getArtifacts(IProgressMonitor monitor) {
		if (artifacts == null) {
			artifacts = new HashMap<String, String>();
		}
		if (artifacts.isEmpty()) {
			try {
				IProvisioningAgent agent = Activator.getDefault().getProvisioningAgent();
				IArtifactRepositoryManager aManager = (IArtifactRepositoryManager) agent
						.getService(IArtifactRepositoryManager.SERVICE_NAME);
				URI eclipseURI = getEclipseURL().toURI();
				IArtifactRepository aRepo = aManager.loadRepository(eclipseURI, monitor);
				IQueryResult<IArtifactKey> result = aRepo.query(ArtifactKeyQuery.ALL_KEYS, monitor);
				Iterator<IArtifactKey> it = result.iterator();
				while (it.hasNext()) {
					IArtifactKey key = it.next();
					IArtifactDescriptor[] desc = aRepo.getArtifactDescriptors(key);
					if (key.getId().endsWith(".source")) {
						String plugin = key.getId() + "_" + key.getVersion();
						URI uri = desc[0].getRepository().getLocation();
						artifacts.put(plugin, uri.toString() + "/plugins/" + plugin + ".jar");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return artifacts;
	}

	public URL getEclipseURL() throws MalformedURLException {
		Version version = Platform.getBundle("org.eclipse.platform").getVersion();
		if (version.getMajor() == 4 && version.getMinor() == 3) {
			return new URL("http://download.eclipse.org/releases/kepler/");
		}
		return new URL("http://download.eclipse.org/releases/luna/");
	}
}
