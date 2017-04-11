/**
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
 */
package fr.gouv.vitam.functional.administration.contract.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.AccessContractModel;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.*;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;


public class AccessContractImplTest {


    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "AccessContract";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;

    static ContractService<AccessContractModel> accessContractService;
    static int mongoPort;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        client = new MongoClient(new ServerAddress(DATABASE_HOST, mongoPort));

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));
        accessContractService = new AccessContractImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)));

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        accessContractService.close();
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
    }


    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        assertThat(response.isOk());
        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestMissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_missingName.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        assertThat(!response.isOk());

    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestMissingOriginatingAgenciesReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_missingOriginatingAgencies.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        assertThat(!response.isOk());

    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_duplicate.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        assertThat(!response.isOk());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);

        // Try to recreate the same contract but with id
        response = accessContractService.createContracts(responseCast.getResults());

        assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestAlreadyExistsContract() throws Exception {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

            List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
            RequestResponse response =  accessContractService.createContracts(accessContractModelList);

            RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
            assertThat(responseCast.getResults()).hasSize(2);


            //unset ids
            accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
            response = accessContractService.createContracts(accessContractModelList);

            assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        //find accessContract with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();
       /*
        String q = "{ \"$query\" : [ { \"$eq\" : { \"_id\" : \"fake_id\" } } ] }";

        JsonNode queryDsl = JsonHandler.getFromString(q);
        */
        List<AccessContractModel> accessContractModelList = accessContractService.findContracts(queryDsl);

        assertThat(accessContractModelList).isEmpty();
    }


    /**
     * Check that the created access conrtact have the tenant owner after persisted to database
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response = accessContractService.createContracts(accessContractModelList);

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);

        //We juste test the first contract
        AccessContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getId();
        assertThat(id1).isNotNull();


        AccessContractModel one = accessContractService.findOne(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(Long.valueOf(TENANT_ID));

    }


    /**
     * Access contract of tenant 1, try to get the same contract with id mongo but with tenant 2
     * This sgould not return the contract as tenant 2 is not the owner of the access contract
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response = accessContractService.createContracts(accessContractModelList);

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);

        //We juste test the first contract
        AccessContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getId();
        assertThat(id1).isNotNull();


        VitamThreadUtils.getVitamSession().setTenantId(2);

        final AccessContractModel one = accessContractService.findOne(id1);

        assertThat(one).isNull();

    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestfindByID() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response = accessContractService.createContracts(accessContractModelList);

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);

        //We juste test the first contract
        AccessContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getId();
        assertThat(id1).isNotNull();


        AccessContractModel one = accessContractService.findOne(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        List<AccessContractModel> accessContractModelList = accessContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(accessContractModelList).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindAllThenReturnTwoContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);

        List<AccessContractModel> accessContractModelListSearch = accessContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(accessContractModelListSearch).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>(){});
        RequestResponse response =  accessContractService.createContracts(accessContractModelList);

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>)response;
        assertThat(responseCast.getResults()).hasSize(2);


        AccessContractModel acm = accessContractModelList.iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getId();
        assertThat(id1).isNotNull();

        String name = acm.getName();
        assertThat(name).isNotNull();


        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", name));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();


        List<AccessContractModel> accessContractModelListFound = accessContractService.findContracts(queryDsl);
        assertThat(accessContractModelListFound).hasSize(1);

        AccessContractModel acmFound = accessContractModelListFound.iterator().next();
        assertThat(acmFound).isNotNull();


        assertThat(acmFound.getId()).isEqualTo(id1);
        assertThat(acmFound.getName()).isEqualTo(name);

    }

}