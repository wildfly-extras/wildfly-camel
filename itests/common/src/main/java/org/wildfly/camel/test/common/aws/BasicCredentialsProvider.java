package org.wildfly.camel.test.common.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

public class BasicCredentialsProvider implements AWSCredentialsProvider {

    public static final String AWS_SECRET_KEY = "AWSSecretKey";
    public static final String AWS_ACCESS_ID = "AWSAccessId";

    final String accessId;
    final String secretKey;

    public BasicCredentialsProvider(String accessId, String secretKey) {
        this.accessId = accessId;
        this.secretKey = secretKey;
    }

    public static BasicCredentialsProvider standard() {
        String accessId = System.getenv(AWS_ACCESS_ID);
        String secretKey = System.getenv(AWS_SECRET_KEY);
        return new BasicCredentialsProvider(accessId, secretKey);
    }

    public boolean isValid() {
        return accessId != null && secretKey != null;
    }

    @Override
    public AWSCredentials getCredentials() {
        return new BasicAWSCredentials(accessId, secretKey);
    }

    @Override
    public void refresh() {
    }
}
