package org.wildfly.camel.test.soap.subA;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.wildfly.camel.test.common.types.Customer;

@WebService
public interface CustomerService {

    @WebMethod(operationName = "getGreeting", action = "urn:getGreeting")
    @WebResult(name = "greeting")
    String getGreeting(@WebParam(name = "input") Customer customer);
}
