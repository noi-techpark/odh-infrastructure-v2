// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-bean
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-seda
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-stream
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-openapi-java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-rest
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-paho

package it.bz.opendatahub.inbound.rest;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ContainerNode;

import javax.enterprise.context.ApplicationScoped;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * MQTT configuration as defined by Quarkus.
 * <p>
 * The data in this interface is taken from System properties, ENV variables,
 * .env file and more. Take a look at https://quarkus.io/guides/config-reference
 * to see how it works.
 */
class RestConfig {
    public String storage_url;
    public String storage_topic;

    public Optional<String> storage_user;
    public Optional<String> storage_password;
}

/**
 * Route to read from REST.
 */
@ApplicationScoped
public class RestRoute extends RouteBuilder {
    private final RestConfig restConfig;

    public RestRoute()
    {
        this.restConfig = new RestConfig();

        this.restConfig.storage_url = ConfigProvider.getConfig().getValue("internal_mqtt.url", String.class);
        this.restConfig.storage_topic = ConfigProvider.getConfig().getValue("internal_mqtt.topic", String.class);
        this.restConfig.storage_user = ConfigProvider.getConfig().getOptionalValue("internal_mqtt.user", String.class);
        this.restConfig.storage_password = ConfigProvider.getConfig().getOptionalValue("internal_mqtt.password", String.class);
    } 

    @Override
    public void configure() {
        RestConfigLogger.log(restConfig);
        
        // Exposes REST connection
        // process and forward to the internal queue waiting to be written in rawDataTable
        restConfiguration()
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "ODH inbound REST API")
            .apiProperty("api.version", "0.0.1")
            .bindingMode(RestBindingMode.auto)
            .contextPath("/")
            .host("0.0.0.0")
            .port(8080);
            

        from("rest:post:/{provider}")
            .process(exchange -> {
                Map<String, Object> map = new HashMap<String, Object>();
                ObjectMapper objectMapper = new ObjectMapper();

                String payload = exchange.getIn().getBody(String.class);
                // We start encapsulating the payload in a new message where we have
                // {provider: ..., timestamp: ..., rawdata: ...}
                // timestamp indicates when we received the message
                // provider is the provided which sent the message
                // rawdata is the data sent

                // provider is populated using the uri path of the request (request on /flightdata -> flightdat)
                // we might use a proper function to transform the request path into provider

                map.put("provider", exchange.getIn().getHeader("provider").toString());
                map.put("rawdata",payload);
                map.put("timestamp", ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT));
                
                exchange.getMessage().setBody(objectMapper.writeValueAsString(map));
                exchange.getMessage().setHeader("provider", exchange.getIn().getHeader("provider").toString());

                if (isValidJSON(payload)) {
                    exchange.getMessage().setHeader("validPayload", true);
                } else {
                    exchange.getMessage().setHeader("validPayload", false);
                }
            })
            .log("REST| ${body}")
            .log("REST| ${headers}")
            .choice()
                // if the payload is not a valid json
                .when(header("validPayload").isEqualTo(false))
                // we handle the request as invalid and forward the encapsulated payload to 
                // whatever mechanism we want to use to store malformed data
                .log("ERROR NOT A VALID PAYLOAD, ROUTE TO FAILED STORAGE")
            .otherwise()
                // otherwise we forward the encapsulated message to the 
                // internal queue waiting to be written in rawDataTable
                .to(getInternalStorageQueueConnectionString())
            .end()

            // reset and send responses
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setBody(constant(null))
            /*.endRest()*/;
    }

    public boolean isValidJSON(final String json) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode jsonNode = objectMapper.readTree(json);
            return jsonNode instanceof ContainerNode;
        } catch (Exception jpe) {
            return false;
        }
    }

    // When using Mosquitto
    //      To ensure no message will be lost by the Writer, we have to publish all message with QoS >= 1
    private String getInternalStorageQueueConnectionString() {
        // TODO use AmazonSNS uri if needed
        // for testing purposes we use Mosquitto
        final StringBuilder uri = new StringBuilder(String.format("paho:%s?brokerUrl=%s&qos=2", 
        restConfig.storage_topic, restConfig.storage_url));

        // Check if MQTT credentials are provided. If so, then add the credentials to the connection string
        restConfig.storage_user.ifPresent(user -> uri.append(String.format("&userName=%s", user)));
        restConfig.storage_password.ifPresent(pass -> uri.append(String.format("&password=%s", pass)));

        return uri.toString();
    }
}

final class RestConfigLogger {

    private static Logger LOG = LoggerFactory.getLogger(RestConfigLogger.class);

    private RestConfigLogger() {
        // Private constructor, don't allow new instances
    }
    
    public static void log(RestConfig config) {
        LOG.info("INTERNAL MQTT URL: {}", config.storage_url);
        LOG.info("INTERNAL MQTT user: {}", config.storage_topic);
    }
}