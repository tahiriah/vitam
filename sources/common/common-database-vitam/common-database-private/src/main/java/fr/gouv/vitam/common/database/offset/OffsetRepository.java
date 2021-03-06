/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.database.offset;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import com.google.common.collect.Iterables;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Offset collection repository (use for logbook and metadata database)
 *
 */
public class OffsetRepository {

    public static final String COLLECTION_NAME = "Offset";

    private final MongoCollection<Document> offerCollection;

    /**
     * Constructor
	 * @param mongoDbAccess mongoDbAccess
     */
    public OffsetRepository(MongoDbAccess mongoDbAccess) {
        offerCollection = mongoDbAccess.getMongoDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Create or update offset
     *
     * @param tenant the tenant
     * @param collection the collection name
     */
    public void createOrUpdateOffset(int tenant, String collection, long offset) {

        Bson offsetFilter = and(
            eq("_tenant", tenant),
            eq("collection", collection)
        );

        Bson offsetUpdate = Updates.set("offset", offset);

        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(true);

        offerCollection.updateOne(offsetFilter, offsetUpdate, updateOptions);
    }

    /**
     * Get current offset
     *
     * @param tenant the tenant
     * @param collection the collection name
     * @return the offset value for collection/tenant, 0L if not found
     */
    public long findOffsetBy(int tenant, String collection) {
        Bson offsetFilter = and(
            eq("_tenant", tenant),
            eq("collection", collection)
        );
        FindIterable<Document> documents = offerCollection.find(offsetFilter);

        Document first = Iterables.getFirst(documents, null);
        if (first == null) {
            return 0L;
        }

        return first.get("offset", Long.class);
    }

}
