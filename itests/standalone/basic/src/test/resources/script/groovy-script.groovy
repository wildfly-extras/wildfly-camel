import groovy.xml.MarkupBuilder

// Set message body payload
def body = request.getBody()
request.setBody("Hello ${body}")

// Make sure some common Groovy APIs are accessible without any problems
def writer = new StringWriter()
MarkupBuilder xml = new MarkupBuilder(writer)
xml.message {
    payload(request.body)
}
println writer.toString()
