package edu.ucsb.cs156.happiercows.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import edu.ucsb.cs156.happiercows.JobTestCase;
import edu.ucsb.cs156.happiercows.repositories.GameRepository;
import edu.ucsb.cs156.happiercows.repositories.FarmerRepository;
import edu.ucsb.cs156.happiercows.repositories.HerdRepository;
import edu.ucsb.cs156.happiercows.repositories.UserRepository;
import edu.ucsb.cs156.happiercows.services.GamePlusBuilderService;

@RestClientTest(UpdateCowHealthJobFactoryInd.class)

public class UpdateCowHealthJobFactoryIndTests extends JobTestCase {

    @MockBean
    GameRepository gameRepository;

    @MockBean
    FarmerRepository farmerRepository;

    @MockBean
    UserRepository userRepository;

    @MockBean
    HerdRepository herdRepository;

    @MockBean
    GamePlusBuilderService gamePlusBuilderService;

    @Autowired
    UpdateCowHealthJobFactoryInd updateCowHealthJobFactoryInd;

    @Test
    void test_create() throws Exception {

        // Act
        UpdateCowHealthJobInd updateCowHealthJobInd = (UpdateCowHealthJobInd) updateCowHealthJobFactoryInd.create(1L);

        // Assert
        assertEquals(gameRepository,updateCowHealthJobInd.getGameRepository());
        assertEquals(farmerRepository,updateCowHealthJobInd.getFarmerRepository());
        assertEquals(userRepository,updateCowHealthJobInd.getUserRepository());
        assertEquals(herdRepository,updateCowHealthJobInd.getHerdRepository());
        // cow deaths use real random numbers in [0, 1)
        double random = updateCowHealthJobInd.getRandom().getAsDouble();
        assertTrue(random >= 0.0 && random < 1.0);
        assertEquals(gamePlusBuilderService,updateCowHealthJobInd.getGamePlusBuilderService());

    }
}

