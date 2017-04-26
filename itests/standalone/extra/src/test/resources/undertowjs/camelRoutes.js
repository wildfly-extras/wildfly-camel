$undertow
    .alias('camel', 'jndi:java:jboss/camel/context/undertowjs-context')
    .onGet("/greeting/{name}", {headers: {"content-type": "text/plain"}}, ['camel', function ($exchange, camel) {
        var name = $exchange.param('name');
        var producer = camel.createProducerTemplate();
        return producer.requestBody("direct:start", name);
    }]);
