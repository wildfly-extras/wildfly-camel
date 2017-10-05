/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.printer;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.Sides;

import org.apache.camel.component.printer.PrinterConfiguration;
import org.apache.camel.component.printer.PrinterEndpoint;
import org.apache.camel.component.printer.PrinterOperations;
import org.apache.camel.component.printer.PrinterProducer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class PrinterIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-lpr-tests");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage());
        return archive;
    }


    @Before
    public void setup() {
        PrintService psDefault = Mockito.mock(PrintService.class);
        Mockito.when(psDefault.getName()).thenReturn("DefaultPrinter");
        Mockito.when(psDefault.isDocFlavorSupported(Mockito.any(DocFlavor.class))).thenReturn(Boolean.TRUE);
        PrintServiceLookup psLookup = Mockito.mock(PrintServiceLookup.class);
        Mockito.when(psLookup.getPrintServices()).thenReturn(new PrintService[]{psDefault});
        Mockito.when(psLookup.getDefaultPrintService()).thenReturn(psDefault);
        DocPrintJob docPrintJob = Mockito.mock(DocPrintJob.class);
        Mockito.when(psDefault.createPrintJob()).thenReturn(docPrintJob);
        MediaTray[] trays = new MediaTray[]{
            MediaTray.TOP,
            MediaTray.MIDDLE,
            MediaTray.BOTTOM
        };
        Mockito.when(psDefault.getSupportedAttributeValues(Media.class, null, null)).thenReturn(trays);
        PrintServiceLookup.registerServiceProvider(psLookup);
    }

    @Test
    public void printsWithLandscapeOrientation() throws Exception {
        PrinterEndpoint endpoint = new PrinterEndpoint();
        PrinterConfiguration configuration = new PrinterConfiguration();
        configuration.setHostname("localhost");
        configuration.setPort(631);
        configuration.setPrintername("DefaultPrinter");
        configuration.setMediaSizeName(MediaSizeName.ISO_A4);
        configuration.setInternalSides(Sides.ONE_SIDED);
        configuration.setInternalOrientation(OrientationRequested.REVERSE_LANDSCAPE);
        configuration.setMediaTray("middle");
        configuration.setSendToPrinter(false);

        PrinterProducer producer = new PrinterProducer(endpoint, configuration);
        producer.start();
        PrinterOperations printerOperations = producer.getPrinterOperations();
        PrintRequestAttributeSet attributeSet = printerOperations.getPrintRequestAttributeSet();

        Attribute attribute = attributeSet.get(OrientationRequested.class);
        Assert.assertNotNull(attribute);
        Assert.assertEquals("reverse-landscape", attribute.toString());
    }
}
