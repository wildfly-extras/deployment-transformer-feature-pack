/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.wildfly.deployment.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildFlyRunner;

@RunWith(WildFlyRunner.class)
@ServerControl(manual = true)
public class DeploymentTransformerSmokeTestCase {

    private static final String FACES_URL = "http://localhost:8080/wildfly-deployment-transformer-kitchensink-web/index.jsf";
    private static final String REST_URL = "http://localhost:8080/wildfly-deployment-transformer-kitchensink-web/rest/members/1";

    @Inject
    private ServerController serverController;

    @After
    public void tearDown() {
        serverController.stop();
        System.clearProperty("jboss.home");
        System.clearProperty("wildfly.bootable.jar");
        System.clearProperty("wildfly.bootable.jar.jar");
        System.clearProperty("wildfly.bootable.jar.install.dir");
    }

    @Test
    public void testExpandedComplete() throws IOException, ParseException {
        startExpanded("wildfly-complete");
        test();
    }

    @Test
    public void testExpandedLayers() throws IOException, ParseException {
        startExpanded("wildfly-layers");
        test();

    }

    @Test
    @Ignore("WFLY-17505")
    public void testBootableJarDefault() throws IOException, ParseException {
        startBootableJar("bootable-default");
        test();

    }

    @Test
    public void testBootableJarLayers() throws IOException, ParseException {
        startBootableJar("bootable-layers");
        test();

    }

    private void startExpanded(String name) {
        setJBossHome(name);
        serverController.start();
    }

    private String setJBossHome(String name) {
        String installDir = System.getProperty("test.installation.directory") + File.separatorChar + name;
        System.setProperty("jboss.home", installDir);
        return installDir;
    }

    private void startBootableJar(String name) {
        String installDir = setJBossHome(name);
        System.setProperty("wildfly.bootable.jar.install.dir", installDir);
        System.setProperty("wildfly.bootable.jar.jar", installDir + ".jar");
        System.setProperty("wildfly.bootable.jar", "true");
        serverController.start();
    }

    private void test() throws IOException, ParseException {

        // Deploy the EE 8 kitchensink and check for success
        File deployment = Path.of(System.getProperty("test.ear.path")).toAbsolutePath().toFile();
        try (InputStream stream = new BufferedInputStream(new FileInputStream(deployment))) {

            ModelNode opNode = new ModelNode();
            opNode.get("address").add("deployment", "kitchensink.ear");
            opNode.get("operation").set("add");
            opNode.get("enabled").set(true);
            opNode.get("content").add().get("input-stream-index").set(0);

            ModelNode response = serverController.getClient().getControllerClient().execute(Operation.Factory.create(opNode, List.of(stream)));
            assertEquals(response.toString(), "success", response.get("outcome").asString());
        }

        // Exercise the Jakarta Faces interface
        String responseContent = getResponseContent(Request.get(FACES_URL));
        assertTrue(responseContent, responseContent.contains("John Smith"));
        assertTrue(responseContent, responseContent.contains("john.smith@mailinator.com"));
        assertTrue(responseContent, responseContent.contains("2125551212"));

        // Post an invalid new member
        List<NameValuePair> params = extractHiddenFields(responseContent);
        params.addAll(Form.form()
                .add("reg:name", "Mary J0nes")
                .add("reg:email", "mjones@mailinator.com")
                .add("reg:phoneNumber", "1234567890")
                .add("reg:register", "Register").build());
        Request post = Request.post(FACES_URL).bodyForm(params);
        responseContent = getResponseContent(post);
        assertTrue(responseContent, responseContent.contains("Must not contain numbers"));
        assertTrue(responseContent, responseContent.contains("Mary J0nes"));
        assertTrue(responseContent, responseContent.contains("mjones@mailinator.com"));
        assertTrue(responseContent, responseContent.contains("1234567890"));

        // Now a valid new member
        params = extractHiddenFields(responseContent);
        params.addAll(Form.form()
                .add("reg:name", "Mary Jones")
                .add("reg:email", "mjones@mailinator.com")
                .add("reg:phoneNumber", "1234567890")
                .add("reg:register", "Register").build());
        post = Request.post(FACES_URL).bodyForm(params);
        responseContent = getResponseContent(post);
        assertTrue(responseContent, responseContent.contains("Mary Jones"));
        assertTrue(responseContent, responseContent.contains("mjones@mailinator.com"));
        assertTrue(responseContent, responseContent.contains("1234567890"));

        // Exercise the JAX-RS interface
        responseContent = getResponseContent(Request.get(REST_URL));
        assertTrue(responseContent, responseContent.contains("Mary Jones"));
        assertTrue(responseContent, responseContent.contains("mjones@mailinator.com"));
        assertTrue(responseContent, responseContent.contains("1234567890"));
    }

    private String getResponseContent(Request request) throws IOException, ParseException {
        HttpResponse response = request.execute().returnResponse();
        int respCode = response.getCode();

        // Try and extract entity content even if we will fail due to incorrect response code
        // so we can use it in the failure message
        String content;
        try {
            content = EntityUtils.toString(((HttpEntityContainer) response).getEntity());
        } catch (ParseException e) {
            if (respCode == 200) {
                throw e;
            }
            // For a non-200 if we can't parse an entity, just use the response object's string form
            content = response.toString();
        }
        assertEquals(content, 200, respCode);
        return content;
    }

    private List<NameValuePair> extractHiddenFields(String html) {
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();
        MatcherAssert.assertThat("form is included in the response", form, CoreMatchers.notNullValue());
        List<NameValuePair> params = new ArrayList<>();
        for (Element input : form.select("input")) {
            String type = input.attr("type");
            String value = input.attr("value");
            if ("hidden".equals(type) && !value.isEmpty()) {
                params.add(new BasicNameValuePair(input.attr("name"), value));
            }
        }
        return params;
    }



}
