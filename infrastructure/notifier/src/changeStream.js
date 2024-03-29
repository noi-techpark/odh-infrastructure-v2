// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

const { Timestamp , ChangeStream} = require('mongodb');

/**
 * monitorListingsUsingEventEmitter opens a changestream watching for all updates
 * matching the rule {"$match": {"operationType": "insert"}}, which filters out only new inserted documents
 * https://www.mongodb.com/docs/drivers/node/current/usage-examples/changeStream/
 */
async function monitorListingsUsingEventEmitter(client, fnPublish, topic, pipeline = [
    {"$match": {"operationType": "insert"}},
]) {
        // Get the last checkpoint, if any, otherwise start from timestamp 0 to process opLogs
        let cursor = await getCursor(client);
        console.log(cursor);
        // Each hour the notifier will flish the current cluster timestamp in the collection
        // admin/notifier_checkpoint creating a checkpoint
        const checkpointInterrvall = 3600 * 1000; // 1 hour in milliseconds

        return new Promise((resolve) => {
        /**
         * difference between startAfter and startAtOperationTime
         * https://stackoverflow.com/questions/55184492/difference-between-resumeafter-and-startatoperationtime-in-mongodb-change-stream
        */
        // console.log(collection)
        const afterTime = cursor ? cursor.timestamp : new Timestamp({ t: 0, i: 0 });
        console.log("Reading events starting from " + new Date(afterTime.getHighBits() * 1000))
        console.log("Now is " + new Date())
        // watching the whole deployment https://www.mongodb.com/docs/manual/changeStreams/
        // it could be possible to spin more instances of the notifier with different configurations to improve the performances
        // EG: one notifier instance per database, one notifier per collection, ...
        //
        // in this case the logic to write checkpoints (flushCheckpoint, getCursor) 
        // should be extended to reflect the information about which collection/database the checkpoint refers to
        const changeStream = client.watch(pipeline, { startAtOperationTime: afterTime})

        let updating = false;
        changeStream.on(ChangeStream.CHANGE, async (next) => {
            // console.log(changeStream.resumeToken);
            const operationTime = new Date(next.clusterTime.getHighBits() * 1000);

            if (!updating && (!cursor || operationTime.getTime() - cursor.date.getTime() > checkpointInterrvall))
            {
                try {
                    updating = true;
                    cursor = await flushCheckpoint(client, cursor, next.clusterTime);
                } finally {
                    updating = false;
                }
            }

            // publish {id, db and collection} to the queue the transformers will watch to know about new data
            // mqttclient.publish(topic, JSON.stringify({
            //     id: next.documentKey._id.toString(),
            //     db: next.ns.db,
            //     collection: next.ns.coll
            // }))

            fnPublish(JSON.stringify({
                     id: next.documentKey._id.toString(),
                     db: next.ns.db,
                     collection: next.ns.coll
                }));
        });
    })
};

async function flushCheckpoint(client, oldCursor, timestamp) {
    console.log("Updating checkpoint cursor:");
    console.log("old: ", oldCursor);

    const filter = oldCursor ? { _id: oldCursor._id } : {};
    const options = { upsert: true };
    const updateDoc = {
        $set: {
            timestamp: timestamp,
        },
    };
    const result = await client.db("admin").collection("notifier_checkpoint").updateOne(filter, updateDoc, options);
    console.log("result: ", result);

    const date = new Date(timestamp.getHighBits() * 1000)
    const newCursor = oldCursor ? { _id: oldCursor._id, timestamp: timestamp, date} : { _id: result.upsertedId, timestamp: timestamp, date };
    console.log("new: ", newCursor);

    return newCursor;
}

async function getCursor(client) {
    const checkpoint = await client.db("admin").collection("notifier_checkpoint").findOne({ }, {});
    return checkpoint ? { ...checkpoint, date: new Date(checkpoint.timestamp.getHighBits() * 1000)} : null;
}

module.exports = { monitorListingsUsingEventEmitter };