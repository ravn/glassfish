/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */


package org.glassfish.web.osgi;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiWebContainer
{
    private static class OSGiApplicationInfo
    {
        ApplicationInfo appInfo;
        boolean isDirectoryDeployment;
    }

    private Map<Bundle, OSGiApplicationInfo> applications =
            new HashMap<Bundle, OSGiApplicationInfo>();

    private static final Logger logger =
            Logger.getLogger(OSGiWebContainer.class.getPackage().getName());

    private Deployment deployer = Globals.get(Deployment.class);
    private ArchiveFactory archiveFactory = Globals.get(ArchiveFactory.class);
    private ServerEnvironmentImpl env = Globals.get(ServerEnvironmentImpl.class);

    /**
     * Deploys a web application bundle in GlassFish Web container.
     * This is where the fun is...
     *
     * @param b Web Application Bundle to be deployed.
     */
    public void deploy(final Bundle b) throws Exception
    {
        // The steps are described below:
        // 1. Create an Archive from the bundle
        //    - If the bundle has been installed with reference: scheme,
        //    get hold hold of the underlying file and read from it, else
        //    use the bundle directly to create the archive.
        // 2. Prepare a context for deployment. This includes setting up
        // various deployment options, setting up of an ArchiveHandler,
        // expansion of the archive, etc.
        // 3. Finally deploy and store the result in our inmemory map.

        ActionReport reporter = getReporter();

        ReadableArchive archive = new OSGiBundleArchive(b);

        // Try to obtain a handle to the underlying archive.
        // First see if it is backed by a file or a directory, else treat
        // it as a generic bundle.
        File file = makeFile(archive);

        // Set up a deployment context
        OpsParams opsParams = getDeployParams(archive);
        ExtendedDeploymentContext dc = new OSGiDeploymentContextImpl(reporter,
                logger, archive, opsParams, env, b);
        dc.setArchiveHandler(new OSGiWarHandler());

        // expand if necessary, else set directory deployment to true
        boolean isDirDeployment = file != null && file.isDirectory();
        if (!isDirDeployment)
        {
            archive = expand(archive, dc);
            dc.setSource(archive); // set the new archive as source.
        }

        // Now actual deployment begins
        deploy(b, dc, isDirDeployment);
    }

    private void deploy(Bundle b,
                        ExtendedDeploymentContext dc,
                        boolean dirDeployment)
    {
        // Need to declare outside to do proper cleanup of target dir
        // when deployment fails. We can't rely on exceptions as
        // deployer.deploy() eats most of the exceptions.
        ApplicationInfo appInfo = null;
        try
        {
            appInfo = deployer.deploy(dc);
            if (appInfo != null)
            {
                OSGiApplicationInfo osgiAppInfo = new OSGiApplicationInfo();
                osgiAppInfo.appInfo = appInfo;
                osgiAppInfo.isDirectoryDeployment = dirDeployment;
                applications.put(b, osgiAppInfo);
                logger.logp(Level.INFO, "OSGiWebContainer", "deploy",
                        "deployed bundle {0} at {1}",
                        new Object[]{b, appInfo.getSource().getURI()});
            }
            else
            {
                logger.logp(Level.INFO, "OSGiWebContainer",
                        "deploy", "failed to deploy = {0}", new Object[]{b});
            }
        }
        finally
        {
            if (!dirDeployment && appInfo == null)
            {
                try
                {
                    File dir = dc.getSourceDir();
                    assert (dir.isDirectory());
                    FileUtils.whack(dir);
                    logger.logp(Level.INFO, "OSGiWebContainer", "deploy",
                            "Deleted {0}", new Object[]{dir});
                }
                catch (Exception e2)
                {
                    logger.logp(Level.WARNING, "OSGiWebContainer", "deploy",
                            "Exception while cleaning up target directory.", e2);
                    // don't throw this anymore
                }
            }
        }
    }

    /**
     * Undeploys a web application bundle.
     *
     * @param b
     */
    public void undeploy(Bundle b) throws Exception
    {
        OSGiApplicationInfo osgiAppInfo = applications.get(b);
        if (osgiAppInfo == null)
        {
            throw new RuntimeException("No applications for bundle " + b);
        }
        ActionReport reporter = getReporter();
        UndeployCommandParameters opsParams = getUndeployParams(osgiAppInfo);
        ExtendedDeploymentContext dc = new OSGiDeploymentContextImpl(reporter,
                logger, osgiAppInfo.appInfo.getSource(), opsParams, env, b);
        dc.setArchiveHandler(new OSGiWarHandler());
        deployer.undeploy(opsParams.name(), dc);
        applications.remove(b);
        if (!osgiAppInfo.isDirectoryDeployment)
        {
            // We can always assume dc.getSourceDir will return a valid file
            // because we would have expanded the app during deployment.
            cleanup(dc.getSourceDir());
        }
        logger.logp(Level.INFO, "OSGiWebContainer", "undeploy",
                "Undeployed bundle {0} from {1}", new Object[]{b,
                osgiAppInfo.appInfo.getSource().getURI()});
    }

    public void undeployAll()
    {
        // Take a copy of the entries as undeploy changes the underlying map.
        for (Bundle b : new HashSet<Bundle>(applications.keySet()))
        {
            try
            {
                undeploy(b);
            }
            catch (Exception e)
            {
                logger.logp(Level.SEVERE, "OSGiWebContainer", "undeployAll",
                        "Exception undeploying bundle {0}",
                        new Object[]{b.getLocation()});
                logger.logp(Level.SEVERE, "OSGiWebContainer", "undeployAll",
                        "Exception Stack Trace", e);
            }
        }
    }

    private ActionReport getReporter()
    {
        return Globals.getDefaultHabitat().getComponent(ActionReport.class,
                "plain");
    }

    /**
     * Return a File object that corresponds to this archive.
     * return null if it can't determine the underlying file object.
     *
     * @param a The archive
     * @return
     */
    private File makeFile(ReadableArchive a)
    {
        try
        {
            return new File(a.getURI());
        }
        catch (Exception e)
        {
            // Ignore, if we can't convert
        }
        return null;
    }

    private OpsParams getDeployParams(ReadableArchive sourceArchive)
    {
        DeployCommandParameters parameters = new DeployCommandParameters();
        parameters.name = sourceArchive.getName();

        // Set the contextroot explicitly, else it defaults to name.
        try
        {
            // We expect WEB_CONTEXT_ROOT to be always present.
            // This is mandated in the spec.
            parameters.contextroot = sourceArchive.getManifest().
                    getMainAttributes().getValue(Constants.WEB_CONTEXT_PATH);
        }
        catch (IOException e)
        {
            // ignore and continue
        }
        if (parameters.contextroot == null || parameters.contextroot.length() == 0)
        {
            throw new RuntimeException(Constants.WEB_CONTEXT_PATH +
                    " manifest header is mandatory");
        }
        parameters.enabled = Boolean.TRUE;
        parameters.origin = DeployCommandParameters.Origin.deploy;
        parameters.force = true;
        return parameters;
    }

    private UndeployCommandParameters getUndeployParams(
            OSGiApplicationInfo osgiAppInfo)
    {
        UndeployCommandParameters parameters =
                new UndeployCommandParameters();
        parameters.name = osgiAppInfo.appInfo.getName();
        parameters.origin = DeployCommandParameters.Origin.undeploy;
        return parameters;
    }

    private ReadableArchive expand(ReadableArchive sourceArchive,
                                   ExtendedDeploymentContext dc)
            throws IOException
    {
        // ok we need to explode the directory somwhere and
        // remember to delete it on shutdown
        // We can't use archive name as it can contain file separator, so
        // we shall use a temporary name
        File tmpFile = File.createTempFile("osgiapp", "");

        // create a directory in place of the tmp file.
        tmpFile.delete();
        tmpFile = new File(tmpFile.getAbsolutePath());
        tmpFile.deleteOnExit();
        if (tmpFile.mkdirs())
        {
            WritableArchive targetArchive = archiveFactory.createArchive(tmpFile);
            new OSGiWarHandler().expand(sourceArchive, targetArchive, dc);
            logger.logp(Level.INFO, "OSGiWebContainer", "expand",
                    "Expanded at {0}", new Object[]{targetArchive.getURI()});
            return archiveFactory.openArchive(tmpFile);
        }
        throw new IOException("Not able to expand " + sourceArchive.getName() +
                " in " + tmpFile);
    }

    private void cleanup(File dir)
    {
        assert(dir.isDirectory() && dir.exists());
        FileUtils.whack(dir);
        logger.logp(Level.INFO, "OSGiWebContainer", "cleanup",
                "Deleted {0}", new Object[]{dir});
    }

}
