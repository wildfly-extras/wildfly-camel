/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.camel.test.catalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.wildfly.camel.catalog.CatalogCreator;
import org.wildfly.camel.catalog.CatalogCreator.RoadMap;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class GenerateRoadmapIssuesTest {

    private static final String MILESTONE = "4.8.0";
    private static final Label LABEL = new Label().setName("feature");
    
    private static final Path auxfile = CatalogCreator.basedir().resolve("target/issues.txt");
    
    @Test
    public void createIssuesStepA() throws Exception {
        
        CatalogCreator creator = new CatalogCreator().collect();
        
        List<String> collection = new ArrayList<>();
        for (RoadMap rm : creator.getRoadmaps()) {
            String prefix = null;
            File file = rm.getOutpath().toFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                while (line != null) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (line.equals("[undecided]")) {
                            prefix = rm.getKind() + " ";
                        } else if (line.startsWith("[")) {
                            prefix = null;
                        } else if (prefix != null) {
                            collection.add(prefix + line);
                        }
                    }
                    line = br.readLine();
                }
            }
        }

        try (PrintWriter pw = new PrintWriter(auxfile.toFile())) {
            for (String entry : collection) {
                pw.println(entry);
            }
        }
    }

    @Test
    public void createIssuesStepB() throws Exception {

        String accessToken = System.getenv("GitHubAccessToken");
        Assume.assumeNotNull("GitHubAccessToken not null", accessToken);
        
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(accessToken);
        
        String githubUser = "wildfly-extras";
        String githubRepo = "wildfly-camel";
        
        Milestone milestone = null;
        MilestoneService milestoneService = new MilestoneService(client);
        for (Milestone aux : milestoneService.getMilestones(githubUser, githubRepo, IssueService.STATE_OPEN)) {
            if  (aux.getTitle().equals(MILESTONE)) {
                milestone = aux;
                break;
            }
        }
        Assert.assertNotNull("Milestone not null", milestone);
        
        IssueService issueService = new IssueService(client);
        try (BufferedReader br = new BufferedReader(new FileReader(auxfile.toFile()))) {
            String line = br.readLine();
            while (line != null) {
                String title = "Add support for " + line;
                System.out.println(title);
                Issue issue = new Issue();
                issue.setTitle(title);
                issue.setLabels(Collections.singletonList(LABEL));
                issue.setMilestone(milestone);
                issueService.createIssue(githubUser, githubRepo, issue);
                line = br.readLine();
                Thread.sleep(3 * 1000);
            }
        }
    }

}