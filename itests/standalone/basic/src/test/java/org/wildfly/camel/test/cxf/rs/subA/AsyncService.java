package org.wildfly.camel.test.cxf.rs.subA;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

@Path("/async")
public class AsyncService {

    @GET
    @Path("/hello")
    public void hayHello(@Suspended AsyncResponse response) {
        response.resume("Hello Kermit");
    }
}
