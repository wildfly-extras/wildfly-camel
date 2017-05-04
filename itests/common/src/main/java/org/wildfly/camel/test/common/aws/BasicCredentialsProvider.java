package org.wildfly.camel.test.common.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

public class BasicCredentialsProvider implements AWSCredentialsProvider {

    final String accessId;
    final String secretKey;

    public BasicCredentialsProvider(String accessId, String secretKey) {
        this.accessId = accessId;
        this.secretKey = secretKey;
    }

    @Override
    public AWSCredentials getCredentials() {
        return new BasicAWSCredentials(accessId, secretKey);
    }

    @Override
    public void refresh() {
    }
}