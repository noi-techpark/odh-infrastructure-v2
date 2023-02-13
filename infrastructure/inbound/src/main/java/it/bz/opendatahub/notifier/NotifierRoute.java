// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-bean
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-paho
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-jackson
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-mongodb
// camel-k: dependency=mvn:io.quarkus:quarkus-mongodb-client
// camel-k: dependency=mvn:org.apache.camel:camel-jackson:3.6.0


package it.bz.opendatahub.notifier;

import org.apache.camel.builder.RouteBuilder;

import javax.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.time.*;

import org.apache.camel.component.jackson.JacksonDataFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.microprofile.config.ConfigProvider;

import it.bz.opendatahub.RabbitMQConnection;


class MongoDBConnection {
    String host;
}

// TODO implement notifier with camel
// read checkpoints
// write checkpoints
// create new message
/**
 * Route to 
 */
// @ApplicationScoped
// public class NotifierRoute extends RouteBuilder {

//     private final RabbitMQConnection rabbitMQConfig;
//     private final MongoDBConnection mongoDBConnection;

//     public NotifierRoute()
//     {
//         this.rabbitMQConfig = new RabbitMQConnection();
//         this.mongoDBConnection = new MongoDBConnection();

//         this.mongoDBConnection.host = ConfigProvider.getConfig().getValue("mongodb.host", String.class);
//     } 

//     @Override
//     public void configure() {
//         from(this.getDatabaseEventStreamString())
//             .end()
//             .log("NOTIFIER| ${body}")
//             .log("NOTIFIER| ${headers}");
//             // .process(exchange -> {
//             //     // First we unmarshal the payload
//             //     Map<String, Object> body = (HashMap<String, Object>)exchange.getMessage().getBody(Map.class);
//             //     Object timestamp = body.get("timestamp");
//             //     // we convert the timestamp field into a valid BSON TimeStamp
//             //     if (timestamp != null)
//             //     {
//             //         Instant instant = Instant.parse((String)timestamp);
//             //         Date dateTimestamp = Date.from(instant);
//             //         body.put("bsontimestamp", dateTimestamp);
//             //     }
//             //     exchange.getMessage().setBody(body);
//             //     // we then compute the database connection using the message body (in this case we only care bout the field `provider`)
//             //     // and store the connection string in the `database` header to be used later
//             //     exchange.getMessage().setHeader("database", getDatabaseString((String)body.get("provider")));
//             // })
//             // // we don't use `.to()` because the connection string is dynamic and we use the previously set header `database`
//             // // to send the data to the database
//             // .recipientList(header("database"));
//     }

//     /**
//      * For the purpose of the PoC, we use a single MongoDB deployment as rawDataTable and we store data in {provider} db / {provider} collection
//      * provider = flightdata -> data stored in flightdata/flightadata.
//      * 
//      * If we need to use multiple deployments or custom paths, you should edit this function.
//      * References:
//      * https://camel.apache.org/camel-quarkus/2.10.x/reference/extensions/mongodb.html
//      * https://quarkus.io/guides/mongodb
//      */
//     private String getDatabaseEventStreamString() {
//         final StringBuilder uri = new StringBuilder(String.format("mongodb://dummy?hosts=%s"+
//             "&database=test"+
//             "&collection=test"+
//             "&consumerType=changeStreams"+
//             "&streamFilter={'$match': {'operationType': 'insert'}", 
//         this.mongoDBConnection.host));
//         System.out.println(uri.toString());
//         return uri.toString();
//     }
// }

// final class WriterConfigLogger {

//     private static Logger LOG = LoggerFactory.getLogger(WriterConfigLogger.class);

//     private WriterConfigLogger() {
//         // Private constructor, don't allow new instances
//     }
// }