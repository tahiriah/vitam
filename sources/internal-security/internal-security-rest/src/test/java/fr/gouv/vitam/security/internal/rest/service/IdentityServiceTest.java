/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.security.internal.rest.service;

import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Optional;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

public class IdentityServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private IdentityService identityService;
    @Mock
    private IdentityRepository identityRepository;

    @Test
    public void should_store_certificate() throws Exception {
        // Given
        IdentityInsertModel identityInsertModel = new IdentityInsertModel();
        identityInsertModel.setContextId("contextId");
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        identityInsertModel.setCertificate(certificate);

        // When
        identityService.createIdentity(identityInsertModel);

        // Then
        ArgumentCaptor<IdentityModel> identityModelCaptor = ArgumentCaptor.forClass(IdentityModel.class);
        then(identityRepository).should().createIdentity(identityModelCaptor.capture());
        IdentityModel identityModel = identityModelCaptor.getValue();

        assertThat(identityModel.getSubjectDN()).isEqualTo(
            "EMAILADDRESS=personal-basic@thawte.com, CN=Thawte Personal Basic CA, OU=Certification Services Division, O=Thawte Consulting, L=Cape Town, ST=Western Cape, C=ZA");
        assertThat(identityModel.getSerialNumber()).isEqualTo(BigInteger.ZERO);
        assertThat(identityModel.getIssuerDN()).isEqualTo(
            "EMAILADDRESS=personal-basic@thawte.com, CN=Thawte Personal Basic CA, OU=Certification Services Division, O=Thawte Consulting, L=Cape Town, ST=Western Cape, C=ZA");
        assertThat(identityModel.getCertificate()).isEqualTo(certificate);
    }

    @Test
    public void should_read_certificate() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");

        // When
        identityService.findIdentity(toByteArray(stream));

        // Then
        then(identityRepository).should().findIdentity(
            "EMAILADDRESS=personal-basic@thawte.com, CN=Thawte Personal Basic CA, OU=Certification Services Division, O=Thawte Consulting, L=Cape Town, ST=Western Cape, C=ZA",
            BigInteger.ZERO);
    }

    @Test
    public void should_update_certificate() throws Exception {
        // Given
        IdentityInsertModel identityInsertModel = new IdentityInsertModel();
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        identityInsertModel.setCertificate(toByteArray(stream));
        String contextId = "contextId";

        identityService.createIdentity(identityInsertModel);
        IdentityModel identityModel = new IdentityModel();
        given(identityRepository.findIdentity(
            "EMAILADDRESS=personal-basic@thawte.com, CN=Thawte Personal Basic CA, OU=Certification Services Division, O=Thawte Consulting, L=Cape Town, ST=Western Cape, C=ZA",
            BigInteger.ZERO)).willReturn(of(identityModel));

        // When
        identityInsertModel.setContextId(contextId);
        Optional<IdentityModel> result = identityService.linkContextToIdentity(identityInsertModel);

        // Then
        then(identityRepository).should()
            .linkContextToIdentity(identityModel.getSubjectDN(), identityModel.getContextId(),
                identityModel.getSerialNumber());
        assertThat(result).isPresent().hasValueSatisfying(identity ->
            assertThat(identity.getContextId()).isEqualTo(contextId)
        );
    }
}
