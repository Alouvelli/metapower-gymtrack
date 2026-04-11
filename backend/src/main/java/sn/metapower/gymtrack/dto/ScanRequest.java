package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sn.metapower.gymtrack.entities.ScanSource;

public record ScanRequest(
        @NotBlank String qrToken,
        @NotNull Long siteId,
        ScanSource scanSource
) {}
