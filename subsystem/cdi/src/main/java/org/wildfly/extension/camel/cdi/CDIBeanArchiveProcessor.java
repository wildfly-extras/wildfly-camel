package org.wildfly.extension.camel.cdi;

import java.util.List;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.wildfly.extension.camel.deployment.CamelDeploymentSettings;

final class CDIBeanArchiveProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        List<DeploymentUnit> subDeployments = depUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);

        // Return if camel disabled or not a CDI deployment
        if (!depSettings.isEnabled() || !WeldDeploymentMarker.isPartOfWeldDeployment(depUnit)) {
            return;
        }

        // Return if we're not an EAR deployment with 1 or more sub-deployments
        if (depUnit.getName().endsWith(".ear") && subDeployments.isEmpty()) {
            return;
        }

        // Make sure external bean archives from the camel-cdi module are visible to sub deployments
        List<BeanDeploymentArchiveImpl> deploymentArchives = depUnit.getAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES);
        BeanDeploymentArchiveImpl rootArchive = depUnit.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);

        for (BeanDeploymentArchiveImpl bda : deploymentArchives) {
            if (bda.getBeanArchiveType().equals(BeanDeploymentArchiveImpl.BeanArchiveType.EXTERNAL)) {
                for (BeanDeploymentArchive topLevelBda : rootArchive.getBeanDeploymentArchives()) {
                    bda.addBeanDeploymentArchive(topLevelBda);
                }
            }

            for (DeploymentUnit subDepUnit : subDeployments) {
                BeanDeploymentArchive subBda = subDepUnit.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);
                bda.addBeanDeploymentArchive(subBda);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
    }
}
