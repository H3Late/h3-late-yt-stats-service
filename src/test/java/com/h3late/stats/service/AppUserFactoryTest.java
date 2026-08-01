package com.h3late.stats.service;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.repository.AppUserRepository;
import com.h3late.stats.repository.LinkedIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    public void createWithIdentity_retriesOnDiscriminatorCollision() {
        AppUser saved = AppUser.builder().id(1L).username("Danny").discriminator("482913").build();
        when(appUserRepo.save(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate discriminator"))
                .thenReturn(saved);

        AppUser result = appUserFactory.createWithIdentity(
                AuthProvider.GOOGLE, "google-sub-1", "Danny", "danny@example.com", null);

        assertEquals(saved, result);
        verify(appUserRepo, times(2)).save(any(AppUser.class));
        verify(linkedIdentityRepo, times(1)).save(any());
    }

    @Test
    public void createWithIdentity_exhaustsRetries_throwsIllegalState() {
        when(appUserRepo.save(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate discriminator"));

        assertThrows(IllegalStateException.class, () -> appUserFactory.createWithIdentity(
                AuthProvider.GOOGLE, "google-sub-1", "Danny", "danny@example.com", null));

        verify(appUserRepo, times(10)).save(any(AppUser.class));
        verify(linkedIdentityRepo, never()).save(any());
    }
}
