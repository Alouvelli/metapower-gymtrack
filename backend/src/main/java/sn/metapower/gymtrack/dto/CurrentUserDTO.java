package sn.metapower.gymtrack.dto;

import java.util.List;

public record CurrentUserDTO(
        Long id,
        String username,
        String email,
        String nom,
        String prenom,
        List<String> roles,
        List<GerantSiteDTO> siteAssignments
) {}
