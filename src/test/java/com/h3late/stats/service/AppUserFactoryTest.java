package com.h3late.stats.service;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.entity.LinkedIdentity;
import com.h3late.stats.repository.AppUserRepository;
import com.h3late.stats.repository.LinkedIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AppUserFactoryTest {

    private AppUserRepository appUserRepo;
    private LinkedIdentityRepository linkedIdentityRepo;
    private AppUserFactory appUserFactory;

    @BeforeEach
    public void setUp() {
        appUserRepo = Mockito.mock(AppUserRepository.class);
        linkedIdentityRepo = Mockito.mock(LinkedIdentityRepository.class);
        appUserFactory = new AppUserFactory(appUserRepo, linkedIdentityRepo);
    }

    @Test
    public void createWithIdentity_savesUserAndLinkedIdentity() {
        AppUser saved = AppUser.builder().id(1L).username("Danny").build();
        when(appUserRepo.save(any(AppUser.class))).thenReturn(saved);

        AppUser result = appUserFactory.createWithIdentity(
                AuthProvider.GOOGLE, "google-sub-1", "Danny", "danny@example.com", null);

        assertEquals(saved, result);

        ArgumentCaptor<LinkedIdentity> captor = ArgumentCaptor.forClass(LinkedIdentity.class);
        verify(linkedIdentityRepo).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(AuthProvider.GOOGLE, captor.getValue().getProvider());
        assertEquals("google-sub-1", captor.getValue().getProviderUserId());
    }

    @Test
    public void createWithIdentity_concurrentCollision_propagatesException() {
        when(appUserRepo.save(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate provider identity"));

        assertThrows(DataIntegrityViolationException.class, () -> appUserFactory.createWithIdentity(
                AuthProvider.GOOGLE, "google-sub-1", "Danny", "danny@example.com", null));

        verify(linkedIdentityRepo, never()).save(any());
    }
}
