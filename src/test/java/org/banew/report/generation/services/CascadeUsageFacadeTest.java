package org.banew.report.generation.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CascadeUsageFacadeTest {

    @InjectMocks
    private CascadeUsageFacade cascadeUsageFacade;

    @Test
    void process_comFilePresentAndIsNotBuild_successFolderCreation() {
        //cascadeUsageFacade.process();
    }

    @Test
    void givePrompt() {
    }
}