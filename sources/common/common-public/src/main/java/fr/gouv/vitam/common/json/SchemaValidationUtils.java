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
package fr.gouv.vitam.common.json;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.messages.JsonSchemaValidationBundle;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

public class SchemaValidationUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaValidationUtils.class);

    private JsonSchema jsonSchema;

    public static final String ARCHIVE_UNIT_SCHEMA_FILENAME = "json-schema/archive-unit-schema.json";
    public static final String ACCESS_CONTRACT_SCHEMA_FILENAME = "json-schema/access-contract-schema.json";
    public static final String ACCESSION_REGISTER_DETAIL_SCHEMA_FILENAME = "json-schema/accession-register-detail.schema.json";
    public static final String ACCESSION_REGISTER_SUMMARY_SCHEMA_FILENAME = "json-schema/accession-register-summary.schema.json";
    public static final String AGENCIES_SCHEMA_FILENAME = "json-schema/agencies.schema.json";
    public static final String CONTEXT_SCHEMA_FILENAME = "json-schema/context.schema.json";
    public static final String FILE_FORMAT_SCHEMA_FILENAME = "json-schema/file-format.schema.json";
    public static final String FILE_RULES_SCHEMA_FILENAME = "json-schema/file-rules.schema.json";
    public static final String INGEST_CONTRACT_SCHEMA_FILENAME = "json-schema/ingest-contract.schema.json";
    public static final String PROFILE_SCHEMA_FILENAME = "json-schema/profile.schema.json";
    public static final String SECURITY_PROFILE_SCHEMA_FILENAME = "json-schema/security-profile.schema.json";

    /**
     * Constructor with a default schema filename
     * 
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
     */
    public SchemaValidationUtils() throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        setSchema(ARCHIVE_UNIT_SCHEMA_FILENAME);
    }

    /**
     * Constructor with a specified schema filename
     * 
     * @param schemaFilename
     * @throws FileNotFoundException
     * @throws ProcessingException
     * @throws InvalidParseOperationException
     */
    protected SchemaValidationUtils(String schemaFilename)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        setSchema(schemaFilename);
    }

    private void setSchema(String schemaFilename)
        throws FileNotFoundException, ProcessingException, InvalidParseOperationException {
        // override for date format
        final Library library = DraftV4Library.get().thaw()
            .addFormatAttribute("date-time-vitam", VitamDateTimeAttribute.getInstance())
            .freeze();
        final MessageBundle bundle = MessageBundles.getBundle(JsonSchemaValidationBundle.class);
        final ValidationConfiguration cfg = ValidationConfiguration.newBuilder()
            .setDefaultLibrary("http://vitam-json-schema.org/draft-04/schema#", library)
            .setValidationMessages(bundle).freeze();
        final JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(cfg).freeze();

        // build archive schema validator

        JsonNode schemaJson =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(schemaFilename));
        jsonSchema = factory.getJsonSchema(schemaJson);
    }

    /**
     * Validate the json against the schema of the specified collectionName
     * @param jsonNode
     * @param collectionName
     * @return a status ({@link SchemaValidationStatus})
     * @throws FileNotFoundException if no schema has been found fot the specified collectionname
     * @throws InvalidParseOperationException
     * @throws ProcessingException
     */
    public SchemaValidationStatus validateJson(JsonNode jsonNode, String collectionName) throws FileNotFoundException, InvalidParseOperationException, ProcessingException {

        if ("AccessContract".equals(collectionName)){
            setSchema(ACCESS_CONTRACT_SCHEMA_FILENAME);
        }
        else if ("AccessionRegisterDetail".equals(collectionName)){
            // TODO : use ACCESSION_REGISTER_DETAIL_SCHEMA_FILENAME and fix validation 
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        }
        else if ("AccessionRegisterSummary".equals(collectionName)){
            // TODO : should we validate by json schema (ACCESSION_REGISTER_SUMMARY_SCHEMA_FILENAME) 
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        }
        else if ("Agencies".equals(collectionName)){
            setSchema(AGENCIES_SCHEMA_FILENAME);
        }
        else if ("Context".equals(collectionName)){
            setSchema(CONTEXT_SCHEMA_FILENAME);
        }
        else if ("FileFormat".equals(collectionName)){
            // TODO : should we validate by json schema (FILE_FORMAT_SCHEMA_FILENAME)
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        }
        else if ("FileRules".equals(collectionName)){
            setSchema(FILE_RULES_SCHEMA_FILENAME);
        }
        else if ("IngestContract".equals(collectionName)){
            setSchema(INGEST_CONTRACT_SCHEMA_FILENAME);
        }
        else if ("Profile".equals(collectionName)){
            setSchema(PROFILE_SCHEMA_FILENAME);
        }
        else if ("SecurityProfile".equals(collectionName)){
            setSchema(SECURITY_PROFILE_SCHEMA_FILENAME);
        }
        // used for test
        else if("CollectionSample".equals(collectionName)) {
            return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
        }
        else {
            throw  new FileNotFoundException("No json schema found for collection "+collectionName);
        }

        return validateJson(jsonNode);
    }

    /**
     * Validate a json with a schema
     *
     * @param jsonNode the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    protected SchemaValidationStatus validateJson(JsonNode jsonNode) {
        try {
            ProcessingReport report = jsonSchema.validate(jsonNode);

            if (!report.isSuccess()) {
                JsonNode error = (( ListProcessingReport ) report).asJson();
                ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.set( "validateJson",  error);
                LOGGER.error("Json is not valid : \n" + errorNode.toString());
                return new SchemaValidationStatus(errorNode.toString(),
                    SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
            }

        } catch (ProcessingException  e) {
            LOGGER.error("File is not a valid json file", e);
            return new SchemaValidationStatus("File is not a valid json file",
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
    }

    /**
     * Validate a json with the schema archive-unit-schema
     * 
     * @param archiveUnit the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    public SchemaValidationStatus validateUnit(JsonNode archiveUnit) {
        try {
            ProcessingReport report = jsonSchema.validate(archiveUnit);
            if (!report.isSuccess()) {
                JsonNode error = (( ListProcessingReport ) report).asJson();
                ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.set( "validateUnitReport",  error);
                LOGGER.error("Archive unit is not valid : \n" + errorNode.toString());
                String instancePointer = error.get(0).get("instance").get("pointer").asText();
                if (instancePointer.contains("StartDate") || instancePointer.contains("EndDate")) {
                    return new SchemaValidationStatus(errorNode.toString(),
                            SchemaValidationStatusEnum.RULE_DATE_FORMAT);
                }
                if (error.get(0).get("required") !=null && error.get(0).get("missing") != null) {
                    return new SchemaValidationStatus(errorNode.toString(),
                        SchemaValidationStatusEnum.EMPTY_REQUIRED_FIELD);
                }
                return new SchemaValidationStatus(errorNode.toString(),
                    SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
            }
            if (archiveUnit.get(SedaConstants.TAG_RULE_START_DATE) != null && 
                archiveUnit.get(SedaConstants.TAG_RULE_END_DATE) != null) {
                final Date startDate = LocalDateUtil.getDate(
                    archiveUnit.get(SedaConstants.TAG_RULE_START_DATE).asText());
                final Date endDate = LocalDateUtil.getDate(
                    archiveUnit.get(SedaConstants.TAG_RULE_END_DATE).asText());
                if (endDate.before(startDate)) {
                    final String errorMessage = "EndDate is before StartDate, unit Title : " + archiveUnit.get("Title").asText();
                    ObjectNode error = JsonHandler.createObjectNode();
                    error.put("Error", errorMessage);
                    ObjectNode errorNode = JsonHandler.createObjectNode();
                    errorNode.set( "validateUnitReport",  error);
                    LOGGER.error(errorMessage);
                    return new SchemaValidationStatus(errorNode.toString(),
                        SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
                }  
            }
        } catch (ProcessingException | ParseException e) {
            LOGGER.error("File is not a valid json file", e);
            return new SchemaValidationStatus("File is not a valid json file",
                SchemaValidationStatusEnum.NOT_JSON_FILE);
        }
        return new SchemaValidationStatus("Correct file", SchemaValidationStatusEnum.VALID);
    }

    /**
     * Validate a json for update with a schema
     * 
     * @param archiveUnit the json to be validated
     * @return a status ({@link SchemaValidationStatus})
     */
    public SchemaValidationStatus validateUpdateUnit(JsonNode archiveUnit) {
        ObjectNode unitCopy = ((ObjectNode) archiveUnit).deepCopy();
        final Iterator<String> names = unitCopy.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if ("_mgt".equals(name)) {
                final JsonNode value = unitCopy.remove(name);
                unitCopy.set("Management", value);
                break;
            }
        }
        return validateUnit(unitCopy);
    }

}
