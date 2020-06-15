/*
 * Salesforce Query DTO generated by camel-salesforce-maven-plugin
 */
package org.wildfly.camel.test.salesforce.dto;

import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.camel.component.salesforce.api.dto.AbstractQueryRecordsBase;

import java.util.List;
import javax.annotation.Generated;

/**
 * Salesforce QueryRecords DTO for type Pricebook2
 */
@Generated("org.apache.camel.maven.CamelSalesforceMojo")
public class QueryRecordsPricebook2 extends AbstractQueryRecordsBase {

    @XStreamImplicit
    private List<Pricebook2> records;

    public List<Pricebook2> getRecords() {
        return records;
    }

    public void setRecords(List<Pricebook2> records) {
        this.records = records;
    }
}
