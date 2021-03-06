/*
 * Salesforce DTO generated by camel-salesforce-maven-plugin
 */
package org.wildfly.camel.test.salesforce.dto;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Salesforce Enumeration DTO for picklist CustomerPriority__c
 */
@Generated("org.apache.camel.maven.CamelSalesforceMojo")
public enum Account_CustomerPriorityEnum {

    // High
    HIGH("High"),

    // Low
    LOW("Low"),

    // Medium
    MEDIUM("Medium");


    final String value;

    private Account_CustomerPriorityEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static Account_CustomerPriorityEnum fromValue(String value) {
        for (Account_CustomerPriorityEnum e : Account_CustomerPriorityEnum.values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException(value);
    }

}
