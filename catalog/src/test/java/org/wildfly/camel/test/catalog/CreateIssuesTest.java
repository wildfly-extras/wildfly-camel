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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.wildfly.camel.test.catalog.CreateCatalogTest.RoadMap;

/**
 * This is not an actual test. 
 * 
 * It consumes the [planned] sections in the roadmap files and
 * creates GitHub issues through the API
 */
public final class CreateIssuesTest {

    Path outdir = Paths.get("src/main/resources");
    Path issues = outdir.resolve("issues.txt");
    
    RoadMap componentRM = new RoadMap(outdir.resolve("component.roadmap"));
    RoadMap dataformatRM = new RoadMap(outdir.resolve("dataformat.roadmap"));
    RoadMap languageRM = new RoadMap(outdir.resolve("language.roadmap"));
    
    //@Test
    public void createCatalog() throws Exception {

        createIssueList();
        
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token("26de14bbde8858d5ed5e4d42343e5...");
        
        IssueService issueService = new IssueService(client);
        String githubUser = "wildfly-extras";
        String githubRepo = "wildfly-camel";
        
        try (BufferedReader br = new BufferedReader(new FileReader(issues.toFile()))) {
            String line = br.readLine();
            while (line != null) {
                String title = "Add support for " + line;
                System.out.println(title);
                Issue issue = new Issue();
                issue.setTitle(title);
                issueService.createIssue(githubUser, githubRepo, issue);
                line = br.readLine();
                Thread.sleep(3 * 1000);
            }
        }
    }

    void createIssueList() throws Exception {
        
        List<String> collection = new ArrayList<>();
        for (RoadMap rm : Arrays.asList(componentRM, dataformatRM, languageRM)) {
            String prefix = null;
            File file = rm.outpath.toFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                while (line != null) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (line.equals("[planned]")) {
                            prefix = rm.type + " ";
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

        if (!issues.toFile().exists()) {
            try (PrintWriter pw = new PrintWriter(issues.toFile())) {
                for (String entry : collection) {
                    pw.println(entry);
                }
            }
        }
    }
}
